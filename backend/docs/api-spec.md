# 京东企业版商城后端接口规范

**版本**: v1

**适用范围**: `/Users/love/Documents/code/local/Front/jingdong/frontend` 当前前端契约

**后端技术约束**:
- Spring Boot 3.x
- Java 17
- Maven
- 轻量 JWT 认证
- 不引入重型 Spring Security 认证流程

---

## 1. 设计目标

本规范的目标不是先做“完整电商中台”，而是先提供一套能够稳定支撑前端联调的后端契约。当前前端已经固定了请求路径、响应字段和 Token 注入方式，因此后端应优先满足以下要求：

- 路径和字段与前端现有 `services/*.ts`、`types/domain.ts` 保持一致
- 所有业务接口统一返回固定响应包结构
- 登录后通过 `Authorization: Bearer <token>` 携带 access token
- 第一阶段允许使用内存/H2 数据承载，但接口语义不能变化

---

## 2. 通用约定

### 2.1 Base URL

建议后端统一以 `/api` 作为前缀，前端通过 `VITE_API_BASE_URL` 指向后端域名，例如：

```text
https://api.example.com/api
```

若后端暂不加 `/api` 前缀，前端也可以继续直接访问根路径，但不建议长期这样做。

### 2.2 统一响应格式

成功响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

失败响应：

```json
{
  "code": 4001,
  "message": "手机号或密码错误",
  "data": null
}
```

### 2.3 HTTP 状态码

- `200 OK`: 成功
- `400 Bad Request`: 参数错误
- `401 Unauthorized`: 未登录或 token 无效
- `403 Forbidden`: 无权限
- `404 Not Found`: 资源不存在
- `409 Conflict`: 状态冲突，例如手机号已注册
- `500 Internal Server Error`: 系统异常

### 2.4 JWT 约定

- 登录成功后返回 `accessToken`
- `refreshToken` 字段保留，第一阶段可返回固定值或短期有效值
- 前端请求头：

```http
Authorization: Bearer <accessToken>
```

- `POST /auth/logout` 第一阶段允许只做“客户端退出 + 服务端记录/忽略”，不强依赖黑名单

### 2.5 ID 和金额约定

- 所有业务 ID 统一使用字符串
- 所有金额字段统一返回数值类型，后端内部使用 `BigDecimal`

### 2.6 静态资源约定

前端当前会直接使用以下字段做渲染：

- `category.icon`
- `merchant.heroColor`
- `merchant.logoText`
- `product.imageText`

其中：

- `category.icon` 必须返回前端可访问的 URL
- `heroColor` 当前允许继续返回 CSS 渐变字符串
- `logoText`、`imageText` 可继续作为占位展示字段

`category.icon` 是本次规范里最需要注意的字段。如果后端返回不可访问的本地路径，前端首页图标会直接失效。

---

## 3. 数据模型摘要

### 3.1 UserSession

```json
{
  "accessToken": "jwt-token",
  "refreshToken": "refresh-token",
  "expiresAt": "2026-04-30T12:00:00Z",
  "user": {
    "id": "u_demo",
    "mobile": "13800000000",
    "nickname": "企业采购员",
    "memberLevel": "PLUS企业会员"
  }
}
```

### 3.2 Merchant

```json
{
  "id": "m1",
  "name": "沃尔玛精选超市",
  "monthlySales": 12890,
  "minOrderAmount": 0,
  "deliveryFee": 5,
  "etaMinutes": 28,
  "tags": ["满99减20", "会员88折"],
  "description": "品牌商超，覆盖生鲜百货与日常补给",
  "notice": "平台严选商家，支持预约配送与电子发票",
  "rating": 4.9,
  "heroColor": "linear-gradient(180deg, #1f9af8 0%, #0e85f0 100%)",
  "logoText": "沃尔",
  "categories": [
    { "id": "fresh", "name": "新鲜水果" }
  ]
}
```

### 3.3 Product

```json
{
  "id": "p1",
  "merchantId": "m1",
  "merchantName": "沃尔玛精选超市",
  "categoryId": "fresh",
  "name": "泰国进口金枕榴莲果肉 300g",
  "sales": 342,
  "price": 39.9,
  "originalPrice": 59.9,
  "imageText": "榴莲",
  "unit": "份",
  "description": "冷链到仓，新鲜果肉即开即食",
  "stock": 180
}
```

### 3.4 Address

```json
{
  "id": "addr_1",
  "city": "北京市",
  "district": "朝阳区",
  "street": "大望路商务区",
  "detail": "SOHO现代城 A 座 10 层 1008",
  "contactName": "张采购",
  "phone": "13800000000",
  "tag": "公司",
  "isDefault": true
}
```

### 3.5 Order

