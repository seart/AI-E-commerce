import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useSessionStore } from '@/stores/session'
import type { ApiEnvelope } from '@/types/domain'

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 10000,
})

http.interceptors.request.use((config) => {
  const session = useSessionStore()
  if (session.accessToken) {
    config.headers.Authorization = `Bearer ${session.accessToken}`
  }
  config.headers['X-Requested-With'] = 'XMLHttpRequest'
  return config
})

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
