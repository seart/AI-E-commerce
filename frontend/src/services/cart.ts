import { request } from '@/services/request'
import { mockServer } from '@/services/mock/server'
import type { CartItem } from '@/types/domain'

// 购物车接口：所有方法都返回最新购物车列表，方便 store 直接整体替换状态。
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
        // productId 是为了兼容旧后端字段；新商品中心实际按 skuId 处理。
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
