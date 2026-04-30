# 商品中心实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立后端、运营后台、用户端联动的商品中心，支持类目、品牌、规格、SPU/SKU、商品详情、商品搜索和购物车兼容。

**Architecture:** 后端以兼容式 SPU/SKU 为核心，新增 `product_spus`、`brands`、`spec_groups`、`spec_options` 表，继续使用 `products` 表作为可购买 SKU 表。后台将旧商品弹窗升级为商品中心工作台。用户端新增商品搜索和详情页，购物车/结算逐步使用 `skuId`，同时保留 `productId` 兼容。

**Tech Stack:** Spring Boot 3.3.5, Java 17, Maven, MyBatis-Plus, MySQL, Redis, Vue 3, Vite, Pinia, Element Plus, pnpm.

---

## 全局约束

- 所有文档、注释性说明、页面文案使用中文。
- 后端不引入 Flyway，不引入 JPA。
- 前端和后台只能使用 `pnpm`。
- 不实现库存锁定、库存流水、仓库批次、并发条件扣减；这些留给库存中心。
- 不删除历史商品行，不让订单历史依赖的 `products.id` 失效。
- 所有后台接口继续位于 `/admin/**`，由 Spring Security 限制 `ADMIN` 和 `OPERATOR`。
- RocketMQ 日志测试时写入 `logs/rocketmqlogs`。

## 文件结构

### 后端新增或修改

- 修改 `backend/src/main/resources/db/schema.sql`：新增品牌、规格、SPU 表，补齐类目和 SKU 字段。
- 修改 `backend/src/main/resources/db/data.sql`：补充品牌、规格、SPU/SKU 兼容种子数据。
- 修改 `backend/src/main/java/com/jingdong/backend/config/DatabaseMigrationRunner.java`：启动时补列、建表、迁移旧商品到 SPU。
- 修改 `backend/src/main/java/com/jingdong/backend/entity/DataEntities.java`：新增实体并扩展 `CategoryEntity`、`ProductEntity`。
- 新增 `backend/src/main/java/com/jingdong/backend/mapper/BrandMapper.java`。
- 新增 `backend/src/main/java/com/jingdong/backend/mapper/SpecGroupMapper.java`。
- 新增 `backend/src/main/java/com/jingdong/backend/mapper/SpecOptionMapper.java`。
- 新增 `backend/src/main/java/com/jingdong/backend/mapper/ProductSpuMapper.java`。
- 新增 `backend/src/main/java/com/jingdong/backend/dto/product/ProductDtos.java`：用户端商品 DTO。
- 修改 `backend/src/main/java/com/jingdong/backend/dto/admin/AdminDtos.java`：后台商品中心 DTO。
- 新增 `backend/src/main/java/com/jingdong/backend/service/ProductCenterService.java`：商品中心领域服务。
- 新增 `backend/src/main/java/com/jingdong/backend/controller/ProductController.java`：用户端商品搜索和详情。
- 修改 `backend/src/main/java/com/jingdong/backend/controller/AdminController.java`：后台商品中心接口。
- 修改 `backend/src/main/java/com/jingdong/backend/service/AdminService.java`：委托商品中心服务和审计。
- 修改 `backend/src/main/java/com/jingdong/backend/store/DatabaseStore.java`：用户端可见性、购物车/下单兼容、旧接口兼容。
- 修改 `backend/src/main/java/com/jingdong/backend/api/ErrorCode.java`：新增必要业务错误码。
- 新增或修改 `backend/src/test/java/com/jingdong/backend/ProductCenterIntegrationTests.java`。
- 修改 `backend/src/test/java/com/jingdong/backend/ThirdStageIntegrationTests.java`：确认后台权限和交易兼容。

### 后台新增或修改

- 修改 `admin/src/types/domain.ts`：新增商品中心类型和请求类型。
- 修改 `admin/src/api/admin.ts`：新增商品中心 API 函数。
- 修改 `admin/src/router/index.ts`：保留 `/products`，页面升级为商品中心。
- 修改 `admin/src/views/ProductsView.vue`：改为商品中心工作台。
- 新增 `admin/src/views/product-center/ProductListPanel.vue`。
- 新增 `admin/src/views/product-center/ProductEditorDrawer.vue`。
- 新增 `admin/src/views/product-center/CategoryPanel.vue`。
- 新增 `admin/src/views/product-center/BrandPanel.vue`。
- 新增 `admin/src/views/product-center/SpecPanel.vue`。
- 新增 `admin/src/views/product-center/productCenterState.ts`。

### 用户端新增或修改

