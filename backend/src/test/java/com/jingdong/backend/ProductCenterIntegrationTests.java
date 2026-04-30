package com.jingdong.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
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
class ProductCenterIntegrationTests {
  private static final String DEMO_USER_ID = "u_demo";

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
    jdbcTemplate.update("delete from products where id like 'sku_pc_%'");
    jdbcTemplate.update("delete from product_spus where id like 'spu_pc_%'");
    jdbcTemplate.update("delete from spec_options where id like 'speco_pc_%'");
    jdbcTemplate.update("delete from spec_groups where id like 'specg_pc_%'");
    jdbcTemplate.update("delete from brands where id like 'brand_pc_%'");
    jdbcTemplate.update("delete from categories where id like 'cat_pc_%'");
    jdbcTemplate.update("update merchants set status = 'ACTIVE' where id = 'm1'");
  }

  @Test
  void customerCannotUseAdminProductCenterApis() {
    HttpHeaders headers = authHeaders("13800000000", "123456");

    ResponseEntity<Map> response = restTemplate.exchange(
        "/admin/product-spus",
        HttpMethod.GET,
        new HttpEntity<>(headers),
        Map.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void adminCanCreateBrandCategorySpecSpuAndSku() {
    HttpHeaders adminHeaders = authHeaders("13900000000", "123456");
    Seed seed = seedDictionaries(adminHeaders);

    ResponseEntity<Map> response = restTemplate.postForEntity(
        "/admin/product-spus",
        new HttpEntity<>(spuPayload(seed, "阶段一测试牛奶", "sku_pc_milk"), adminHeaders),
        Map.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> spu = data(response);
    assertThat(spu.get("id")).isEqualTo("spu_pc_milk");
    assertThat(spu.get("skuCount")).isEqualTo(1);

    ResponseEntity<Map> searchResponse = restTemplate.exchange(
        "/products/search?keyword=牛奶",
        HttpMethod.GET,
        new HttpEntity<>(adminHeaders),
        Map.class
    );
    assertThat(searchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat((List<?>) searchResponse.getBody().get("data")).isNotEmpty();
  }

  @Test
  void productSaveRejectsDisabledMerchantOrInvalidCategory() {
    HttpHeaders adminHeaders = authHeaders("13900000000", "123456");
    Seed seed = seedDictionaries(adminHeaders);

    jdbcTemplate.update("update merchants set status = 'DISABLED' where id = 'm1'");
    ResponseEntity<Map> disabledMerchant = restTemplate.postForEntity(
        "/admin/product-spus",
        new HttpEntity<>(spuPayload(seed, "禁用商家商品", "sku_pc_disabled_merchant"), adminHeaders),
        Map.class
    );
    assertThat(disabledMerchant.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    jdbcTemplate.update("update merchants set status = 'ACTIVE' where id = 'm1'");
    ResponseEntity<Map> invalidCategory = restTemplate.postForEntity(
        "/admin/product-spus",
        new HttpEntity<>(
            spuPayload(seed.withCategory("missing_category"), "无效类目商品", "sku_pc_invalid_category"),
            adminHeaders
        ),
        Map.class
    );
    assertThat(invalidCategory.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void duplicateSkuSpecCombinationIsRejected() {
    HttpHeaders adminHeaders = authHeaders("13900000000", "123456");
    Seed seed = seedDictionaries(adminHeaders);
    Map<String, Object> payload = spuPayload(seed, "重复规格商品", "sku_pc_dup_a");
    payload.put("id", "spu_pc_dup");
    payload.put("skus", List.of(
        skuPayload(seed, "sku_pc_dup_a"),
        skuPayload(seed, "sku_pc_dup_b")
    ));

    ResponseEntity<Map> response = restTemplate.postForEntity(
        "/admin/product-spus",
        new HttpEntity<>(payload, adminHeaders),
        Map.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void spuCannotBeOnShelfWithoutOnShelfSku() {
    HttpHeaders adminHeaders = authHeaders("13900000000", "123456");
    Seed seed = seedDictionaries(adminHeaders);
    Map<String, Object> payload = spuPayload(seed, "空 SKU 商品", "sku_pc_empty");
    payload.put("id", "spu_pc_empty");
    payload.put("skus", List.of());

    ResponseEntity<Map> response = restTemplate.postForEntity(
        "/admin/product-spus",
        new HttpEntity<>(payload, adminHeaders),
        Map.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void productSearchOnlyReturnsVisibleProducts() {
    HttpHeaders adminHeaders = authHeaders("13900000000", "123456");
    Seed seed = seedDictionaries(adminHeaders);
    restTemplate.postForEntity(
        "/admin/product-spus",
        new HttpEntity<>(spuPayload(seed, "可见性测试商品", "sku_pc_visible"), adminHeaders),
        Map.class
    );

    ResponseEntity<Map> visible = restTemplate.exchange(
        "/products/search?keyword=可见性",
        HttpMethod.GET,
        new HttpEntity<>(adminHeaders),
        Map.class
    );
    assertThat((List<?>) visible.getBody().get("data")).isNotEmpty();

    ResponseEntity<Map> statusResponse = restTemplate.exchange(
        "/admin/product-spus/spu_pc_milk/skus/sku_pc_visible/status",
        HttpMethod.PATCH,
        new HttpEntity<>(Map.of("status", "OFF_SHELF"), adminHeaders),
        Map.class
    );
    assertThat(statusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Map> hidden = restTemplate.exchange(
        "/products/search?keyword=可见性",
        HttpMethod.GET,
        new HttpEntity<>(adminHeaders),
        Map.class
    );
    assertThat((List<?>) hidden.getBody().get("data")).isEmpty();
  }

  @Test
  void cartAndCheckoutAcceptSkuIdWithProductIdCompatibility() {
    HttpHeaders adminHeaders = authHeaders("13900000000", "123456");
    Seed seed = seedDictionaries(adminHeaders);
    restTemplate.postForEntity(
        "/admin/product-spus",
        new HttpEntity<>(spuPayload(seed, "SKU 下单测试商品", "sku_pc_checkout"), adminHeaders),
        Map.class
    );

    HttpHeaders customerHeaders = authHeaders("13800000000", "123456");
    ResponseEntity<Map> cartResponse = restTemplate.postForEntity(
        "/cart/items",
        new HttpEntity<>(Map.of("skuId", "sku_pc_checkout"), customerHeaders),
        Map.class
    );
    assertThat(cartResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Map> orderResponse = restTemplate.postForEntity(
        "/orders",
        new HttpEntity<>(
            Map.of(
                "addressId", "addr_demo_1",
                "items", List.of(Map.of("skuId", "sku_pc_checkout", "quantity", 1))
            ),
            customerHeaders
        ),
        Map.class
    );
    assertThat(orderResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(data(orderResponse).get("status")).isEqualTo("PENDING_PAYMENT");
  }

  private Seed seedDictionaries(HttpHeaders adminHeaders) {
    String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    String brandId = "brand_pc_" + suffix;
    String categoryId = "cat_pc_" + suffix;
    String groupId = "specg_pc_" + suffix;
    String optionId = "speco_pc_" + suffix;

    assertThat(restTemplate.postForEntity(
        "/admin/brands",
        new HttpEntity<>(Map.of(
            "id", brandId,
            "name", "测试品牌" + suffix,
            "logo", "",
            "description", "阶段一测试品牌",
            "status", "ACTIVE",
            "sortOrder", 900
        ), adminHeaders),
        Map.class
    ).getStatusCode()).isEqualTo(HttpStatus.OK);

    assertThat(restTemplate.postForEntity(
        "/admin/categories",
        new HttpEntity<>(Map.of(
            "id", categoryId,
            "name", "测试类目" + suffix,
            "icon", "/api/static/category/supermarket.png",
            "level", 1,
            "type", "PRODUCT",
            "status", "ACTIVE",
            "sortOrder", 900
        ), adminHeaders),
        Map.class
    ).getStatusCode()).isEqualTo(HttpStatus.OK);

    assertThat(restTemplate.postForEntity(
        "/admin/spec-groups",
        new HttpEntity<>(Map.of(
            "id", groupId,
            "name", "规格" + suffix,
            "status", "ACTIVE",
            "sortOrder", 900
        ), adminHeaders),
        Map.class
    ).getStatusCode()).isEqualTo(HttpStatus.OK);

    assertThat(restTemplate.postForEntity(
        "/admin/spec-groups/" + groupId + "/options",
        new HttpEntity<>(Map.of(
            "id", optionId,
            "name", "标准装",
            "status", "ACTIVE",
            "sortOrder", 900
        ), adminHeaders),
        Map.class
    ).getStatusCode()).isEqualTo(HttpStatus.OK);

    return new Seed(brandId, categoryId, groupId, optionId);
  }

  private Map<String, Object> spuPayload(Seed seed, String name, String skuId) {
    Map<String, Object> payload = new java.util.LinkedHashMap<>();
    payload.put("id", "spu_pc_milk");
    payload.put("merchantId", "m1");
    payload.put("categoryId", seed.categoryId());
    payload.put("brandId", seed.brandId());
    payload.put("name", name);
    payload.put("subtitle", "阶段一商品中心测试");
    payload.put("mainImage", "测");
    payload.put("detail", "商品中心测试详情");
    payload.put("detailImages", List.of());
    payload.put("status", "ON_SHELF");
    payload.put("sortOrder", 900);
    payload.put("skus", List.of(skuPayload(seed, skuId)));
    return payload;
  }

  private Map<String, Object> skuPayload(Seed seed, String skuId) {
    return Map.of(
        "skuId", skuId,
        "skuCode", skuId.toUpperCase(Locale.ROOT),
        "specs", List.of(Map.of(
            "groupId", seed.groupId(),
            "groupName", "规格",
            "optionId", seed.optionId(),
            "optionName", "标准装"
        )),
        "price", 19.9,
        "originalPrice", 29.9,
        "unit", "件",
        "stock", 10,
        "status", "ON_SHELF"
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

  @SuppressWarnings("unchecked")
  private Map<String, Object> data(ResponseEntity<Map> response) {
    return (Map<String, Object>) response.getBody().get("data");
  }

  private record Seed(String brandId, String categoryId, String groupId, String optionId) {
    Seed withCategory(String nextCategoryId) {
      return new Seed(brandId, nextCategoryId, groupId, optionId);
    }
  }
}
