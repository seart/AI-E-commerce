package com.jingdong.backend.payment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.payment", name = "real-gateway-enabled", havingValue = "false")
public class SimulatedPaymentGatewayConfig {
  // 未接入真实支付凭证时启用模拟网关，前端仍能跑完整支付流程。
  @Bean
  PaymentGateway simulatedAlipayQrPaymentGateway() {
    return new SimulatedPaymentGateway("ALIPAY_QR");
  }

  @Bean
  PaymentGateway simulatedWechatQrPaymentGateway() {
    return new SimulatedPaymentGateway("WECHAT_QR");
  }

  private record SimulatedPaymentGateway(String channel) implements PaymentGateway {
    @Override
    public PaymentGatewayPrepayResponse prepay(PaymentGatewayPrepayRequest request) {
      return new PaymentGatewayPrepayResponse(
          "qr://" + channel + "/" + request.outTradeNo(),
          "SIMULATED-" + request.outTradeNo(),
          request.expireAt()
      );
    }

    @Override
    public void close(String outTradeNo) {
      // 模拟网关无外部订单需要关闭，本地订单状态由业务层维护。
    }
  }
}