```json
{
  "id": "order_1",
  "orderNo": "JD202604240001",
  "createdAt": "2026-04-24T12:00:00Z",
  "totalAmount": 79.8,
  "status": "PAID",
  "statusText": "支付成功",
  "items": [],
  "address": {}
}
```

---

## 4. 接口清单

## 4.1 认证模块

### POST `/auth/login`

**说明**: 用户登录

**请求体**:

```json
{
  "mobile": "13800000000",
  "password": "123456"
}
```

**响应体 `data`**:

```json
{
  "accessToken": "jwt-access-token",
  "refreshToken": "jwt-refresh-token",
  "expiresAt": "2026-05-01T12:00:00Z",
  "user": {
    "id": "u_demo",
    "mobile": "13800000000",
    "nickname": "企业采购员",
    "memberLevel": "PLUS企业会员"
  }
}
```

**失败场景**:
- 手机号或密码错误
- 用户被禁用

### POST `/auth/register`

**说明**: 用户注册

**请求体**:

```json
{
  "mobile": "13800000001",
  "password": "123456",
  "confirmPassword": "123456"
}
```

**响应体 `data`**:

```json
{
  "success": true
}
```

**失败场景**:
- 手机号已注册
- 密码长度不符合要求
- 两次密码不一致

### POST `/auth/logout`

**说明**: 用户退出登录

**请求头**:
- `Authorization: Bearer <token>`

**响应体 `data`**:

```json
{
  "success": true
}
```

---

## 4.2 首页与商家模块

### GET `/home`

**说明**: 首页聚合数据

**响应体 `data`**:

```json
{
  "banners": [
    {
      "id": "b1",
      "title": "京东秒送企业版",
      "subtitle": "全城优选商家，1小时安心达",
      "background": "linear-gradient(135deg, #ff7a18 0%, #e1251b 100%)"
    }
  ],
  "categories": [
    {
      "id": "supermarket",
      "name": "超市便利",
      "icon": "https://cdn.example.com/category/supermarket.png"
    }
  ],
  "featuredMerchants": []
}
```

### GET `/merchants/search?keyword=`

**说明**: 搜索商家，`keyword` 为空时返回推荐商家

**响应体 `data`**:

```json
[
  {
    "id": "m1",
    "name": "沃尔玛精选超市",
    "monthlySales": 12890,
    "minOrderAmount": 0,
    "deliveryFee": 5,
    "etaMinutes": 28,
    "tags": ["满99减20", "会员88折"],
    "description": "品牌商超，覆盖生鲜百货与日常补给",
    "notice": "平台严选商家，支持预约配送与电子发票",
    "rating": 4.9,
    "heroColor": "linear-gradient(180deg, #1f9af8 0%, #0e85f0 100%)",
    "logoText": "沃尔",
    "categories": []
  }
]
```

### GET `/merchants/{merchantId}`

**说明**: 商家详情及商品列表

**响应体 `data`**:

```json
{
  "merchant": {
    "id": "m1",
    "name": "沃尔玛精选超市",
    "monthlySales": 12890,
    "minOrderAmount": 0,
    "deliveryFee": 5,
    "etaMinutes": 28,
    "tags": ["满99减20", "会员88折"],
    "description": "品牌商超，覆盖生鲜百货与日常补给",
    "notice": "平台严选商家，支持预约配送与电子发票",
    "rating": 4.9,
    "heroColor": "linear-gradient(180deg, #1f9af8 0%, #0e85f0 100%)",
    "logoText": "沃尔",
    "categories": [
      { "id": "fresh", "name": "新鲜水果" }
    ]
  },
  "products": [
    {
      "id": "p1",
      "merchantId": "m1",
      "merchantName": "沃尔玛精选超市",
      "categoryId": "fresh",
      "name": "泰国进口金枕榴莲果肉 300g",
      "sales": 342,
      "price": 39.9,
      "originalPrice": 59.9,
      "imageText": "榴莲",
      "unit": "份",
      "description": "冷链到仓，新鲜果肉即开即食",
      "stock": 180
    }
  ]
}
```

---

## 4.3 购物车模块

### GET `/cart/items`

**说明**: 获取当前用户购物车

**响应体 `data`**:

```json
[
  {
    "id": "p1",
    "merchantId": "m1",
    "merchantName": "沃尔玛精选超市",
    "categoryId": "fresh",
    "name": "泰国进口金枕榴莲果肉 300g",
    "sales": 342,
    "price": 39.9,
    "originalPrice": 59.9,
    "imageText": "榴莲",
    "unit": "份",
    "description": "冷链到仓，新鲜果肉即开即食",
    "stock": 180,
    "quantity": 2,
    "checked": true
  }
]
```

### POST `/cart/items`

**说明**: 加入购物车

**请求体**:

```json
{
  "productId": "p1"
}
```

**响应体 `data`**: 更新后的购物车数组

### PATCH `/cart/items/{productId}`

