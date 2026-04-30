<template>
  <div class="checkout-view">
    <div class="header">
      <el-icon class="back-icon" @click="goBack"><ArrowLeft /></el-icon>
      <h2>确认订单</h2>
    </div>

    <div class="address-card" v-if="defaultAddress" @click="goToAddressList">
      <div class="address-info">
        <div class="title">
          收货地址
          <el-tag size="small" type="danger" v-if="defaultAddress.isDefault" style="margin-left: 8px">
            默认
          </el-tag>
        </div>
        <div class="detail">{{ formatAddress(defaultAddress) }}</div>
        <div class="user">
          {{ defaultAddress.contactName }} {{ defaultAddress.phone }} · {{ defaultAddress.tag }}
        </div>
      </div>
      <el-icon class="arrow-icon"><ArrowRight /></el-icon>
    </div>

    <div class="address-card" v-else @click="goToAddressList">
      <div class="address-info">
        <div class="title">请先新增收货地址</div>
        <div class="detail">提交订单前需要选择有效的配送地址</div>
      </div>
      <el-icon class="arrow-icon"><ArrowRight /></el-icon>
    </div>

    <div class="order-list" v-if="selectedGroups.length > 0">
      <div v-for="group in selectedGroups" :key="group.merchantId" class="merchant-block">
        <div class="merchant-name">{{ group.merchantName }}</div>

        <div class="product-item" v-for="item in group.items" :key="item.id">
          <div class="product-img">{{ item.imageText }}</div>
          <div class="product-info">
            <div class="name">{{ item.productName || item.name }}</div>
            <div class="snapshot" v-if="item.brandName || item.specText">
              <span v-if="item.brandName">{{ item.brandName }}</span>
              <span v-if="item.specText">{{ item.specText }}</span>
            </div>
            <div class="price-qty">
              <span class="price">¥{{ item.price.toFixed(2) }} x {{ item.quantity }}</span>
              <span class="amount">¥{{ (item.price * item.quantity).toFixed(2) }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="expand-btn">共 {{ checkedItems.length }} 件商品</div>
    </div>

    <el-empty v-else description="没有可结算的商品，正在返回购物车" />

    <div class="submit-bar">
      <div class="amount-text">
        实付金额 <b>¥{{ exactCheckedTotalPrice.toFixed(2) }}</b>
      </div>
      <el-button type="primary" class="submit-btn" @click="confirmSubmit">提交订单</el-button>
    </div>

    <el-dialog
      v-model="showConfirmDialog"
      title="选择支付方式"
      width="320px"
      align-center
      :show-close="false"
    >
      <div class="pay-choice">
        <p>提交后订单会进入待支付状态，请在超时时间内完成扫码支付。</p>
        <el-radio-group v-model="selectedChannel" class="pay-radio-group">
          <el-radio-button label="ALIPAY_QR">支付宝扫码</el-radio-button>
          <el-radio-button label="WECHAT_QR">微信扫码</el-radio-button>
        </el-radio-group>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showConfirmDialog = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="processPayment">
            生成二维码
          </el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showPayDialog"
      title="扫码支付"
      width="340px"
      align-center
      :close-on-click-modal="false"
      @closed="clearPaymentPolling"
    >
      <div class="qr-panel" v-if="activePayment">
        <div class="qr-amount">¥{{ activePayment.amount.toFixed(2) }}</div>
        <canvas ref="payCanvas" class="qr-canvas" aria-label="支付二维码"></canvas>
        <p class="qr-tip">
          {{ activePayment.channel === 'ALIPAY_QR' ? '请使用支付宝扫码支付' : '请使用微信扫码支付' }}
        </p>
        <p class="qr-expire" v-if="activePayment.expireAt">
          {{ formatDateTime(activePayment.expireAt) }} 前有效
        </p>
      </div>
      <template #footer>
        <div class="pay-footer">
          <el-button @click="finishShopping">稍后去订单查看</el-button>
          <el-button type="primary" :loading="paymentChecking" @click="checkPaymentStatus">
            我已完成支付
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showSuccessDialog"
      title="支付结果"
      width="300px"
      align-center
      :show-close="false"
    >
      <div class="success-content">
        <el-icon class="success-icon"><CircleCheckFilled /></el-icon>
        <p>支付成功，订单已提交</p>
      </div>
      <template #footer>
        <div class="success-footer">
          <el-button type="primary" class="success-btn" @click="finishShopping">查看订单</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight, CircleCheckFilled } from '@element-plus/icons-vue'
