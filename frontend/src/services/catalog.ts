import { request } from '@/services/request'
import { mockServer } from '@/services/mock/server'
import type { HomePageData, Merchant, MerchantDetail, ProductCard, ProductDetail } from '@/types/domain'

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

  searchProducts(keyword: string, filters: { merchantId?: string; categoryId?: string; brandId?: string } = {}) {
    return request<ProductCard[]>(
      {
        method: 'get',
        url: '/products/search',
        params: { keyword, ...filters },
      },
      () => mockServer.searchProducts(keyword, filters),
    )
  },

  getProductDetail(productId: string) {
    return request<ProductDetail>(
      {
        method: 'get',
        url: `/products/${productId}`,
      },
      () => mockServer.getProductDetail(productId),
    )
  },
}