**说明**: 修改商品数量

**请求体**:

```json
{
  "quantity": 3
}
```

**响应体 `data`**: 更新后的购物车数组

### PATCH `/cart/items/{productId}/checked`

**说明**: 修改单商品勾选状态

**请求体**:

```json
{
  "checked": true
}
```

### PATCH `/cart/merchants/{merchantId}/checked`

**说明**: 修改某商家下全部购物车商品勾选状态

**请求体**:

```json
{
  "checked": false
}
```

### PATCH `/cart/checked`

**说明**: 修改购物车全选状态

**请求体**:

```json
{
  "checked": true
}
```

### DELETE `/cart/checked`

**说明**: 删除所有已勾选商品

**响应体 `data`**: 更新后的购物车数组

---

## 4.4 地址模块

### GET `/addresses`

**说明**: 获取地址列表

**响应体 `data`**:

```json
[
  {
    "id": "addr_1",
    "city": "北京市",
    "district": "朝阳区",
    "street": "大望路商务区",
    "detail": "SOHO现代城 A 座 10 层 1008",
    "contactName": "张采购",
    "phone": "13800000000",
    "tag": "公司",
    "isDefault": true
  }
]
```

### POST `/addresses`

**说明**: 新建地址

**请求体**:

```json
{
  "city": "北京市",
  "district": "朝阳区",
  "street": "大望路商务区",
  "detail": "SOHO现代城 A 座 10 层 1008",
  "contactName": "张采购",
  "phone": "13800000000",
  "tag": "公司",
  "isDefault": true
}
```

**响应体 `data`**: 新建后的地址对象

### PUT `/addresses/{addressId}`

**说明**: 修改地址

**请求体**:

```json
{
  "city": "北京市",
  "district": "朝阳区",
  "street": "大望路商务区",
  "detail": "SOHO现代城 A 座 12 层 1201",
  "contactName": "张采购",
  "phone": "13800000000",
  "tag": "公司",
  "isDefault": true
}
```

**响应体 `data`**: 修改后的地址对象

---

## 4.5 订单模块

### GET `/orders`

**说明**: 获取当前用户订单列表，按 `createdAt` 倒序返回

**响应体 `data`**:

```json
[
  {
    "id": "order_1",
    "orderNo": "JD202604240001",
    "createdAt": "2026-04-24T12:00:00Z",
    "totalAmount": 79.8,
    "status": "PAID",
    "statusText": "支付成功",
    "items": [],
    "address": {
      "id": "addr_1",
      "city": "北京市",
      "district": "朝阳区",
      "street": "大望路商务区",
      "detail": "SOHO现代城 A 座 10 层 1008",
      "contactName": "张采购",
      "phone": "13800000000",
      "tag": "公司",
      "isDefault": true
    }
  }
]
```

### POST `/orders`

**说明**: 创建订单

**请求体**:

```json
{
  "addressId": "addr_1",
  "items": [
    {
      "productId": "p1",
      "quantity": 2
    }
  ]
}
```

**响应体 `data`**: 新建订单对象

**业务约束**:
- `addressId` 必须属于当前用户
- `items` 不可为空
- 商品必须存在
- 第一阶段允许直接下单成功，不接支付网关
- 成功后应从购物车中移除已结算商品

---

## 4.6 个人中心模块

### GET `/profile`

**说明**: 获取当前用户个人信息

**响应体 `data`**:

```json
{
  "id": "u_demo",
  "nickname": "企业采购员",
  "mobile": "13800000000",
  "avatarText": "企",
  "memberLevel": "PLUS企业会员",
  "stats": {
    "redPackets": 6,
    "coupons": 12,
    "points": 2680,
    "credit": 5000
  }
}
```

---

## 5. 建议的错误码

| code | message | 说明 |
|---|---|---|
| 0 | success | 成功 |
| 4001 | 参数错误 | 请求参数不合法 |
| 4002 | 手机号或密码错误 | 登录失败 |
| 4003 | 该手机号已注册 | 注册失败 |
| 4004 | 商品不存在或已下架 | 购物车/下单失败 |
| 4005 | 地址不存在 | 地址更新或下单失败 |
| 4006 | 请选择有效的收货地址 | 订单创建失败 |
| 4010 | 未登录或登录已过期 | token 缺失或无效 |
| 4040 | 商家不存在或已下线 | 商家详情失败 |
| 5000 | 系统异常 | 服务端异常 |

---

## 6. 第一阶段非目标

以下内容不纳入第一阶段接口实现：

- 第三方支付网关接入
- 库存锁定和并发扣减
- 优惠券核销和复杂促销结算
- 订单取消/售后/退款流程
- 后台运营管理接口
- Redis 黑名单式 JWT 强制注销

这些能力可以在前后端联调稳定后进入第二阶段。
