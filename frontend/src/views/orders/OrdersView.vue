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
            <div class="payment-line">
              <span>{{ paymentText(order.paymentStatus) }}</span>
              <span v-if="order.paymentChannel"> · {{ channelText(order.paymentChannel) }}</span>
              <span v-if="order.paymentExpireAt && order.status === 'PENDING_PAYMENT'">
                · {{ formatDateTime(order.paymentExpireAt) }} 前支付
              </span>
            </div>
            <div class="order-time">{{ formatDateTime(order.createdAt) }}</div>
            <div class="order-actions">
              <el-button
                v-if="order.status === 'PENDING_PAYMENT'"
                size="small"
                :loading="actionLoading === order.id"
                @click="cancel(order.id)"
              >
                取消订单
              </el-button>
              <el-button
                v-if="canRefund(order.status)"
                size="small"
                type="warning"
                :loading="actionLoading === order.id"
                @click="refund(order.id)"
              >
                申请退款
              </el-button>
            </div>
          </div>
        </div>

        <el-empty v-else description="您还没有下过单，去首页逛逛吧" />
      </template>
    </el-skeleton>

    <TabBar />
  </div>
</template>

<script lang="ts" setup>
import { onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'
import TabBar from '@/components/TabBar.vue'
import { useOrderStore } from '@/stores/order'
import { formatAddress, formatDateTime } from '@/utils/format'
import type { OrderStatus, PaymentChannel, PaymentStatus } from '@/types/domain'

defineOptions({ name: 'OrdersView' })

const orderStore = useOrderStore()
const { orders, loading } = storeToRefs(orderStore)
const actionLoading = ref('')

onMounted(async () => {
  await orderStore.loadOrders(true)
})

function canRefund(status: OrderStatus) {
  return ['PAID', 'PREPARING', 'DELIVERING', 'COMPLETED'].includes(status)
}

async function cancel(orderId: string) {
  actionLoading.value = orderId
  try {
    await orderStore.cancelOrder(orderId, '用户取消待支付订单')
    ElMessage.success('订单已取消，库存已释放')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '取消订单失败')
  } finally {
    actionLoading.value = ''
  }
}

async function refund(orderId: string) {
  actionLoading.value = orderId
  try {
    await orderStore.requestRefund(orderId, '用户申请退款')
    ElMessage.success('退款申请已提交')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '申请退款失败')
  } finally {
    actionLoading.value = ''
  }
}

function paymentText(status: PaymentStatus) {
  return {
    PENDING: '待发起支付',
    PAYING: '等待支付',
    PAID: '已支付',
    CLOSED: '支付关闭',
    EXPIRED: '支付超时',
  }[status]
}

function channelText(channel: PaymentChannel) {
  return {
    ALIPAY_QR: '支付宝',
    WECHAT_QR: '微信',
  }[channel]
}
</script>

<style scoped>
.orders-view {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 70px;
}

.order-time,
.order-address,
.payment-line {
  font-size: 12px;
  color: #999;
  margin-top: 12px;
}

.payment-line {
  color: #666;
}

.order-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
  gap: 8px;
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
