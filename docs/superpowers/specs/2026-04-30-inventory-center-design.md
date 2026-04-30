# 库存中心设计规格

## 背景

当前系统已经完成商品中心和基础交易链路，但库存仍由 `products.stock` 一个字段承担全部语义。下单时直接扣减该字段，待支付取消、支付超时、退款确认时再直接加回。这个实现可以支撑演示，但缺少大型电商系统需要的库存账户、锁定库存、流水和幂等回滚能力。

本阶段建设轻量库存中心，目标是在不引入多仓、批次和供应链复杂度的前提下，把交易链路中的库存变化变成可审计、可幂等、可并发保护的领域能力。

## 目标

1. 建立 SKU 级库存账户，区分可售库存、锁定库存和已售库存。
2. 建立库存流水，记录下单锁定、支付确认、待支付释放、退款回补和后台调整。
3. 创建订单时锁定库存，而不是只做普通字段覆盖。
4. 支付成功时确认售出；支付超时或待支付取消时释放锁定库存；退款确认时回补已售库存。
5. 同一订单、同一 SKU、同一业务动作必须幂等，库存只能回滚一次。
6. `products.stock` 保留为用户端和后台现有页面的可售库存展示字段，由库存中心同步维护。
7. 后台提供库存账户、库存流水和人工调整入口，并记录审计日志。

## 非目标

- 不实现多仓库、门店库存、批次、效期、调拨、盘点任务和采购入库。
- 不实现秒杀库存队列、Redis 预扣库存或异步库存中心。
- 不改变当前模拟支付策略，不接入真实支付网关。
- 不引入 Flyway，继续使用 `schema.sql` 与启动迁移逻辑兼容存量库。

## 数据模型

### inventory_accounts

SKU 级库存账户。

- `sku_id`：SKU ID，对应 `products.id`。
- `available_quantity`：可售库存，用户端展示和下单校验使用。
- `locked_quantity`：待支付订单锁定库存。
- `sold_quantity`：已支付确认售出的库存。
- `version`：预留乐观锁版本。
- `updated_at`：最后更新时间。

`products.stock` 与 `available_quantity` 保持同步，作为兼容字段继续服务既有商品接口。

### inventory_transactions

库存流水。

- `biz_type`：业务动作，如 `ORDER_LOCK`、`PAYMENT_CONFIRM`、`ORDER_RELEASE`、`ORDER_RESTOCK`、`ADMIN_ADJUST`、`PRODUCT_STOCK_SYNC`。
- `biz_id`：业务幂等键。订单相关动作使用订单 ID，后台调整使用请求级唯一 ID。
- `sku_id`：SKU ID。
- `quantity`：影响数量，必须为正数。
- `before_*` / `after_*`：动作前后可售、锁定、已售快照。
- `reason`：业务原因。
- `request_id`：请求 ID，便于日志串联。

`biz_type + biz_id + sku_id` 建唯一索引，保证相同业务动作重复投递或重复调用时只应用一次。

## 交易链路

### 创建订单

1. 服务端读取当前 SKU、商家和价格信息生成订单快照。
2. 库存中心按 SKU 条件扣减 `available_quantity` 并增加 `locked_quantity`。
3. 同步扣减 `products.stock`。
4. 写入 `ORDER_LOCK` 流水。
5. 任意 SKU 库存不足时，整个订单创建事务回滚。

### 支付成功

1. 支付回调仍由支付服务做订单状态条件更新。
2. 只有订单从 `PENDING_PAYMENT` 成功变为 `PAID` 时，库存中心确认售出。
3. 确认售出扣减 `locked_quantity`，增加 `sold_quantity`，不再改变 `products.stock`。
4. 写入 `PAYMENT_CONFIRM` 流水；重复回调不重复扣减。

### 待支付关闭

支付超时关单或用户取消待支付订单时：

1. 订单状态先条件更新为 `PAYMENT_CLOSED`。
2. 库存中心释放锁定库存，增加 `available_quantity`，减少 `locked_quantity`。
3. 同步增加 `products.stock`。
4. 写入 `ORDER_RELEASE` 流水；MQ 重复投递或用户重复操作不会重复释放。

### 退款与已支付取消

退款确认或已支付备货中取消时：

1. 订单进入 `REFUNDED` 或 `CANCELED` 后触发售出回补。
2. 库存中心减少 `sold_quantity`，增加 `available_quantity`。
3. 同步增加 `products.stock`。
4. 写入 `ORDER_RESTOCK` 流水；同一订单同一 SKU 只能回补一次。

## 后台能力

后台接口统一在 `/api/admin/**` 下，继续由 Spring Security 限制 `ADMIN` 和 `OPERATOR` 访问。

- `GET /admin/inventory/accounts`：库存账户列表，支持关键字和低库存过滤。
- `GET /admin/inventory/transactions`：库存流水列表，支持 SKU、订单和业务类型过滤。
- `POST /admin/inventory/accounts/{skuId}/adjust`：人工调整可售库存，必须填写原因，写入审计日志和库存流水。

## 安全与风控

- 客户端不能提交最终价格、库存或库存变更结果。
- 库存扣减、释放、售出确认和回补必须在服务端事务内完成。
- 退款确认和后台库存调整属于高风险动作，必须记录审计日志和 request ID。
- 支付成功、超时关单、用户取消和后台退款可能并发发生，库存流水幂等键必须阻止重复回滚。
- 后台库存调整只调整可售库存，不直接篡改锁定或已售库存。

## 验收

- 后端新增库存中心集成测试，覆盖下单锁定、支付确认、支付超时释放、退款回补和后台调整。
- 合并前运行 `cd backend && mvn -nsu -Drocketmq.log.root=logs/rocketmqlogs clean test`。
- 后台构建通过：`cd admin && pnpm build`。
- 用户端构建通过：`cd frontend && pnpm build`。
- HTTP 冒烟覆盖登录、后台库存接口、普通用户访问后台 `403`、下单锁定、预支付、支付确认或超时释放。
