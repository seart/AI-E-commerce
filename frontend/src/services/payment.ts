import { request } from '@/services/request'
import { mockServer } from '@/services/mock/server'
import type { PaymentChannel, PaymentPrepayResponse, PaymentStatusResponse } from '@/types/domain'

// 支付接口当前支持模拟支付链路；真实支付网关是否启用由后端配置决定。
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
