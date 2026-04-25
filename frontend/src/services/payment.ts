import { request } from '@/services/request'
import { mockServer } from '@/services/mock/server'
import type { PaymentChannel, PaymentPrepayResponse, PaymentStatusResponse } from '@/types/domain'

export const paymentService = {
  prepay(orderId: string, channel: PaymentChannel) {
    return request<PaymentPrepayResponse>(
      {
        method: 'post',
        url: '/payments/prepay',
        data: { orderId, channel },
      },
      () => mockServer.prepay(orderId, channel),
    )
  },

  getStatus(paymentId: string) {
    return request<PaymentStatusResponse>(
      {
        method: 'get',
        url: `/payments/${paymentId}`,
      },
      () => mockServer.getPaymentStatus(paymentId),
    )
  },
}
