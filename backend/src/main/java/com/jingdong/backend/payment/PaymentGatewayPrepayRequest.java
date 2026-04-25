package com.jingdong.backend.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentGatewayPrepayRequest(
    String channel,
    String orderId,
    String orderNo,
    String outTradeNo,
    BigDecimal amount,
    String subject,
    LocalDateTime expireAt
) {}
