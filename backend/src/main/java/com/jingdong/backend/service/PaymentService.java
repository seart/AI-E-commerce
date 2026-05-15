package com.jingdong.backend.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jingdong.backend.api.ErrorCode;
import com.jingdong.backend.dto.payment.PaymentDtos.PaymentPrepayRequest;
import com.jingdong.backend.dto.payment.PaymentDtos.PaymentPrepayResponse;
import com.jingdong.backend.dto.payment.PaymentDtos.PaymentStatusResponse;
import com.jingdong.backend.entity.DataEntities.OrderEntity;
import com.jingdong.backend.entity.DataEntities.PaymentEntity;
import com.jingdong.backend.exception.BusinessException;
import com.jingdong.backend.mapper.OrderMapper;
import com.jingdong.backend.mapper.PaymentMapper;
import com.jingdong.backend.payment.PaymentGateway;
import com.jingdong.backend.payment.PaymentGatewayPrepayRequest;
import com.jingdong.backend.payment.PaymentGatewayPrepayResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
  private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

  private final ObjectMapper objectMapper;
  private final OrderMapper orderMapper;
  private final PaymentMapper paymentMapper;
  private final AuditLogService auditLogService;
  private final InventoryService inventoryService;
  private final Map<String, PaymentGateway> gateways;

  public PaymentService(
      ObjectMapper objectMapper,
      OrderMapper orderMapper,
      PaymentMapper paymentMapper,
      AuditLogService auditLogService,
      InventoryService inventoryService,
      List<PaymentGateway> gateways
  ) {
    this.objectMapper = objectMapper;
    this.orderMapper = orderMapper;
    this.paymentMapper = paymentMapper;
    this.auditLogService = auditLogService;
    this.inventoryService = inventoryService;
    this.gateways = gateways.stream()
        .collect(Collectors.toUnmodifiableMap(PaymentGateway::channel, Function.identity(), (left, right) -> left));
  }

  @Transactional
  public PaymentPrepayResponse prepay(String userId, PaymentPrepayRequest request) {
    // 预支付入口：校验订单归属、状态和支付有效期，然后生成扫码支付单。
    OrderEntity order = ownedOrder(userId, request.orderId());
    LocalDateTime now = LocalDateTime.now();
    if ("PAYMENT_CLOSED".equals(order.getStatus())) {
      throw new BusinessException(ErrorCode.PAYMENT_EXPIRED);
    }
    if (!"PENDING_PAYMENT".equals(order.getStatus())) {
      throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
    }
    if (order.getPaymentExpireAt() != null && !order.getPaymentExpireAt().isAfter(now)) {
      closeExpiredOrder(order.getId());
      throw new BusinessException(ErrorCode.PAYMENT_EXPIRED);
    }

    String channel = normalizeChannel(request.channel());
    PaymentEntity existing = reusablePayment(order.getId(), channel, now);
    if (existing != null) {
      // 同一订单同一渠道已有未过期二维码时直接复用，避免频繁创建网关订单。
      return toPrepayResponse(existing);
    }

    PaymentGateway gateway = gateway(channel);
    String outTradeNo = nextOutTradeNo();
    LocalDateTime expireAt = order.getPaymentExpireAt();
    PaymentGatewayPrepayResponse gatewayResponse = gateway.prepay(new PaymentGatewayPrepayRequest(
        channel,
        order.getId(),
        order.getOrderNo(),
        outTradeNo,
        order.getTotalAmount(),
        "到家订单 " + order.getOrderNo(),
        expireAt
    ));

    PaymentEntity payment = new PaymentEntity();
    payment.setId(uid("pay"));
    payment.setOrderId(order.getId());
    payment.setUserId(userId);
    payment.setChannel(channel);
    payment.setStatus("PAYING");
    payment.setAmount(order.getTotalAmount());
    payment.setOutTradeNo(outTradeNo);
    payment.setQrCode(gatewayResponse.qrContent());
    payment.setGatewayOrderNo(gatewayResponse.gatewayOrderNo());
    payment.setRequestId(MDC.get("requestId"));
    payment.setExpireAt(gatewayResponse.expireAt() == null ? expireAt : gatewayResponse.expireAt());
    payment.setCreatedAt(now);
    payment.setUpdatedAt(now);
    paymentMapper.insert(payment);

    orderMapper.update(null, Wrappers.<OrderEntity>lambdaUpdate()
        .eq(OrderEntity::getId, order.getId())
        .eq(OrderEntity::getStatus, "PENDING_PAYMENT")
        .set(OrderEntity::getPaymentStatus, "PAYING")
        .set(OrderEntity::getPaymentChannel, channel));
    // 到这里本地支付单已创建，订单仍然是待支付状态，等待网关异步通知确认。
    auditLogService.record("PAYMENT_PREPAY", "ORDER", order.getId(), channel + " 创建扫码支付");
    return toPrepayResponse(payment);
  }

  public PaymentStatusResponse status(String userId, String paymentId) {
    PaymentEntity payment = paymentMapper.selectById(paymentId);
    if (payment == null) {
      throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
    }
    if (!userId.equals(payment.getUserId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    OrderEntity order = orderMapper.selectById(payment.getOrderId());
    return toStatusResponse(payment, order);
  }

  @Transactional
  public void confirmGatewayPaid(String outTradeNo, String transactionId, String notifyPayload) {
    // 支付网关回调根据 outTradeNo 找本地支付单；该方法需要幂等，避免重复通知重复扣库存。
    PaymentEntity payment = paymentByOutTradeNo(outTradeNo);
    if (payment == null) {
      throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
    }
    if ("PAID".equals(payment.getStatus())) {
      // 已处理过的支付通知直接返回，保证回调幂等。
      return;
    }

    OrderEntity order = orderMapper.selectById(payment.getOrderId());
    if (order == null) {
      throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
    }
    if ("PAYMENT_CLOSED".equals(order.getStatus())) {
      // 订单已超时关闭时不再改订单状态，只记录一次迟到通知方便排查。
      payment.setNotifyPayload(notifyPayload);
      paymentMapper.updateById(payment);
      auditLogService.record("PAYMENT_LATE_NOTIFY", "ORDER", order.getId(), payment.getChannel());
      return;
    }

    LocalDateTime now = LocalDateTime.now();
    int updated = orderMapper.update(null, Wrappers.<OrderEntity>lambdaUpdate()
        .eq(OrderEntity::getId, order.getId())
        .eq(OrderEntity::getStatus, "PENDING_PAYMENT")
        .set(OrderEntity::getStatus, "PAID")
        .set(OrderEntity::getStatusText, "支付成功")
        .set(OrderEntity::getPaymentStatus, "PAID")
        .set(OrderEntity::getPaymentChannel, payment.getChannel())
        .set(OrderEntity::getPaidAt, now)
        .set(OrderEntity::getStatusHistoryJson,
            appendStatusHistory(order.getStatusHistoryJson(), "PAID", "支付网关异步通知成功")));
    if (updated > 0 || "PAID".equals(order.getStatus())) {
      // 订单支付成功后，把锁定库存转为已售库存。
      inventoryService.confirmOrderPaid(order.getId(), "支付网关异步通知成功");
      payment.setStatus("PAID");
      payment.setTransactionId(transactionId);
      payment.setNotifyPayload(notifyPayload);
      payment.setPaidAt(now);
      payment.setUpdatedAt(now);
      paymentMapper.updateById(payment);
      auditLogService.record("PAYMENT_PAID", "ORDER", order.getId(), payment.getChannel());
    }
  }

  @Transactional
  public void closeExpiredOrder(String orderId) {
    // 超时关单只处理待支付订单；已支付、已取消或已关闭订单不会再次回滚库存。
    OrderEntity order = orderMapper.selectById(orderId);
    if (order == null || !"PENDING_PAYMENT".equals(order.getStatus())) {
      return;
    }
    LocalDateTime now = LocalDateTime.now();
    if (order.getPaymentExpireAt() != null && order.getPaymentExpireAt().isAfter(now)) {
      return;
    }

    closeGatewayPayments(orderId);
    int updated = orderMapper.update(null, Wrappers.<OrderEntity>lambdaUpdate()
        .eq(OrderEntity::getId, orderId)
        .eq(OrderEntity::getStatus, "PENDING_PAYMENT")
        .set(OrderEntity::getStatus, "PAYMENT_CLOSED")
        .set(OrderEntity::getStatusText, "支付关闭")
        .set(OrderEntity::getPaymentStatus, "CLOSED")
        .set(OrderEntity::getClosedAt, now)
        .set(OrderEntity::getStatusHistoryJson,
            appendStatusHistory(order.getStatusHistoryJson(), "PAYMENT_CLOSED", "支付超时自动关闭")));
    if (updated > 0) {
      // 关单成功后释放下单时锁定的库存，库存流水保证同一订单只释放一次。
      inventoryService.releaseOrderStock(orderId, "支付超时自动关闭");
      paymentMapper.update(null, Wrappers.<PaymentEntity>lambdaUpdate()
          .eq(PaymentEntity::getOrderId, orderId)
          .in(PaymentEntity::getStatus, List.of("CREATED", "PAYING"))
          .set(PaymentEntity::getStatus, "EXPIRED")
          .set(PaymentEntity::getClosedAt, now));
      auditLogService.record("PAYMENT_TIMEOUT_CLOSE", "ORDER", orderId, "支付超时自动关单");
    }
  }

  private OrderEntity ownedOrder(String userId, String orderId) {
    // 用户只能操作自己的支付单，避免通过 payment/order id 越权查询或支付他人订单。
    OrderEntity order = orderMapper.selectById(orderId);
    if (order == null) {
      throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
    }
    if (!userId.equals(order.getUserId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    return order;
  }

  private PaymentEntity reusablePayment(String orderId, String channel, LocalDateTime now) {
    // 查找当前订单同渠道仍在支付中的二维码，提升扫码页刷新体验。
    PaymentEntity payment = paymentMapper.selectOne(Wrappers.<PaymentEntity>lambdaQuery()
        .eq(PaymentEntity::getOrderId, orderId)
        .eq(PaymentEntity::getChannel, channel)
        .eq(PaymentEntity::getStatus, "PAYING")
        .gt(PaymentEntity::getExpireAt, now)
        .orderByDesc(PaymentEntity::getCreatedAt)
        .last("limit 1"));
    return payment == null || payment.getQrCode() == null ? null : payment;
  }

  private PaymentGateway gateway(String channel) {
    PaymentGateway gateway = gateways.get(channel);
    if (gateway == null) {
      throw new BusinessException(ErrorCode.PAYMENT_CHANNEL_NOT_CONFIGURED);
    }
    return gateway;
  }

  private PaymentEntity paymentByOutTradeNo(String outTradeNo) {
    return paymentMapper.selectOne(Wrappers.<PaymentEntity>lambdaQuery()
        .eq(PaymentEntity::getOutTradeNo, outTradeNo)
        .last("limit 1"));
  }

  private void closeGatewayPayments(String orderId) {
    List<PaymentEntity> payments = paymentMapper.selectList(Wrappers.<PaymentEntity>lambdaQuery()
        .eq(PaymentEntity::getOrderId, orderId)
        .in(PaymentEntity::getStatus, List.of("CREATED", "PAYING")));
    for (PaymentEntity payment : payments) {
      PaymentGateway gateway = gateways.get(payment.getChannel());
      if (gateway == null) {
        continue;
      }
      try {
        gateway.close(payment.getOutTradeNo());
      } catch (Exception exception) {
        // 网关关单失败不能阻塞本地超时关单，否则库存会长期被待支付订单锁住。
        log.warn("Close payment gateway order failed, outTradeNo={}, requestId={}",
            payment.getOutTradeNo(),
            MDC.get("requestId"),
            exception);
      }
    }
  }

  private PaymentPrepayResponse toPrepayResponse(PaymentEntity payment) {
    return new PaymentPrepayResponse(
        payment.getId(),
        payment.getOrderId(),
        payment.getChannel(),
        payment.getStatus(),
        payment.getAmount(),
        payment.getOutTradeNo(),
        payment.getTransactionId(),
        payment.getQrCode(),
        toInstantString(payment.getExpireAt())
    );
  }

  private PaymentStatusResponse toStatusResponse(PaymentEntity payment, OrderEntity order) {
    return new PaymentStatusResponse(
        payment.getId(),
        payment.getOrderId(),
        payment.getChannel(),
        payment.getStatus(),
        order == null ? null : order.getStatus(),
        payment.getTransactionId(),
        toInstantString(payment.getExpireAt()),
        toInstantString(payment.getPaidAt()),
        toInstantString(payment.getClosedAt())
    );
  }

  private String appendStatusHistory(String existing, String status, String reason) {
    try {
      // 订单状态历史保存在 JSON 字段里，追加时先读旧数组再写回。
      List<Map<String, String>> history = existing == null || existing.isBlank()
          ? new ArrayList<>()
          : new ArrayList<>(objectMapper.readValue(existing, new TypeReference<List<Map<String, String>>>() {}));
      Map<String, String> item = new LinkedHashMap<>();
      item.put("status", status);
      item.put("text", statusText(status));
      item.put("reason", reason == null ? "" : reason);
      item.put("changedAt", LocalDateTime.now().toString());
      history.add(item);
      return objectMapper.writeValueAsString(history);
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to append order status history", exception);
    }
  }

  private String statusText(String status) {
    return switch (status) {
      case "PAID" -> "支付成功";
      case "PAYMENT_CLOSED" -> "支付关闭";
      default -> status;
    };
  }

  private String normalizeChannel(String channel) {
    return channel == null ? "" : channel.trim().toUpperCase(Locale.ROOT);
  }

  private String toInstantString(LocalDateTime dateTime) {
    return dateTime == null ? null : dateTime.atZone(ZoneId.systemDefault()).toInstant().toString();
  }

  private String nextOutTradeNo() {
    return "PAY" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
  }

  private String uid(String prefix) {
    return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
  }
}
