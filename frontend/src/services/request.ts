import type { AxiosRequestConfig } from 'axios'
import { ENV } from '@/config/env'
import { http } from '@/api/http'

// 业务 service 的统一请求入口：
// - enableMock=true 时走本地 mockServer，方便纯前端调试。
// - enableMock=false 时走真实后端接口。
export async function request<T>(
  config: AxiosRequestConfig,
  mockHandler?: () => Promise<T>,
) {
  if (ENV.enableMock) {
    if (!mockHandler) {
      throw new Error('当前请求未配置 mock 处理器')
    }

    return mockHandler()
  }

  return http.request<T, T>(config)
}
