# 账号与权限中心 Spring Security 改造设计

## 背景

当前后端已经具备轻量 JWT 登录、注册、登出、Redis Token 黑名单、PBKDF2 密码升级、后台 `ADMIN`/`OPERATOR` 拦截、审计日志和登录限流。现有实现主要依赖 `AuthInterceptor`、`AdminInterceptor` 和 `UserContext` 的 `ThreadLocal`。

用户已明确要求引入 Spring Security 改造账号与权限中心，因此本设计覆盖仓库原规则中“不引入重型 Spring Security”的默认限制。其他规则继续保持：后端使用 Spring Boot + Maven + MyBatis-Plus，不使用 JPA；接口基础路径保持 `/api`；后台接口保持 `/admin/**`；Redis 只用于 JWT 黑名单、限流等运行时安全状态。

## 目标

第一阶段把账号与权限中心升级为 Spring Security + JWT + 轻量 RBAC，作为后续商品、库存、交易、财务和风控模块的统一身份基础。

必须达成：

- 使用 Spring Security 统一处理认证、授权、异常响应和 CORS。
- 保持现有前端登录协议不破坏：`POST /auth/login` 返回 `accessToken`、`refreshToken`、`expiresAt` 和 `user`。
- `/admin/**` 只允许 `ADMIN` 和 `OPERATOR`。
- `CUSTOMER` 访问 `/admin/**` 返回 `403`。
- 未登录或 Token 无效访问受保护接口返回 `401`。
- 密码继续使用 PBKDF2；历史明文密码在成功登录后自动升级。
- Redis 继续用于 access token 黑名单、登录限流、后续风控运行时状态。
- 关键安全事件写审计日志并关联 request ID。

## 非目标

第一阶段不做 OAuth2、第三方登录、短信登录、管理员 MFA、多租户组织架构、细粒度菜单权限、ABAC 策略引擎和独立 IAM 服务。

这些能力可以在风控与安全中心、后台权限中心成熟后单独演进。

## 总体方案

采用“Spring Security 接管请求入口，业务服务保留现有响应契约”的方案。

请求进入后由 `SecurityFilterChain` 配置开放路径、受保护路径和后台角色规则。`JwtAuthenticationFilter` 解析 Bearer Token，校验签名、过期时间、黑名单、用户状态，然后写入 `SecurityContextHolder`。业务层通过改造后的 `UserContext` 从 `SecurityContextHolder` 读取当前用户，不再依赖拦截器设置 `ThreadLocal`。

登录仍由 `AuthController` 调用 `AuthService.login` 完成。登录成功后签发 access token 和 refresh token。登出时将 access token 加入 Redis 黑名单。刷新 Token 新增 `POST /auth/refresh`，校验 refresh token 后签发新的 access token。

## 认证与授权流程

### 登录

1. `POST /auth/login` 接收手机号和密码。
2. 按手机号 + IP 做限流。
3. 查询用户并校验密码。
4. 用户状态不是 `ACTIVE` 时拒绝登录。
5. 如果密码仍是历史明文，成功登录后更新为 PBKDF2 哈希。
6. 记录最后登录时间。
7. 写入 `AUTH_LOGIN` 审计日志。
8. 返回 access token、refresh token、过期时间和用户信息。

### 访问受保护接口

1. `JwtAuthenticationFilter` 读取 `Authorization: Bearer <token>`。
2. 校验 JWT 签名、`typ=access`、`exp`、`jti`。
3. 查询 Redis 黑名单，黑名单命中返回 `401`。
4. 查询用户，用户不存在或状态非 `ACTIVE` 返回 `401`。
5. 将用户 ID、手机号、角色、状态写入 Spring Security `Authentication`。
6. Controller 和 Service 从 `UserContext.userId()`、`UserContext.role()` 读取当前身份。

### 后台授权

后台接口继续使用 `/admin/**`，由 Spring Security 配置：

