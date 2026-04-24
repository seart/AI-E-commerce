import { request } from '@/services/request'
import { mockServer } from '@/services/mock/server'
import type { HomePageData, Merchant, MerchantDetail } from '@/types/domain'

export const catalogService = {
  getHomePage() {
    return request<HomePageData>(
      {
        method: 'get',
        url: '/home',
      },
      () => mockServer.getHomePage(),
    )
  },

  searchMerchants(keyword: string) {
    return request<Merchant[]>(
      {
        method: 'get',
        url: '/merchants/search',
        params: { keyword },
      },
      () => mockServer.searchMerchants(keyword),
    )
  },

  getMerchantDetail(merchantId: string) {
    return request<MerchantDetail>(
      {
        method: 'get',
        url: `/merchants/${merchantId}`,
      },
      () => mockServer.getMerchantDetail(merchantId),
    )
  },
}
