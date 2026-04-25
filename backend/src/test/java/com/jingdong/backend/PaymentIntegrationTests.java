package com.jingdong.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jingdong.backend.api.ErrorCode;
import com.jingdong.backend.dto.order.OrderDtos.CheckoutItemRequest;
import com.jingdong.backend.dto.order.OrderDtos.CreateOrderRequest;
import com.jingdong.backend.dto.order.OrderDtos.OrderResponse;
import com.jingdong.backend.dto.payment.PaymentDtos.PaymentPrepayRequest;
import com.jingdong.backend.dto.payment.PaymentDtos.PaymentPrepayResponse;
import com.jingdong.backend.exception.BusinessException;
import com.jingdong.backend.payment.PaymentGateway;
import com.jingdong.backend.payment.PaymentGatewayPrepayRequest;
import com.jingdong.backend.payment.PaymentGatewayPrepayResponse;
import com.jingdong.backend.service.OrderService;
import com.jingdong.backend.service.PaymentService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "app.payment.real-gateway-enabled=false"
)
class PaymentIntegrationTests {
  private static final String DEMO_USER_ID = "u_demo";

  @Autowired
  private OrderService orderService;

  @Autowired
  private PaymentService paymentService;

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void resetDemoData() {
    jdbcTemplate.update(
        "delete from payments where order_id in (select id from orders where user_id = ?)",
        DEMO_USER_ID
    );
    jdbcTemplate.update(
        "delete from order_items where order_id in (select id from orders where user_id = ?)",
        DEMO_USER_ID
    );
    jdbcTemplate.update("delete from orders where user_id = ?", DEMO_USER_ID);
    jdbcTemplate.update("delete from cart_items where user_id = ?", DEMO_USER_ID);
    jdbcTemplate.update("update merchants set status = 'ACTIVE' where id = 'm1'");
    jdbcTemplate.update("update products set stock = 20, status = 'ON_SHELF' where id = 'p1'");
  }

  @Test
  void createOrderLocksStockAndWaitsForPayment() {
    OrderResponse order = createDemoOrder(2);

    assertThat(order.status()).isEqualTo("PENDING_PAYMENT");
    assertThat(order.statusText()).isEqualTo("待支付");
    assertThat(order.paymentStatus()).isEqualTo("PENDING");
    assertThat(order.paymentExpireAt()).isNotBlank();
    assertThat(stock("p1")).isEqualTo(18);
  }

  @Test
  void prepayCreatesQrPaymentAndRejectsOtherUsersOrder() {
    OrderResponse order = createDemoOrder(1);

    PaymentPrepayResponse prepay = paymentService.prepay(
        DEMO_USER_ID,
        new PaymentPrepayRequest(order.id(), "ALIPAY_QR")
    );

    assertThat(prepay.channel()).isEqualTo("ALIPAY_QR");
    assertThat(prepay.status()).isEqualTo("PAYING");
    assertThat(prepay.qrContent()).startsWith("qr://ALIPAY_QR/");

    assertThatThrownBy(() -> paymentService.prepay(
        "u_admin",
        new PaymentPrepayRequest(order.id(), "ALIPAY_QR")
    )).isInstanceOf(BusinessException.class)
        .satisfies(error -> assertThat(((BusinessException) error).errorCode())
            .isEqualTo(ErrorCode.FORBIDDEN));
  }

  @Test
  @SuppressWarnings("unchecked")
  void paymentPrepayEndpointReturnsQrPayloadOverHttp() {
    String token = login();
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);

