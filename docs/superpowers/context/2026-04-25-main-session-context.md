# 主会话压缩上下文

本文档是后续子代理执行任务时的最小上下文包。主会话只保留路线、规则、验收和合并职责；各功能模块通过独立分支、独立计划、独立子代理任务完成。

## 当前仓库状态

- 仓库路径：`/Users/love/Documents/code/local/Front/AI-E-commerce`
- 远程仓库：`git@github.com:seart/AI-E-commerce.git`
- 主分支：`main`
- 本地 `main` 已合并商品中心阶段提交 `9546a7c`；远端推送因当前环境缺少 GitHub 凭据暂未完成。
- 已保留的账号权限分支：`codex/account-permission-security-20260425-1`
- 已保留的商品中心分支：`codex/product-center-stage-1-4`
- 当前库存中心分支：`codex/inventory-center-20260430-1`
- 本地可忽略输出包括：`.worktrees/`、`backend/logs/`、`backend/target/`、`frontend/dist/`、`admin/dist/`、`node_modules/`。

## 全局协作规则

- 所有面向用户的说明使用中文，除非用户明确要求其他语言。
- 所有新增或更新的项目文档、方案、规范和说明默认使用中文正文。
- 路径、命令、代码标识、协议字段和必要英文技术名词可以保留原文。
- 前端和后台统一使用 `pnpm`，禁止使用 `npm install`、`npm run` 或生成 npm lockfile。
- 后端使用 Spring Boot + Maven + MyBatis-Plus，禁止使用 JPA。
- 鉴权使用 Spring Security + 轻量 JWT/RBAC；不要改回 MVC 拦截器鉴权。
- Redis 只用于 JWT 黑名单、限流等安全或运行时状态；不要缓存首页、商家详情页或商品详情数据。
- 除非用户明确要求，后端不创建 Docker 打包方案。
- 必须保护用户已有改动，禁止破坏性 git 操作。

## 已完成模块

### 账号与权限中心

- 已引入 Spring Security。
- 已删除旧 MVC 鉴权拦截器。
- 已实现无状态 JWT 过滤器、`CurrentUserPrincipal`、统一 401/403 JSON 响应。
- `/admin/**` 仅允许 `ADMIN` 和 `OPERATOR`，`CUSTOMER` 返回 `403`。
- JWT 包含 `typ`、`role`、`jti`、过期时间。
- 退出登录同时拉黑 token 指纹和 `jti`。
- 新增 `/auth/refresh` 刷新访问令牌。
- `UserContext` 已优先读取 Spring Security 上下文。
- 登录限流包含手机号维度和 IP 维度。
- 禁用用户不能继续使用旧 token。

### 商品中心

- 已建立类目、品牌、规格组、规格值、SPU、SKU 基础模型。
- 后台商品中心支持商品、类目、品牌、规格维护。
- 用户端支持商品搜索、商品详情和 SKU 维度加购/下单。
- 旧 `/admin/products` 和 `productId` 交易链路继续兼容。
- `backend/README.md` 已按代码对齐商品中心、支付模拟、订单闭环和 MySQL 落库说明。

### 已验证命令

- `cd backend && mvn -nsu -Drocketmq.log.root=logs/rocketmqlogs clean test`
- 最近一次结果：31 个测试通过，0 失败。

## 后续模块路线

按用户指定顺序继续：

1. 库存中心
2. 购物车与结算中心、订单中心、支付中心、优惠券与促销中心
3. 财务与结算中心
4. 风控与安全中心

## 七阶段执行约定

每个功能模块使用独立分支和独立工作树，命名格式：

- `codex/<module-name>-YYYYMMDD-N`
- 同一天同模块多次改造时，`N` 从 `1` 递增。

每个模块严格按 Superpowers 子代理流程：

1. 主会话读取现状、明确模块边界并写中文设计规格。
2. 主会话写中文实施计划。
3. 每个任务派发独立实现子代理。
4. 实现子代理完成代码、测试、提交和自检。
5. 派发规格审查子代理，确认实现符合规格。
6. 派发代码质量审查子代理，确认实现质量和安全边界。
7. 所有任务通过后，派发最终审查，主会话运行验证、合并回 `main`，保留功能分支。

## 库存中心起点

现有系统在商品中心合并后已有 SKU 级商品主数据，但库存仍需要中心化治理：

- 商品 SKU 仍在 `products` 表，`products.stock` 继续作为可售库存兼容字段。
- 订单创建、支付确认、超时关单、取消和退款会影响库存。
- 库存中心本阶段新增 `inventory_accounts`、`inventory_transactions` 和 `InventoryService`。
- 后台商品中心增加库存标签页，库存接口仍位于 `/api/admin/**`。

库存中心第一轮补齐：

- SKU 库存账户。
- 下单锁定库存。
- 支付成功确认售出。
- 待支付取消和支付超时释放锁定库存。
- 退款确认和已支付取消回补已售库存。
- 库存流水和后台人工调整审计。

## 验收基线

按改动范围运行：

- 后端：`cd backend && mvn -nsu -Drocketmq.log.root=logs/rocketmqlogs clean test`
- 用户端：`cd frontend && pnpm build`
- 后台端：`cd admin && pnpm build`
- 修改鉴权、后台、订单相关行为时，必须用 HTTP 冒烟覆盖登录、后台访问、普通用户 `403` 和被修改接口行为。

如果命令失败，必须停止并报告准确失败信息，不得猜测完成。