- `hasAnyRole("ADMIN", "OPERATOR")`
- `CUSTOMER` 命中 `AccessDeniedHandler`，统一返回当前 `ApiResponse.failure(ErrorCode.FORBIDDEN)` 格式。

后台高危操作继续由业务层写审计日志，后续风控中心会在此基础上增加二次确认和风险事件。

### 登出

1. `POST /auth/logout` 读取当前 access token。
2. 解析 Token 过期时间。
3. 以 token 指纹或 `jti` 写入 Redis 黑名单，TTL 到 access token 过期为止。
4. 写入 `AUTH_LOGOUT` 审计日志。
5. 返回幂等成功。

### 刷新 Token

新增 `POST /auth/refresh`：

```json
{
  "refreshToken": "jwt-refresh-token"
}
```

成功响应沿用 `UserSessionResponse`。刷新时必须校验：

- `typ=refresh`
- 签名合法
- 未过期
- 用户仍是 `ACTIVE`
- refresh token 未被列入黑名单

第一阶段 refresh token 采用 JWT，不落库；登出只拉黑 access token。后续如果需要“全端登出”和“设备管理”，再增加 `user_sessions` 表。

## Spring Security 设计

新增或改造这些后端组件：

- `SecurityConfig`
  - 定义 `SecurityFilterChain`
  - 关闭 session，使用 stateless
  - 关闭 form login、http basic
  - 配置 CORS
  - 配置开放路径和后台授权规则
  - 注册统一 401/403 handler
  - 注册 `JwtAuthenticationFilter`

- `JwtAuthenticationFilter`
  - 继承 `OncePerRequestFilter`
  - 解析 Bearer Token
  - 只对非开放路径做认证
  - 成功时写入 `SecurityContextHolder`
  - 失败时走统一 401 响应

- `CurrentUserPrincipal`
  - 保存用户 ID、手机号、角色、状态
  - 暴露 authorities，例如 `ROLE_CUSTOMER`、`ROLE_ADMIN`、`ROLE_OPERATOR`

- `SecurityErrorHandler`
  - 输出现有 `ApiResponse` 错误格式
  - 401 使用 `ErrorCode.UNAUTHORIZED`
  - 403 使用 `ErrorCode.FORBIDDEN`

- `UserContext`
  - 改为读取 `SecurityContextHolder`
  - 保持 `userId()`、`role()`、`isAdminOperator()` 方法签名兼容现有业务代码

删除或停用：

- `AuthInterceptor`
- `AdminInterceptor`
- `WebConfig.addInterceptors`

`WebConfig` 中的 CORS 逻辑迁移到 Spring Security。保留 `CorsProperties` 作为配置来源。

## JWT 设计

Access token payload 包含：

```json
{
  "sub": "u_demo",
  "typ": "access",
  "role": "CUSTOMER",
  "iat": 1777082400,
  "exp": 1777687200,
  "jti": "uuid"
}
```

Refresh token payload 包含：

```json
{
  "sub": "u_demo",
  "typ": "refresh",
  "iat": 1777082400,
  "exp": 1779674400,
  "jti": "uuid"
}
```

`JwtTokenService` 需要提供：

- 创建 access token
- 创建 refresh token
- 解析 claims
- 校验 token type
- 获取过期时间
- 获取 jti

签名算法第一阶段继续使用现有 HS256 实现，密钥从 `JWT_SECRET` 环境变量覆盖。后续可替换为成熟 JWT 库，但不作为本阶段要求。

## 数据模型

第一阶段复用现有 `users` 表：

- `id`
- `mobile`
- `password`
- `nickname`
- `member_level`
- `role`
- `status`
- `last_login_at`
- `created_at`
- `updated_at`

本阶段不新增 `roles`、`permissions`、`user_sessions` 表。原因是当前只有 `CUSTOMER`、`ADMIN`、`OPERATOR` 三类角色，枚举式 RBAC 足够支撑后台和交易链路。

为了后续扩展，代码中角色统一通过枚举或常量集中管理，避免散落字符串。

## 接口契约

保留：

