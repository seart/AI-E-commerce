# 京东企业版商城前端

这是一个基于 `Vue 3 + Vite + Pinia + Element Plus` 的移动端电商前端项目，已经从“页面 demo”调整为更适合企业交付的 `API-first` 结构。

## 当前改造点

- 统一通过 `pnpm` 管理依赖与脚本
- 新增统一环境配置：`VITE_API_BASE_URL`、`VITE_ENABLE_MOCK`、`VITE_API_TIMEOUT`
- 新增统一 `axios` 请求层和登录态注入
- 页面不再直接读本地 mock 文件，改为 `services -> stores -> views` 数据流
- 提供可控 mock 适配层
  - 开发环境下如果未配置 `VITE_API_BASE_URL`，默认启用 mock 数据
  - 生产环境建议显式配置真实 API 地址，并将 `VITE_ENABLE_MOCK=false`
- 购物车、地址、订单、个人中心、认证均已切换为异步请求模型

## 目录说明

```text
src/
  api/          # HTTP 客户端
  config/       # 环境配置
  constants/    # 常量
  services/     # 业务接口层
  stores/       # Pinia 状态管理
  types/        # 领域模型与接口类型
  utils/        # 格式化与存储工具
  views/        # 页面视图
```

## 启动要求

- Node.js: `^20.19.0 || >=22.12.0`
- 包管理器: `pnpm`

## 安装与启动

```sh
pnpm install
pnpm dev
```

## 构建与检查

```sh
pnpm type-check
pnpm build
pnpm lint
```

## 环境变量

复制 `.env.example` 后按实际环境调整：

```sh
cp .env.example .env.local
```

示例说明：

- `VITE_API_BASE_URL`
  - 真实后端服务地址
  - 例如 `https://api.example.com`
- `VITE_ENABLE_MOCK`
  - `true` 时强制走本地 mock 适配
  - `false` 时走真实 HTTP 请求
- `VITE_API_TIMEOUT`
  - 请求超时时间，单位毫秒

## 建议的后端接口域

当前前端默认按以下接口域组织，若后端契约不同，可在 `src/services/*.ts` 中集中调整：

- `/auth/login`
- `/auth/register`
- `/auth/logout`
- `/home`
- `/merchants/search`
- `/merchants/:id`
- `/cart/items`
- `/addresses`
- `/orders`
- `/profile`

## 说明

当前工作区已使用 `pnpm` 完成构建验证；移动端用户前端继续保持独立项目结构，运营后台已拆分到仓库根目录 `admin/`。

## 端到端联调与系统测试

前端项目目前建议并支持采用真实的后段接口进行端到端全链路测试。经过联合测试验证，各大核心模块均能完全闭环交互。

**测试指南：**
1. 确保后端的 Spring Boot 服务和所依赖的数据库已经正常启动（后端默认占用 `8080` 端口）。
2. 配置前端本地环境变量 `.env.local`，保证真实接口指向正确（默认为 `VITE_API_BASE_URL=http://localhost:8080/api`）。
3. 启动本地前端服务：
   ```sh
   pnpm dev
   ```
4. 在本地浏览器中访问 `http://localhost:5173` 进行全页面访问。
5. **推荐验证的核心路径**：可以使用11位标准手机号（如 `13800001234`）进行注册和登录 -> 预览首页与商家商品 -> 添加进入购物车 -> 提交结算 -> 填写收货地址 -> 选择支付宝或微信扫码支付 -> 支付状态轮询成功后查看订单。
6. 测试结束后，若不再使用建议关闭前端服务进程并释放 `5173` 端口。
