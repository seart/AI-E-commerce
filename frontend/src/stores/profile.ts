import { ref } from 'vue'
import { defineStore } from 'pinia'
import { profileService } from '@/services/profile'
import type { UserProfile } from '@/types/domain'

export const useProfileStore = defineStore('profile', () => {
  const profile = ref<UserProfile | null>(null)
  const loading = ref(false)

  async function loadProfile(force = false) {
    if (loading.value || (profile.value && !force)) {
      return profile.value
    }

    loading.value = true
    try {
      profile.value = await profileService.getProfile()
      return profile.value
    } finally {
      loading.value = false
    }
  }

  function reset() {
    profile.value = null
  }

  return {
    profile,
    loading,
    loadProfile,
    reset,
  }
})
