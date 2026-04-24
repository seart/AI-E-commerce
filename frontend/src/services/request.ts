import type { AxiosRequestConfig } from 'axios'
import { ENV } from '@/config/env'
import { http } from '@/api/http'

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
