export function loadJson<T>(key: string, fallback: T): T {
  // SSR 或测试环境没有 window 时直接返回默认值。
  if (typeof window === 'undefined') {
    return fallback
  }

  const rawValue = window.localStorage.getItem(key)
  if (!rawValue) {
    return fallback
  }

  try {
    return JSON.parse(rawValue) as T
  } catch {
    // 本地缓存被手工改坏时不让页面崩溃，回退到调用方提供的默认值。
    return fallback
  }
}

export function saveJson<T>(key: string, value: T) {
  // 统一 JSON 序列化写入 localStorage。
  if (typeof window === 'undefined') {
    return
  }

  window.localStorage.setItem(key, JSON.stringify(value))
}

export function removeStorage(key: string) {
  if (typeof window === 'undefined') {
    return
  }

  window.localStorage.removeItem(key)
}
