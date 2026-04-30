# 商品中心设计规格

## 背景

当前系统已经具备基础商品能力：用户端可以查看首页、搜索商家、进入商家详情并把商品加入购物车；后台可以维护商家和单层商品；后端已有 `categories`、`merchants`、`merchant_categories`、`products` 等表。但这些能力仍然是“商家 + 单 SKU 商品”的轻量模型，不足以支撑大型电商系统的商品中心。

本阶段采用方案 C：后端、运营后台、用户端一起改造。目标是形成可管理、可展示、可购买的商品主数据闭环，并为下一阶段库存中心预留清晰边界。

## 目标

1. 建立商品中心基础模型：类目、品牌、规格组、规格值、SPU、SKU。
2. 后台支持商品资料、品牌、规格、SPU/SKU、详情内容和上下架管理。
3. 用户端支持商品搜索、商品详情页、规格选择、购物车和结算快照展示。
4. 保持现有购物车、结算、订单链路兼容，避免一次改造打断交易主流程。
5. 所有商品关键操作写入审计日志，并继续受 `/admin/**` 权限保护。

## 非目标

以下能力不放入本阶段，留给后续中心：

- 库存中心：锁定库存、库存流水、仓库、批次、库存预占、并发条件扣减、库存回滚幂等标记。
- 促销中心：营销价、优惠券、满减、秒杀、会员价。
- 搜索中心：ES、复杂相关性排序、拼写纠错、联想词。
- 内容中心：富文本编辑器、视频、复杂素材库。
- 商家治理：入驻审核、资质管理、品牌授权审批。

## 现状摘要

### 后端

- `server.servlet.context-path=/api`，所有接口实际以 `/api` 为前缀。
- `schema.sql` 已有 `categories`、`merchants`、`merchant_categories`、`products`。
- `DatabaseMigrationRunner` 是当前轻量启动迁移入口；项目规则禁止引入 Flyway。
- `products.id` 当前贯穿用户端展示、购物车、下单和订单快照。
- `/home` 当前没有过滤非启用商家。
- `/merchants/search` 只返回商家，且搜索商品时未过滤下架商品。
- `/merchants/{merchantId}` 会过滤启用商家和上架商品。
- 后台 `ProductUpsertRequest` 是单商品结构，缺少品牌、SPU、SKU、规格和详情字段。

### 后台端

- `ProductsView.vue` 当前用一个弹窗承载商品新增编辑。
- `categoryId` 是手输字符串。
- 商品保存使用 `Partial<Product>`，缺少明确请求类型。
- 没有类目、品牌、规格、SPU/SKU 管理入口。

### 用户端

- `Product` 是扁平模型，`CartItem extends Product`。
- `CheckoutItem` 只有 `productId` 和 `quantity`。
- 搜索入口文案包含商品，但结果页只展示商家。
- 没有商品详情页。
- 商家详情页直接展示商品列表并用 `product.id` 加购。

## 核心设计

### 领域模型

本阶段采用“兼容式 SPU/SKU”：

- `product_spus` 表示商品公共信息。
- `products` 表继续作为可购买 SKU 表，保留原表名和原主键，减少交易链路迁移风险。
- `products.id` 在兼容期仍可被旧字段称为 `productId`，新代码语义上把它视为 `skuId`。
- 用户端和后台新增类型时使用 `skuId`；对外请求短期同时接受 `skuId` 和 `productId`。

### 类目

现有类目有两层概念：

- 首页频道类目：面向用户首页频道展示。
- 商家内商品类目：面向商家详情和商品归类。

本阶段先不拆表。通过字段和接口明确用途：

- `categories` 增加 `parent_id`、`level`、`type`、`status`、`created_at`、`updated_at`。
- `type=CHANNEL` 表示首页频道类目。
- `type=PRODUCT` 表示商品类目。
- `merchant_categories` 继续表达商家内展示类目，但保存商品时必须校验 `categoryId` 属于对应商家的类目，或属于全局商品类目并能映射到商家类目。

### 品牌

新增 `brands` 表：

- `id`
- `name`
- `logo`
- `description`
- `status`
- `sort_order`
- `created_at`
- `updated_at`

商品 SPU 可以选择品牌。品牌在第一期不是强制字段，但后台列表、用户商品卡和详情页都展示品牌名。

### 规格

新增两类表：

- `spec_groups`：规格组，例如“容量”“颜色”“口味”。
- `spec_options`：规格值，例如“500ml”“红色”“原味”。

规格组和规格值是全局字典。SKU 保存规格组合时，使用 JSON 快照保存选中的规格项，避免后续字典改名影响订单历史和购物车展示。

