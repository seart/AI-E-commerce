package com.jingdong.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jingdong.backend.api.ErrorCode;
import com.jingdong.backend.dto.auth.AuthDtos.LoginRequest;
import com.jingdong.backend.dto.order.OrderDtos.CheckoutItemRequest;
import com.jingdong.backend.dto.order.OrderDtos.CreateOrderRequest;
import com.jingdong.backend.dto.order.OrderDtos.OrderResponse;
import com.jingdong.backend.exception.BusinessException;
import com.jingdong.backend.service.AdminService;
import com.jingdong.backend.service.AuthService;
import com.jingdong.backend.service.OrderService;
import com.jingdong.backend.service.RateLimiterService;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "app.payment.real-gateway-enabled=false"
)
class ThirdStageIntegrationTests {
  private static final String DEMO_USER_ID = "u_demo";

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private AuthService authService;

  @Autowired
  private OrderService orderService;

  @Autowired
  private AdminService adminService;

  @Autowired
  private RateLimiterService rateLimiterService;

  @Autowired
  private StringRedisTemplate redisTemplate;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void resetRateLimits() {
    redisTemplate.delete(List.of(
        "rate:login:13800000000",
        "rate:login:13900000000",
        "rate:login-ip:127.0.0.1",
        "rate:login-ip:0:0:0:0:0:0:0:1",
        "rate:login-ip:unknown",
        "rate:order:" + DEMO_USER_ID
    ));
  }

