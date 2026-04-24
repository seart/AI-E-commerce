<template>
  <div class="orders-view">
    <div class="orders-header">
      <h2>我的订单</h2>
    </div>

    <el-skeleton :loading="loading" animated :rows="6">
      <template #default>
        <div class="order-list" v-if="orders.length > 0">
          <div class="order-card" v-for="order in orders" :key="order.id">
            <div class="order-title">
              <span class="merchant-name">订单号: {{ order.orderNo }}</span>
              <span class="order-status">{{ order.statusText }}</span>
            </div>

            <div class="order-content">
              <div class="product-imgs">
                <div class="img-box" v-for="item in order.items" :key="item.id">
                  <div class="fake-img-text">{{ item.imageText }}</div>
                </div>
              </div>
              <div class="order-price">
                <div class="price-val">¥{{ order.totalAmount.toFixed(2) }}</div>
                <div class="count-val">共{{ order.items.length }}件</div>
              </div>
            </div>

            <div class="order-address">{{ formatAddress(order.address) }}</div>
            <div class="order-time">{{ formatDateTime(order.createdAt) }}</div>
          </div>
        </div>

        <el-empty v-else description="您还没有下过单，去首页逛逛吧" />
      </template>
    </el-skeleton>

    <TabBar />
  </div>
</template>

<script lang="ts" setup>
import { onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import TabBar from '@/components/TabBar.vue'
import { useOrderStore } from '@/stores/order'
import { formatAddress, formatDateTime } from '@/utils/format'

defineOptions({ name: 'OrdersView' })

const orderStore = useOrderStore()
const { orders, loading } = storeToRefs(orderStore)

onMounted(async () => {
  await orderStore.loadOrders(true)
})
</script>

<style scoped>
.orders-view {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 70px;
}

.order-time,
.order-address {
  font-size: 12px;
  color: #999;
  margin-top: 12px;
}

.fake-img-text {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #e1251b;
  background: linear-gradient(135deg, #fff0eb 0%, #ffe4d8 100%);
  border-radius: 10px;
}

.orders-header {
  background: #fff;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 12px;
}

.orders-header h2 {
  font-size: 16px;
  margin: 0;
  color: #333;
}

.order-list {
  padding: 0 12px;
}

.order-card {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.03);
}

.order-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.merchant-name {
  font-weight: 700;
  font-size: 14px;
  color: #333;
}

.order-status {
  font-size: 14px;
  color: #e1251b;
}

.order-content {
  display: flex;
  align-items: center;
}

.product-imgs {
  flex: 1;
  display: flex;
  gap: 8px;
  overflow-x: auto;
}

.product-imgs::-webkit-scrollbar {
  display: none;
}

.img-box {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  flex-shrink: 0;
}

.order-price {
  margin-left: 12px;
  text-align: right;
}

.price-val {
  color: #e1251b;
  font-size: 14px;
  font-weight: 700;
}

.count-val {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}
</style>
