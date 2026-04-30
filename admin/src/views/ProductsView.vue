<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getBrands,
  getCategories,
  getMerchants,
  getProductSpu,
  getProductSpus,
  getSpecGroups,
  saveProductSpu,
  updateProductSpuStatus,
} from '@/api/admin'
import type { Brand, Category, Merchant, ProductSpu, ProductSpuUpsertRequest, ProductStatus, SpecGroup } from '@/types/domain'
import BrandPanel from './product-center/BrandPanel.vue'
import CategoryPanel from './product-center/CategoryPanel.vue'
import ProductEditorDrawer from './product-center/ProductEditorDrawer.vue'
import ProductListPanel from './product-center/ProductListPanel.vue'
import SpecPanel from './product-center/SpecPanel.vue'

const products = ref<ProductSpu[]>([])
const merchants = ref<Merchant[]>([])
const categories = ref<Category[]>([])
const brands = ref<Brand[]>([])
const specGroups = ref<SpecGroup[]>([])
const activeTab = ref('products')
const loading = ref(false)
const productLoading = ref(false)
const editorVisible = ref(false)
const editorSaving = ref(false)
const currentProduct = ref<ProductSpu | null>(null)

const productCountText = computed(() => `${products.value.length} 个 SPU`)
const skuCountText = computed(() => `${products.value.reduce((sum, item) => sum + item.skuCount, 0)} 个 SKU`)

async function loadDictionaries() {
  const [merchantRows, categoryRows, brandRows, specRows] = await Promise.all([
    getMerchants(),
    getCategories(),
    getBrands(),
    getSpecGroups(),
  ])
  merchants.value = merchantRows
  categories.value = categoryRows
  brands.value = brandRows
  specGroups.value = specRows
}

async function loadProducts() {
  products.value = await getProductSpus()
}

async function loadAll() {
  loading.value = true
  try {
    await Promise.all([loadDictionaries(), loadProducts()])
  } finally {
    loading.value = false
  }
}

function openCreate() {
  currentProduct.value = null
  editorVisible.value = true
}

async function openEdit(product: ProductSpu) {
  productLoading.value = true
  try {
    currentProduct.value = await getProductSpu(product.id)
    editorVisible.value = true
  } finally {
    productLoading.value = false
  }
}

async function submitProduct(payload: ProductSpuUpsertRequest) {
  editorSaving.value = true
  try {
    await saveProductSpu(payload)
    ElMessage.success('商品已保存')
    editorVisible.value = false
    await loadProducts()
  } finally {
    editorSaving.value = false
  }
}

async function toggleProductStatus(product: ProductSpu, status: ProductStatus) {
  await updateProductSpuStatus(product.id, status)
  ElMessage.success(status === 'ON_SHELF' ? '商品已上架' : '商品已下架')
  await loadProducts()
}

onMounted(loadAll)
</script>

<template>
  <el-card class="page-card">
    <div class="toolbar">
      <div>
        <h2>商品中心</h2>
        <p class="page-hint">{{ productCountText }} / {{ skuCountText }}</p>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="product-center-tabs">
      <el-tab-pane label="商品" name="products">
        <ProductListPanel
          :products="products"
          :merchants="merchants"
          :categories="categories"
          :loading="loading || productLoading"
          @refresh="loadAll"
          @create="openCreate"
          @edit="openEdit"
          @view="openEdit"
          @toggle-status="toggleProductStatus"
        />
      </el-tab-pane>
      <el-tab-pane label="类目" name="categories">
        <CategoryPanel :categories="categories" :loading="loading" @refresh="loadAll" @changed="loadAll" />
      </el-tab-pane>
      <el-tab-pane label="品牌" name="brands">
        <BrandPanel :brands="brands" :loading="loading" @refresh="loadAll" @changed="loadAll" />
      </el-tab-pane>
      <el-tab-pane label="规格" name="specs">
        <SpecPanel :spec-groups="specGroups" :loading="loading" @refresh="loadAll" @changed="loadAll" />
      </el-tab-pane>
    </el-tabs>
  </el-card>

  <ProductEditorDrawer
    v-model="editorVisible"
    :product="currentProduct"
    :merchants="merchants"
    :categories="categories"
    :brands="brands"
    :spec-groups="specGroups"
    :saving="editorSaving"
    @submit="submitProduct"
  />
</template>

<style scoped>
.product-center-tabs {
  margin-top: 4px;
}
</style>
