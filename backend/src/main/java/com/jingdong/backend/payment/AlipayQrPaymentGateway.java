package com.jingdong.backend.payment;

import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jingdong.backend.api.ErrorCode;
import com.jingdong.backend.config.PaymentProperties;
import com.jingdong.backend.exception.BusinessException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.payment", name = "real-gateway-enabled", havingValue = "true", matchIfMissing = true)
public class AlipayQrPaymentGateway implements PaymentGateway {
  private final ObjectProvider<AlipayClient> alipayClientProvider;
  private final ObjectMapper objectMapper;
  private final PaymentProperties paymentProperties;

  public AlipayQrPaymentGateway(
      ObjectProvider<AlipayClient> alipayClientProvider,
      ObjectMapper objectMapper,
      PaymentProperties paymentProperties
  ) {
    this.alipayClientProvider = alipayClientProvider;
    this.objectMapper = objectMapper;
    this.paymentProperties = paymentProperties;
  }

  @Override
  public String channel() {
    return "ALIPAY_QR";
  }

  @Override
  public PaymentGatewayPrepayResponse prepay(PaymentGatewayPrepayRequest request) {
    AlipayClient client = alipayClientProvider.getIfAvailable();
    if (client == null) {
      throw new BusinessException(ErrorCode.PAYMENT_CHANNEL_NOT_CONFIGURED, "支付宝支付未启用或配置不完整");
    }
    try {
      AlipayTradePrecreateRequest alipayRequest = new AlipayTradePrecreateRequest();
      alipayRequest.setNotifyUrl(paymentProperties.getPublicBaseUrl() + "/payments/notify/alipay");
      alipayRequest.setBizContent(objectMapper.writeValueAsString(Map.of(
          "out_trade_no", request.outTradeNo(),
          "total_amount", request.amount().setScale(2).toPlainString(),
          "subject", request.subject(),
          "timeout_express", timeoutExpress(request.expireAt())
      )));
      // payment-spring-boot 提供 AlipayClient Bean；证书模式下使用 certificateExecute 完成正式下单。
      AlipayTradePrecreateResponse response = client.certificateExecute(alipayRequest);
      if (response == null || !response.isSuccess() || response.getQrCode() == null || response.getQrCode().isBlank()) {
        String message = response == null ? "支付宝无响应" : response.getSubMsg();
        throw new BusinessException(ErrorCode.PAYMENT_GATEWAY_FAILED, message == null ? "支付宝预下单失败" : message);
      }
      return new PaymentGatewayPrepayResponse(response.getQrCode(), response.getOutTradeNo(), request.expireAt());
    } catch (BusinessException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new BusinessException(ErrorCode.PAYMENT_GATEWAY_FAILED, "支付宝预下单失败");
    }
  }

  @Override
  public void close(String outTradeNo) {
    AlipayClient client = alipayClientProvider.getIfAvailable();
    if (client == null) {
      return;
    }
    try {
      AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
      request.setBizContent(objectMapper.writeValueAsString(Map.of("out_trade_no", outTradeNo)));
      client.certificateExecute(request);
    } catch (Exception ignored) {
      // 超时关单已在业务层做兜底，第三方关单异常只记录在上层日志，避免影响库存释放。
    }
  }

  private String timeoutExpress(LocalDateTime expireAt) {
    long minutes = Math.max(1, Duration.between(LocalDateTime.now(), expireAt).toMinutes());
    return minutes + "m";
  }
}
