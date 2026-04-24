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

当前工作区环境里缺少 `pnpm` 可执行命令，因此代码已经按 `pnpm` 规范整理，但我这边还无法直接完成最终构建验证。补上 `pnpm` 后建议立即执行：

```sh
pnpm install
pnpm type-check
pnpm build
```
