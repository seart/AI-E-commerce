package com.jingdong.backend.config;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payment")
public class PaymentProperties {
  // 支付相关配置集中绑定 app.payment.*，可通过环境变量覆盖。
  private int expireMinutes = 15;
  private boolean sdkEnabled;
  private boolean realGatewayEnabled = true;
  private String publicBaseUrl = "http://localhost:8080/api";
  private String wechatTenantId = "default";
  private RocketMq rocketmq = new RocketMq();

  public int getExpireMinutes() {
    return expireMinutes;
  }

  public void setExpireMinutes(int expireMinutes) {
    this.expireMinutes = expireMinutes;
  }

  public boolean isSdkEnabled() {
    return sdkEnabled;
  }

  public void setSdkEnabled(boolean sdkEnabled) {
    this.sdkEnabled = sdkEnabled;
  }

  public boolean isRealGatewayEnabled() {
    return realGatewayEnabled;
  }

  public void setRealGatewayEnabled(boolean realGatewayEnabled) {
    this.realGatewayEnabled = realGatewayEnabled;
  }

  public String getPublicBaseUrl() {
    return publicBaseUrl;
  }

  public void setPublicBaseUrl(String publicBaseUrl) {
    this.publicBaseUrl = publicBaseUrl;
  }

  public String getWechatTenantId() {
    return wechatTenantId;
  }

  public void setWechatTenantId(String wechatTenantId) {
    this.wechatTenantId = wechatTenantId;
  }

  public RocketMq getRocketmq() {
    return rocketmq;
  }

  public void setRocketmq(RocketMq rocketmq) {
    this.rocketmq = rocketmq;
  }

  public static class RocketMq {
    // 支付超时关单消息配置，默认关闭，联调或生产按需开启。
    private boolean enabled;
    private String closeTopic = "jingdong-payment-close";
    private String closeConsumerGroup = "jingdong-payment-close-consumer";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getCloseTopic() {
      return closeTopic;
    }

    public void setCloseTopic(String closeTopic) {
      this.closeTopic = closeTopic;
    }

    public String getCloseConsumerGroup() {
      return closeConsumerGroup;
    }

    public void setCloseConsumerGroup(String closeConsumerGroup) {
      this.closeConsumerGroup = closeConsumerGroup;
    }
  }
}
