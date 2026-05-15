import type { Address } from '@/types/domain'

// 金额展示统一保留两位小数，避免各页面格式不一致。
export function formatCurrency(amount: number) {
  return amount.toFixed(2)
}

export function formatAddress(address?: Address | null) {
  // 地址按省略空字段的方式拼接，避免出现多余空格。
  if (!address) {
    return ''
  }

  return [address.city, address.district, address.street, address.detail].filter(Boolean).join(' ')
}

export function formatMaskedPhone(phone: string) {
  // 手机号脱敏用于地址、个人中心等展示场景。
  if (phone.length < 7) {
    return phone
  }

  return `${phone.slice(0, 3)}****${phone.slice(-4)}`
}

export function formatDateTime(iso: string) {
  // 后端返回 ISO 时间，前端统一转成中文日期时间展示。
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(iso))
}
