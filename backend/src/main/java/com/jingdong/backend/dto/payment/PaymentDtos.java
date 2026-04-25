package com.jingdong.backend.dto.payment;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public final class PaymentDtos {
  private PaymentDtos() {}

  public record PaymentPrepayRequest(
      @NotBlank(message = "订单 ID 不能为空")
      String orderId,

      @NotBlank(message = "支付渠道不能为空")
      String channel
  ) {}

  public record PaymentPrepayResponse(
      String paymentId,
      String orderId,
      String channel,
      String status,
      BigDecimal amount,
      String outTradeNo,
      String transactionId,
      String qrContent,
      String expireAt
  ) {}

  public record PaymentStatusResponse(
      String paymentId,
      String orderId,
      String channel,
      String status,
      String orderStatus,
      String transactionId,
      String expireAt,
      String paidAt,
      String closedAt
  ) {}
}
