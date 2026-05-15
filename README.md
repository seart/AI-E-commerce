#企业版电商项目运行说明

这是一个仿的企业级电商项目，包含三个独立部分：

- `frontend/`：移动端用户页面，基于 `Vue 3 + Vite`。
- `admin/`：后台管理页面，基于 `Vue 3 + Vite + Element Plus`。
- `backend/`：Spring Boot 后端，使用 Maven 构建，接口统一挂载在 `/api` 下。

项目里的前端命令统一使用 `pnpm`，不要使用 `npm install` 或 `npm run`。

## 1. .env.example 是什么环境

`.env.example` 不是某一个运行环境，它只是“环境变量模板”。

它的作用是告诉新同学：“这个项目需要哪些环境变量”。你真正本地运行时，一般复制它生成 `.env.local`：

```bash
cd frontend
cp .env.example .env.local
```

然后修改 `.env.local` 里的值。`.env.local` 通常只给自己电脑使用，不提交到 Git。

Vite 常见环境文件含义：

- `.env.example`：模板文件，不会自动生效，给人看的。
- `.env.local`：本地私有配置，会自动生效，通常不提交。
- `.env.test`：测试环境打包配置，执行 `vite build --mode test` 时生效。
- `.env.production`：生产环境打包配置，执行 `vite build` 或 `vite build --mode production` 时生效。

一般标准写法确实会分成本地、测试、生产三类：

- 本地开发：`.env.local`
- 测试环境：`.env.test`
- 生产环境：`.env.production`

注意：前端里以 `VITE_` 开头的变量会被打进浏览器代码里，不能放密码、JWT 密钥、数据库账号这类敏感信息。

## 2. 后端启动

后端默认地址：

```text
http://localhost:8080/api
```

先启动项目需要的 MySQL 和 Redis。当前后端 README 里使用的是本机 Docker Compose：

```bash
docker compose -f /Users/love/Documents/docker/docker-compose-file/jindong/docker-compose.yml up -d
```

再启动后端：

```bash
cd /Users/love/Documents/code/local/Front/AI-E-commerce/backend
mvn spring-boot:run
```

后端默认配置：

- API：`http://localhost:8080/api`
- Swagger：`http://localhost:8080/api/swagger-ui.html`
- MySQL：`localhost:33061/jingdong`
- Redis：`localhost:63791`

## 3. 移动端本地对接后端

移动端项目在 `frontend/`。

第一次运行：

```bash
cd /Users/love/Documents/code/local/Front/AI-E-commerce/frontend
pnpm install
cp .env.example .env.local
```

把 `frontend/.env.local` 改成：

```env
VITE_APP_TITLE=企业版商城
VITE_API_BASE_URL=http://localhost:8080/api
VITE_ENABLE_MOCK=false
VITE_API_TIMEOUT=15000
```

启动移动端：

```bash
pnpm dev
```

访问地址：

```text
http://localhost:5173
```

特别注意：`frontend/.env.example` 里 `VITE_API_BASE_URL` 默认是空的。移动端代码里有一个规则：开发环境下如果没有配置真实 API 地址，就会启用 mock 数据。要真实对接后端，必须在 `.env.local` 里配置 `VITE_API_BASE_URL=http://localhost:8080/api`。

## 4. 后台管理端本地对接后端

后台项目在 `admin/`。

第一次运行：

```bash
cd /Users/love/Documents/code/local/Front/AI-E-commerce/admin
pnpm install
cp .env.example .env.local
```

确认 `admin/.env.local` 是：

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

启动后台：

```bash
pnpm dev
```

访问地址：

```text
http://localhost:5174
```

后台默认管理员账号：

```text
13900000000 / 123456
```

## 5. 同时启动后端、移动端、后台

最适合新手的方式是开三个终端。

终端 1：启动后端

```bash
cd /Users/love/Documents/code/local/Front/AI-E-commerce/backend
mvn spring-boot:run
```

终端 2：启动移动端

```bash
cd /Users/love/Documents/code/local/Front/AI-E-commerce/frontend
pnpm dev
```

终端 3：启动后台

```bash
cd /Users/love/Documents/code/local/Front/AI-E-commerce/admin
pnpm dev
```

启动成功后：

- 移动端：`http://localhost:5173`
- 后台端：`http://localhost:5174`
- 后端 API：`http://localhost:8080/api`

如果你已经比较熟悉终端，也可以用一个终端同时启动两个前端：

```bash
cd /Users/love/Documents/code/local/Front/AI-E-commerce
(cd frontend && pnpm dev) & (cd admin && pnpm dev) & wait
```

## 6. 测试环境打包

项目已经提供测试环境配置：

- `frontend/.env.test`
- `admin/.env.test`

现在默认测试 API 也指向本地后端：

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

如果你有真正的测试服务器，比如：

```text
https://test-api.example.com/api
```