- 修改 `frontend/src/types/domain.ts`：新增品牌、SPU、SKU、规格、商品详情、购物车快照类型。
- 修改 `frontend/src/services/catalog.ts`：新增商品搜索和商品详情接口。
- 修改 `frontend/src/stores/catalog.ts`：新增商品搜索、详情缓存和加载状态。
- 修改 `frontend/src/router/index.ts`：新增 `/products/:id`。
- 新增 `frontend/src/views/product/ProductDetail.vue`。
- 修改 `frontend/src/views/search/SearchList.vue`：商品/商家搜索结果。
- 修改 `frontend/src/views/merchant/MerchantDetail.vue`：SPU 商品卡、规格选择入口。
- 修改 `frontend/src/stores/cart.ts`：以 `skuId` 为主、兼容 `productId`。
- 修改 `frontend/src/views/cart/CartView.vue`：显示品牌和规格快照。
- 修改 `frontend/src/views/checkout/CheckoutView.vue`：提交 `skuId` 和兼容 `productId`。
- 修改 `frontend/src/services/mock/server.ts`：mock 数据兼容新类型，保证开发兜底不崩。

---

## Task 1: 后端数据模型、迁移和 DTO 契约

**Files:**
- Modify: `backend/src/main/resources/db/schema.sql`
- Modify: `backend/src/main/resources/db/data.sql`
- Modify: `backend/src/main/java/com/jingdong/backend/config/DatabaseMigrationRunner.java`
- Modify: `backend/src/main/java/com/jingdong/backend/entity/DataEntities.java`
- Create: `backend/src/main/java/com/jingdong/backend/mapper/BrandMapper.java`
- Create: `backend/src/main/java/com/jingdong/backend/mapper/SpecGroupMapper.java`
- Create: `backend/src/main/java/com/jingdong/backend/mapper/SpecOptionMapper.java`
- Create: `backend/src/main/java/com/jingdong/backend/mapper/ProductSpuMapper.java`
- Create: `backend/src/main/java/com/jingdong/backend/dto/product/ProductDtos.java`
- Modify: `backend/src/main/java/com/jingdong/backend/dto/admin/AdminDtos.java`
- Modify: `backend/src/main/java/com/jingdong/backend/api/ErrorCode.java`

- [ ] **Step 1: 扩展 `schema.sql`**

在 `categories` 表增加字段：

```sql
parent_id VARCHAR(64) NULL,
level INT NOT NULL DEFAULT 1,
type VARCHAR(40) NOT NULL DEFAULT 'CHANNEL',
status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE',
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
```

新增表：

