import axios, { AxiosError } from 'axios'
import { ENV } from '@/config/env'
import { getAccessToken } from '@/services/session'
import type { ApiEnvelope, ApiErrorPayload } from '@/types/api'

// 后端统一响应通常是 { code, message, data }，这个类型守卫用于兼容少数直接返回 data 的接口。
function isEnvelope<T>(value: unknown): value is ApiEnvelope<T> {
  return Boolean(
    value &&
      typeof value === 'object' &&
      'code' in value &&
      'message' in value &&
      'data' in value,
  )
}

// 全项目共享的 axios 实例：baseURL 来自 VITE_API_BASE_URL。
// 生产部署常见值是 `/api`，浏览器最终会请求当前域名下的 `/api/**`。
export const http = axios.create({
  baseURL: ENV.apiBaseUrl || undefined,
  timeout: ENV.requestTimeout,
})

// 请求前统一注入 JWT 和 AJAX 标识，业务 service 不需要重复写请求头。
http.interceptors.request.use((config) => {
  const token = getAccessToken()
  config.headers = config.headers ?? {}

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  config.headers['X-Requested-With'] = 'XMLHttpRequest'

  return config
})

// 响应层只把成功业务数据返回给页面；业务错误和网络错误统一转成 Error。
http.interceptors.response.use(
  (response) => {
    const payload = response.data

    if (isEnvelope(payload)) {
      if (payload.code !== 0 && payload.code !== 200) {
        return Promise.reject(new Error(payload.message || '请求失败'))
      }
      return payload.data
    }

    return payload
  },
  (error: AxiosError<ApiErrorPayload>) => {
    const message =
      error.response?.data?.message ||
      error.message ||
      '网络请求失败，请稍后再试'

    return Promise.reject(new Error(message))
  },
)
