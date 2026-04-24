import { ref } from 'vue'
import { defineStore } from 'pinia'
import { orderService } from '@/services/order'
import type { CheckoutItem, Order } from '@/types/domain'

export const useOrderStore = defineStore('order', () => {
  const orders = ref<Order[]>([])
  const loading = ref(false)
  const submitting = ref(false)

  async function loadOrders(force = false) {
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
      orders.value = [order, ...orders.value]
      return order
    } finally {
      submitting.value = false
    }
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
    reset,
  }
})
