import { request } from '@/services/request'
import { mockServer } from '@/services/mock/server'
import type { LoginPayload, RegisterPayload, UserSession } from '@/types/domain'

// 用户端认证接口。这里的 URL 会和 VITE_API_BASE_URL 拼接成最终后端地址，
// 例如生产配置 `/api` + `/auth/login` = `/api/auth/login`。
export const authService = {
  login(payload: LoginPayload) {
    return request<UserSession>(
      {
        method: 'post',
        url: '/auth/login',
        data: payload,
      },
      () => mockServer.login(payload),
    )
  },

  register(payload: RegisterPayload) {
    return request<{ success: boolean }>(
      {
        method: 'post',
        url: '/auth/register',
        data: payload,
      },
      () => mockServer.register(payload),
    )
  },

  logout() {
    return request<{ success: boolean }>(
      {
        method: 'post',
        url: '/auth/logout',
      },
      () => mockServer.logout(),
    )
  },
}
