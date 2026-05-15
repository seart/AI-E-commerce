import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useSessionStore } from '@/stores/session'
import type { ApiEnvelope } from '@/types/domain'

// 后台管理端共享 axios 实例。
// VITE_API_BASE_URL=/api 时，请求 `/admin/orders` 会变成 `/api/admin/orders`。
export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 10000,
})

// 后台所有请求都需要带 JWT；后端会根据角色判断是否允许访问 `/admin/**`。
http.interceptors.request.use((config) => {
  const session = useSessionStore()
  if (session.accessToken) {
    config.headers.Authorization = `Bearer ${session.accessToken}`
  }
  config.headers['X-Requested-With'] = 'XMLHttpRequest'
  return config
})

// 统一拆后端 ApiEnvelope；401 时清理会话并回登录页，避免继续使用失效 token。
http.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiEnvelope<unknown>
    if (payload && typeof payload === 'object' && 'code' in payload) {
      if (payload.code !== 0) {
        return Promise.reject(new Error(payload.message || '请求失败'))
      }
      return payload.data
    }
    return response.data
  },
  (error) => {
    const message = error.response?.data?.message || error.message || '网络请求失败'
    if (error.response?.status === 401) {
      useSessionStore().clear()
      router.replace('/login')
    }
    ElMessage.error(message)
    return Promise.reject(new Error(message))
  },
)
