import { http } from '@/api/http'
import type {
  AdminUser,
  AuditLog,
  DashboardResponse,
  Merchant,
  Order,
  Product,
  SessionResponse,
} from '@/types/domain'

export function login(mobile: string, password: string) {
  return http.post<SessionResponse, SessionResponse>('/auth/login', { mobile, password })
}

export function logout() {
  return http.post<unknown, unknown>('/auth/logout')
}

export function getDashboard() {
  return http.get<DashboardResponse, DashboardResponse>('/admin/dashboard/summary')
}

export function getMerchants() {
  return http.get<Merchant[], Merchant[]>('/admin/merchants')
}

export function saveMerchant(merchant: Partial<Merchant>) {
  return merchant.id
    ? http.put<Merchant, Merchant>(`/admin/merchants/${merchant.id}`, merchant)
    : http.post<Merchant, Merchant>('/admin/merchants', merchant)
}

export function getProducts() {
  return http.get<Product[], Product[]>('/admin/products')
}

export function saveProduct(product: Partial<Product>) {
  return product.id
    ? http.put<Product, Product>(`/admin/products/${product.id}`, product)
    : http.post<Product, Product>('/admin/products', product)
}

export function getOrders() {
  return http.get<Order[], Order[]>('/admin/orders')
}

export function updateOrderStatus(orderId: string, status: string, reason = '后台操作') {
  return http.patch<Order, Order>(`/admin/orders/${orderId}/status`, { status, reason })
}

export function getUsers() {
  return http.get<AdminUser[], AdminUser[]>('/admin/users')
}

export function updateUserStatus(userId: string, status: string) {
  return http.patch<AdminUser, AdminUser>(`/admin/users/${userId}/status`, { status })
}

export function getAuditLogs(params: { actorId?: string; action?: string; from?: string; to?: string }) {
  return http.get<AuditLog[], AuditLog[]>('/admin/audit-logs', { params })
}
