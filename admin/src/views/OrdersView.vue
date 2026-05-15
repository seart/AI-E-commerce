<script setup lang="ts">
// 订单管理页：后台推进订单状态，并处理退款确认。
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getOrders, updateOrderStatus } from '@/api/admin'
import type { Order } from '@/types/domain'

const orders = ref<Order[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    orders.value = await getOrders()
  } finally {
    loading.value = false
  }
}

async function move(order: Order, status: string) {
  await updateOrderStatus(order.id, status, '后台推进订单')
  ElMessage.success('订单状态已更新')
  await load()
}

function paymentText(order: Order) {
  const statusText = {
    PENDING: '待支付',
    PAYING: '扫码中',
    PAID: '已支付',
    CLOSED: '已关闭',
    EXPIRED: '已超时',
  }[order.paymentStatus]
  const channelMap: Record<string, string> = {
    ALIPAY_QR: '支付宝',
    WECHAT_QR: '微信',
  }
  const channelText = order.paymentChannel ? channelMap[order.paymentChannel] : ''
  return channelText ? `${statusText} / ${channelText}` : statusText
}

onMounted(load)
</script>

<template>
  <el-card class="page-card">
    <div class="toolbar">
      <h2>订单管理</h2>
      <el-button @click="load">刷新</el-button>
    </div>
    <el-table v-loading="loading" :data="orders" row-key="id">
      <el-table-column prop="orderNo" label="订单号" width="190" />
      <el-table-column prop="statusText" label="状态" width="120" />
      <el-table-column label="支付" width="150">
        <template #default="{ row }">{{ paymentText(row) }}</template>
      </el-table-column>
      <el-table-column prop="totalAmount" label="金额" width="120" />
      <el-table-column label="收货人" width="150">
        <template #default="{ row }">{{ row.address.contactName }} {{ row.address.phone }}</template>
      </el-table-column>
      <el-table-column prop="paymentExpireAt" label="支付超时" min-width="190" />
      <el-table-column prop="createdAt" label="创建时间" min-width="190" />
      <el-table-column label="操作" width="320">
        <template #default="{ row }">
          <el-button size="small" :disabled="row.status !== 'PAID'" @click="move(row, 'PREPARING')">备货</el-button>
          <el-button size="small" :disabled="row.status !== 'PREPARING'" @click="move(row, 'DELIVERING')">配送</el-button>
          <el-button size="small" :disabled="row.status !== 'DELIVERING'" @click="move(row, 'COMPLETED')">完成</el-button>
          <el-button size="small" type="warning" :disabled="row.status !== 'REFUND_REQUESTED'" @click="move(row, 'REFUNDED')">退款</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