import QRCode from 'qrcode'
import { useCartStore } from '@/stores/cart'
import { useOrderStore } from '@/stores/order'
import { useAddressStore } from '@/stores/address'
import { paymentService } from '@/services/payment'
import { formatAddress, formatDateTime } from '@/utils/format'
import type { PaymentChannel, PaymentPrepayResponse } from '@/types/domain'

defineOptions({ name: 'CheckoutView' })

const router = useRouter()
const cartStore = useCartStore()
const orderStore = useOrderStore()
const addressStore = useAddressStore()

const { selectedGroups, checkedItems, exactCheckedTotalPrice } = storeToRefs(cartStore)
const { defaultAddress } = storeToRefs(addressStore)
const { submitting } = storeToRefs(orderStore)

const showConfirmDialog = ref(false)
const showPayDialog = ref(false)
const showSuccessDialog = ref(false)
const selectedChannel = ref<PaymentChannel>('ALIPAY_QR')
const activePayment = ref<PaymentPrepayResponse | null>(null)
const payCanvas = ref<HTMLCanvasElement | null>(null)
const paymentChecking = ref(false)
let paymentPollTimer: number | undefined

onMounted(async () => {
  await Promise.all([cartStore.loadCart(true), addressStore.loadAddresses(true)])

  if (checkedItems.value.length === 0) {
    router.replace('/cart')
  }
})

onBeforeUnmount(() => {
  clearPaymentPolling()
})

function goToAddressList() {
  router.push('/address')
}

function goBack() {
  router.back()
}

function confirmSubmit() {
  if (!defaultAddress.value) {
    ElMessage.warning('请先新增收货地址')
    return
  }

  if (checkedItems.value.length === 0) {
    ElMessage.warning('没有可提交的商品')
    return
  }

  showConfirmDialog.value = true
}

async function processPayment() {
  if (!defaultAddress.value) {
    return
  }

  try {
    const order = await orderStore.createOrder(
      defaultAddress.value.id,
      checkedItems.value.map((item) => ({
        productId: item.productId ?? item.id,
        skuId: item.skuId ?? item.productId ?? item.id,
        quantity: item.quantity,
      })),
    )
    const payment = await paymentService.prepay(order.id, selectedChannel.value)
    await cartStore.loadCart(true)
    showConfirmDialog.value = false
    activePayment.value = payment
    showPayDialog.value = true
    await nextTick()
    await renderPaymentQr()
    startPaymentPolling()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交订单失败')
  }
}

async function renderPaymentQr() {
  if (!payCanvas.value || !activePayment.value) {
    return
  }

  await QRCode.toCanvas(payCanvas.value, activePayment.value.qrContent, {
    width: 220,
    margin: 1,
    color: {
      dark: '#171717',
      light: '#ffffff',
    },
  })
}

function startPaymentPolling() {
  clearPaymentPolling()
  paymentPollTimer = window.setInterval(() => {
    void checkPaymentStatus()
  }, 2500)
}

function clearPaymentPolling() {
  if (paymentPollTimer) {
    window.clearInterval(paymentPollTimer)
    paymentPollTimer = undefined
  }
}

async function checkPaymentStatus() {
  if (!activePayment.value || paymentChecking.value) {
    return
  }

  paymentChecking.value = true
  try {
    const status = await paymentService.getStatus(activePayment.value.paymentId)
    if (status.status === 'PAID' || status.orderStatus === 'PAID') {
      clearPaymentPolling()
      await orderStore.loadOrders(true)
      showPayDialog.value = false
      showSuccessDialog.value = true
      return
    }

    if (status.status === 'CLOSED' || status.status === 'EXPIRED' || status.orderStatus === 'PAYMENT_CLOSED') {
      clearPaymentPolling()
      await orderStore.loadOrders(true)
      showPayDialog.value = false
      ElMessage.error('支付已超时关闭，库存已释放')
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '查询支付状态失败')
  } finally {
    paymentChecking.value = false
  }
}

