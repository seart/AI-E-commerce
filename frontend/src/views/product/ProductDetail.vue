<template>
  <div class="product-detail">
    <div class="header">
      <el-icon class="back-icon" @click="goBack"><ArrowLeft /></el-icon>
      <h2>商品详情</h2>
    </div>

    <el-skeleton :loading="productDetailLoading" animated :rows="8">
      <template #default>
        <div v-if="detail" class="detail-body">
          <div class="hero">
            <div class="product-img">{{ detail.product.mainImage || detail.product.imageText }}</div>
          </div>

          <section class="product-main">
            <div class="price-row">
              <span class="price-symbol">¥</span>
              <span class="price-num">{{ currentPriceText }}</span>
              <span v-if="selectedSku?.originalPrice" class="old-price">
                ¥{{ selectedSku.originalPrice.toFixed(2) }}
              </span>
            </div>
            <h1>{{ detail.product.name }}</h1>
            <p class="subtitle">{{ detail.product.subtitle || detail.product.description }}</p>
            <div class="meta-row">
              <span>{{ detail.product.merchantName }}</span>
              <span v-if="detail.product.brandName">{{ detail.product.brandName }}</span>
              <span>月售{{ detail.product.sales }}</span>
            </div>
          </section>

          <section class="sku-section" v-if="detail.skus.length > 0">
            <div class="section-title">规格</div>
            <div class="sku-list">
              <button
                v-for="sku in detail.skus"
                :key="sku.skuId"
                class="sku-option"
                :class="{ active: selectedSku?.skuId === sku.skuId, disabled: sku.status !== 'ON_SHELF' || sku.stock <= 0 }"
                :disabled="sku.status !== 'ON_SHELF' || sku.stock <= 0"
                @click="selectedSkuId = sku.skuId"
              >
                <span>{{ sku.specText || sku.skuCode || '默认规格' }}</span>
                <b>¥{{ sku.price.toFixed(2) }}</b>
                <small>库存{{ sku.stock }}{{ sku.unit }}</small>
              </button>
            </div>
          </section>

          <section class="detail-section">
            <div class="section-title">详情</div>
            <p>{{ detail.detail || detail.product.description }}</p>
            <div v-if="detail.detailImages.length > 0" class="detail-images">
              <div v-for="item in detail.detailImages" :key="item" class="detail-image">{{ item }}</div>
            </div>
          </section>
        </div>

        <el-empty v-else description="商品不存在或已下架" />
      </template>
    </el-skeleton>

    <div class="bottom-bar" v-if="detail">
      <div class="bottom-price">
        <span>¥</span>
        <b>{{ currentPriceText }}</b>
      </div>
      <el-button
        type="primary"
        class="cart-btn"
        :disabled="!selectedSku || selectedSku.stock <= 0 || cartMutating"
        @click="addSelectedSku"
      >
        加入购物车
      </el-button>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useCartStore } from '@/stores/cart'
import { useCatalogStore } from '@/stores/catalog'

defineOptions({ name: 'ProductDetail' })

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()
const catalogStore = useCatalogStore()
const { productDetails, productDetailLoading } = storeToRefs(catalogStore)
const { mutating: cartMutating } = storeToRefs(cartStore)

const productId = computed(() => String(route.params.id ?? ''))
const selectedSkuId = ref('')
const detail = computed(() => productDetails.value[productId.value] ?? null)
const selectedSku = computed(() => {
  const skus = detail.value?.skus ?? []
  return skus.find((item) => item.skuId === selectedSkuId.value) ?? skus[0] ?? null
})

const currentPriceText = computed(() => {
  if (selectedSku.value) {
    return selectedSku.value.price.toFixed(2)
  }
  if (!detail.value) {
    return '0.00'
  }
  const min = detail.value.product.minPrice.toFixed(2)
  const max = detail.value.product.maxPrice.toFixed(2)
  return min === max ? min : `${min} - ${max}`
})