  @Test
  void adminEndpointsRejectCustomerAndAllowAdmin() {
    String customerToken = login("13800000000", "123456");
    HttpHeaders customerHeaders = new HttpHeaders();
    customerHeaders.setBearerAuth(customerToken);

    ResponseEntity<Map> forbidden = restTemplate.exchange(
        "/admin/dashboard/summary",
        HttpMethod.GET,
        new HttpEntity<>(customerHeaders),
        Map.class
    );
    assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(forbidden.getBody().get("code")).isEqualTo(ErrorCode.FORBIDDEN.code());

    String adminToken = login("13900000000", "123456");
    HttpHeaders adminHeaders = new HttpHeaders();
    adminHeaders.setBearerAuth(adminToken);
    ResponseEntity<Map> dashboard = restTemplate.exchange(
        "/admin/dashboard/summary",
        HttpMethod.GET,
        new HttpEntity<>(adminHeaders),
        Map.class
    );
    assertThat(dashboard.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(dashboard.getBody().get("code")).isEqualTo(0);
  }

  @Test
  void legacyPlainPasswordIsUpgradedAfterSuccessfulLogin() {
    jdbcTemplate.update(
        "update users set password = ?, status = 'ACTIVE', role = 'CUSTOMER' where mobile = ?",
        "123456",
        "13800000000"
    );

    authService.login(new LoginRequest("13800000000", "123456"));

    String password = jdbcTemplate.queryForObject(
        "select password from users where mobile = ?",
        String.class,
        "13800000000"
    );
    assertThat(password).startsWith("{pbkdf2}");
  }

  @Test
  void orderStatusFlowDeductsAndRollsBackStock() {
    deleteDemoOrders();
    jdbcTemplate.update("update merchants set status = 'ACTIVE' where id = 'm1'");
    jdbcTemplate.update("update products set stock = 20, status = 'ON_SHELF' where id = 'p1'");
    resetInventory("p1", 20);

    OrderResponse progressingOrder = createDemoOrder(2);
    markPaid(progressingOrder.id());
    assertThat(stock("p1")).isEqualTo(18);

    assertThat(adminService.updateOrderStatus(progressingOrder.id(), "PREPARING", "test").status())
        .isEqualTo("PREPARING");
    assertThat(adminService.updateOrderStatus(progressingOrder.id(), "DELIVERING", "test").status())
        .isEqualTo("DELIVERING");
    assertThat(adminService.updateOrderStatus(progressingOrder.id(), "COMPLETED", "test").status())
        .isEqualTo("COMPLETED");
    assertThatThrownBy(() -> adminService.updateOrderStatus(progressingOrder.id(), "REFUNDED", "test"))
        .isInstanceOf(BusinessException.class);

    OrderResponse canceledOrder = createDemoOrder(1);
    assertThat(stock("p1")).isEqualTo(17);
    assertThat(orderService.cancelOrder(DEMO_USER_ID, canceledOrder.id(), "test cancel").status())
        .isEqualTo("PAYMENT_CLOSED");
    assertThat(stock("p1")).isEqualTo(18);

    OrderResponse refundOrder = createDemoOrder(1);
    markPaid(refundOrder.id());
    assertThat(stock("p1")).isEqualTo(17);
    assertThat(orderService.requestRefund(DEMO_USER_ID, refundOrder.id(), "test refund").status())
        .isEqualTo("REFUND_REQUESTED");
    assertThat(adminService.updateOrderStatus(refundOrder.id(), "REFUNDED", "test refund").status())
        .isEqualTo("REFUNDED");
    assertThat(stock("p1")).isEqualTo(18);

    Integer auditCount = jdbcTemplate.queryForObject(
        "select count(*) from audit_logs where action = 'ADMIN_UPDATE_ORDER_STATUS'",
        Integer.class
    );
    assertThat(auditCount).isGreaterThan(0);
  }

  @Test
  void fixedWindowRateLimiterBlocksAfterLimit() {
    String key = "test:" + UUID.randomUUID();
    rateLimiterService.check(key, 2, Duration.ofMinutes(1));
    rateLimiterService.check(key, 2, Duration.ofMinutes(1));

    assertThatThrownBy(() -> rateLimiterService.check(key, 2, Duration.ofMinutes(1)))
        .isInstanceOf(BusinessException.class)
        .satisfies(error -> assertThat(((BusinessException) error).errorCode())
            .isEqualTo(ErrorCode.RATE_LIMITED));
  }

  @Test
  void disabledUserCannotReuseOldAccessToken() {
    jdbcTemplate.update("update users set status = 'ACTIVE' where id = ?", DEMO_USER_ID);
    String token = login("13800000000", "123456");

    try {
      jdbcTemplate.update("update users set status = 'DISABLED' where id = ?", DEMO_USER_ID);

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(token);
      ResponseEntity<Map> profileResponse = restTemplate.exchange(
          "/profile",
          HttpMethod.GET,
          new HttpEntity<>(headers),
          Map.class
      );
      assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
      assertThat(profileResponse.getBody().get("code")).isEqualTo(ErrorCode.UNAUTHORIZED.code());
    } finally {
      jdbcTemplate.update("update users set status = 'ACTIVE' where id = ?", DEMO_USER_ID);
    }
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

  private void deleteDemoOrders() {
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
    jdbcTemplate.update("delete from inventory_transactions where sku_id = ?", "p1");
  }

  private void resetInventory(String productId, int available) {
    int updated = jdbcTemplate.update("""
        update inventory_accounts
        set available_quantity = ?, locked_quantity = 0, sold_quantity = 0, version = version + 1
        where sku_id = ?
        """, available, productId);
    if (updated == 0) {
      jdbcTemplate.update("""
          insert into inventory_accounts (sku_id, available_quantity, locked_quantity, sold_quantity, version)
          values (?, ?, 0, 0, 0)
          """, productId, available);
    }
  }

  private void markPaid(String orderId) {
    jdbcTemplate.update("""
        update orders
        set status = 'PAID', status_text = '支付成功', payment_status = 'PAID', paid_at = now()
        where id = ?
        """,
        orderId
    );
  }

  @SuppressWarnings("unchecked")
  private String login(String mobile, String password) {
    ResponseEntity<Map> response = restTemplate.postForEntity(
        "/auth/login",
        Map.of("mobile", mobile, "password", password),
        Map.class
    );
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
    return (String) data.get("accessToken");
  }
}
