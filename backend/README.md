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

- 手机号：`13800000000`
- 密码：`123456`

登录成功后，前端需要在后续请求中携带：

```http
Authorization: Bearer <accessToken>
```

## 当前实现范围

- 使用 MyBatis-Plus 接入 MySQL，不使用 JPA，不使用 Flyway。
- MySQL 本机连接：`localhost:33061/jingdong`，账号：`jingdong / 123456`。
- Redis 本机连接：`localhost:63791`，密码：`12345`。
- 已实现轻量 JWT 登录、注册、登出，不引入 Spring Security。
- Redis 仅用于 logout 后的 JWT 黑名单，不缓存首页或商家详情页。
- 已实现首页、商家详情、商家搜索、购物车、地址、订单、个人中心接口。
- 下单接口会直接模拟支付成功，订单状态返回 `PAID` / `支付成功`。
- 用户、商家、商品、购物车、地址、订单均已落 MySQL。
- 表结构和初始化数据使用 Spring Boot `schema.sql` / `data.sql` 轻量初始化。

## 接口规范

- 接口规范见 `docs/api-spec.md`。
- 后端开发方案见 `docs/implementation-plan.md`。
