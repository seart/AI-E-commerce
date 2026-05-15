# 企业版商城后端实现计划

> **For agentic workers:** 当前先输出实现计划，不直接进入代码实现。后续如果进入执行阶段，建议按任务拆分逐项落地。

**Goal:** 基于现有前端契约，落地一个可联调的 Spring Boot + Maven 后端，使用轻量 JWT 认证，优先打通首页、商家、购物车、地址、订单、个人中心全链路。

**Architecture:** 后端按“Controller -> Service -> Store/Repository”三层结构组织。第一阶段用内存/H2 承载数据，保证接口和字段稳定；第二阶段再平滑切换到 MySQL、Redis 和更完整的会话治理。

**Tech Stack:** Java 17、Spring Boot 3.x、Maven、Spring Web、Spring Validation、轻量 JWT、Springdoc OpenAPI

---

## 1. 推荐方案

本项目推荐采用“契约优先 + 分阶段演进”的方案：

- 第一阶段做一个能稳定联调的后端
- 第二阶段再做持久化和生产增强

不推荐一开始就上完整电商中台能力。当前最重要的是把前端已经固定下来的接口和字段稳定下来，否则前后端会在接口变更上来回返工。

---

## 2. 关键设计决策

### 2.1 认证

采用轻量 JWT 方案，不引入重型 Spring Security 流程。

具体做法：

- 登录成功后签发 JWT access token
- 请求进入时用自定义 `HandlerInterceptor` 或 `Filter` 解析 `Authorization: Bearer <token>`
- 解析成功后把用户信息放入 `UserContext`
- 退出登录第一阶段只做幂等成功返回，不强制维护服务端黑名单

这样做的原因：

- 当前前端只依赖 Bearer Token，不依赖复杂角色授权模型
- 业务接口集中在当前登录用户自己的数据，不需要先引入重型权限框架
- 未来要升级到更强的认证模型时，可以保留 JWT 服务和拦截器边界，不会推翻现有接口

### 2.2 存储

第一阶段建议用两种可选实现：

- 方案 A：纯内存存储，启动即带种子数据
- 方案 B：H2 文件数据库，便于本地重启保留数据

我更推荐方案 B。原因是它既能保持开发轻量，又比纯内存更接近真实 CRUD 场景。

### 2.3 数据边界

第一阶段严格对齐前端字段，不做额外抽象。以下字段虽然偏展示层，但必须保留：

- `merchant.heroColor`
- `merchant.logoText`
- `product.imageText`
- `category.icon`

其中 `category.icon` 不能返回前端构建期资源路径，必须改成真实可访问 URL。

---

## 3. 目录结构建议

```text
backend/
  pom.xml
  docs/
    api-spec.md
    implementation-plan.md
  src/main/java/com/jingdong/backend/
    JingdongBackendApplication.java
    config/
    auth/
    api/
    exception/
    controller/
    service/
    store/
    dto/
  src/main/resources/
    application.yml
```

建议的职责划分：

- `api/`: 统一响应体、通用返回结构
- `auth/`: JWT、用户上下文、认证拦截
- `exception/`: 统一异常和错误码映射
- `controller/`: HTTP 接口入口
- `service/`: 业务编排
- `store/`: 第一阶段的内存/H2 数据读写
- `dto/`: 请求与响应对象

---

## 4. 模块拆分

### 模块 1：基础设施

目标：

- Spring Boot 工程启动
- 统一响应包 `ApiResponse<T>`
- 全局异常处理
- 参数校验
- Markdown 接口文档与 Swagger UI

产出：

- `ApiResponse`
- `BusinessException`
- `GlobalExceptionHandler`
- `backend/docs/api-spec.md` 接口说明
- `Swagger/OpenAPI` 配置

### 模块 2：JWT 认证

目标：

- 登录签发 token
- 请求解析 token
- 当前用户上下文获取

产出：

- `JwtTokenService`
- `JwtProperties`
- `AuthInterceptor` 或 `JwtAuthenticationFilter`
- `UserContext`

说明：

这里不使用 Spring Security 认证链，避免引入不必要的复杂度。

### 模块 3：首页与商家

目标：

- 提供首页聚合数据
- 提供商家搜索
- 提供商家详情与商品列表

产出：

- `HomeController`
- `MerchantController`
- Banner/Category/Merchant/Product DTO

### 模块 4：用户与个人中心

目标：

- 登录、注册、退出
- 获取个人中心信息

产出：

