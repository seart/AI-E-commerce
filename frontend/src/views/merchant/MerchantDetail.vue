<template>
  <div class="merchant-detail">
    <div class="merchant-header" :style="{ background: detail?.merchant.heroColor ?? defaultHero }">
      <div class="header-top">
        <el-icon class="back-icon" @click="goBack"><ArrowLeft /></el-icon>
        <div class="search-wrap">
          <el-input
            v-model.trim="searchKeyword"
            class="merchant-search"
            placeholder="搜索商家商品"
            :prefix-icon="Search"
          />
        </div>
      </div>

      <div class="merchant-card-float" v-if="detail">
        <div class="merchant-avatar">
          <span>{{ detail.merchant.logoText }}</span>
        </div>
        <div class="merchant-info-text">
          <h2 class="title">{{ detail.merchant.name }}</h2>
          <p class="subtitle">
            月售{{ detail.merchant.monthlySales }} | 起送¥{{ detail.merchant.minOrderAmount }} |
            运费¥{{ detail.merchant.deliveryFee }} | {{ detail.merchant.etaMinutes }}分钟送达
          </p>
          <div class="tags">
            <el-tag
              v-for="tag in detail.merchant.tags"
              :key="tag"
              type="danger"
              size="small"
              effect="plain"
            >
              {{ tag }}
            </el-tag>
          </div>
          <div class="notice">{{ detail.merchant.notice }}</div>
        </div>
      </div>
    </div>

    <el-skeleton :loading="detailLoading" animated :rows="6" class="skeleton-wrap">
      <template #default>
        <div class="content-area" v-if="detail">
          <div class="scroll-sidebar">
            <el-menu :default-active="activeCategoryId" class="custom-menu" @select="handleSelect">
              <el-menu-item index="all">全部商品</el-menu-item>
              <el-menu-item
                v-for="category in detail.merchant.categories"
                :key="category.id"
                :index="category.id"
              >
                {{ category.name }}
              </el-menu-item>
            </el-menu>
          </div>

          <div class="scroll-content">
            <div v-for="item in filteredProducts" :key="item.id" class="product-item">
              <div class="product-img">{{ item.imageText }}</div>
              <div class="product-info">
                <div>
                  <div class="product-name">{{ item.name }}</div>
                  <div class="product-desc">{{ item.description }}</div>
                  <div class="product-sales">月售{{ item.sales }}件 · 库存{{ item.stock }}{{ item.unit }}</div>
                </div>
                <div class="product-bottom">
                  <div class="product-price">
                    <span class="price-symbol">¥</span>
                    <span class="price-num">{{ item.price.toFixed(2) }}</span>
                    <span v-if="item.originalPrice" class="old-price">¥{{ item.originalPrice.toFixed(2) }}</span>
                  </div>
                  <el-button
                    type="primary"
                    circle
                    class="add-btn"
                    size="small"
                    :disabled="cartMutating"
                    @click="handleAddToCart(item)"
                  >
                    <el-icon><Plus /></el-icon>
                  </el-button>
                </div>
              </div>
            </div>

            <el-empty v-if="filteredProducts.length === 0" description="暂无匹配商品" />
          </div>
        </div>
      </template>
    </el-skeleton>

    <div class="bottom-cart-bar" v-if="currentMerchantTotal > 0">
      <div class="cart-icon-wrap">
        <el-icon class="cart-icon"><ShoppingCart /></el-icon>
        <div class="badge-dot"></div>
      </div>
      <div class="price-info">
        <span class="symbol">¥</span>
        <span class="num">{{ currentMerchantTotal.toFixed(2) }}</span>
      </div>
      <div class="checkout-jump" @click="goToCart">去结算</div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'
import { useRouter, useRoute } from 'vue-router'
import { ArrowLeft, Search, Plus, ShoppingCart } from '@element-plus/icons-vue'
import { useCartStore } from '@/stores/cart'
import { useCatalogStore } from '@/stores/catalog'
import type { Product } from '@/types/domain'

defineOptions({ name: 'MerchantDetail' })

const defaultHero = 'linear-gradient(180deg, #1f9af8 0%, #0e85f0 100%)'
const router = useRouter()
const route = useRoute()
const cartStore = useCartStore()
const catalogStore = useCatalogStore()
const { merchantDetails, detailLoading } = storeToRefs(catalogStore)
const { mutating: cartMutating } = storeToRefs(cartStore)

const searchKeyword = ref('')
const activeCategoryId = ref('all')

const currentMerchantId = computed(() => String(route.params.id ?? ''))
const detail = computed(() => merchantDetails.value[currentMerchantId.value] ?? null)

const filteredProducts = computed(() => {
  const products = detail.value?.products ?? []
  const normalizedKeyword = searchKeyword.value.trim().toLowerCase()

  return products.filter((item) => {
    const matchCategory =
      activeCategoryId.value === 'all' || item.categoryId === activeCategoryId.value
    const matchKeyword =
      !normalizedKeyword ||
      [item.name, item.description].join(' ').toLowerCase().includes(normalizedKeyword)

    return matchCategory && matchKeyword
  })
})

const currentMerchantTotal = computed(() => {
  return detail.value ? cartStore.getMerchantTotal(detail.value.merchant.id) : 0
})

