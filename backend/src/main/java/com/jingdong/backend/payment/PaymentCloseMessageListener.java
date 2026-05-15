package com.jingdong.backend.payment;

import com.jingdong.backend.service.PaymentService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.payment.rocketmq", name = "enabled", havingValue = "true")
@RocketMQMessageListener(
    topic = "${app.payment.rocketmq.close-topic:jingdong-payment-close}",
    consumerGroup = "${app.payment.rocketmq.close-consumer-group:jingdong-payment-close-consumer}"
)
public class PaymentCloseMessageListener implements RocketMQListener<PaymentCloseMessage> {
  // 消费延迟关单消息，真正的幂等关单逻辑在 PaymentService。
  private final PaymentService paymentService;

  public PaymentCloseMessageListener(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @Override
  public void onMessage(PaymentCloseMessage message) {
    // MQ 可能重复投递，PaymentService 内部用订单状态条件更新保证库存只回滚一次。
    paymentService.closeExpiredOrder(message.orderId());
  }
}
