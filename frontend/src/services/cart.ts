import { request } from '@/services/request'
import { mockServer } from '@/services/mock/server'
import type { CartItem } from '@/types/domain'

export const cartService = {
  getCartItems() {
    return request<CartItem[]>(
      {
        method: 'get',
        url: '/cart/items',
      },
      () => mockServer.getCartItems(),
    )
  },

  addCartItem(skuId: string) {
    return request<CartItem[]>(
      {
        method: 'post',
        url: '/cart/items',
        data: { skuId, productId: skuId },
      },
      () => mockServer.addCartItem(skuId),
    )
  },

  updateCartItemQuantity(productId: string, quantity: number) {
    return request<CartItem[]>(
      {
        method: 'patch',
        url: `/cart/items/${productId}`,
        data: { quantity },
      },
      () => mockServer.updateCartItemQuantity(productId, quantity),
    )
  },

  setCartItemChecked(productId: string, checked: boolean) {
    return request<CartItem[]>(
      {
        method: 'patch',
        url: `/cart/items/${productId}/checked`,
        data: { checked },
      },
      () => mockServer.setCartItemChecked(productId, checked),
    )
  },

  setMerchantCartChecked(merchantId: string, checked: boolean) {
    return request<CartItem[]>(
      {
        method: 'patch',
        url: `/cart/merchants/${merchantId}/checked`,
        data: { checked },
      },
      () => mockServer.setMerchantCartChecked(merchantId, checked),
    )
  },

  setAllCartChecked(checked: boolean) {
    return request<CartItem[]>(
      {
        method: 'patch',
        url: '/cart/checked',
        data: { checked },
      },
      () => mockServer.setAllCartChecked(checked),
    )
  },

  clearCheckedCartItems() {
    return request<CartItem[]>(
      {
        method: 'delete',
        url: '/cart/checked',
      },
      () => mockServer.clearCheckedCartItems(),
    )
  },
}
