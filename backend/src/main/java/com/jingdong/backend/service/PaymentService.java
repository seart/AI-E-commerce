package com.jingdong.backend.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jingdong.backend.api.ErrorCode;
import com.jingdong.backend.dto.payment.PaymentDtos.PaymentPrepayRequest;
import com.jingdong.backend.dto.payment.PaymentDtos.PaymentPrepayResponse;
import com.jingdong.backend.dto.payment.PaymentDtos.PaymentStatusResponse;
import com.jingdong.backend.entity.DataEntities.OrderEntity;
import com.jingdong.backend.entity.DataEntities.OrderItemEntity;
import com.jingdong.backend.entity.DataEntities.PaymentEntity;
import com.jingdong.backend.entity.DataEntities.ProductEntity;
import com.jingdong.backend.exception.BusinessException;
import com.jingdong.backend.mapper.OrderItemMapper;
import com.jingdong.backend.mapper.OrderMapper;
import com.jingdong.backend.mapper.PaymentMapper;
import com.jingdong.backend.mapper.ProductMapper;
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
  private final OrderItemMapper orderItemMapper;
  private final ProductMapper productMapper;
  private final PaymentMapper paymentMapper;
  private final AuditLogService auditLogService;
  private final Map<String, PaymentGateway> gateways;

  public PaymentService(
      ObjectMapper objectMapper,
      OrderMapper orderMapper,
      OrderItemMapper orderItemMapper,
      ProductMapper productMapper,
      PaymentMapper paymentMapper,
      AuditLogService auditLogService,
      List<PaymentGateway> gateways
  ) {
    this.objectMapper = objectMapper;
    this.orderMapper = orderMapper;
    this.orderItemMapper = orderItemMapper;
    this.productMapper = productMapper;
    this.paymentMapper = paymentMapper;
    this.auditLogService = auditLogService;
    this.gateways = gateways.stream()
        .collect(Collectors.toUnmodifiableMap(PaymentGateway::channel, Function.identity(), (left, right) -> left));
  }

  @Transactional
  public PaymentPrepayResponse prepay(String userId, PaymentPrepayRequest request) {
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
        "京东到家订单 " + order.getOrderNo(),
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
    PaymentEntity payment = paymentByOutTradeNo(outTradeNo);
    if (payment == null) {
      throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
    }
    if ("PAID".equals(payment.getStatus())) {
      return;
    }

    OrderEntity order = orderMapper.selectById(payment.getOrderId());
    if (order == null) {
      throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
    }
    if ("PAYMENT_CLOSED".equals(order.getStatus())) {
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
      rollbackStock(orderId);
      paymentMapper.update(null, Wrappers.<PaymentEntity>lambdaUpdate()
          .eq(PaymentEntity::getOrderId, orderId)
          .in(PaymentEntity::getStatus, List.of("CREATED", "PAYING"))
          .set(PaymentEntity::getStatus, "EXPIRED")
          .set(PaymentEntity::getClosedAt, now));
      auditLogService.record("PAYMENT_TIMEOUT_CLOSE", "ORDER", orderId, "支付超时自动关单");
    }
  }

  private OrderEntity ownedOrder(String userId, String orderId) {
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

  private void rollbackStock(String orderId) {
    for (OrderItemEntity item : orderItemMapper.selectList(Wrappers.<OrderItemEntity>lambdaQuery()
        .eq(OrderItemEntity::getOrderId, orderId))) {
      ProductEntity product = productMapper.selectById(item.getProductId());
      if (product != null) {
        product.setStock(product.getStock() + item.getQuantity());
        productMapper.updateById(product);
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
