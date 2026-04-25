# Jingdong Backend

这是配合当前前端项目联调使用的 Spring Boot 后端，使用 Maven 构建，接口统一挂载在 `/api` 下。

## 本地运行

先启动本机 Docker 里的 MySQL 和 Redis：

```bash
docker compose -f /Users/love/Documents/docker/docker-compose-file/jindong/docker-compose.yml up -d
```

后端本机运行：

```bash
cd backend
mvn test
mvn spring-boot:run
```

服务启动后：

- API Base URL: `http://localhost:8080/api`
- Swagger UI: `http://localhost:8080/api/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api/v3/api-docs`

前端本地环境建议配置：

```bash
VITE_API_BASE_URL=http://localhost:8080/api
```

## 测试账号

- 普通用户：`13800000000 / 123456`
- 后台管理员：`13900000000 / 123456`

登录成功后，前端需要在后续请求中携带：

```http
Authorization: Bearer <accessToken>
```

## 当前实现范围

- 使用 MyBatis-Plus 接入 MySQL，不使用 JPA，不使用 Flyway。
- MySQL 本机连接：`localhost:33061/jingdong`，账号：`jingdong / 123456`。
- Redis 本机连接：`localhost:63791`，密码：`12345`。
- 已实现轻量 JWT 登录、注册、登出，不引入 Spring Security。
- Redis 用于 logout 后的 JWT 黑名单、登录限流、下单限流，不缓存首页或商家详情页。
- 已实现首页、商家详情、商家搜索、购物车、地址、订单、个人中心接口。
- 下单接口会先创建 `PENDING_PAYMENT` 待支付订单并扣减库存，支付成功回调后进入 `PAID` / `支付成功`。
- 已接入 `cn.felord:payment-spring-boot-starter:1.0.20.RELEASE`，支持支付宝扫码和微信 Native 扫码预下单。
- 支付超时关单通过 RocketMQ 延迟消息触发；未配置 RocketMQ 地址时可先保持 `PAYMENT_ROCKETMQ_ENABLED=false` 做本地测试。
- 已实现订单状态闭环：`PAID -> PREPARING -> DELIVERING -> COMPLETED`，并支持待支付取消、退款申请、退款确认。
- 已实现后台 `/admin/**` 接口，支持管理员/运营访问，普通用户返回 `403`。
- 已实现 PBKDF2 密码哈希；历史明文密码会在首次成功登录后自动升级。
- 已实现审计日志、请求 ID、结构化请求日志与健康检查 `/api/health`。
- 用户、商家、商品、购物车、地址、订单均已落 MySQL。
- 表结构和初始化数据使用 Spring Boot `schema.sql` / `data.sql` 轻量初始化；存量库字段补齐由 `DatabaseMigrationRunner` 在启动时完成，不使用 Flyway。

## 独立后台项目

运营后台位于仓库根目录 `admin/`，与移动端用户前端 `frontend/` 分离。

```bash
cd admin
pnpm install
pnpm dev
```

默认访问：`http://localhost:5174`，API 指向：`http://localhost:8080/api`。

## 接口规范

- 接口规范见 `docs/api-spec.md`。
- 后端开发方案见 `docs/implementation-plan.md`。

## 端到端联调与单元测试

后端现已内置基础的集成测试与单元测试（位于 `src/test/java` 目录下），并且能够无缝对接前端完成全链路页面端业务流测试。

**1. 运行后端基础测试用例：**
在终端执行测试，确保数据库等组件均联通：
```bash
mvn clean test
```

**2. 配合前端的闭环测试流程：**
1. 启动 Docker 环境的 MySQL/Redis 容器。
2. 运行当前 Spring Boot 后端服务：`mvn spring-boot:run`。后端默认绑定于 `8080` 端口。
3. 结合启动环境的 `Vite` 前端项目在浏览器打开，模拟真实用户进行以下闭环测试：
   - 账户注册与会话登录
   - 首页精选和商家访问（验证 `HomeController`, `MerchantController`）
   - 购物车增改减（验证 `CartController`）
   - 下单、扫码预支付、支付回调或状态轮询、收货地址调度（验证 `OrderController`, `PaymentController`, `AddressController`）
4. **注意**：进行自动化或脚本测试验证完毕后，为了避免资源泄露引起下次部署冲突，请注意使用以下终端指令杀掉进程释放所有端口：
   ```bash
   lsof -ti :8080 | xargs kill -9
   ```
