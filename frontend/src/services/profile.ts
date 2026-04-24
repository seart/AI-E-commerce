import { request } from '@/services/request'
import { mockServer } from '@/services/mock/server'
import type { UserProfile } from '@/types/domain'

export const profileService = {
  getProfile() {
    return request<UserProfile>(
      {
        method: 'get',
        url: '/profile',
      },
      () => mockServer.getProfile(),
    )
  },
}
