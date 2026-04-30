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

export type CommonStatus = 'ACTIVE' | 'DISABLED'
export type ProductStatus = 'DRAFT' | 'ON_SHELF' | 'OFF_SHELF'
export type CategoryType = 'CHANNEL' | 'PRODUCT'

export interface Category {
  id: string
  name: string
  icon: string
  parentId?: string | null
  level: number
  type: CategoryType
  status: CommonStatus
  sortOrder: number
}

export interface CategoryUpsertRequest {
  id?: string
  name: string
  icon?: string
  parentId?: string | null
  level?: number
  type?: CategoryType
  status?: CommonStatus
  sortOrder?: number
}

export interface Brand {
  id: string
  name: string
  logo: string
  description: string
  status: CommonStatus
  sortOrder: number
}

export interface BrandUpsertRequest {
  id?: string
  name: string
  logo?: string
  description?: string
  status?: CommonStatus
  sortOrder?: number
}

export interface SpecOption {
  id: string
  groupId: string
  name: string
  status: CommonStatus
  sortOrder: number
}

export interface SpecGroup {
  id: string
  name: string
  status: CommonStatus
  sortOrder: number
  options: SpecOption[]
}

export interface SpecGroupUpsertRequest {
  id?: string
  name: string
  status?: CommonStatus
  sortOrder?: number
}

export interface SpecOptionUpsertRequest {
  id?: string
  name: string
  status?: CommonStatus
  sortOrder?: number
}

export interface SkuSpec {
  groupId: string
  groupName: string
  optionId: string
  optionName: string
}

export interface ProductSku {
  skuId: string
  productId: string
  spuId: string
  skuCode: string
  specs: SkuSpec[]
  specText: string
  price: number
  originalPrice: number
  unit: string
  stock: number
  status: 'ON_SHELF' | 'OFF_SHELF'
}

export interface ProductSkuUpsertRequest {
  skuId?: string
  skuCode?: string
  specs: SkuSpec[]
  price: number
  originalPrice: number
  unit: string
  stock: number
  status: 'ON_SHELF' | 'OFF_SHELF'
}

export interface ProductSpu {
  id: string
  merchantId: string
  merchantName: string
  categoryId: string
  categoryName: string
  brandId?: string | null
  brandName: string
  name: string
  subtitle: string
  mainImage: string
  detail: string
  detailImages: string[]
  status: ProductStatus
  sortOrder: number
  skuCount: number
  totalStock: number
  minPrice: number
  maxPrice: number
  skus: ProductSku[]
}

export interface ProductSpuUpsertRequest {
  id?: string
  merchantId: string
  categoryId: string
  brandId?: string | null
  name: string
  subtitle?: string
  mainImage?: string
  detail?: string
  detailImages?: string[]
  status?: ProductStatus
  sortOrder?: number
  skus?: ProductSkuUpsertRequest[]
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