async function loadPage() {
  if (!currentMerchantId.value) {
    return
  }

  await Promise.all([
    catalogStore.loadMerchantDetail(currentMerchantId.value, true),
    cartStore.loadCart(),
  ])
  activeCategoryId.value = 'all'
}

onMounted(loadPage)

watch(currentMerchantId, async () => {
  searchKeyword.value = ''
  await loadPage()
})

function goBack() {
  router.back()
}

function handleSelect(index: string) {
  activeCategoryId.value = index
}

async function handleAddToCart(product: Product) {
  try {
    await cartStore.addToCart(product)
    ElMessage.success('已加入购物车')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加入购物车失败')
  }
}

function goToCart() {
  router.push({ name: 'CartView' })
}
</script>

<style scoped>
.merchant-detail {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f5f5;
  overflow: hidden;
}

.merchant-header {
  position: relative;
  padding: 16px 18px 54px;
  border-bottom-left-radius: 18px;
  border-bottom-right-radius: 18px;
}

.header-top {
  display: flex;
  align-items: center;
  margin-bottom: 18px;
}

.back-icon {
  font-size: 20px;
  color: #fff;
  margin-right: 12px;
  cursor: pointer;
}

.search-wrap {
  flex: 1;
}

.merchant-search :deep(.el-input__wrapper) {
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: none;
}

.merchant-card-float {
  position: absolute;
  bottom: -56px;
  left: 18px;
  right: 18px;
  background: rgba(255, 255, 255, 0.98);
  border-radius: 18px;
  box-shadow: 0 18px 36px rgba(0, 0, 0, 0.08);
  padding: 14px;
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.merchant-avatar {
  width: 56px;
  height: 56px;
  background: rgba(225, 37, 27, 0.08);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #e1251b;
  font-size: 18px;
  font-weight: 700;
}

.merchant-info-text {
  flex: 1;
}

.merchant-info-text .title {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.merchant-info-text .subtitle,
.notice {
  margin: 6px 0 0;
  font-size: 12px;
  color: #666;
  line-height: 1.5;
}

.merchant-info-text .tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.skeleton-wrap {
  margin-top: 58px;
}

.content-area {
  flex: 1;
  display: flex;
  margin-top: 62px;
  background: #fff;
  border-radius: 18px 18px 0 0;
  overflow: hidden;
}

.scroll-sidebar {
  width: 92px;
  background-color: #f7f7f7;
  overflow-y: auto;
}

.custom-menu {
  border-right: none;
  background-color: transparent;
}

.custom-menu .el-menu-item {
  height: 52px;
  line-height: 52px;
  padding: 0 10px;
  font-size: 14px;
  text-align: center;
  color: #333;
}

.custom-menu .el-menu-item.is-active {
  background-color: #fff;
  color: #e1251b;
  font-weight: 700;
}

.scroll-content {
  flex: 1;
  overflow-y: auto;
  padding: 14px 14px 94px;
}

.product-item {
  display: flex;
  gap: 12px;
  margin-bottom: 18px;
}

.product-img {
  width: 74px;
  height: 74px;
  background: linear-gradient(135deg, #fff0eb 0%, #ffe4d8 100%);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #e1251b;
  font-size: 14px;
  font-weight: 700;
  flex-shrink: 0;
}

.product-info {
  flex: 1;
  min-width: 0;
}

.product-name {
  font-size: 14px;
  font-weight: 700;
  color: #333;
  line-height: 1.5;
}

.product-desc,
.product-sales {
  font-size: 12px;
  color: #8c8f99;
  margin-top: 6px;
  line-height: 1.5;
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

.price-symbol {
  font-size: 12px;
}

.price-num {
  font-size: 18px;
  font-weight: 700;
}

.old-price {
  font-size: 12px;
  color: #999;
  text-decoration: line-through;
  margin-left: 6px;
}

.add-btn {
  width: 28px;
  height: 28px;
  min-height: 28px;
  padding: 0;
}

.bottom-cart-bar {
  position: fixed;
  bottom: 12px;
  left: 18px;
  right: 18px;
  max-width: 444px;
  margin: 0 auto;
  height: 52px;
  background: rgba(0, 0, 0, 0.86);
  border-radius: 26px;
  display: flex;
  align-items: center;
  padding-left: 16px;
  z-index: 100;
  backdrop-filter: blur(10px);
}

.cart-icon-wrap {
  position: relative;
  width: 42px;
  height: 42px;
  background: #e1251b;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: -18px;
  border: 4px solid #fff;
}

.cart-icon {
  font-size: 20px;
  color: #fff;
}

.badge-dot {
  position: absolute;
  top: 0;
  right: 0;
  width: 8px;
  height: 8px;
  background: #ffd166;
  border-radius: 50%;
}

.price-info {
  flex: 1;
  margin-left: 12px;
  color: #fff;
}

.price-info .symbol {
  font-size: 14px;
}

.price-info .num {
  font-size: 20px;
  font-weight: 700;
}

.checkout-jump {
  height: 100%;
  background: linear-gradient(135deg, #ffb703 0%, #fb8500 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  padding: 0 24px;
  border-radius: 0 26px 26px 0;
  display: flex;
  align-items: center;
  cursor: pointer;
}
</style>