async function loadProduct() {
  if (!productId.value) {
    return
  }
  const loaded = await catalogStore.loadProductDetail(productId.value, true)
  selectedSkuId.value = loaded?.skus.find((item) => item.status === 'ON_SHELF' && item.stock > 0)?.skuId ?? loaded?.skus[0]?.skuId ?? ''
}

onMounted(loadProduct)

watch(productId, async () => {
  selectedSkuId.value = ''
  await loadProduct()
})

function goBack() {
  router.back()
}

async function addSelectedSku() {
  if (!selectedSku.value) {
    ElMessage.warning('请选择规格')
    return
  }
  try {
    await cartStore.addSkuToCart(selectedSku.value.skuId)
    ElMessage.success('已加入购物车')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加入购物车失败')
  }
}
</script>

<style scoped>
.product-detail {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 76px;
}

.header {
  position: sticky;
  top: 0;
  z-index: 20;
  background: #fff;
  height: 44px;
  display: flex;
  align-items: center;
  padding: 0 18px;
  border-bottom: 1px solid #f0f0f0;
}

.back-icon {
  font-size: 20px;
  color: #333;
  cursor: pointer;
}

.header h2 {
  flex: 1;
  text-align: center;
  font-size: 16px;
  margin: 0 20px 0 0;
  color: #333;
}

.detail-body {
  padding-bottom: 12px;
}

.hero {
  background: #fff;
  padding: 18px;
}

.product-img {
  height: 220px;
  border-radius: 18px;
  background: linear-gradient(135deg, #fff0eb 0%, #ffe4d8 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #e1251b;
  font-size: 28px;
  font-weight: 800;
  text-align: center;
  padding: 18px;
  box-sizing: border-box;
}

.product-main,
.sku-section,
.detail-section {
  margin: 12px 18px 0;
  background: #fff;
  border-radius: 16px;
  padding: 16px;
}

.price-row,
.bottom-price {
  color: #e1251b;
}

.price-symbol {
  font-size: 14px;
}

.price-num {
  font-size: 28px;
  font-weight: 800;
}

.old-price {
  font-size: 13px;
  color: #999;
  text-decoration: line-through;
  margin-left: 8px;
}

.product-main h1 {
  margin: 8px 0 0;
  font-size: 18px;
  line-height: 1.45;
  color: #333;
}

.subtitle {
  margin: 8px 0 0;
  color: #666;
  font-size: 13px;
  line-height: 1.6;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
  font-size: 12px;
  color: #8c8f99;
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  color: #333;
  margin-bottom: 12px;
}

.sku-list {
  display: grid;
  gap: 10px;
}

.sku-option {
  border: 1px solid #eee;
  background: #fff;
  border-radius: 12px;
  padding: 10px 12px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 4px 10px;
  text-align: left;
  color: #333;
}

.sku-option span {
  font-size: 14px;
  font-weight: 700;
}

.sku-option b {
  color: #e1251b;
  font-size: 14px;
}

.sku-option small {
  grid-column: 1 / -1;
  color: #999;
  font-size: 12px;
}

.sku-option.active {
  border-color: #e1251b;
  background: #fff7f6;
}

.sku-option.disabled {
  opacity: 0.48;
}

.detail-section p {
  margin: 0;
  color: #555;
  font-size: 13px;
  line-height: 1.7;
}

.detail-images {
  margin-top: 12px;
  display: grid;
  gap: 10px;
}

.detail-image {
  min-height: 72px;
  border-radius: 12px;
  background: #f8fafc;
  color: #667085;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px;
  text-align: center;
  box-sizing: border-box;
}

.bottom-bar {
  position: fixed;
  left: 50%;
  bottom: 0;
  transform: translateX(-50%);
  width: 100%;
  max-width: 480px;
  height: 58px;
  background: #fff;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 18px;
  box-sizing: border-box;
  z-index: 30;
}

.bottom-price span {
  font-size: 13px;
}

.bottom-price b {
  font-size: 22px;
}

.cart-btn {
  border-radius: 22px;
  width: 138px;
  height: 40px;
}
</style>
