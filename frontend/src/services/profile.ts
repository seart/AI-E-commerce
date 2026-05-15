import { request } from '@/services/request'
import { mockServer } from '@/services/mock/server'
import type { UserProfile } from '@/types/domain'

// 个人中心接口，读取当前登录用户资料和统计信息。
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
