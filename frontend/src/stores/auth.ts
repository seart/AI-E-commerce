import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authService } from '@/services/auth'
import { clearStoredSession, getStoredSession, setStoredSession } from '@/services/session'
import type { LoginPayload, RegisterPayload, UserSession } from '@/types/domain'

// 用户端登录态 store：负责把登录结果同时放进内存和 localStorage。
export const useAuthStore = defineStore('auth', () => {
  const session = ref<UserSession | null>(getStoredSession())
  const submitting = ref(false)

  const user = computed(() => session.value?.user ?? null)
  const isAuthenticated = computed(() => Boolean(session.value?.accessToken))

  // 应用启动或刷新页面后，从本地缓存恢复会话。
  function hydrate() {
    session.value = getStoredSession()
  }

  async function login(payload: LoginPayload) {
    submitting.value = true
    try {
      const nextSession = await authService.login(payload)
      session.value = nextSession
      setStoredSession(nextSession)
      return nextSession
    } finally {
      submitting.value = false
    }
  }

  async function register(payload: RegisterPayload) {
    submitting.value = true
    try {
      return await authService.register(payload)
    } finally {
      submitting.value = false
    }
  }

  async function logout() {
    try {
      await authService.logout()
    } finally {
      // 即使后端登出请求失败，也清理前端本地会话，避免用户继续停留在已登录界面。
      session.value = null
      clearStoredSession()
    }
  }

  return {
    session,
    user,
    submitting,
    isAuthenticated,
    hydrate,
    login,
    register,
    logout,
  }
})
