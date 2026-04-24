import { STORAGE_KEYS } from '@/constants/storage'
import type { UserSession } from '@/types/domain'
import { loadJson, removeStorage, saveJson } from '@/utils/storage'

export function getStoredSession() {
  return loadJson<UserSession | null>(STORAGE_KEYS.session, null)
}

export function setStoredSession(session: UserSession) {
  saveJson(STORAGE_KEYS.session, session)
}

export function clearStoredSession() {
  removeStorage(STORAGE_KEYS.session)
}

export function getAccessToken() {
  return getStoredSession()?.accessToken ?? ''
}

export function hasStoredSession() {
  return Boolean(getAccessToken())
}
