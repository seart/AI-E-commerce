# Jingdong Admin

独立运营后台项目，基于 `Vue 3 + Vite + Pinia + Element Plus`，不和移动端用户前端共用路由或构建产物。

## 本地启动

```bash
pnpm install
pnpm dev
```

- 默认地址：`http://localhost:5174`
- 默认 API：`http://localhost:8080/api`
- 管理员账号：`13900000000 / 123456`

如需调整后端地址，可复制 `.env.example` 为 `.env.local` 后修改：

```bash
VITE_API_BASE_URL=http://localhost:8080/api
```

## 功能范围

- 数据概览：订单数、销售额、商品数、用户数。
- 订单管理：订单列表、状态推进、退款确认。
- 商品管理：新增、编辑、上下架、库存调整。
- 商家管理：新增、编辑、启停。
- 用户管理：用户启用/禁用，当前登录用户不允许禁用自己。
- 审计日志：按操作者、动作、时间范围筛选。

## 构建验证

```bash
pnpm build
```

当前项目固定使用 TypeScript `5.9.3`，避免 TypeScript 6 与 Element Plus 依赖声明的兼容问题。
