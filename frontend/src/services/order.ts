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

  cancelOrder(orderId: string, reason = '用户取消订单') {
    return request<Order>(
      {
        method: 'post',
        url: `/orders/${orderId}/cancel`,
        data: { reason },
      },
      () => mockServer.cancelOrder(orderId, reason),
    )
  },

  requestRefund(orderId: string, reason = '用户申请退款') {
    return request<Order>(
      {
        method: 'post',
        url: `/orders/${orderId}/refund`,
        data: { reason },
      },
      () => mockServer.requestRefund(orderId, reason),
    )
  },
}
