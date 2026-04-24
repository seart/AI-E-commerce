import { request } from '@/services/request'
import { mockServer } from '@/services/mock/server'
import type { LoginPayload, RegisterPayload, UserSession } from '@/types/domain'

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
