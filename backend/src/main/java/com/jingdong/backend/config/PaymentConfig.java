package com.jingdong.backend.config;

import cn.felord.payment.autoconfigure.EnableMobilePay;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PaymentProperties.class)
public class PaymentConfig {
  @Configuration
  @EnableMobilePay
  @ConditionalOnProperty(prefix = "app.payment", name = "sdk-enabled", havingValue = "true")
  static class MobilePaymentSdkConfiguration {
  }
}