function finishShopping() {
  clearPaymentPolling()
  showSuccessDialog.value = false
  showPayDialog.value = false
  router.push('/orders')
}
</script>

<style scoped>
.checkout-view {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 70px;
}

.header {
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

.address-card {
  margin: 12px 18px;
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  display: flex;
  align-items: center;
}

.address-info {
  flex: 1;
}

.address-info .title {
  font-weight: 700;
  font-size: 16px;
  color: #333;
  margin-bottom: 8px;
}

.address-info .detail {
  font-size: 14px;
  color: #333;
  line-height: 1.5;
  margin-bottom: 6px;
}

.address-info .user {
  font-size: 12px;
  color: #666;
}

.arrow-icon {
  font-size: 16px;
  color: #999;
}

.order-list {
  background: #fff;
  margin: 0 18px;
  border-radius: 16px;
  padding: 16px;
}

.merchant-block + .merchant-block {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid #f5f5f5;
}

.merchant-name {
  font-weight: 700;
  font-size: 14px;
  color: #333;
  margin-bottom: 16px;
}

.product-item {
  display: flex;
  margin-bottom: 16px;
}

.product-item:last-child {
  margin-bottom: 0;
}

.product-img {
  width: 46px;
  height: 46px;
  background: linear-gradient(135deg, #fff0eb 0%, #ffe4d8 100%);
  border-radius: 10px;
  margin-right: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #e1251b;
  font-size: 12px;
  font-weight: 700;
}

.product-info {
  flex: 1;
}

.product-info .name {
  font-size: 14px;
  color: #333;
  line-height: 1.4;
}

.snapshot {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 5px;
  color: #8c8f99;
  font-size: 12px;
}

.price-qty {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.price-qty .price {
  font-size: 12px;
  color: #e1251b;
}

.price-qty .amount {
  font-size: 14px;
  font-weight: 700;
  color: #333;
}

.expand-btn {
  text-align: center;
  font-size: 12px;
  color: #999;
  background: #f9f9f9;
  padding: 8px 0;
  border-radius: 8px;
  margin-top: 16px;
}

.submit-bar {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 480px;
  height: 50px;
  background: #fff;
  display: flex;
  box-shadow: 0 -2px 6px rgba(0, 0, 0, 0.05);
  z-index: 10;
  padding-bottom: env(safe-area-inset-bottom);
  box-sizing: content-box;
}

.amount-text {
  flex: 1;
  display: flex;
  align-items: center;
  padding-left: 18px;
  font-size: 14px;
  color: #333;
}

.amount-text b {
  font-size: 18px;
  color: #e1251b;
  margin-left: 4px;
}

.submit-btn {
  height: 50px;
  border-radius: 0;
  width: 120px;
  font-size: 16px;
}

.success-content {
  text-align: center;
  padding: 10px 0;
}

.pay-choice p,
.qr-tip,
.qr-expire {
  margin: 0;
  color: #666;
  font-size: 13px;
  line-height: 1.6;
}

.pay-radio-group {
  margin-top: 14px;
  width: 100%;
}

.qr-panel {
  text-align: center;
}

.qr-amount {
  color: #e1251b;
  font-size: 24px;
  font-weight: 800;
  margin-bottom: 12px;
}

.qr-canvas {
  width: 220px;
  height: 220px;
  padding: 10px;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 12px 34px rgba(0, 0, 0, 0.08);
}

.qr-tip {
  margin-top: 12px;
  color: #333;
  font-weight: 700;
}

.qr-expire {
  margin-top: 4px;
}

.pay-footer {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.success-icon {
  font-size: 48px;
  color: #67c23a;
  margin-bottom: 12px;
}

.success-footer {
  width: 100%;
}

.success-btn {
  width: 100%;
  border-radius: 20px;
}
</style>
