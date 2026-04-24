import type { Address } from '@/types/domain'

export function formatCurrency(amount: number) {
  return amount.toFixed(2)
}

export function formatAddress(address?: Address | null) {
  if (!address) {
    return ''
  }

  return [address.city, address.district, address.street, address.detail].filter(Boolean).join(' ')
}

export function formatMaskedPhone(phone: string) {
  if (phone.length < 7) {
    return phone
  }

  return `${phone.slice(0, 3)}****${phone.slice(-4)}`
}

export function formatDateTime(iso: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(iso))
}
