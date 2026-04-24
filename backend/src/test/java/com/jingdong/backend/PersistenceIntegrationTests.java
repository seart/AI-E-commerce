package com.jingdong.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.jingdong.backend.dto.auth.AuthDtos.RegisterRequest;
import com.jingdong.backend.dto.order.OrderDtos.CheckoutItemRequest;
import com.jingdong.backend.dto.order.OrderDtos.CreateOrderRequest;
import com.jingdong.backend.dto.order.OrderDtos.OrderResponse;
import com.jingdong.backend.service.AuthService;
import com.jingdong.backend.service.CartService;
import com.jingdong.backend.service.OrderService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class PersistenceIntegrationTests {
  private static final String TEST_MOBILE = "13900000099";
  private static final String DEMO_USER_ID = "u_demo";

  @Autowired
  private AuthService authService;

  @Autowired
  private CartService cartService;

  @Autowired
  private OrderService orderService;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  void registerWritesUserToMysql() {
    jdbcTemplate.update("delete from users where mobile = ?", TEST_MOBILE);

    authService.register(new RegisterRequest(TEST_MOBILE, "123456", "123456"));

    Integer userCount = jdbcTemplate.queryForObject(
        "select count(*) from users where mobile = ?",
        Integer.class,
        TEST_MOBILE
    );
    assertThat(userCount).isEqualTo(1);
  }

  @Test
  void cartAndOrderWriteToMysql() {
    jdbcTemplate.update(
        "delete from order_items where order_id in (select id from orders where user_id = ?)",
        DEMO_USER_ID
    );
    jdbcTemplate.update("delete from orders where user_id = ?", DEMO_USER_ID);
    jdbcTemplate.update("delete from cart_items where user_id = ?", DEMO_USER_ID);

    cartService.addCartItem(DEMO_USER_ID, "p1");

    Integer cartCount = jdbcTemplate.queryForObject(
        "select count(*) from cart_items where user_id = ? and product_id = ?",
        Integer.class,
        DEMO_USER_ID,
        "p1"
    );
    assertThat(cartCount).isEqualTo(1);

    OrderResponse order = orderService.createOrder(
        DEMO_USER_ID,
        new CreateOrderRequest(
            "addr_demo_1",
            List.of(new CheckoutItemRequest("p1", 1))
        )
    );

    Integer orderCount = jdbcTemplate.queryForObject(
        "select count(*) from orders where id = ? and status = 'PAID'",
        Integer.class,
        order.id()
    );
    Integer orderItemCount = jdbcTemplate.queryForObject(
        "select count(*) from order_items where order_id = ?",
        Integer.class,
        order.id()
    );
    Integer remainingCartCount = jdbcTemplate.queryForObject(
        "select count(*) from cart_items where user_id = ? and product_id = ?",
        Integer.class,
        DEMO_USER_ID,
        "p1"
    );

    assertThat(orderCount).isEqualTo(1);
    assertThat(orderItemCount).isEqualTo(1);
    assertThat(remainingCartCount).isZero();
  }
}