- `AuthController`
- `ProfileController`
- User/Profile DTO

### 模块 5：地址管理

目标：

- 地址列表
- 新增地址
- 编辑地址
- 默认地址唯一性控制

产出：

- `AddressController`
- Address DTO
- 地址默认值修正规则

### 模块 6：购物车

目标：

- 获取购物车
- 新增商品
- 修改数量
- 单商品勾选
- 商家维度勾选
- 全选
- 清空已勾选

产出：

- `CartController`
- CartItem DTO
- 基于 `userId + productId` 的唯一购物车项规则

### 模块 7：订单

目标：

- 查询订单
- 创建订单
- 地址校验
- 购物车已勾选项结算后移除

产出：

- `OrderController`
- Order DTO
- OrderItem DTO

---

## 5. 开发顺序

建议开发顺序如下：

1. 初始化 Maven 工程和基础依赖
2. 搭统一响应、异常、校验
3. 搭 JWT 登录态链路
4. 先做只读接口：`/home`、`/merchants/search`、`/merchants/{id}`
5. 再做用户接口：`/auth/*`、`/profile`
6. 再做地址接口：`/addresses`
7. 再做购物车接口：`/cart/*`
8. 最后做订单接口：`/orders`
9. 补 Swagger 示例、MockMvc 测试、README

这个顺序的原因很简单：

- 只读接口最容易稳定
- JWT 登录态必须先打通，否则后面的用户态接口都没法测
- 订单依赖地址和购物车，所以应放最后

---

## 6. 测试策略

第一阶段至少做以下测试：

- Controller 层：`MockMvc` 接口测试
- Service 层：关键业务单测
- JWT：生成、解析、过期、非法 token 测试
- 订单：地址不存在、商品不存在、购物车清理逻辑测试
- 地址：默认地址唯一性测试
- 购物车：数量更新与勾选逻辑测试

不建议第一阶段跳过测试直接写接口。当前项目接口多，且购物车和订单有明显状态联动，不测很容易在联调中反复返工。

---

## 7. 数据初始化策略

第一阶段建议直接把前端 mock 数据迁移成后端种子数据：

- 商家
- 商品
- 首页 banner
- 首页分类
- 演示用户
- 演示地址

好处：

- 前端切换到真实 API 后，页面效果不会突然变化太大
- 联调时更容易判断是接口问题还是展示问题

需要调整的一点：

- `category.icon` 需要改成后端可访问的静态资源 URL，或直接用 CDN URL

---

## 8. 风险点与处理建议

### 风险 1：前端契约包含展示字段

例如 `heroColor`、`logoText`、`imageText` 这些字段偏展示层，但前端当前直接依赖。

处理建议：

- 第一阶段保留这些字段
- 第二阶段若要重构，应先改前端契约再改后端

### 风险 2：JWT logout 语义不完整

纯 JWT 场景下，服务端无法天然“主动失效”已经签发的 token。

处理建议：

- 第一阶段：退出登录仅返回成功，由前端删除 token
- 第二阶段：如确实需要强制失效，再补 Redis 黑名单

### 风险 3：订单还没有支付链路

前端当前下单即视为成功。

处理建议：

- 第一阶段：`POST /orders` 直接生成 `PAID` 状态订单，便于联调
- 第二阶段：拆成“下单”和“支付确认”两个流程

---

## 9. 第一阶段交付物

第一阶段建议交付以下内容：

- Maven Spring Boot 工程
- 可运行的本地后端服务
- Markdown 接口文档
- Swagger UI
- 与前端契约一致的全部接口
- JWT 登录认证
- 种子数据
- 基础测试
- 启动说明文档

达到这个交付物后，前端就可以切换 `VITE_API_BASE_URL` 做真实联调。

---

## 10. 第二阶段演进方向

在第一阶段联调稳定后，再进入第二阶段：

- 存储切换到 MySQL
- 引入 Redis
- 完善 JWT 刷新与失效策略
- 增加订单状态流转
- 增加库存与营销规则
- 增加后台运营管理接口
- 增加日志、审计、监控、限流

---

## 11. 当前建议结论

当前最可行的路径是：

- 先做轻量 JWT + Spring Boot + Maven
- 先做契约优先的联调后端
- 不上重型 Spring Security 认证链
- 不在第一阶段引入完整支付、库存、活动、风控

这个方案能最快把前后端联调闭环做出来，同时保留后续升级空间，不会把项目一开始就推到过重的实现复杂度里。
