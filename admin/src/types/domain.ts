export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

export interface AdminUser {
  id: string
  mobile: string
  nickname: string
  memberLevel: string
  role: 'CUSTOMER' | 'ADMIN' | 'OPERATOR'
  status: 'ACTIVE' | 'DISABLED'
  lastLoginAt?: string | null
}

export interface SessionResponse {
  accessToken: string
  refreshToken: string
  expiresAt: string
  user: AdminUser
}

export interface DashboardResponse {
  summary: {
    orderCount: number
    salesAmount: number
    productCount: number
    userCount: number
  }
}

export interface Merchant {
  id: string
  name: string
  sales: number
  minOrderPrice: number
  deliveryFee: number
  deliveryMinutes: number
  description: string
  notice: string
  rating: number
  logoBackground: string
  logoText: string
  status: 'ACTIVE' | 'DISABLED'
  sortOrder: number
}

export interface Product {
  id: string
  merchantId: string
  merchantName: string
  categoryId: string
  name: string
  sales: number
  price: number
  originalPrice: number
  imageText: string
  unit: string
  description: string
  stock: number
  status: 'ON_SHELF' | 'OFF_SHELF'
  sortOrder: number
}

export interface Order {
  id: string
  orderNo: string
  createdAt: string
  totalAmount: number
  status: string
  statusText: string
  paymentStatus: 'PENDING' | 'PAYING' | 'PAID' | 'CLOSED' | 'EXPIRED'
  paymentChannel?: 'ALIPAY_QR' | 'WECHAT_QR' | null
  paidAt?: string | null
  paymentExpireAt?: string | null
  closedAt?: string | null
  items: Array<{ id: string; name: string; quantity: number; amount: number }>
  address: {
    contactName: string
    phone: string
    city: string
    district: string
    detail: string
  }
}

export interface AuditLog {
  id: string
  actorId: string | null
  actorRole: string | null
  action: string
  targetType: string
  targetId: string | null
  detail: string | null
  requestId: string | null
  createdAt: string
}
