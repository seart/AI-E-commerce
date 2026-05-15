import { http } from '@/api/http'
import type {
  AdminUser,
  AuditLog,
  Brand,
  BrandUpsertRequest,
  Category,
  CategoryUpsertRequest,
  DashboardResponse,
  InventoryAccount,
  InventoryTransaction,
  Merchant,
  Order,
  Product,
  ProductSku,
  ProductSkuUpsertRequest,
  ProductSpu,
  ProductSpuUpsertRequest,
  SessionResponse,
  SpecGroup,
  SpecGroupUpsertRequest,
  SpecOption,
  SpecOptionUpsertRequest,
} from '@/types/domain'

// 后台 API 集中在这个文件，路径全部以 `/admin/**` 表示运营后台接口。
// 最终地址 = VITE_API_BASE_URL + 这里的路径，例如 `/api/admin/products`。
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
  // 有 id 表示编辑已有商家；没有 id 表示新建商家。
  return merchant.id
    ? http.put<Merchant, Merchant>(`/admin/merchants/${merchant.id}`, merchant)
    : http.post<Merchant, Merchant>('/admin/merchants', merchant)
}

export function getProducts() {
  return http.get<Product[], Product[]>('/admin/products')
}

export function saveProduct(product: Partial<Product>) {
  // 兼容旧的单商品接口；新的 SPU/SKU 商品中心接口在下方 product-spus 区域。
  return product.id
    ? http.put<Product, Product>(`/admin/products/${product.id}`, product)
    : http.post<Product, Product>('/admin/products', product)
}

export function getCategories() {
  return http.get<Category[], Category[]>('/admin/categories')
}

export function saveCategory(category: CategoryUpsertRequest) {
  return category.id
    ? http.put<Category, Category>(`/admin/categories/${category.id}`, category)
    : http.post<Category, Category>('/admin/categories', category)
}

export function updateCategoryStatus(categoryId: string, status: Category['status']) {
  return http.patch<Category, Category>(`/admin/categories/${categoryId}/status`, { status })
}

export function getBrands() {
  return http.get<Brand[], Brand[]>('/admin/brands')
}

export function saveBrand(brand: BrandUpsertRequest) {
  return brand.id
    ? http.put<Brand, Brand>(`/admin/brands/${brand.id}`, brand)
    : http.post<Brand, Brand>('/admin/brands', brand)
}

export function updateBrandStatus(brandId: string, status: Brand['status']) {
  return http.patch<Brand, Brand>(`/admin/brands/${brandId}/status`, { status })
}

export function getSpecGroups() {
  return http.get<SpecGroup[], SpecGroup[]>('/admin/spec-groups')
}

export function saveSpecGroup(group: SpecGroupUpsertRequest) {
  return group.id
    ? http.put<SpecGroup, SpecGroup>(`/admin/spec-groups/${group.id}`, group)
    : http.post<SpecGroup, SpecGroup>('/admin/spec-groups', group)
}

export function saveSpecOption(groupId: string, option: SpecOptionUpsertRequest) {
  return option.id
    ? http.put<SpecOption, SpecOption>(`/admin/spec-options/${option.id}`, option)
    : http.post<SpecOption, SpecOption>(`/admin/spec-groups/${groupId}/options`, option)
}

export function updateSpecOptionStatus(optionId: string, status: SpecOption['status']) {
  return http.patch<SpecOption, SpecOption>(`/admin/spec-options/${optionId}/status`, { status })
}

export function getProductSpus() {
  return http.get<ProductSpu[], ProductSpu[]>('/admin/product-spus')
}

export function getProductSpu(spuId: string) {
  return http.get<ProductSpu, ProductSpu>(`/admin/product-spus/${spuId}`)
}

export function saveProductSpu(spu: ProductSpuUpsertRequest) {
  // 商品 SPU 保存：同一个函数同时处理新增和编辑，页面无需关心 HTTP method。
  return spu.id
    ? http.put<ProductSpu, ProductSpu>(`/admin/product-spus/${spu.id}`, spu)
    : http.post<ProductSpu, ProductSpu>('/admin/product-spus', spu)
}

export function updateProductSpuStatus(spuId: string, status: ProductSpu['status']) {
  return http.patch<ProductSpu, ProductSpu>(`/admin/product-spus/${spuId}/status`, { status })
}

export function saveProductSku(spuId: string, sku: ProductSkuUpsertRequest) {
  // SKU 必须挂在某个 SPU 下保存，后端用 spuId 保证规格归属关系。
  return sku.skuId
    ? http.put<ProductSku, ProductSku>(`/admin/product-spus/${spuId}/skus/${sku.skuId}`, sku)
    : http.post<ProductSku, ProductSku>(`/admin/product-spus/${spuId}/skus`, sku)
}

export function updateProductSkuStatus(spuId: string, skuId: string, status: ProductSku['status']) {
  return http.patch<ProductSku, ProductSku>(`/admin/product-spus/${spuId}/skus/${skuId}/status`, { status })
}

export function getInventoryAccounts(params: { keyword?: string; lowStockOnly?: boolean } = {}) {
  return http.get<InventoryAccount[], InventoryAccount[]>('/admin/inventory/accounts', { params })
}

export function getInventoryTransactions(
  params: { skuId?: string; orderId?: string; bizType?: string; limit?: number } = {},
) {
  return http.get<InventoryTransaction[], InventoryTransaction[]>('/admin/inventory/transactions', { params })
}

export function adjustInventory(skuId: string, payload: { delta: number; reason: string }) {
  return http.post<InventoryAccount, InventoryAccount>(`/admin/inventory/accounts/${skuId}/adjust`, payload)
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
