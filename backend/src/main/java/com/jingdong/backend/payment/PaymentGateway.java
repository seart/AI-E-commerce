package com.jingdong.backend.payment;

public interface PaymentGateway {
  String channel();

  PaymentGatewayPrepayResponse prepay(PaymentGatewayPrepayRequest request);

  void close(String outTradeNo);
}
