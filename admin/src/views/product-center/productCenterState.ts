import type {
  Category,
  CommonStatus,
  ProductSku,
  ProductSkuUpsertRequest,
  ProductSpu,
  ProductSpuUpsertRequest,
  ProductStatus,
  SkuSpec,
  SpecGroup,
} from '@/types/domain'

// 商品中心编辑抽屉内部使用的 SKU 草稿结构。
// 它比后端 DTO 多 clientId 和 selectedSpecKeys，用来服务前端动态表单。
export interface ProductSkuDraft {
  clientId: string
  skuId?: string
  skuCode: string
  selectedSpecKeys: string[]
  price: number
  originalPrice: number
  unit: string
  stock: number
  status: 'ON_SHELF' | 'OFF_SHELF'
}

// 商品 SPU 草稿结构：页面表单先编辑草稿，点击保存时再转换成后端请求体。
export interface ProductSpuDraft {
  id?: string
  merchantId: string
  categoryId: string
  brandId: string
  name: string
  subtitle: string
  mainImage: string
  detail: string
  detailImagesText: string
  status: ProductStatus
  sortOrder: number
  skus: ProductSkuDraft[]
}

export const commonStatusText: Record<CommonStatus, string> = {
  ACTIVE: '启用',
  DISABLED: '停用',
}

export const productStatusText: Record<ProductStatus, string> = {
  DRAFT: '草稿',
  ON_SHELF: '上架',
  OFF_SHELF: '下架',
}

export const skuStatusText: Record<ProductSkuDraft['status'], string> = {
  ON_SHELF: '上架',
  OFF_SHELF: '下架',
}

export function commonStatusLabel(status: CommonStatus) {
  return commonStatusText[status] ?? status
}

export function productStatusLabel(status: ProductStatus) {
  return productStatusText[status] ?? status
}

export function productStatusTag(status: ProductStatus) {
  if (status === 'ON_SHELF') return 'success'
  if (status === 'DRAFT') return 'warning'
  return 'info'
}

export function commonStatusTag(status: CommonStatus) {
  return status === 'ACTIVE' ? 'success' : 'info'
}

export function categoryTypeText(type: Category['type']) {
  return type === 'PRODUCT' ? '商品类目' : '首页频道'
}

export function specKey(groupId: string, optionId: string) {
  // 用稳定字符串表示“规格组 + 规格值”，方便 Element Plus 多选控件绑定。
  return `${groupId}::${optionId}`
}

export function splitSpecKey(key: string) {
  // specKey 的反向解析，保存 SKU 时需要还原成后端需要的 groupId/optionId。
  const [groupId, optionId] = key.split('::')
  return { groupId: groupId ?? '', optionId: optionId ?? '' }
}

export function makeSkuDraft(sku?: ProductSku): ProductSkuDraft {
  // 编辑已有 SKU 时从后端数据回填；新增 SKU 时生成本地 clientId 供 v-for 使用。
  return {
    clientId: sku?.skuId || `local-${Date.now()}-${Math.random().toString(16).slice(2)}`,
    skuId: sku?.skuId,
    skuCode: sku?.skuCode ?? '',
    selectedSpecKeys: sku?.specs.map((item) => specKey(item.groupId, item.optionId)) ?? [],
    price: Number(sku?.price ?? 0),
    originalPrice: Number(sku?.originalPrice ?? sku?.price ?? 0),
    unit: sku?.unit ?? '件',
    stock: Number(sku?.stock ?? 0),
    status: sku?.status ?? 'ON_SHELF',
  }
}

export function makeProductDraft(
  product: ProductSpu | null,
  defaults: { merchantId?: string; categoryId?: string; brandId?: string },
): ProductSpuDraft {
  // 把后端 SPU 转成表单草稿；新增商品时使用商家/类目/品牌默认值。
  return {
    id: product?.id,
    merchantId: product?.merchantId ?? defaults.merchantId ?? '',
    categoryId: product?.categoryId ?? defaults.categoryId ?? '',
    brandId: product?.brandId ?? defaults.brandId ?? '',
    name: product?.name ?? '',
    subtitle: product?.subtitle ?? '',
    mainImage: product?.mainImage ?? '',
    detail: product?.detail ?? '',
    detailImagesText: product?.detailImages.join('\n') ?? '',
    status: product?.status ?? 'DRAFT',
    sortOrder: product?.sortOrder ?? 100,
    skus: product?.skus.length ? product.skus.map(makeSkuDraft) : [makeSkuDraft()],
  }
}

export function selectedSpecs(keys: string[], groups: SpecGroup[]): SkuSpec[] {
  // 把前端选择的 specKey 列表转换为后端保存 SKU 规格所需的完整规格对象。
  const groupsById = new Map(groups.map((group) => [group.id, group]))
  return keys
    .map((key) => {
      const { groupId, optionId } = splitSpecKey(key)
      const group = groupsById.get(groupId)
      const option = group?.options.find((item) => item.id === optionId)
      if (!group || !option) return null
      return {
        groupId: group.id,
        groupName: group.name,
        optionId: option.id,
        optionName: option.name,
      }
    })
    .filter((item): item is SkuSpec => Boolean(item))
}

export function skuDraftToRequest(sku: ProductSkuDraft, groups: SpecGroup[]): ProductSkuUpsertRequest {
  // 清洗 SKU 表单字段，避免把空字符串、临时字段直接传给后端。
  return {
    skuId: sku.skuId,
    skuCode: sku.skuCode.trim() || undefined,
    specs: selectedSpecs(sku.selectedSpecKeys, groups),
    price: Number(sku.price ?? 0),
    originalPrice: Number(sku.originalPrice ?? sku.price ?? 0),
    unit: sku.unit.trim() || '件',
    stock: Number(sku.stock ?? 0),
    status: sku.status,
  }
}

export function productDraftToRequest(form: ProductSpuDraft, groups: SpecGroup[]): ProductSpuUpsertRequest {
  // 保存商品前的最后转换：处理图片换行文本、数字字段和 SKU 子表单。
  return {
    id: form.id,
    merchantId: form.merchantId,
    categoryId: form.categoryId,
    brandId: form.brandId || null,
    name: form.name.trim(),
    subtitle: form.subtitle.trim(),
    mainImage: form.mainImage.trim(),
    detail: form.detail.trim(),
    detailImages: form.detailImagesText
      .split('\n')
      .map((item) => item.trim())
      .filter(Boolean),
    status: form.status,
    sortOrder: Number(form.sortOrder ?? 100),
    skus: form.skus.map((sku) => skuDraftToRequest(sku, groups)),
  }
}
