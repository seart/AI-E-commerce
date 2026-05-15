import { ref } from 'vue'
import { defineStore } from 'pinia'
import { orderService } from '@/services/order'
import type { CheckoutItem, Order } from '@/types/domain'

// 订单 store：负责订单列表缓存，以及下单/取消/退款后的本地列表同步。
export const useOrderStore = defineStore('order', () => {
  const orders = ref<Order[]>([])
  const loading = ref(false)
  const submitting = ref(false)

  async function loadOrders(force = false) {
    // 订单列表默认缓存一次，需要重新拉取时传 force=true。
    if (loading.value || (orders.value.length > 0 && !force)) {
      return orders.value
    }

    loading.value = true
    try {
      orders.value = await orderService.getOrders()
      return orders.value
    } finally {
      loading.value = false
    }
  }

  async function createOrder(addressId: string, items: CheckoutItem[]) {
    submitting.value = true
    try {
      const order = await orderService.createOrder(addressId, items)
      // 新订单插到列表最前面，符合“最新订单优先”的页面展示习惯。
      orders.value = [order, ...orders.value]
      return order
    } finally {
      submitting.value = false
    }
  }

  async function cancelOrder(orderId: string, reason?: string) {
    const order = await orderService.cancelOrder(orderId, reason)
    orders.value = orders.value.map((item) => (item.id === order.id ? order : item))
    return order
  }

  async function requestRefund(orderId: string, reason?: string) {
    const order = await orderService.requestRefund(orderId, reason)
    orders.value = orders.value.map((item) => (item.id === order.id ? order : item))
    return order
  }

  function reset() {
    orders.value = []
  }

  return {
    orders,
    loading,
    submitting,
    loadOrders,
    createOrder,
    cancelOrder,
    requestRefund,
    reset,
  }
})