- `POST /auth/login`
- `POST /auth/register`
- `POST /auth/logout`
- `GET /profile`
- `/admin/**`

新增：

- `POST /auth/refresh`

开放路径：

- `/auth/login`
- `/auth/register`
- `/auth/refresh`
- `/health`
- `/static/**`
- `/payments/notify/**`
- `/v3/api-docs/**`
- `/swagger-ui/**`
- `/swagger-ui.html`
- `/error`

需要认证路径：

- 除开放路径外的用户端接口
- `/profile`
- `/cart/**`
- `/addresses/**`
- `/orders/**`
- `/payments/prepay`
- `/payments/{paymentId}`

后台路径：

- `/admin/**` 仅 `ADMIN`、`OPERATOR`

## 错误响应

安全层返回结构必须与业务层一致：

401：

```json
{
  "code": 4010,
  "message": "请先登录",
  "data": null
}
```

403：

```json
{
  "code": 4030,
  "message": "无权限访问",
  "data": null
}
```

具体 `code` 和 `message` 使用现有 `ErrorCode` 枚举，不在安全层硬编码新值。

## 安全与风控基线

本阶段必须满足：

- 客户端传入的角色不可信，角色只从数据库读取。
- 禁用用户不能登录，旧 token 也不能继续访问接口。
- 后台接口由服务端强制鉴权，前端隐藏菜单不作为权限控制。
- 登出后的 access token 不可继续访问敏感接口。
- 登录失败和登录限流不泄露“手机号存在与否”。
- 日志不输出明文密码、完整 Token、支付敏感字段。
- 审计日志覆盖登录、登出、用户禁用、后台访问拒绝和后台高危操作。

建议近期增强：

- 记录登录失败次数和失败原因分类。
- 后台高危操作增加二次确认。
- 支持全端登出和设备会话列表。
- 管理员 MFA。

长期平台化：

- 权限表、角色表、菜单权限、操作权限。
- 风控策略引擎。
- 统一风险事件表和告警。

## 测试计划

后端必须新增或更新集成测试覆盖：

- 未登录访问 `/profile` 返回 `401`。
- 登录成功后访问 `/profile` 返回 `200`。
- `CUSTOMER` 访问 `/admin/dashboard/summary` 返回 `403`。
- `ADMIN` 访问 `/admin/dashboard/summary` 返回 `200`。
- 登出后旧 access token 返回 `401`。
- 再次登录签发的新 access token 可访问 `/profile`。
- 禁用用户不能登录。
- 禁用用户的旧 access token 不能继续访问 `/profile`。
- 历史明文密码登录成功后升级为 PBKDF2。
- refresh token 可换取新的 access token。
- access token 调用 `/auth/refresh` 返回 `401` 或 `400`。
- refresh token 访问普通业务接口返回 `401`。

完成实现后按仓库规则运行：

```bash
cd backend && mvn clean test
```

因为本模块修改鉴权和后台访问，还必须启动后端做 HTTP 冒烟：

- 登录
- 后台访问成功
- 普通用户访问后台 `403`
- 登出后旧 token 访问失败
- refresh token 换取 access token

## 后续模块衔接

账号与权限中心完成后，后续模块按以下顺序推进：

1. 商品中心：类目、品牌、SPU、SKU、规格、上下架。
2. 库存中心：库存账户、锁定库存、库存流水、幂等回滚。
3. 交易中心：购物车、结算试算、订单、支付、优惠券、促销。
4. 财务与结算中心：商家结算单、平台佣金、退款扣减、财务导出。
5. 风控与安全中心：登录、下单、支付、退款、优惠券和后台高危操作的风险事件与策略。

每个后续模块单独编写设计和实施计划，不把多个领域塞进同一个实施任务。

## 自检结果

- 无未完成条目。
- 设计范围只覆盖账号与权限中心。
- Spring Security 改造与用户明确要求一致。
- 与现有 `/api`、`/admin/**`、PBKDF2、Redis 使用边界兼容。
- 后续模块边界已列出，但不进入本阶段实现。
