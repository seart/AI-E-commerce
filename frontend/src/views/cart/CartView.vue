<template>
  <div class="cart-view">
    <div class="cart-header">
      <h2>购物车</h2>
    </div>

    <el-skeleton :loading="loading" animated :rows="5">
      <template #default>
        <div class="cart-list" v-if="cartGroupedByMerchant.length > 0">
          <div class="merchant-group" v-for="group in cartGroupedByMerchant" :key="group.merchantId">
            <div class="merchant-title">
              <el-checkbox
                :model-value="group.items.every((item) => item.checked)"
                @change="handleToggleMerchant(group.merchantId, Boolean($event))"
              />
              <span class="merchant-name">{{ group.merchantName }}</span>
            </div>

            <div class="product-item" v-for="item in group.items" :key="item.id">
              <div class="row-left">
                <el-checkbox
                  :model-value="item.checked"
                  @change="handleToggleItem(item.id, Boolean($event))"
                />
              </div>
              <div class="row-img">{{ item.imageText }}</div>
              <div class="row-info">
                <div>
                  <div class="title">{{ item.productName || item.name }}</div>
                  <div class="snapshot" v-if="item.brandName || item.specText">
                    <span v-if="item.brandName">{{ item.brandName }}</span>
                    <span v-if="item.specText">{{ item.specText }}</span>
                  </div>
                </div>
                <div class="price-action">
                  <span class="price">¥{{ item.price.toFixed(2) }}</span>
                  <div class="quantity-ctrl">
                    <el-icon class="ctrl-icon" @click="changeQuantity(item.id, -1)">
                      <Minus />
                    </el-icon>
                    <div class="qty-num">{{ item.quantity }}</div>
                    <el-icon class="ctrl-icon add" @click="changeQuantity(item.id, 1)">
                      <Plus />
                    </el-icon>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <el-empty v-else description="购物车空空如也，快去商家选购吧" />
      </template>
    </el-skeleton>

    <div class="checkout-bar" v-if="cartGroupedByMerchant.length > 0">
      <div class="check-all">
        <el-checkbox :model-value="isAllChecked" label="全选" @change="handleToggleAll" />
      </div>
      <div class="total-info">
        合计：<span class="total-price">¥{{ exactCheckedTotalPrice.toFixed(2) }}</span>
      </div>
      <el-button
        type="primary"
        class="checkout-btn"
        :disabled="checkedItems.length === 0 || mutating"
        @click="goToCheckout"
      >
        去结算 ({{ checkedItems.length }})
      </el-button>
    </div>

    <TabBar />
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import type { CheckboxValueType } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { Plus, Minus } from '@element-plus/icons-vue'
import TabBar from '@/components/TabBar.vue'
import { useCartStore } from '@/stores/cart'

defineOptions({ name: 'CartView' })

const router = useRouter()
const cartStore = useCartStore()

const { cartGroupedByMerchant, exactCheckedTotalPrice, checkedItems, items, loading, mutating } =
  storeToRefs(cartStore)

const isAllChecked = computed(() => {
  return items.value.length > 0 && items.value.every((item) => item.checked)
})

onMounted(async () => {
  await cartStore.loadCart()
})

async function changeQuantity(productId: string, delta: number) {
  try {
    await cartStore.updateQuantity(productId, delta)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '更新购物车失败')
  }
}

async function handleToggleItem(productId: string, checked: boolean) {
  try {
    await cartStore.setItemChecked(productId, checked)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '更新商品勾选状态失败')
  }
}

async function handleToggleMerchant(merchantId: string, checked: boolean) {
  try {
    await cartStore.toggleMerchantChecked(merchantId, checked)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '更新商家勾选状态失败')
  }
}

async function handleToggleAll(value: CheckboxValueType) {
  try {
    await cartStore.toggleAllChecked(Boolean(value))
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '更新全选状态失败')
  }
}

function goToCheckout() {
  if (checkedItems.value.length === 0) {
    return
  }

  router.push('/checkout')
}
</script>

<style scoped>
.cart-view {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 120px;
}

.cart-header {
  background: #fff;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #f0f0f0;
}

.cart-header h2 {
  font-size: 16px;
  margin: 0;
  color: #333;
}

.cart-list {
  padding: 12px;
}

.merchant-group {
  background: #fff;
  border-radius: 16px;
  padding: 12px;
  margin-bottom: 12px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.03);
}

.merchant-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 16px;
}

.product-item {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.product-item:last-child {
  margin-bottom: 0;
}

.row-left {
  margin-right: 12px;
}

.row-img {
  width: 68px;
  height: 68px;
  background: linear-gradient(135deg, #fff0eb 0%, #ffe4d8 100%);
  border-radius: 14px;
  margin-right: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #e1251b;
  font-size: 14px;
  font-weight: 700;
}

.row-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 68px;
}

.title {
  font-size: 14px;
  color: #333;
  line-height: 1.5;
}

.snapshot {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;
  color: #8c8f99;
  font-size: 12px;
}

.price-action {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price {
  color: #e1251b;
  font-size: 16px;
  font-weight: 700;
}

.quantity-ctrl {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ctrl-icon {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 1px solid #ddd;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
}

.ctrl-icon.add {
  background: #e1251b;
  border-color: #e1251b;
  color: #fff;
}

.qty-num {
  min-width: 20px;
  text-align: center;
  font-size: 14px;
  color: #333;
}

.checkout-bar {
  position: fixed;
  bottom: 50px;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 480px;
  height: 50px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 18px;
  box-sizing: border-box;
  box-shadow: 0 -2px 6px rgba(0, 0, 0, 0.03);
  z-index: 99;
}

.check-all {
  display: flex;
  align-items: center;
}

.check-all :deep(.el-checkbox__label) {
  font-size: 14px;
  color: #666;
}

.total-info {
  flex: 1;
  text-align: right;
  margin-right: 16px;
  font-size: 14px;
  color: #333;
}

.total-price {
  color: #e1251b;
  font-weight: 700;
  font-size: 18px;
}

.checkout-btn {
  border-radius: 20px;
  padding: 8px 24px;
}
</style>
