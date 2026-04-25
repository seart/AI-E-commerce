package com.jingdong.backend.payment;

import java.time.LocalDateTime;

public record PaymentGatewayPrepayResponse(
    String qrContent,
    String gatewayOrderNo,
    LocalDateTime expireAt
) {}
