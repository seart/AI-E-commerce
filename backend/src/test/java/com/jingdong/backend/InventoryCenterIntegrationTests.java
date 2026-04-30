package com.jingdong.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jingdong.backend.dto.order.OrderDtos.CheckoutItemRequest;
import com.jingdong.backend.dto.order.OrderDtos.CreateOrderRequest;
import com.jingdong.backend.dto.order.OrderDtos.OrderResponse;
import com.jingdong.backend.dto.payment.PaymentDtos.PaymentPrepayRequest;
import com.jingdong.backend.dto.payment.PaymentDtos.PaymentPrepayResponse;
import com.jingdong.backend.exception.BusinessException;
import com.jingdong.backend.service.AdminService;
import com.jingdong.backend.service.OrderService;
import com.jingdong.backend.service.PaymentService;
import java.util.List;
import java.util.Map;
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
class InventoryCenterIntegrationTests {
  private static final String DEMO_USER_ID = "u_demo";
  private static final String SKU_ID = "p1";

  @Autowired
  private OrderService orderService;

  @Autowired
  private PaymentService paymentService;

  @Autowired
  private AdminService adminService;

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private StringRedisTemplate redisTemplate;

  @BeforeEach
  void resetDemoData() {
    redisTemplate.delete(List.of(
        "rate:login:13800000000",
        "rate:login:13900000000",
        "rate:login-ip:127.0.0.1",
        "rate:login-ip:0:0:0:0:0:0:0:1",
        "rate:login-ip:unknown",
        "rate:order:" + DEMO_USER_ID
    ));
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
    jdbcTemplate.update("delete from inventory_transactions where sku_id = ?", SKU_ID);
    jdbcTemplate.update("update merchants set status = 'ACTIVE' where id = 'm1'");
    jdbcTemplate.update("update product_spus set status = 'ON_SHELF' where id = (select spu_id from products where id = ?)", SKU_ID);
    jdbcTemplate.update("update products set stock = 20, status = 'ON_SHELF' where id = ?", SKU_ID);
    int updated = jdbcTemplate.update("""
        update inventory_accounts
        set available_quantity = 20, locked_quantity = 0, sold_quantity = 0, version = version + 1
        where sku_id = ?
        """, SKU_ID);
    if (updated == 0) {
      jdbcTemplate.update("""
          insert into inventory_accounts (sku_id, available_quantity, locked_quantity, sold_quantity, version)
          values (?, 20, 0, 0, 0)
          """, SKU_ID);
    }
  }

  @Test
  void createOrderLocksInventoryAndPaymentConfirmMovesLockedToSold() {
    OrderResponse order = createDemoOrder(2);

    assertInventory(18, 2, 0);
    assertThat(stock()).isEqualTo(18);
    assertThat(transactionCount("ORDER_LOCK", order.id())).isEqualTo(1);

    PaymentPrepayResponse prepay = paymentService.prepay(
        DEMO_USER_ID,
        new PaymentPrepayRequest(order.id(), "WECHAT_QR")
    );
    paymentService.confirmGatewayPaid(prepay.outTradeNo(), "wx_inventory_001", "{}");
    paymentService.confirmGatewayPaid(prepay.outTradeNo(), "wx_inventory_001", "{}");

    assertInventory(18, 0, 2);
    assertThat(stock()).isEqualTo(18);
    assertThat(transactionCount("PAYMENT_CONFIRM", order.id())).isEqualTo(1);
  }

  @Test
  void timeoutCloseReleasesLockedInventoryOnce() {
    OrderResponse order = createDemoOrder(3);
    PaymentPrepayResponse prepay = paymentService.prepay(
        DEMO_USER_ID,
        new PaymentPrepayRequest(order.id(), "ALIPAY_QR")
    );
    jdbcTemplate.update("update orders set payment_expire_at = date_sub(now(), interval 1 minute) where id = ?", order.id());
    jdbcTemplate.update("update payments set expire_at = date_sub(now(), interval 1 minute) where id = ?", prepay.paymentId());

    paymentService.closeExpiredOrder(order.id());
    paymentService.closeExpiredOrder(order.id());

    assertInventory(20, 0, 0);
    assertThat(stock()).isEqualTo(20);
    assertThat(transactionCount("ORDER_RELEASE", order.id())).isEqualTo(1);
  }

