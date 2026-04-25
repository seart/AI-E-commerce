package com.jingdong.backend.payment;

import com.jingdong.backend.config.PaymentProperties;
import java.time.Duration;
import java.time.LocalDateTime;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class PaymentCloseMessagePublisher {
  private static final Logger log = LoggerFactory.getLogger(PaymentCloseMessagePublisher.class);

  private final PaymentProperties paymentProperties;
  private final ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider;

  public PaymentCloseMessagePublisher(
      PaymentProperties paymentProperties,
      ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider
  ) {
    this.paymentProperties = paymentProperties;
    this.rocketMQTemplateProvider = rocketMQTemplateProvider;
  }

  public void publish(String orderId, LocalDateTime expireAt) {
    if (!paymentProperties.getRocketmq().isEnabled()) {
      return;
    }
    RocketMQTemplate rocketMQTemplate = rocketMQTemplateProvider.getIfAvailable();
    if (rocketMQTemplate == null) {
      log.warn("RocketMQTemplate is unavailable, skip payment close message, orderId={}", orderId);
      return;
    }

    long delaySeconds = Math.max(1, Duration.between(LocalDateTime.now(), expireAt).toSeconds());
    // RocketMQ 5.x 支持秒级延迟消息，这里把关单时间交给 MQ，避免应用本地定时任务漂移。
    rocketMQTemplate.syncSendDelayTimeSeconds(
        paymentProperties.getRocketmq().getCloseTopic(),
        new PaymentCloseMessage(orderId),
        delaySeconds
    );
  }
}
