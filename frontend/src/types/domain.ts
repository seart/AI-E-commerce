export interface AuthUser {
  id: string
  mobile: string
  nickname: string
  memberLevel: string
  role?: 'CUSTOMER' | 'ADMIN' | 'OPERATOR'
  status?: 'ACTIVE' | 'DISABLED'
}

export interface UserSession {
  accessToken: string
  refreshToken?: string
  expiresAt?: string
  user: AuthUser
}

export interface LoginPayload {
  mobile: string
  password: string
}

export interface RegisterPayload extends LoginPayload {
  confirmPassword: string
}

export interface Category {
  id: string
  name: string
  icon: string
}

export interface Banner {
  id: string
  title: string
  subtitle: string
  background: string
}

export interface MerchantCategory {
  id: string
  name: string
}

export interface Merchant {
  id: string
  name: string
  monthlySales: number
  minOrderAmount: number
  deliveryFee: number
  etaMinutes: number
  tags: string[]
  description: string
  notice: string
  rating: number
  heroColor: string
  logoText: string
  categories: MerchantCategory[]
}

export interface Product {
  id: string
  merchantId: string
  merchantName: string
  categoryId: string
  name: string
  sales: number
  price: number
  originalPrice?: number
  imageText: string
  unit: string
  description: string
  stock: number
}

export interface MerchantDetail {
  merchant: Merchant
  products: Product[]
}

export interface HomePageData {
  banners: Banner[]
  categories: Category[]
  featuredMerchants: Merchant[]
}

export interface Address {
  id: string
  city: string
  district: string
  street: string
  detail: string
  contactName: string
  phone: string
  tag: string
  isDefault: boolean
}

export interface AddressInput {
  city: string
  district: string
  street: string
  detail: string
  contactName: string
  phone: string
  tag: string
  isDefault: boolean
}

export interface CartItem extends Product {
  quantity: number
  checked: boolean
}

export interface CartMerchantGroup {
  merchantId: string
  merchantName: string
  items: CartItem[]
}

export interface CheckoutItem {
  productId: string
  quantity: number
}

export interface OrderLine extends CartItem {
  amount: number
}

export type OrderStatus =
  | 'PENDING_PAYMENT'
  | 'PAYMENT_CLOSED'
  | 'PAID'
  | 'PREPARING'
  | 'DELIVERING'
  | 'COMPLETED'
  | 'CANCELED'
  | 'REFUND_REQUESTED'
  | 'REFUNDED'

export type PaymentChannel = 'ALIPAY_QR' | 'WECHAT_QR'

export type PaymentStatus = 'PENDING' | 'PAYING' | 'PAID' | 'CLOSED' | 'EXPIRED'

export interface Order {
  id: string
  orderNo: string
  createdAt: string
  totalAmount: number
  status: OrderStatus
  statusText: string
  paymentStatus: PaymentStatus
  paymentChannel?: PaymentChannel | null
  paidAt?: string | null
  paymentExpireAt?: string | null
  closedAt?: string | null
  items: OrderLine[]
  address: Address
}

export interface PaymentPrepayResponse {
  paymentId: string
  orderId: string
  channel: PaymentChannel
  status: PaymentStatus
  amount: number
  outTradeNo: string
  transactionId?: string | null
  qrContent: string
  expireAt: string
}

export interface PaymentStatusResponse {
  paymentId: string
  orderId: string
  channel: PaymentChannel
  status: PaymentStatus
  orderStatus: OrderStatus
  transactionId?: string | null
  expireAt?: string | null
  paidAt?: string | null
  closedAt?: string | null
}

export interface UserProfileStats {
  redPackets: number
  coupons: number
  points: number
  credit: number
}

export interface UserProfile {
  id: string
  nickname: string
  mobile: string
  avatarText: string
  memberLevel: string
  stats: UserProfileStats
}
