import { defineStore } from 'pinia'
import type { AdminUser, SessionResponse } from '@/types/domain'

const STORAGE_KEY = 'jingdong-admin-session'

export const useSessionStore = defineStore('session', {
  state: () => ({
    accessToken: '',
    user: null as AdminUser | null,
  }),
  getters: {
    isAdmin: (state) => state.user?.role === 'ADMIN' || state.user?.role === 'OPERATOR',
  },
  actions: {
    hydrate() {
      const raw = localStorage.getItem(STORAGE_KEY)
      if (!raw) return
      const session = JSON.parse(raw) as Pick<SessionResponse, 'accessToken' | 'user'>
      this.accessToken = session.accessToken
      this.user = session.user
    },
    setSession(session: SessionResponse) {
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