### SPU

新增 `product_spus` 表：

- `id`
- `merchant_id`
- `category_id`
- `brand_id`
- `name`
- `subtitle`
- `main_image`
- `detail`
- `detail_images_json`
- `status`
- `sort_order`
- `created_at`
- `updated_at`

SPU 状态：

- `DRAFT`：草稿，不对用户可见。
- `ON_SHELF`：上架，至少存在一个上架 SKU 才可对用户展示。
- `OFF_SHELF`：下架，不对用户可见。

### SKU

继续使用 `products` 表作为 SKU 表，并轻量补列：

- `spu_id`
- `brand_id`
- `sku_code`
- `specs_json`
- `main_image`
- `status`
- `sort_order`
- `updated_at`

保留原字段：

- `merchant_id`
- `category_id`
- `name`
- `price`
- `original_price`
- `image_text`
- `unit`
- `description`
- `stock`
- `sales`

兼容策略：

- 单 SKU 商品可以自动创建一个 SPU 和一个 SKU。
- 老数据启动时通过轻量迁移补齐 SPU，保持原 `products.id` 不变。
- 购物车和订单继续引用 `products.id`，但新 DTO 同时输出 `skuId`。

## 后端接口设计

### 用户端接口

新增：

- `GET /products/search?keyword=&merchantId=&categoryId=&brandId=`
  - 返回商品搜索结果，商品优先，过滤 `ON_SHELF` SPU/SKU、`ACTIVE` 商家。
- `GET /products/{productId}`
  - 返回商品详情。`productId` 可以是 `spuId` 或 `skuId`，后端优先按 SPU 查询，未命中再按 SKU 查询。

调整：

- `GET /home`
  - 只返回 `ACTIVE` 商家。
- `GET /merchants/search`
  - 商家命中仍返回商家；商品命中只使用上架 SKU 和上架 SPU。
- `GET /merchants/{merchantId}`
  - 返回 SPU 商品卡，而不是强制返回完整 SKU 详情。
  - 单 SKU 商品卡可直接加购。
  - 多 SKU 商品卡提示用户选规格。

### 后台接口

新增字典接口：

- `GET /admin/categories`
- `POST /admin/categories`
- `PUT /admin/categories/{categoryId}`
- `PATCH /admin/categories/{categoryId}/status`
- `GET /admin/brands`
- `POST /admin/brands`
- `PUT /admin/brands/{brandId}`
- `PATCH /admin/brands/{brandId}/status`
- `GET /admin/spec-groups`
- `POST /admin/spec-groups`
- `PUT /admin/spec-groups/{groupId}`
- `POST /admin/spec-groups/{groupId}/options`
- `PUT /admin/spec-options/{optionId}`
- `PATCH /admin/spec-options/{optionId}/status`

新增商品接口：

- `GET /admin/product-spus`
- `GET /admin/product-spus/{spuId}`
- `POST /admin/product-spus`
- `PUT /admin/product-spus/{spuId}`
- `PATCH /admin/product-spus/{spuId}/status`
- `POST /admin/product-spus/{spuId}/skus`
- `PUT /admin/product-spus/{spuId}/skus/{skuId}`
- `PATCH /admin/product-spus/{spuId}/skus/{skuId}/status`

兼容保留：

- `GET /admin/products`
- `POST /admin/products`
- `PUT /admin/products/{productId}`

兼容接口内部转向 SPU/SKU 服务。旧后台页面改造完成后，新代码优先使用 SPU/SKU 接口。

## 后端校验规则

### 商品保存

- 商品名必填。
- 商家必须存在且状态为 `ACTIVE`。
- 类目必须存在，且状态为 `ACTIVE`。
- 品牌如果传入，必须存在且状态为 `ACTIVE`。
- 价格不能小于 0。
- 原价不能小于现价。
- 库存不能小于 0。
- SKU 规格组合不能重复。
- SPU 上架时必须至少有一个 `ON_SHELF` SKU。
- SKU 上架时必须有有效价格、库存、单位、名称和所属 SPU。

### 查询可见性

- 用户端只展示 `ACTIVE` 商家。
- 用户端只展示 `ON_SHELF` SPU。
- 用户端只展示 `ON_SHELF` SKU。
- 库存为 0 的 SKU 可以展示，但不能加入购物车或下单。

### 购物车兼容

- 加购接口短期继续接受 `productId`。
- DTO 同时输出 `productId` 和 `skuId`，两者在兼容期相同。
- 前端新代码使用 `skuId`，但发送请求时可以继续带 `productId`，后端按 SKU 处理。

