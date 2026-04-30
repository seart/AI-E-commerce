# 库存中心实施计划

**Goal:** 把订单链路中的库存变化从直接修改 `products.stock` 升级为 SKU 级库存账户、库存流水和幂等库存服务。

**Architecture:** 新增 `inventory_accounts` 与 `inventory_transactions`。`InventoryService` 作为唯一库存变更入口，订单、支付、退款、后台调整都调用它。`products.stock` 保留为可售库存兼容字段。

## 范围

- 后端库存账户、库存流水、启动迁移和存量 SKU 初始化。
- 下单、待支付取消、支付超时关单、支付成功、退款确认的库存接入。
- 后台库存账户/流水/调整接口和审计。
- 后台商品中心增加库存标签页，展示账户和流水，支持人工调整可售库存。
- README 和主上下文更新。

## 不做

- 多仓、多批次、调拨、盘点、采购入库。
- Redis 库存预扣、秒杀队列、异步库存中心。
- 真实支付网关。

## 任务

### Task 1: 数据模型和迁移

- 修改 `backend/src/main/resources/db/schema.sql`，新增库存账户与流水表。
- 修改 `DatabaseMigrationRunner`，创建库存表并从 `products.stock` 初始化账户。
- 修改 `DataEntities`，新增库存实体。
- 新增 `InventoryAccountMapper` 与 `InventoryTransactionMapper`。

### Task 2: 库存服务

- 新增 `InventoryService`。
- 实现账户初始化、账户查询、流水查询。
- 实现下单锁定、支付确认、待支付释放、售出回补、后台调整。
- 通过流水唯一键保证业务幂等。
- 保持 `products.stock` 与可售库存同步。

### Task 3: 交易链路接入

- 修改 `DatabaseStore.createOrder`，用库存服务锁定库存。
- 修改 `DatabaseStore.cancelOrder` 和 `adminUpdateOrderStatus`，用库存服务释放或回补。
- 修改 `PaymentService.confirmGatewayPaid` 和 `closeExpiredOrder`，用库存服务确认售出或释放锁定库存。
- 删除重复的本地库存回滚逻辑。

### Task 4: 后台接口和前端

- 新增库存 DTO。
- 修改 `AdminController` 与 `AdminService`，增加库存账户、流水和调整接口。
- 修改 `admin/src/types/domain.ts` 与 `admin/src/api/admin.ts`。
- 在商品中心增加库存标签页。

### Task 5: 测试与文档

- 新增库存中心集成测试。
- 更新 `backend/README.md` 当前实现范围和接口概览。
- 更新主上下文，记录商品中心已完成和库存中心分支。

## 验证

1. `cd backend && mvn -nsu -Drocketmq.log.root=logs/rocketmqlogs clean test`
2. `cd frontend && pnpm build`
3. `cd admin && pnpm build`
4. 启动后端并做 HTTP 冒烟：管理员登录、普通用户后台 `403`、库存账户查询、库存调整、下单锁定、预支付、支付确认或超时释放。
