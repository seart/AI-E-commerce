package com.jingdong.backend.controller;

import cn.felord.payment.alipay.AliPayProperties;
import cn.felord.payment.wechat.enumeration.TradeState;
import cn.felord.payment.wechat.v3.WechatApiProvider;
import cn.felord.payment.wechat.v3.model.ResponseSignVerifyParams;
import com.alipay.api.internal.util.AlipaySignature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jingdong.backend.api.ApiResponse;
import com.jingdong.backend.api.ErrorCode;
import com.jingdong.backend.auth.UserContext;
import com.jingdong.backend.config.PaymentProperties;
import com.jingdong.backend.dto.payment.PaymentDtos.PaymentPrepayRequest;
import com.jingdong.backend.dto.payment.PaymentDtos.PaymentPrepayResponse;
import com.jingdong.backend.dto.payment.PaymentDtos.PaymentStatusResponse;
import com.jingdong.backend.exception.BusinessException;
import com.jingdong.backend.service.PaymentService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentController {
  private final PaymentService paymentService;
  private final ObjectMapper objectMapper;
  private final PaymentProperties paymentProperties;
  private final ObjectProvider<WechatApiProvider> wechatApiProvider;
  private final ObjectProvider<AliPayProperties> aliPayProperties;

  public PaymentController(
      PaymentService paymentService,
      ObjectMapper objectMapper,
      PaymentProperties paymentProperties,
      ObjectProvider<WechatApiProvider> wechatApiProvider,
      ObjectProvider<AliPayProperties> aliPayProperties
  ) {
    this.paymentService = paymentService;
    this.objectMapper = objectMapper;
    this.paymentProperties = paymentProperties;
    this.wechatApiProvider = wechatApiProvider;
    this.aliPayProperties = aliPayProperties;
  }

  @PostMapping("/prepay")
  public ApiResponse<PaymentPrepayResponse> prepay(@Valid @RequestBody PaymentPrepayRequest request) {
    return ApiResponse.success(paymentService.prepay(UserContext.userId(), request));
  }

  @GetMapping("/{paymentId}")
  public ApiResponse<PaymentStatusResponse> status(@PathVariable String paymentId) {
    return ApiResponse.success(paymentService.status(UserContext.userId(), paymentId));
  }

  @PostMapping("/notify/alipay")
  public String alipayNotify(@RequestParam Map<String, String> params) {
    if (!verifyAlipay(params)) {
      return "failure";
    }
    String tradeStatus = params.get("trade_status");
    if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
      paymentService.confirmGatewayPaid(params.get("out_trade_no"), params.get("trade_no"), toJson(params));
    }
    return "success";
  }

  @PostMapping("/notify/wechat")
  public Map<String, String> wechatNotify(
      @RequestHeader(value = "Wechatpay-Serial", required = false) String serial,
      @RequestHeader(value = "Wechatpay-Signature", required = false) String signature,
      @RequestHeader(value = "Wechatpay-Timestamp", required = false) String timestamp,
      @RequestHeader(value = "Wechatpay-Nonce", required = false) String nonce,
      @RequestBody String body
  ) {
    WechatApiProvider provider = wechatApiProvider.getIfAvailable();
    if (paymentProperties.isSdkEnabled() && provider != null) {
      ResponseSignVerifyParams verifyParams = new ResponseSignVerifyParams();
      verifyParams.setWechatpaySerial(serial);
      verifyParams.setWechatpaySignature(signature);
      verifyParams.setWechatpayTimestamp(timestamp);
      verifyParams.setWechatpayNonce(nonce);
      verifyParams.setBody(body);
      // SDK 会完成微信支付 v3 签名校验和密文解密，回调只在 SUCCESS 时推进订单。
      return provider.callback(paymentProperties.getWechatTenantId()).transactionCallback(verifyParams, data -> {
        if (TradeState.SUCCESS.equals(data.getTradeState())) {
          paymentService.confirmGatewayPaid(data.getOutTradeNo(), data.getTransactionId(), toJson(data));
        }
      });
    }

    confirmWechatPlainNotify(body);
    return Map.of("code", "SUCCESS", "message", "成功");
  }

  private boolean verifyAlipay(Map<String, String> params) {
    if (!paymentProperties.isSdkEnabled()) {
      return true;
    }
    AliPayProperties properties = aliPayProperties.getIfAvailable();
    AliPayProperties.V1 v1 = properties == null ? null : properties.getV1();
    if (v1 == null || v1.getAlipayPublicCertPath() == null || v1.getAlipayPublicCertPath().isBlank()) {
      return false;
    }
    try {
      return AlipaySignature.rsaCertCheckV1(params, v1.getAlipayPublicCertPath(), v1.getCharset(), v1.getSignType());
    } catch (Exception exception) {
      return false;
    }
  }

  private void confirmWechatPlainNotify(String body) {
    try {
      JsonNode node = objectMapper.readTree(body);
      String tradeState = text(node, "trade_state");
      if ("SUCCESS".equals(tradeState)) {
        paymentService.confirmGatewayPaid(text(node, "out_trade_no"), text(node, "transaction_id"), body);
      }
    } catch (Exception exception) {
      throw new BusinessException(ErrorCode.PAYMENT_GATEWAY_FAILED, "微信支付回调解析失败");
    }
  }

  private String text(JsonNode node, String field) {
    JsonNode value = node.get(field);
    return value == null ? null : value.asText();
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception exception) {
      return String.valueOf(value);
    }
  }
}
