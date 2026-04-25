package com.jingdong.backend.payment;

import cn.felord.payment.wechat.v3.WechatApiProvider;
import cn.felord.payment.wechat.v3.WechatResponseEntity;
import cn.felord.payment.wechat.v3.model.Amount;
import cn.felord.payment.wechat.v3.model.PayParams;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jingdong.backend.api.ErrorCode;
import com.jingdong.backend.config.PaymentProperties;
import com.jingdong.backend.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.payment", name = "real-gateway-enabled", havingValue = "true", matchIfMissing = true)
public class WechatQrPaymentGateway implements PaymentGateway {
  private final ObjectProvider<WechatApiProvider> wechatApiProvider;
  private final PaymentProperties paymentProperties;

  public WechatQrPaymentGateway(
      ObjectProvider<WechatApiProvider> wechatApiProvider,
      PaymentProperties paymentProperties
  ) {
    this.wechatApiProvider = wechatApiProvider;
    this.paymentProperties = paymentProperties;
  }

  @Override
  public String channel() {
    return "WECHAT_QR";
  }

  @Override
  public PaymentGatewayPrepayResponse prepay(PaymentGatewayPrepayRequest request) {
    WechatApiProvider provider = wechatApiProvider.getIfAvailable();
    if (provider == null) {
      throw new BusinessException(ErrorCode.PAYMENT_CHANNEL_NOT_CONFIGURED, "微信支付未启用或配置不完整");
    }

    PayParams payParams = new PayParams();
    payParams.setDescription(request.subject());
    payParams.setOutTradeNo(request.outTradeNo());
    payParams.setNotifyUrl(paymentProperties.getPublicBaseUrl() + "/payments/notify/wechat");
    payParams.setTimeExpire(request.expireAt().atZone(ZoneId.systemDefault()).toOffsetDateTime());
    Amount amount = new Amount();
    amount.setCurrency("CNY");
    amount.setTotal(toCents(request.amount()));
    payParams.setAmount(amount);

    WechatResponseEntity<ObjectNode> response = provider
        .directPayApi(paymentProperties.getWechatTenantId())
        .nativePay(payParams);
    ObjectNode body = response.getBody();
    String codeUrl = body == null ? null : body.path("code_url").asText(null);
    if (!response.is2xxSuccessful() || codeUrl == null || codeUrl.isBlank()) {
      throw new BusinessException(ErrorCode.PAYMENT_GATEWAY_FAILED, "微信 Native 支付预下单失败");
    }
    return new PaymentGatewayPrepayResponse(codeUrl, request.outTradeNo(), request.expireAt());
  }

  @Override
  public void close(String outTradeNo) {
    WechatApiProvider provider = wechatApiProvider.getIfAvailable();
    if (provider == null) {
      return;
    }
    provider.directPayApi(paymentProperties.getWechatTenantId()).close(outTradeNo);
  }

  private int toCents(BigDecimal amount) {
    return amount.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).intValueExact();
  }
}
