package com.jingdong.backend.service;

import com.jingdong.backend.config.PaymentProperties;
import com.jingdong.backend.dto.order.OrderDtos.CreateOrderRequest;
import com.jingdong.backend.dto.order.OrderDtos.OrderResponse;
import com.jingdong.backend.payment.PaymentCloseMessagePublisher;
import com.jingdong.backend.store.DatabaseStore;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
  private final DatabaseStore store;
  private final RateLimiterService rateLimiterService;
  private final AuditLogService auditLogService;
  private final PaymentProperties paymentProperties;
  private final PaymentCloseMessagePublisher paymentCloseMessagePublisher;

  public OrderService(
      DatabaseStore store,
      RateLimiterService rateLimiterService,
      AuditLogService auditLogService,
      PaymentProperties paymentProperties,
      PaymentCloseMessagePublisher paymentCloseMessagePublisher
  ) {
    this.store = store;
    this.rateLimiterService = rateLimiterService;
    this.auditLogService = auditLogService;
    this.paymentProperties = paymentProperties;
    this.paymentCloseMessagePublisher = paymentCloseMessagePublisher;
  }

  public List<OrderResponse> getOrders(String userId) {
    return store.getOrders(userId);
  }

  public OrderResponse createOrder(String userId, CreateOrderRequest request) {
    // 下单接口限流，避免同一用户短时间内大量创建待支付订单占用库存。
    rateLimiterService.check("order:" + userId, 20, Duration.ofMinutes(1));
    LocalDateTime expireAt = LocalDateTime.now().plusMinutes(paymentProperties.getExpireMinutes());
    // createOrder 会在 DatabaseStore 内完成订单创建和库存锁定。
    OrderResponse order = store.createOrder(userId, request.addressId(), request.items(), expireAt);
    auditLogService.record("ORDER_CREATE", "ORDER", order.id(), "用户创建订单");
    // 订单创建后立即投递延迟关单消息；本地未配置 RocketMQ 时会安全跳过，联调环境开启即可生效。
    paymentCloseMessagePublisher.publish(order.id(), expireAt);
    return order;
  }

  public OrderResponse cancelOrder(String userId, String orderId, String reason) {
    // 取消订单会触发库存释放；底层用库存流水保证同一订单不会重复回滚。
    OrderResponse order = store.cancelOrder(userId, orderId, reason);
    auditLogService.record("ORDER_CANCEL", "ORDER", orderId, reason);
    return order;
  }

  public OrderResponse requestRefund(String userId, String orderId, String reason) {
    // 用户只提交退款申请，真正退款确认由后台操作完成。
    OrderResponse order = store.requestRefund(userId, orderId, reason);
    auditLogService.record("ORDER_REFUND_REQUEST", "ORDER", orderId, reason);
    return order;
  }
}
