import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authService } from '@/services/auth'
import { clearStoredSession, getStoredSession, setStoredSession } from '@/services/session'
import type { LoginPayload, RegisterPayload, UserSession } from '@/types/domain'

export const useAuthStore = defineStore('auth', () => {
  const session = ref<UserSession | null>(getStoredSession())
  const submitting = ref(false)

  const user = computed(() => session.value?.user ?? null)
  const isAuthenticated = computed(() => Boolean(session.value?.accessToken))

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