## 后台端设计

### 路由和导航

保持现有 `products` 路由入口，但页面升级为商品中心工作台。第一期使用标签页：

- 商品列表
- 类目管理
- 品牌管理
- 规格管理

商家管理仍保留独立入口。

### 商品列表

字段：

- 商品名
- 主图或展示文案
- 商家
- 类目
- 品牌
- SKU 数
- 价格区间
- 总库存
- 状态
- 排序
- 更新时间

操作：

- 新增商品
- 编辑 SPU
- 编辑 SKU
- 上架/下架
- 查看用户端详情

### 商品编辑

使用抽屉或独立编辑面板，不再继续扩张旧弹窗。

分区：

- 基础信息：名称、商家、类目、品牌、副标题、排序、状态。
- 展示信息：主图、详情描述、详情图片。
- SKU 信息：规格组合、价格、原价、库存、单位、SKU 编码、状态。

### 字典管理

类目、品牌、规格在商品中心内部标签页完成。

类目字段：

- 名称
- 图标
- 类型
- 父级
- 状态
- 排序

品牌字段：

- 名称
- Logo
- 描述
- 状态
- 排序

规格字段：

- 规格组名称
- 规格值名称
- 状态
- 排序

## 用户端设计

### 搜索结果

`/search-list` 改为商品和商家结果并存：

- 默认展示商品结果优先。
- 商家结果保留。
- 商品卡展示品牌、商品名、最低价、销量、商家名、主图或展示文案。
- 点击商品卡进入 `/products/:id`。

### 商家详情

商家详情页继续保留左右类目布局：

- 商品卡改为 SPU 卡。
- 展示品牌、名称、副标题、最低价、销量、可售库存。
- 单 SKU 商品显示加号快捷加购。
- 多 SKU 商品显示“选规格”按钮。
- 点击商品卡进入商品详情。

### 商品详情页

新增 `/products/:id`：

- 顶部展示主图或主视觉。
- 展示品牌、商品名、副标题、价格区间或选中 SKU 价格。
- 展示规格组和规格值。
- 展示库存、销量、商家信息、详情描述。
- 选中 SKU 后允许加入购物车。
- 多 SKU 未选全规格时，按钮提示“请选择规格”。

### 购物车和结算

购物车行从“商品模型继承”调整为“购买快照”：

- `skuId`
- `productId`
- `spuId`
- `merchantId`
- `merchantName`
- `brandName`
- `productName`
- `specText`
- `imageText`
- `price`
- `quantity`
- `checked`
- `stock`
- `status`

结算提交继续传 `productId`，同时新增 `skuId` 字段；后端以 `skuId` 优先，缺失时使用 `productId`。

## 安全和风控

- 后台商品中心所有接口走 `/admin/**`，只能由 `ADMIN` 和 `OPERATOR` 访问。
- 商品、类目、品牌、规格、上下架操作写审计日志。
- 审计日志包含 request ID、操作者、目标类型、目标 ID、动作和摘要。
- 用户端不能信任客户端传入价格、库存、品牌、规格，订单金额和商品快照必须以后端当前 SKU 为准。
- 商品下架后不能新增购物车或下单，但历史订单继续展示快照。

## 测试和验收

### 后端

- 商品保存校验。
- 非启用商家不能保存上架商品。
- 非启用类目或品牌不能用于上架商品。
- SKU 规格组合不能重复。
- 用户端搜索不返回下架商品和停用商家商品。
- 商家详情不返回下架商品。
- 商品详情仅返回可售商品。
- 下架 SKU 不可加购、不可下单。
- 后台接口 `CUSTOMER` 返回 `403`。

命令：

```bash
cd backend && mvn -nsu -Drocketmq.log.root=logs/rocketmqlogs clean test
```

### 后台端

- TypeScript 类型和 API 函数同步。
- 商品中心页面构建通过。
- 字典、商品、SKU 表单校验能阻止非法提交。

命令：

```bash
cd admin && pnpm build
```

### 用户端

- 搜索页展示商品结果和商家结果。
- 商品详情页可选择规格并加购。
- 商家详情页单 SKU 快速加购、多 SKU 进入规格选择。
- 购物车和结算展示 SKU 快照。

命令：

```bash
cd frontend && pnpm build
```

### HTTP 冒烟

修改后台和商品接口后，至少覆盖：

- 管理员登录。
- 管理员访问商品中心接口成功。
- 普通用户访问 `/admin/**` 返回 `403`。
- 创建类目、品牌、规格、SPU、SKU。
- 用户端搜索商品。
- 查看商品详情。
- 加购 SKU。
