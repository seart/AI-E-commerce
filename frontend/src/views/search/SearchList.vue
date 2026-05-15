<template>
  <!-- 搜索结果页：根据关键词展示商家和商品结果。 -->
  <div class="search-list">
    <div class="search-header">
      <el-icon class="back-icon" @click="goBack"><ArrowLeft /></el-icon>
      <el-input
        v-model.trim="keyword"
        class="search-input"
        :prefix-icon="Search"
        clearable
        placeholder="搜索商家或商品"
        @keyup.enter="runSearch"
      />
    </div>

    <el-skeleton :loading="searchLoading" animated :rows="5">
      <template #default>
        <div class="list-container" v-if="products.length > 0 || merchants.length > 0">
          <section v-if="products.length > 0" class="result-section">
            <h3>商品</h3>
            <div
              class="product-card"
              v-for="item in products"
              :key="item.id"
              @click="goToProduct(item.id)"
            >
              <div class="product-img">{{ item.mainImage || item.imageText }}</div>
              <div class="product-info">
                <div class="product-name">{{ item.name }}</div>
                <div class="merchant-sales">
                  {{ item.merchantName }} · {{ item.brandName || '精选商品' }} · 月售{{ item.sales }}
                </div>
                <div class="merchant-desc">{{ item.subtitle || item.description }}</div>
                <div class="product-bottom">
                  <div class="product-price">
                    <span>¥</span>
                    <b>{{ priceText(item) }}</b>
                    <small v-if="item.stock > 0">库存{{ item.stock }}{{ item.unit }}</small>
                  </div>
                  <el-button
                    v-if="item.singleSku"
                    type="primary"
                    circle
                    class="add-btn"
                    size="small"
                    :disabled="cartMutating"
                    @click.stop="handleAddToCart(item)"
                  >
                    <el-icon><Plus /></el-icon>
                  </el-button>
                  <el-button v-else size="small" round @click.stop="goToProduct(item.id)">选规格</el-button>
                </div>
              </div>
            </div>
          </section>

          <section v-if="merchants.length > 0" class="result-section">
            <h3>商家</h3>
          <div
            class="merchant-card"
            v-for="item in merchants"
            :key="item.id"
            @click="goToMerchant(item.id)"
          >
            <div class="merchant-img" :style="{ background: item.heroColor }">
              <div class="img-placeholder">{{ item.logoText }}</div>
            </div>
            <div class="merchant-info">
              <div class="merchant-name">{{ item.name }}</div>
              <div class="merchant-sales">
                月售{{ item.monthlySales }} | 起送¥{{ item.minOrderAmount }} | 配送费¥{{ item.deliveryFee }}
              </div>
              <div class="merchant-tags">
                <el-tag
                  v-for="tag in item.tags"
                  :key="tag"
                  type="danger"
                  size="small"
                  effect="plain"
                  class="custom-tag"
                >
                  {{ tag }}
                </el-tag>
              </div>
              <div class="merchant-desc">{{ item.description }}</div>
            </div>
          </div>
          </section>
        </div>

        <el-empty v-else description="未搜索到相关商品或商家" />
      </template>
    </el-skeleton>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Plus, Search } from '@element-plus/icons-vue'
import { useCartStore } from '@/stores/cart'
import { useCatalogStore } from '@/stores/catalog'
import type { ProductCard } from '@/types/domain'

defineOptions({ name: 'SearchList' })

const route = useRoute()
const router = useRouter()
const catalogStore = useCatalogStore()
const cartStore = useCartStore()
const { merchants, products, searchLoading } = storeToRefs(catalogStore)
const { mutating: cartMutating } = storeToRefs(cartStore)
const keyword = ref('')

async function runSearch() {
  await catalogStore.searchAll(keyword.value)
  catalogStore.addSearchHistory(keyword.value)
}

onMounted(async () => {
  keyword.value = String(route.query.q ?? '')
  await runSearch()
})

watch(
  () => route.query.q,
  async (value) => {
    const nextKeyword = String(value ?? '')
    if (nextKeyword === keyword.value) {
      return
    }

    keyword.value = nextKeyword
    await runSearch()
  },
)

function goBack() {
  router.back()
}

function goToMerchant(id: string) {
  router.push(`/merchant/${id}`)
}

function goToProduct(id: string) {
  router.push(`/products/${id}`)
}

function priceText(product: ProductCard) {
  const min = product.minPrice.toFixed(2)
  const max = product.maxPrice.toFixed(2)
  return min === max ? min : `${min} - ${max}`
}

async function handleAddToCart(product: ProductCard) {
  if (!product.skuId) {
    goToProduct(product.id)
    return
  }
  try {
    await cartStore.addSkuToCart(product.skuId)
    ElMessage.success('已加入购物车')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加入购物车失败')
  }
}
</script>

<style scoped>
.search-list {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 12px 18px;
}

.search-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.back-icon {
  font-size: 20px;
  color: #333;
  margin-right: 12px;
  cursor: pointer;
}

.search-input {
  flex: 1;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 18px;
  background: #fff;
  box-shadow: none;
}

.list-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.result-section h3 {
  margin: 4px 0 10px;
  font-size: 15px;
  color: #333;
}

.merchant-card,
.product-card {
  background: #fff;
  border-radius: 16px;
  padding: 12px;
  display: flex;
  gap: 12px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.03);
  cursor: pointer;
}

.product-card + .product-card,
.merchant-card + .merchant-card {
  margin-top: 12px;
}

.merchant-img {
  width: 64px;
  height: 64px;
  flex-shrink: 0;
  border-radius: 16px;
  overflow: hidden;
}

.img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: #fff;
  font-weight: 700;
}

.merchant-info,
.product-info {
  flex: 1;
  min-width: 0;
}

.merchant-name,
.product-name {
  font-size: 16px;
  font-weight: 700;
  color: #333;
  line-height: 1.45;
}

.merchant-sales,
.merchant-desc {
  font-size: 12px;
  color: #666;
  margin-top: 6px;
}

.merchant-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.custom-tag {
  border-radius: 10px;
}

.product-img {
  width: 72px;
  height: 72px;
  flex-shrink: 0;
  border-radius: 16px;
  background: linear-gradient(135deg, #fff0eb 0%, #ffe4d8 100%);
  color: #e1251b;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  text-align: center;
  padding: 8px;
  box-sizing: border-box;
}

.product-bottom {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-top: 10px;
}

.product-price {
  color: #e1251b;
}

.product-price span {
  font-size: 12px;
}

.product-price b {
  font-size: 18px;
}

.product-price small {
  display: block;
  color: #999;
  font-size: 11px;
  margin-top: 2px;
}

.add-btn {
  width: 28px;
  height: 28px;
  min-height: 28px;
  padding: 0;
}
</style>
