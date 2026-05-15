import { defineStore } from 'pinia'
import type { AdminUser, SessionResponse } from '@/types/domain'

const STORAGE_KEY = 'jingdong-admin-session'

// 后台登录态 store：只保存后台需要的 accessToken 和当前用户信息。
export const useSessionStore = defineStore('session', {
  state: () => ({
    accessToken: '',
    user: null as AdminUser | null,
  }),
  getters: {
    // 前端菜单守卫只做体验层判断；后端 `/admin/**` 仍会再次校验角色。
    isAdmin: (state) => state.user?.role === 'ADMIN' || state.user?.role === 'OPERATOR',
  },
  actions: {
    hydrate() {
      // 页面刷新后从 localStorage 恢复后台登录态。
      const raw = localStorage.getItem(STORAGE_KEY)
      if (!raw) return
      const session = JSON.parse(raw) as Pick<SessionResponse, 'accessToken' | 'user'>
      this.accessToken = session.accessToken
      this.user = session.user
    },
    setSession(session: SessionResponse) {
      // 登录成功后持久化最小会话数据，避免把无关响应字段写进本地缓存。
      this.accessToken = session.accessToken
      this.user = session.user
      localStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({ accessToken: session.accessToken, user: session.user }),
      )
    },
    clear() {
      this.accessToken = ''
      this.user = null
      localStorage.removeItem(STORAGE_KEY)
    },
  },
})