就把两个 `.env.test` 里的 `VITE_API_BASE_URL` 改成真实测试服务器地址。

移动端测试环境打包：

```bash
cd /Users/love/Documents/code/local/Front/AI-E-commerce/frontend
pnpm build:test
```

后台测试环境打包：

```bash
cd /Users/love/Documents/code/local/Front/AI-E-commerce/admin
pnpm build:test
```

打包完成后，产物在：

- `frontend/dist/`
- `admin/dist/`

## 7. 正式环境打包

项目已有生产环境配置：

- `frontend/.env.production`
- `admin/.env.production`

当前生产环境建议分成三个路径：

```text
/          移动端页面
/admin/   后台管理页面
/api/     后端接口
```

这里一定要分清楚：

- 页面路径：用户在浏览器里打开的前端页面地址，比如 `/`、`/admin/`。
- 接口路径：前端请求后端数据的地址，比如 `/api/orders`、`/api/admin/orders`。

两个前端都可以使用同一个接口基础地址：

```env
VITE_API_BASE_URL=/api
```

但这不代表两个页面都部署到 `/api`。`/api` 只能给后端接口使用。

移动端接口示例：

```text
VITE_API_BASE_URL=/api
移动端请求 /orders
最终地址 /api/orders
```

后台接口示例：

```text
VITE_API_BASE_URL=/api
后台请求 /admin/orders
最终地址 /api/admin/orders
```

它们最终请求的是不同后端接口，不会混在一起。

后台生产环境还多了一个页面基础路径：

```env
VITE_APP_BASE=/admin/
```

它表示后台页面部署在 `/admin/` 下，例如：

```text
http://192.168.1.2/admin/
```

移动端正式打包：

```bash
cd /Users/love/Documents/code/local/Front/AI-E-commerce/frontend
pnpm build:prod
```

后台正式打包：

```bash
cd /Users/love/Documents/code/local/Front/AI-E-commerce/admin
pnpm build:prod
```

打包完成后，同样产出：

- `frontend/dist/`
- `admin/dist/`

## 8. 生产部署示例：前端和后端在不同服务器

假设：

```text
前端服务器：192.168.1.2
后端服务器：192.168.2.3:8080
```

推荐让浏览器只访问前端服务器：

```text
移动端页面：http://192.168.1.2/
后台页面：http://192.168.1.2/admin/
接口地址：http://192.168.1.2/api/...
```

再由 `192.168.1.2` 上的 Nginx 把 `/api/` 转发到后端：

```nginx
server {
    listen 80;
    server_name 192.168.1.2;

    root /data/www/frontend;
    index index.html;

    location /api/ {
        proxy_pass http://192.168.2.3:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /admin/ {
        alias /data/www/admin/;
        try_files $uri $uri/ /admin/index.html;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

这个配置下：

- 浏览器请求 `/api/orders`，Nginx 转发到 `http://192.168.2.3:8080/api/orders`。
- 浏览器请求 `/api/admin/orders`，Nginx 转发到 `http://192.168.2.3:8080/api/admin/orders`。
- 移动端页面和后台页面不会共用同一个页面路径。

## 9. 常用验证命令

后端测试：

```bash
cd /Users/love/Documents/code/local/Front/AI-E-commerce/backend
mvn clean test
```

移动端构建验证：

```bash
cd /Users/love/Documents/code/local/Front/AI-E-commerce/frontend
pnpm build:prod
```

后台构建验证：

```bash
cd /Users/love/Documents/code/local/Front/AI-E-commerce/admin
pnpm build:prod
```

## 10. 新手常见问题

问题：为什么我启动移动端后看到的是假数据？

回答：因为 `frontend` 在开发环境下，如果 `VITE_API_BASE_URL` 为空，会自动启用 mock。请检查 `frontend/.env.local`，确保：

```env
VITE_API_BASE_URL=http://localhost:8080/api
VITE_ENABLE_MOCK=false
```

问题：为什么生产环境写 `/api`，而不是 `http://localhost:8080/api`？

回答：`localhost` 只代表访问者自己的电脑。正式上线后，用户浏览器里的 `localhost` 不是你的服务器，所以生产环境一般写 `/api`，由 Nginx 或网关转发到真正后端。

问题：移动端和后台的 `VITE_API_BASE_URL` 都写 `/api`，请求会不会乱？

回答：不会。`VITE_API_BASE_URL` 只是后端接口前缀。移动端请求的是 `/api/orders`、`/api/home`；后台请求的是 `/api/admin/orders`、`/api/admin/products`。真正不能相同的是页面部署路径：移动端建议部署到 `/`，后台建议部署到 `/admin/`。

问题：移动端和后台能不能合并成一个前端项目？

回答：当前项目结构要求它们保持独立。`frontend/` 是移动端用户应用，`admin/` 是运营后台应用，不要把后台路由合并进移动端。
