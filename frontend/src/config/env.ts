const apiBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim() ?? ''
const mockFlag = import.meta.env.VITE_ENABLE_MOCK?.trim()

// 前端环境变量集中出口，避免页面和 service 到处直接读取 import.meta.env。
export const ENV = {
  apiBaseUrl,
  appTitle: import.meta.env.VITE_APP_TITLE?.trim() || '企业版商城',
  requestTimeout: Number(import.meta.env.VITE_API_TIMEOUT ?? 15000),
  // 本地开发未配置真实 API 时自动走 mock；配置 VITE_API_BASE_URL 后才会请求后端。
  enableMock: mockFlag === 'true' || (!apiBaseUrl && import.meta.env.DEV),
}
