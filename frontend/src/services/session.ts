import { STORAGE_KEYS } from '@/constants/storage'
import type { UserSession } from '@/types/domain'
import { loadJson, removeStorage, saveJson } from '@/utils/storage'

// 用户端会话存取集中在这里，避免各页面直接操作 localStorage。
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
  // http 拦截器通过这个方法拿 Bearer Token。
  return getStoredSession()?.accessToken ?? ''
}

export function hasStoredSession() {
  return Boolean(getAccessToken())
}