  @Test
  void refundRestocksSoldInventoryOnce() {
    OrderResponse order = createDemoOrder(1);
    PaymentPrepayResponse prepay = paymentService.prepay(
        DEMO_USER_ID,
        new PaymentPrepayRequest(order.id(), "WECHAT_QR")
    );
    paymentService.confirmGatewayPaid(prepay.outTradeNo(), "wx_inventory_002", "{}");

    assertInventory(19, 0, 1);
    assertThat(orderService.requestRefund(DEMO_USER_ID, order.id(), "库存中心退款").status())
        .isEqualTo("REFUND_REQUESTED");
    assertThat(adminService.updateOrderStatus(order.id(), "REFUNDED", "库存中心退款确认").status())
        .isEqualTo("REFUNDED");
    assertThatThrownBy(() -> adminService.updateOrderStatus(order.id(), "REFUNDED", "重复退款确认"))
        .isInstanceOf(BusinessException.class);

    assertInventory(20, 0, 0);
    assertThat(stock()).isEqualTo(20);
    assertThat(transactionCount("ORDER_RESTOCK", order.id())).isEqualTo(1);
  }

  @Test
  @SuppressWarnings("unchecked")
  void adminInventoryApisRequireAdminAndCanAdjustAvailableStock() {
    HttpHeaders adminHeaders = authHeaders("13900000000", "123456");
    HttpHeaders customerHeaders = authHeaders("13800000000", "123456");

    ResponseEntity<Map> forbidden = restTemplate.exchange(
        "/admin/inventory/accounts",
        HttpMethod.GET,
        new HttpEntity<>(customerHeaders),
        Map.class
    );
    assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    ResponseEntity<Map> adjust = restTemplate.postForEntity(
        "/admin/inventory/accounts/" + SKU_ID + "/adjust",
        new HttpEntity<>(Map.of("delta", -2, "reason", "库存中心测试调整"), adminHeaders),
        Map.class
    );
    assertThat(adjust.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> data = (Map<String, Object>) adjust.getBody().get("data");
    assertThat(data.get("availableQuantity")).isEqualTo(18);

    ResponseEntity<Map> accounts = restTemplate.exchange(
        "/admin/inventory/accounts?keyword=" + SKU_ID,
        HttpMethod.GET,
        new HttpEntity<>(adminHeaders),
        Map.class
    );
    assertThat(accounts.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat((List<?>) accounts.getBody().get("data")).isNotEmpty();
    assertThat(stock()).isEqualTo(18);
    assertThat(transactionCount("ADMIN_ADJUST", null)).isEqualTo(1);
  }

  private OrderResponse createDemoOrder(int quantity) {
    return orderService.createOrder(
        DEMO_USER_ID,
        new CreateOrderRequest(
            "addr_demo_1",
            List.of(new CheckoutItemRequest(SKU_ID, quantity))
        )
    );
  }

  private HttpHeaders authHeaders(String mobile, String password) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(login(mobile, password));
    return headers;
  }

  @SuppressWarnings("unchecked")
  private String login(String mobile, String password) {
    ResponseEntity<Map> response = restTemplate.postForEntity(
        "/auth/login",
        Map.of("mobile", mobile, "password", password),
        Map.class
    );
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return (String) ((Map<String, Object>) response.getBody().get("data")).get("accessToken");
  }

  private void assertInventory(int available, int locked, int sold) {
    Map<String, Object> row = jdbcTemplate.queryForMap(
        "select available_quantity, locked_quantity, sold_quantity from inventory_accounts where sku_id = ?",
        SKU_ID
    );
    assertThat(((Number) row.get("available_quantity")).intValue()).isEqualTo(available);
    assertThat(((Number) row.get("locked_quantity")).intValue()).isEqualTo(locked);
    assertThat(((Number) row.get("sold_quantity")).intValue()).isEqualTo(sold);
  }

  private int stock() {
    return jdbcTemplate.queryForObject(
        "select stock from products where id = ?",
        Integer.class,
        SKU_ID
    );
  }

  private int transactionCount(String bizType, String orderId) {
    if (orderId == null) {
      return jdbcTemplate.queryForObject(
          "select count(*) from inventory_transactions where sku_id = ? and biz_type = ?",
          Integer.class,
          SKU_ID,
          bizType
      );
    }
    return jdbcTemplate.queryForObject(
        "select count(*) from inventory_transactions where sku_id = ? and biz_type = ? and biz_id = ?",
        Integer.class,
        SKU_ID,
        bizType,
        orderId
    );
  }
}