    ResponseEntity<Map> orderResponse = restTemplate.postForEntity(
        "/orders",
        new HttpEntity<>(
            new CreateOrderRequest("addr_demo_1", List.of(new CheckoutItemRequest("p1", 1))),
            headers
        ),
        Map.class
    );
    assertThat(orderResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> order = (Map<String, Object>) orderResponse.getBody().get("data");

    ResponseEntity<Map> prepayResponse = restTemplate.postForEntity(
        "/payments/prepay",
        new HttpEntity<>(
            Map.of("orderId", order.get("id"), "channel", "ALIPAY_QR"),
            headers
        ),
        Map.class
    );

    assertThat(prepayResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> prepay = (Map<String, Object>) prepayResponse.getBody().get("data");
    assertThat(prepay.get("qrContent")).asString().startsWith("qr://ALIPAY_QR/");
  }

  @Test
  void gatewayPaidCallbackIsIdempotentAndMovesOrderToPaid() {
    OrderResponse order = createDemoOrder(1);
    PaymentPrepayResponse prepay = paymentService.prepay(
        DEMO_USER_ID,
        new PaymentPrepayRequest(order.id(), "WECHAT_QR")
    );

    paymentService.confirmGatewayPaid(prepay.outTradeNo(), "wx_tx_001", "{\"trade_state\":\"SUCCESS\"}");
    paymentService.confirmGatewayPaid(prepay.outTradeNo(), "wx_tx_001", "{\"trade_state\":\"SUCCESS\"}");

    Map<String, Object> row = jdbcTemplate.queryForMap(
        "select status, payment_status, payment_channel from orders where id = ?",
        order.id()
    );
    assertThat(row.get("status")).isEqualTo("PAID");
    assertThat(row.get("payment_status")).isEqualTo("PAID");
    assertThat(row.get("payment_channel")).isEqualTo("WECHAT_QR");
    assertThat(stock("p1")).isEqualTo(19);

    Integer paidAuditCount = jdbcTemplate.queryForObject(
        "select count(*) from audit_logs where action = 'PAYMENT_PAID' and target_id = ?",
        Integer.class,
        order.id()
    );
    assertThat(paidAuditCount).isGreaterThan(0);
  }

  @Test
  void wechatPaymentNotifyEndpointDoesNotRequireUserJwt() {
    OrderResponse order = createDemoOrder(1);
    PaymentPrepayResponse prepay = paymentService.prepay(
        DEMO_USER_ID,
        new PaymentPrepayRequest(order.id(), "WECHAT_QR")
    );

    ResponseEntity<Map> notifyResponse = restTemplate.postForEntity(
        "/payments/notify/wechat",
        Map.of(
            "trade_state", "SUCCESS",
            "out_trade_no", prepay.outTradeNo(),
            "transaction_id", "wx_notify_001"
        ),
        Map.class
    );

    assertThat(notifyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(jdbcTemplate.queryForObject(
        "select payment_status from orders where id = ?",
        String.class,
        order.id()
    )).isEqualTo("PAID");
  }

  @Test
  void timeoutCloseRollsBackStockOnce() {
    OrderResponse order = createDemoOrder(3);
    PaymentPrepayResponse prepay = paymentService.prepay(
        DEMO_USER_ID,
        new PaymentPrepayRequest(order.id(), "ALIPAY_QR")
    );

    jdbcTemplate.update("update orders set payment_expire_at = date_sub(now(), interval 1 minute) where id = ?", order.id());
    jdbcTemplate.update("update payments set expire_at = date_sub(now(), interval 1 minute) where id = ?", prepay.paymentId());

    paymentService.closeExpiredOrder(order.id());
    paymentService.closeExpiredOrder(order.id());

    Map<String, Object> row = jdbcTemplate.queryForMap(
        "select status, payment_status from orders where id = ?",
        order.id()
    );
    assertThat(row.get("status")).isEqualTo("PAYMENT_CLOSED");
    assertThat(row.get("payment_status")).isEqualTo("CLOSED");
    assertThat(stock("p1")).isEqualTo(20);
  }

  @Test
  void pendingPaymentCanBeCanceledButPaidOrderMustUseRefundFlow() {
    OrderResponse pendingOrder = createDemoOrder(1);

    OrderResponse closed = orderService.cancelOrder(DEMO_USER_ID, pendingOrder.id(), "用户取消待支付订单");
    assertThat(closed.status()).isEqualTo("PAYMENT_CLOSED");
    assertThat(stock("p1")).isEqualTo(20);

    OrderResponse paidOrder = createDemoOrder(1);
    PaymentPrepayResponse prepay = paymentService.prepay(
        DEMO_USER_ID,
        new PaymentPrepayRequest(paidOrder.id(), "WECHAT_QR")
    );
    paymentService.confirmGatewayPaid(prepay.outTradeNo(), "wx_tx_002", "{}");

    assertThatThrownBy(() -> orderService.cancelOrder(DEMO_USER_ID, paidOrder.id(), "已支付取消"))
        .isInstanceOf(BusinessException.class)
        .satisfies(error -> assertThat(((BusinessException) error).errorCode())
            .isEqualTo(ErrorCode.INVALID_ORDER_STATUS));
    assertThat(orderService.requestRefund(DEMO_USER_ID, paidOrder.id(), "申请退款").status())
        .isEqualTo("REFUND_REQUESTED");
  }

  private OrderResponse createDemoOrder(int quantity) {
    return orderService.createOrder(
        DEMO_USER_ID,
        new CreateOrderRequest(
            "addr_demo_1",
            List.of(new CheckoutItemRequest("p1", quantity))
        )
    );
  }

  private int stock(String productId) {
    return jdbcTemplate.queryForObject(
        "select stock from products where id = ?",
        Integer.class,
        productId
    );
  }

  @SuppressWarnings("unchecked")
  private String login() {
    ResponseEntity<Map> response = restTemplate.postForEntity(
        "/auth/login",
        Map.of("mobile", "13800000000", "password", "123456"),
        Map.class
    );
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
    return (String) data.get("accessToken");
  }

  @TestConfiguration
  static class PaymentTestConfiguration {
    @Bean
    @Primary
    PaymentGateway testPaymentGateway() {
      return new PaymentGateway() {
        @Override
        public String channel() {
          return "ALIPAY_QR";
        }

        @Override
        public PaymentGatewayPrepayResponse prepay(PaymentGatewayPrepayRequest request) {
          return new PaymentGatewayPrepayResponse(
              "qr://" + request.channel() + "/" + request.outTradeNo(),
              "test-" + request.channel(),
              request.expireAt()
          );
        }

        @Override
        public void close(String outTradeNo) {
        }
      };
    }

    @Bean
    PaymentGateway testWechatPaymentGateway() {
      return new PaymentGateway() {
        @Override
        public String channel() {
          return "WECHAT_QR";
        }

        @Override
        public PaymentGatewayPrepayResponse prepay(PaymentGatewayPrepayRequest request) {
          return new PaymentGatewayPrepayResponse(
              "qr://" + request.channel() + "/" + request.outTradeNo(),
              "test-" + request.channel(),
              request.expireAt()
          );
        }

        @Override
        public void close(String outTradeNo) {
        }
      };
    }
  }
}
