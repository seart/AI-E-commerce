import { request } from '@/services/request'
import { mockServer } from '@/services/mock/server'
import type { CheckoutItem, Order } from '@/types/domain'

export const orderService = {
  getOrders() {
    return request<Order[]>(
      {
        method: 'get',
        url: '/orders',
      },
      () => mockServer.getOrders(),
    )
  },

  createOrder(addressId: string, items: CheckoutItem[]) {
    return request<Order>(
      {
        method: 'post',
        url: '/orders',
        data: { addressId, items },
      },
      () => mockServer.createOrder(addressId, items),
    )
  },
}
