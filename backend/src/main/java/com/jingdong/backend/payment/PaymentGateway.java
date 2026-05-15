package com.jingdong.backend.payment;

public interface PaymentGateway {
  // 支付渠道适配接口，真实支付宝/微信和模拟网关都实现这一组方法。
  String channel();

  PaymentGatewayPrepayResponse prepay(PaymentGatewayPrepayRequest request);

  void close(String outTradeNo);
}