```sql
CREATE TABLE IF NOT EXISTS brands (
  id VARCHAR(64) PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  logo VARCHAR(255) NOT NULL DEFAULT '',
  description VARCHAR(255) NOT NULL DEFAULT '',
  status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE',
  sort_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_brands_name (name),
  INDEX idx_brands_status_sort (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS spec_groups (
  id VARCHAR(64) PRIMARY KEY,
  name VARCHAR(80) NOT NULL,
  status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE',
  sort_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_spec_groups_name (name),
  INDEX idx_spec_groups_status_sort (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS spec_options (
  id VARCHAR(64) PRIMARY KEY,
  group_id VARCHAR(64) NOT NULL,
  name VARCHAR(80) NOT NULL,
  status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE',
  sort_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_spec_options_group FOREIGN KEY (group_id) REFERENCES spec_groups(id) ON DELETE CASCADE,
  UNIQUE KEY uk_spec_options_group_name (group_id, name),
  INDEX idx_spec_options_group_sort (group_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS product_spus (
  id VARCHAR(64) PRIMARY KEY,
  merchant_id VARCHAR(64) NOT NULL,
  category_id VARCHAR(64) NOT NULL,
  brand_id VARCHAR(64) NULL,
  name VARCHAR(180) NOT NULL,
  subtitle VARCHAR(255) NOT NULL DEFAULT '',
  main_image VARCHAR(255) NOT NULL DEFAULT '',
  detail VARCHAR(1000) NOT NULL DEFAULT '',
  detail_images_json TEXT NOT NULL,
  status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
  sort_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_product_spus_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id) ON DELETE CASCADE,
  INDEX idx_product_spus_merchant (merchant_id),
  INDEX idx_product_spus_category (category_id),
  INDEX idx_product_spus_brand (brand_id),
  INDEX idx_product_spus_status_sort (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

在 `products` 表增加字段：

```sql
spu_id VARCHAR(64) NULL,
brand_id VARCHAR(64) NULL,
sku_code VARCHAR(80) NOT NULL DEFAULT '',
specs_json TEXT NOT NULL,
main_image VARCHAR(255) NOT NULL DEFAULT '',
INDEX idx_products_spu_id (spu_id),
INDEX idx_products_status_sort (status, sort_order)
```

- [ ] **Step 2: 扩展 `DatabaseMigrationRunner`**

添加启动迁移：

```java
addColumn("categories", "parent_id", "VARCHAR(64) NULL");
addColumn("categories", "level", "INT NOT NULL DEFAULT 1");
addColumn("categories", "type", "VARCHAR(40) NOT NULL DEFAULT 'CHANNEL'");
addColumn("categories", "status", "VARCHAR(40) NOT NULL DEFAULT 'ACTIVE'");
addColumn("categories", "created_at", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
addColumn("categories", "updated_at", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
addColumn("products", "spu_id", "VARCHAR(64) NULL");
addColumn("products", "brand_id", "VARCHAR(64) NULL");
addColumn("products", "sku_code", "VARCHAR(80) NOT NULL DEFAULT ''");
addColumn("products", "specs_json", "TEXT NOT NULL");
addColumn("products", "main_image", "VARCHAR(255) NOT NULL DEFAULT ''");
createProductCenterTables();
seedProductCenterDictionaries();
migrateLegacyProductsToSpu();
```

新增私有方法：

```java
private void createProductCenterTables() {
  jdbcTemplate.execute("""
      create table if not exists brands (
        id varchar(64) primary key,
        name varchar(120) not null,
        logo varchar(255) not null default '',
        description varchar(255) not null default '',
        status varchar(40) not null default 'ACTIVE',
        sort_order int not null default 0,
        created_at timestamp not null default current_timestamp,
        updated_at timestamp not null default current_timestamp on update current_timestamp,
        unique key uk_brands_name (name),
        index idx_brands_status_sort (status, sort_order)
      ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci
      """);
  jdbcTemplate.execute("""
      create table if not exists spec_groups (
        id varchar(64) primary key,
        name varchar(80) not null,
        status varchar(40) not null default 'ACTIVE',
        sort_order int not null default 0,
        created_at timestamp not null default current_timestamp,
        updated_at timestamp not null default current_timestamp on update current_timestamp,
        unique key uk_spec_groups_name (name),
        index idx_spec_groups_status_sort (status, sort_order)
      ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci
      """);
  jdbcTemplate.execute("""
      create table if not exists spec_options (
        id varchar(64) primary key,
        group_id varchar(64) not null,
        name varchar(80) not null,
        status varchar(40) not null default 'ACTIVE',
        sort_order int not null default 0,
        created_at timestamp not null default current_timestamp,
        updated_at timestamp not null default current_timestamp on update current_timestamp,
        constraint fk_spec_options_group foreign key (group_id) references spec_groups(id) on delete cascade,
        unique key uk_spec_options_group_name (group_id, name),
        index idx_spec_options_group_sort (group_id, sort_order)
      ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci
      """);
  jdbcTemplate.execute("""
      create table if not exists product_spus (
        id varchar(64) primary key,
        merchant_id varchar(64) not null,
        category_id varchar(64) not null,
        brand_id varchar(64) null,
        name varchar(180) not null,
        subtitle varchar(255) not null default '',
        main_image varchar(255) not null default '',
        detail varchar(1000) not null default '',
        detail_images_json text not null,
        status varchar(40) not null default 'DRAFT',
        sort_order int not null default 0,
        created_at timestamp not null default current_timestamp,
        updated_at timestamp not null default current_timestamp on update current_timestamp,
        constraint fk_product_spus_merchant foreign key (merchant_id) references merchants(id) on delete cascade,
        index idx_product_spus_merchant (merchant_id),
        index idx_product_spus_category (category_id),
        index idx_product_spus_brand (brand_id),
        index idx_product_spus_status_sort (status, sort_order)
      ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci
      """);
}

private void seedProductCenterDictionaries() {
  jdbcTemplate.update("""
      insert ignore into brands (id, name, logo, description, status, sort_order)
      values ('brand_jd', '京东自营', '', '平台自营品牌', 'ACTIVE', 10)
      """);
  jdbcTemplate.update("""
      insert ignore into spec_groups (id, name, status, sort_order)
      values ('spec_capacity', '容量', 'ACTIVE', 10)
      """);
  jdbcTemplate.update("""
      insert ignore into spec_options (id, group_id, name, status, sort_order)
      values ('spec_capacity_default', 'spec_capacity', '标准装', 'ACTIVE', 10)
      """);
}

private void migrateLegacyProductsToSpu() {
  List<Map<String, Object>> products = jdbcTemplate.queryForList("""
      select id, merchant_id, category_id, name, description, image_text, status, sort_order
      from products
      where spu_id is null or spu_id = ''
      """);
  for (Map<String, Object> product : products) {
    String productId = (String) product.get("id");
    String spuId = "spu_" + productId;
    jdbcTemplate.update("""
        insert ignore into product_spus (
          id, merchant_id, category_id, brand_id, name, subtitle, main_image,
          detail, detail_images_json, status, sort_order
        )
        values (?, ?, ?, 'brand_jd', ?, ?, ?, ?, '[]', ?, ?)
        """,
        spuId,
        product.get("merchant_id"),
        product.get("category_id"),
        product.get("name"),
        product.get("description"),
        product.get("image_text"),
        product.get("description"),
        product.get("status"),
        product.get("sort_order")
    );
    jdbcTemplate.update("""
        update products
        set spu_id = ?, brand_id = 'brand_jd', sku_code = ?, specs_json = ?,
            main_image = case when main_image = '' then image_text else main_image end
        where id = ?
        """,
        spuId,
        productId,
        "[{\"groupId\":\"spec_capacity\",\"groupName\":\"容量\",\"optionId\":\"spec_capacity_default\",\"optionName\":\"标准装\"}]",
        productId
    );
  }
}
```

- [ ] **Step 3: 新增实体和 Mapper**

在 `DataEntities.java` 新增实体。每个字段都要提供 getter 和 setter，命名沿用 MyBatis-Plus 驼峰映射：

```java
@TableName("brands")
public static class BrandEntity {
  @TableId
  private String id;
  private String name;
  private String logo;
  private String description;
  private String status;
  private Integer sortOrder;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}

@TableName("spec_groups")
public static class SpecGroupEntity {
  @TableId
  private String id;
  private String name;
  private String status;
  private Integer sortOrder;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}

@TableName("spec_options")
public static class SpecOptionEntity {
  @TableId
  private String id;
  private String groupId;
  private String name;
  private String status;
  private Integer sortOrder;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}

@TableName("product_spus")
public static class ProductSpuEntity {
  @TableId
  private String id;
  private String merchantId;
  private String categoryId;
  private String brandId;
  private String name;
  private String subtitle;
  private String mainImage;
  private String detail;
  private String detailImagesJson;
  private String status;
  private Integer sortOrder;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
```

同时扩展：

```java
CategoryEntity: parentId, level, type, status, createdAt, updatedAt
ProductEntity: spuId, brandId, skuCode, specsJson, mainImage
```

新增 Mapper：

```java
public interface BrandMapper extends BaseMapper<BrandEntity> {}
public interface SpecGroupMapper extends BaseMapper<SpecGroupEntity> {}
public interface SpecOptionMapper extends BaseMapper<SpecOptionEntity> {}
public interface ProductSpuMapper extends BaseMapper<ProductSpuEntity> {}
```

- [ ] **Step 4: 定义 DTO**

创建 `ProductDtos.java`，定义以下 record：

```java
public record BrandResponse(String id, String name, String logo, String description) {}
public record SpecOptionResponse(String id, String groupId, String groupName, String name) {}
public record SkuSpecResponse(String groupId, String groupName, String optionId, String optionName) {}
public record ProductSkuResponse(
    String skuId,
    String productId,
    String spuId,
    String skuCode,
    List<SkuSpecResponse> specs,
    String specText,
    BigDecimal price,
    BigDecimal originalPrice,
    String unit,
    int stock,
    String status
) {}
public record ProductCardResponse(
    String id,
    String spuId,
    String skuId,
    String merchantId,
    String merchantName,
    String categoryId,
    String brandId,
    String brandName,
    String name,
    String subtitle,
    int sales,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    BigDecimal originalPrice,
    String imageText,
    String mainImage,
    String unit,
    String description,
    int stock,
    boolean singleSku
) {}
public record ProductDetailResponse(
    ProductCardResponse product,
    List<ProductSkuResponse> skus,
    List<String> detailImages,
    String detail
) {}
```

扩展 `AdminDtos.java`，定义以下 record：

```java
public record CategoryAdminResponse(
    String id, String name, String icon, String parentId, int level,
    String type, String status, int sortOrder
) {}
public record CategoryUpsertRequest(
    String id, String name, String icon, String parentId, Integer level,
    String type, String status, Integer sortOrder
) {}
public record BrandAdminResponse(
    String id, String name, String logo, String description, String status, int sortOrder
) {}
public record BrandUpsertRequest(
    String id, String name, String logo, String description, String status, Integer sortOrder
) {}
public record SpecOptionAdminResponse(
    String id, String groupId, String name, String status, int sortOrder
) {}
public record SpecGroupAdminResponse(
    String id, String name, String status, int sortOrder, List<SpecOptionAdminResponse> options
) {}
public record SpecGroupUpsertRequest(
    String id, String name, String status, Integer sortOrder
) {}
public record SpecOptionUpsertRequest(
    String id, String name, String status, Integer sortOrder
) {}
public record SkuSpecRequest(
    String groupId, String groupName, String optionId, String optionName
) {}
public record ProductSkuAdminResponse(
    String skuId, String productId, String spuId, String skuCode,
    List<SkuSpecRequest> specs, String specText, BigDecimal price,
    BigDecimal originalPrice, String unit, int stock, String status
) {}
public record ProductSkuUpsertRequest(
    String skuId, String skuCode, List<SkuSpecRequest> specs, BigDecimal price,
    BigDecimal originalPrice, String unit, Integer stock, String status
) {}
public record ProductSpuAdminResponse(
    String id, String merchantId, String merchantName, String categoryId,
    String categoryName, String brandId, String brandName, String name,
    String subtitle, String mainImage, String detail, List<String> detailImages,
    String status, int sortOrder, int skuCount, int totalStock,
    BigDecimal minPrice, BigDecimal maxPrice, List<ProductSkuAdminResponse> skus
) {}
public record ProductSpuUpsertRequest(
    String id, String merchantId, String categoryId, String brandId,
    String name, String subtitle, String mainImage, String detail,
    List<String> detailImages, String status, Integer sortOrder,
    List<ProductSkuUpsertRequest> skus
) {}
public record StatusUpdateRequest(String status) {}
```

- [ ] **Step 5: 编译验证**

Run:

```bash
cd backend && mvn -nsu -Drocketmq.log.root=logs/rocketmqlogs -DskipTests compile
```

Expected: 编译通过。

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/resources/db/schema.sql backend/src/main/resources/db/data.sql backend/src/main/java/com/jingdong/backend/config/DatabaseMigrationRunner.java backend/src/main/java/com/jingdong/backend/entity/DataEntities.java backend/src/main/java/com/jingdong/backend/mapper backend/src/main/java/com/jingdong/backend/dto backend/src/main/java/com/jingdong/backend/api/ErrorCode.java
git commit -m "feat: add product center data model"
```

---

## Task 2: 后端商品中心服务和后台接口

**Files:**
- Create: `backend/src/main/java/com/jingdong/backend/service/ProductCenterService.java`
- Modify: `backend/src/main/java/com/jingdong/backend/controller/AdminController.java`
- Modify: `backend/src/main/java/com/jingdong/backend/service/AdminService.java`
- Modify: `backend/src/main/java/com/jingdong/backend/store/DatabaseStore.java`
- Create: `backend/src/test/java/com/jingdong/backend/ProductCenterIntegrationTests.java`

- [ ] **Step 1: 写失败测试**

创建 `ProductCenterIntegrationTests`，覆盖：

```java
@Test
void customerCannotUseAdminProductCenterApis() {}

@Test
void adminCanCreateBrandCategorySpecSpuAndSku() {}

@Test
void productSaveRejectsDisabledMerchantOrInvalidCategory() {}

@Test
void duplicateSkuSpecCombinationIsRejected() {}

@Test
void spuCannotBeOnShelfWithoutOnShelfSku() {}
```

测试必须使用 `TestRestTemplate` 走 HTTP，并复用已有登录方式获取管理员和普通用户 token。

- [ ] **Step 2: 运行测试确认失败**

Run:

```bash
cd backend && mvn -nsu -Drocketmq.log.root=logs/rocketmqlogs -Dtest=ProductCenterIntegrationTests test
```

Expected: 失败，原因是接口或服务尚未实现。

- [ ] **Step 3: 实现 `ProductCenterService`**

服务职责：

- 查询和保存类目。
- 查询和保存品牌。
- 查询和保存规格组、规格值。
- 查询、保存、上下架 SPU。
- 查询、保存、上下架 SKU。
- 旧 `ProductUpsertRequest` 转换成单 SPU + 单 SKU。
- 写审计日志。

关键校验方法：

```java
private void ensureActiveMerchant(String merchantId)
private void ensureActiveCategory(String categoryId)
private void ensureActiveBrand(String brandId)
private void ensureValidMoney(BigDecimal price, BigDecimal originalPrice)
private void ensureValidStock(Integer stock)
private void ensureNoDuplicateSkuSpecs(String spuId, String skuId, List<SkuSpecRequest> specs)
private void ensureSpuCanBeOnShelf(String spuId)
```

状态常量使用私有静态字符串：

```java
private static final String ACTIVE = "ACTIVE";
private static final String ON_SHELF = "ON_SHELF";
private static final String OFF_SHELF = "OFF_SHELF";
private static final String DRAFT = "DRAFT";
```

- [ ] **Step 4: 扩展后台 Controller**

在 `AdminController` 添加设计规格中的后台接口。状态更新统一使用：

```java
@PatchMapping("/product-spus/{spuId}/status")
public ApiResponse<ProductSpuAdminResponse> updateProductSpuStatus(
    @PathVariable String spuId,
    @RequestBody StatusUpdateRequest request
) {
  return ApiResponse.success(adminService.updateProductSpuStatus(spuId, request.status()));
}
```

SKU 状态接口同理。

- [ ] **Step 5: 保留旧接口兼容**

`AdminService.saveProduct(ProductUpsertRequest request)` 改为委托 `ProductCenterService.saveLegacyProduct(request)`，返回旧 `ProductAdminResponse`。

`DatabaseStore.adminProducts()` 可以继续返回旧列表，但需要包含从 SPU/品牌补齐后的字段；扩展后的 DTO 必须保持旧字段可用。

- [ ] **Step 6: 测试通过**

Run:

```bash
cd backend && mvn -nsu -Drocketmq.log.root=logs/rocketmqlogs -Dtest=ProductCenterIntegrationTests test
```

Expected: `ProductCenterIntegrationTests` 通过。

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/jingdong/backend/service/ProductCenterService.java backend/src/main/java/com/jingdong/backend/controller/AdminController.java backend/src/main/java/com/jingdong/backend/service/AdminService.java backend/src/main/java/com/jingdong/backend/store/DatabaseStore.java backend/src/test/java/com/jingdong/backend/ProductCenterIntegrationTests.java
git commit -m "feat: add admin product center APIs"
```

---

## Task 3: 后端用户端商品搜索、详情和交易兼容

**Files:**
- Create: `backend/src/main/java/com/jingdong/backend/controller/ProductController.java`
- Modify: `backend/src/main/java/com/jingdong/backend/service/CatalogService.java`
- Modify: `backend/src/main/java/com/jingdong/backend/store/DatabaseStore.java`
- Modify: `backend/src/main/java/com/jingdong/backend/dto/merchant/MerchantDtos.java`
- Modify: `backend/src/main/java/com/jingdong/backend/dto/cart/CartDtos.java`
- Modify: `backend/src/main/java/com/jingdong/backend/dto/order/OrderDtos.java`
- Modify: `backend/src/test/java/com/jingdong/backend/ProductCenterIntegrationTests.java`
- Modify: `backend/src/test/java/com/jingdong/backend/ThirdStageIntegrationTests.java`

- [ ] **Step 1: 写失败测试**

在 `ProductCenterIntegrationTests` 增加：

```java
@Test
void productSearchOnlyReturnsVisibleProducts() {}

@Test
void productDetailReturnsSkuSpecsAndBrand() {}

@Test
void merchantDetailReturnsProductCardsWithoutDisabledProducts() {}

@Test
void cartAndCheckoutAcceptSkuIdWithProductIdCompatibility() {}
```

- [ ] **Step 2: 实现用户端 Controller**

新增：

```java
@RestController
@RequestMapping("/products")
public class ProductController {
  private final ProductCenterService productCenterService;

  public ProductController(ProductCenterService productCenterService) {
    this.productCenterService = productCenterService;
  }

  @GetMapping("/search")
  public ApiResponse<List<ProductCardResponse>> search(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String merchantId,
      @RequestParam(required = false) String categoryId,
      @RequestParam(required = false) String brandId
  ) {
    return ApiResponse.success(productCenterService.searchProducts(keyword, merchantId, categoryId, brandId));
  }

  @GetMapping("/{productId}")
  public ApiResponse<ProductDetailResponse> detail(@PathVariable String productId) {
    return ApiResponse.success(productCenterService.productDetail(productId));
  }
}
```

- [ ] **Step 3: 调整可见性查询**

`/home` 只返回 `ACTIVE` 商家。

`/merchants/search` 商品命中必须过滤：

```java
merchant.status == ACTIVE
spu.status == ON_SHELF
sku.status == ON_SHELF
```

`/merchants/{merchantId}` 返回 `ProductCardResponse` 列表，过滤下架 SPU/SKU。

- [ ] **Step 4: 调整购物车和结算 DTO**

`CartDtos` 中购物车行输出新增：

```java
String skuId,
String productId,
String spuId,
String brandName,
String productName,
String specText,
String status
```

`OrderDtos.CheckoutItemRequest` 新增：

```java
String skuId
```

后端读取逻辑：

```java
String purchasableId = request.skuId() != null && !request.skuId().isBlank()
    ? request.skuId()
    : request.productId();
```

- [ ] **Step 5: 确保下架 SKU 不可加购和下单**

`DatabaseStore.product(String productId)` 保持 SKU 可购买实体校验。库存为 0 时允许商品详情展示，但下单抛 `PRODUCT_STOCK_LOW`。

- [ ] **Step 6: 运行后端完整测试**

Run:

```bash
cd backend && mvn -nsu -Drocketmq.log.root=logs/rocketmqlogs clean test
```

Expected: 全部后端测试通过。

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/jingdong/backend/controller/ProductController.java backend/src/main/java/com/jingdong/backend/service/CatalogService.java backend/src/main/java/com/jingdong/backend/store/DatabaseStore.java backend/src/main/java/com/jingdong/backend/dto backend/src/test/java/com/jingdong/backend
git commit -m "feat: add customer product catalog APIs"
```

---

## Task 4: 后台商品中心工作台

**Files:**
- Modify: `admin/src/types/domain.ts`
- Modify: `admin/src/api/admin.ts`
- Modify: `admin/src/views/ProductsView.vue`
- Create: `admin/src/views/product-center/productCenterState.ts`
- Create: `admin/src/views/product-center/ProductListPanel.vue`
- Create: `admin/src/views/product-center/ProductEditorDrawer.vue`
- Create: `admin/src/views/product-center/CategoryPanel.vue`
- Create: `admin/src/views/product-center/BrandPanel.vue`
- Create: `admin/src/views/product-center/SpecPanel.vue`

- [ ] **Step 1: 更新 TypeScript 类型**

在 `domain.ts` 增加：

```ts
export type CommonStatus = 'ACTIVE' | 'DISABLED'
export type ProductStatus = 'DRAFT' | 'ON_SHELF' | 'OFF_SHELF'

export interface Category { id: string; name: string; icon: string; parentId?: string | null; level: number; type: 'CHANNEL' | 'PRODUCT'; status: CommonStatus; sortOrder: number }
export interface Brand { id: string; name: string; logo: string; description: string; status: CommonStatus; sortOrder: number }
export interface SpecOption { id: string; groupId: string; name: string; status: CommonStatus; sortOrder: number }
export interface SpecGroup { id: string; name: string; status: CommonStatus; sortOrder: number; options: SpecOption[] }
export interface SkuSpec { groupId: string; groupName: string; optionId: string; optionName: string }
export interface ProductSku { skuId: string; productId: string; spuId: string; skuCode: string; specs: SkuSpec[]; specText: string; price: number; originalPrice: number; unit: string; stock: number; status: 'ON_SHELF' | 'OFF_SHELF' }
export interface ProductSpu { id: string; merchantId: string; merchantName: string; categoryId: string; categoryName: string; brandId?: string | null; brandName?: string | null; name: string; subtitle: string; mainImage: string; detail: string; detailImages: string[]; status: ProductStatus; sortOrder: number; skuCount: number; totalStock: number; minPrice: number; maxPrice: number; skus: ProductSku[] }
```

新增请求类型，不再用 `Partial<Product>` 提交商品中心数据。

- [ ] **Step 2: 更新 API 函数**

在 `admin.ts` 增加：

```ts
export function getCategories() {}
export function saveCategory(payload: CategoryUpsertRequest) {}
export function updateCategoryStatus(categoryId: string, status: CommonStatus) {}
export function getBrands() {}
export function saveBrand(payload: BrandUpsertRequest) {}
export function updateBrandStatus(brandId: string, status: CommonStatus) {}
export function getSpecGroups() {}
export function saveSpecGroup(payload: SpecGroupUpsertRequest) {}
export function saveSpecOption(groupId: string, payload: SpecOptionUpsertRequest) {}
export function updateSpecOptionStatus(optionId: string, status: CommonStatus) {}
export function getProductSpus() {}
export function getProductSpuDetail(spuId: string) {}
export function saveProductSpu(payload: ProductSpuUpsertRequest) {}
export function updateProductSpuStatus(spuId: string, status: ProductStatus) {}
export function saveProductSku(spuId: string, payload: ProductSkuUpsertRequest) {}
export function updateProductSkuStatus(spuId: string, skuId: string, status: 'ON_SHELF' | 'OFF_SHELF') {}
```

- [ ] **Step 3: 拆分商品中心组件**

`ProductsView.vue` 只负责：

- 加载公共数据。
- 维护当前标签页。
- 组合 `ProductListPanel`、`CategoryPanel`、`BrandPanel`、`SpecPanel`。
- 打开 `ProductEditorDrawer`。

`ProductEditorDrawer` 必须包含基础信息、展示信息、SKU 信息三个区块。

- [ ] **Step 4: 表单校验**

后台校验必须包括：

```ts
商品名必填
商家必选
类目必选
价格 >= 0
原价 >= 价格
库存为整数且 >= 0
SKU 规格组合不可重复
上架 SPU 至少有一个上架 SKU
```

- [ ] **Step 5: 构建后台**

Run:

```bash
cd admin && pnpm build
```

Expected: `vue-tsc --noEmit` 和 `vite build` 成功。

- [ ] **Step 6: Commit**

```bash
git add admin/src/types/domain.ts admin/src/api/admin.ts admin/src/views/ProductsView.vue admin/src/views/product-center
git commit -m "feat: build admin product center workspace"
```

---

## Task 5: 用户端商品搜索、商品详情和购物车快照

**Files:**
- Modify: `frontend/src/types/domain.ts`
- Modify: `frontend/src/services/catalog.ts`
- Modify: `frontend/src/stores/catalog.ts`
- Modify: `frontend/src/router/index.ts`
- Create: `frontend/src/views/product/ProductDetail.vue`
- Modify: `frontend/src/views/search/SearchList.vue`
- Modify: `frontend/src/views/merchant/MerchantDetail.vue`
- Modify: `frontend/src/stores/cart.ts`
- Modify: `frontend/src/views/cart/CartView.vue`
- Modify: `frontend/src/views/checkout/CheckoutView.vue`
- Modify: `frontend/src/services/mock/server.ts`

- [ ] **Step 1: 更新用户端类型**

`domain.ts` 新增：

```ts
export interface Brand { id: string; name: string; logo: string; description: string }
export interface SkuSpec { groupId: string; groupName: string; optionId: string; optionName: string }
export interface ProductSku { skuId: string; productId: string; spuId: string; skuCode: string; specs: SkuSpec[]; specText: string; price: number; originalPrice: number; unit: string; stock: number; status: 'ON_SHELF' | 'OFF_SHELF' }
export interface ProductCard { id: string; spuId: string; skuId?: string | null; merchantId: string; merchantName: string; categoryId: string; brandId?: string | null; brandName?: string | null; name: string; subtitle: string; sales: number; minPrice: number; maxPrice: number; originalPrice?: number; imageText: string; mainImage: string; unit: string; description: string; stock: number; singleSku: boolean }
export interface ProductDetail { product: ProductCard; skus: ProductSku[]; detailImages: string[]; detail: string }
```

`CartItem` 改为快照，不再 `extends Product`。

- [ ] **Step 2: 更新 catalog service/store**

新增：

```ts
searchProducts(keyword: string, filters?: { merchantId?: string; categoryId?: string; brandId?: string })
getProductDetail(productId: string)
```

store 新增：

```ts
products
productDetails
productSearchLoading
productDetailLoading
searchProducts()
loadProductDetail()
```

- [ ] **Step 3: 新增路由和商品详情页**

路由：

```ts
{
  path: '/products/:id',
  name: 'ProductDetail',
  component: () => import('@/views/product/ProductDetail.vue'),
  meta: { requiresAuth: true },
}
```

详情页交互：

- 展示主图、品牌、商品名、副标题、价格。
- 展示规格组，点击规格值选中 SKU。
- 选中 SKU 后可加购。
- 单 SKU 默认选中。
- 库存为 0 时按钮禁用并显示“暂时缺货”。

- [ ] **Step 4: 改造搜索和商家详情**

`SearchList.vue`：

- 同时加载商品和商家结果。
- 商品结果优先展示。
- 商家结果保留。

`MerchantDetail.vue`：

- 商品卡点击进入商品详情。
- 单 SKU 商品保留快捷加购。
- 多 SKU 商品按钮显示“选规格”，点击进入详情页。

- [ ] **Step 5: 改造购物车和结算**

`cart.ts`：

```ts
async function addSkuToCart(skuId: string) {
  return syncCart(() => cartService.addCartItem(skuId))
}
```

保留：

```ts
async function addToCart(product: ProductCard | ProductSku) {
  const skuId = 'skuId' in product && product.skuId ? product.skuId : product.productId
  return addSkuToCart(skuId)
}
```

`CheckoutView.vue` 提交：

```ts
items: checkedItems.value.map((item) => ({
  productId: item.productId,
  skuId: item.skuId,
  quantity: item.quantity,
}))
```

- [ ] **Step 6: 构建用户端**

Run:

```bash
cd frontend && pnpm build
```

Expected: `vite build` 成功。

- [ ] **Step 7: Commit**

```bash
git add frontend/src/types/domain.ts frontend/src/services/catalog.ts frontend/src/stores/catalog.ts frontend/src/router/index.ts frontend/src/views/product frontend/src/views/search/SearchList.vue frontend/src/views/merchant/MerchantDetail.vue frontend/src/stores/cart.ts frontend/src/views/cart/CartView.vue frontend/src/views/checkout/CheckoutView.vue frontend/src/services/mock/server.ts
git commit -m "feat: add customer product discovery flow"
```

---

## Task 6: 三端联调验证和文档收口

**Files:**
- Modify: `backend/docs/api-spec.md`
- Modify: `backend/docs/implementation-plan.md`
- Modify: `docs/superpowers/context/2026-04-25-main-session-context.md`

- [ ] **Step 1: 更新接口文档**

`backend/docs/api-spec.md` 增加：

- 商品搜索接口。
- 商品详情接口。
- 后台类目接口。
- 后台品牌接口。
- 后台规格接口。
- 后台 SPU/SKU 接口。
- 购物车/结算 `skuId` 兼容规则。

- [ ] **Step 2: 更新实施说明**

`backend/docs/implementation-plan.md` 增加商品中心已落地范围和库存中心剩余边界。

- [ ] **Step 3: 更新主会话上下文**

`docs/superpowers/context/2026-04-25-main-session-context.md` 增加商品中心完成状态、分支名、验证命令。

- [ ] **Step 4: 运行完整验证**

Run:

```bash
cd backend && mvn -nsu -Drocketmq.log.root=logs/rocketmqlogs clean test
```

Run:

```bash
cd admin && pnpm build
```

Run:

```bash
cd frontend && pnpm build
```

Expected: 三个命令全部成功。

- [ ] **Step 5: HTTP 冒烟**

启动后端后至少验证：

```text
POST /api/auth/login 管理员登录成功
GET /api/admin/product-spus 管理员成功
GET /api/admin/product-spus 普通用户 403
GET /api/products/search?keyword=水 成功返回商品
GET /api/products/{id} 成功返回商品详情
POST /api/cart/items 使用 skuId 成功加购
```

- [ ] **Step 6: Commit**

```bash
git add backend/docs/api-spec.md backend/docs/implementation-plan.md docs/superpowers/context/2026-04-25-main-session-context.md
git commit -m "docs: update product center documentation"
```

---

## 完成标准

- 后端完整测试通过。
- 后台构建通过。
- 用户端构建通过。
- 商品中心功能分支至少包含上述 6 个任务提交。
- 每个任务经过规格审查和代码质量审查。
- 最终审查确认实现符合设计规格。
- 合并回 `main` 时保留 `codex/product-center-20260425-1` 分支，不删除。
