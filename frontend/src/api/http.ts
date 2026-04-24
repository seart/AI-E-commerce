import axios, { AxiosError } from 'axios'
import { ENV } from '@/config/env'
import { getAccessToken } from '@/services/session'
import type { ApiEnvelope, ApiErrorPayload } from '@/types/api'

function isEnvelope<T>(value: unknown): value is ApiEnvelope<T> {
  return Boolean(
    value &&
      typeof value === 'object' &&
      'code' in value &&
      'message' in value &&
      'data' in value,
  )
}

export const http = axios.create({
  baseURL: ENV.apiBaseUrl || undefined,
  timeout: ENV.requestTimeout,
})

http.interceptors.request.use((config) => {
  const token = getAccessToken()
  config.headers = config.headers ?? {}

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  config.headers['X-Requested-With'] = 'XMLHttpRequest'

  return config
})

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
