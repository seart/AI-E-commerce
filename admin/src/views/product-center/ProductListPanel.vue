<script setup lang="ts">
import { computed, ref } from 'vue'
import { Edit, Plus, Refresh, Search, SwitchButton, View } from '@element-plus/icons-vue'
import type { Category, Merchant, ProductSpu, ProductStatus } from '@/types/domain'
import { productStatusLabel, productStatusTag } from './productCenterState'

const props = defineProps<{
  products: ProductSpu[]
  merchants: Merchant[]
  categories: Category[]
  loading: boolean
}>()

const emit = defineEmits<{
  refresh: []
  create: []
  edit: [product: ProductSpu]
  view: [product: ProductSpu]
  toggleStatus: [product: ProductSpu, status: ProductStatus]
}>()

const keyword = ref('')
const merchantId = ref('')
const status = ref('')

const activeMerchants = computed(() => props.merchants.filter((item) => item.status === 'ACTIVE'))

const filteredProducts = computed(() => {
  const normalized = keyword.value.trim().toLowerCase()
  return props.products.filter((product) => {
    const matchesKeyword =
      !normalized ||
      [product.name, product.subtitle, product.merchantName, product.categoryName, product.brandName]
        .filter(Boolean)
        .some((value) => value.toLowerCase().includes(normalized))
    const matchesMerchant = !merchantId.value || product.merchantId === merchantId.value
    const matchesStatus = !status.value || product.status === status.value
    return matchesKeyword && matchesMerchant && matchesStatus
  })
})

function priceText(product: ProductSpu) {
  const min = Number(product.minPrice ?? 0).toFixed(2)
  const max = Number(product.maxPrice ?? 0).toFixed(2)
  return min === max ? `￥${min}` : `￥${min} - ￥${max}`
}

function nextStatus(product: ProductSpu): ProductStatus {
  return product.status === 'ON_SHELF' ? 'OFF_SHELF' : 'ON_SHELF'
}
</script>

<template>
  <div class="product-list-panel">
    <div class="filter-bar product-filter">
      <el-input v-model="keyword" clearable placeholder="搜索商品、商家、品牌" :prefix-icon="Search" />
      <el-select v-model="merchantId" clearable filterable placeholder="全部商家">
        <el-option v-for="merchant in activeMerchants" :key="merchant.id" :label="merchant.name" :value="merchant.id" />
      </el-select>
      <el-select v-model="status" clearable placeholder="全部状态">
        <el-option label="草稿" value="DRAFT" />
        <el-option label="上架" value="ON_SHELF" />
        <el-option label="下架" value="OFF_SHELF" />
      </el-select>
      <el-button :icon="Refresh" @click="emit('refresh')">刷新</el-button>
      <el-button type="primary" :icon="Plus" @click="emit('create')">新增商品</el-button>
    </div>

    <el-table v-loading="loading" :data="filteredProducts" row-key="id">
      <el-table-column prop="name" label="SPU 商品" min-width="240">
        <template #default="{ row }">
          <strong>{{ row.name }}</strong>
          <div class="muted">{{ row.subtitle || '无副标题' }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="merchantName" label="商家" min-width="150" />
      <el-table-column prop="categoryName" label="类目" min-width="130">
        <template #default="{ row }">{{ row.categoryName || row.categoryId }}</template>
      </el-table-column>
      <el-table-column prop="brandName" label="品牌" width="120">
        <template #default="{ row }">{{ row.brandName || '未设置' }}</template>
      </el-table-column>
      <el-table-column label="价格带" width="150">
        <template #default="{ row }">{{ priceText(row) }}</template>
      </el-table-column>
      <el-table-column prop="skuCount" label="SKU" width="86" />
      <el-table-column prop="totalStock" label="库存" width="100" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="productStatusTag(row.status)">
            {{ productStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="88" />
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button size="small" :icon="View" @click="emit('view', row)">详情</el-button>
          <el-button size="small" :icon="Edit" @click="emit('edit', row)">编辑</el-button>
          <el-button
            size="small"
            :icon="SwitchButton"
            :type="row.status === 'ON_SHELF' ? 'warning' : 'success'"
            @click="emit('toggleStatus', row, nextStatus(row))"
          >
            {{ row.status === 'ON_SHELF' ? '下架' : '上架' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.product-filter {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 180px 140px auto auto;
  gap: 12px;
  align-items: center;
}

@media (max-width: 1180px) {
  .product-filter {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
