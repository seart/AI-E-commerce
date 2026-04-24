const apiBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim() ?? ''
const mockFlag = import.meta.env.VITE_ENABLE_MOCK?.trim()

export const ENV = {
  apiBaseUrl,
  appTitle: import.meta.env.VITE_APP_TITLE?.trim() || '京东企业版商城',
  requestTimeout: Number(import.meta.env.VITE_API_TIMEOUT ?? 15000),
  enableMock: mockFlag === 'true' || (!apiBaseUrl && import.meta.env.DEV),
}
