import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { cartService } from '@/services/cart'
import type { CartItem, CartMerchantGroup, Product, ProductCard, ProductSku } from '@/types/domain'

// 购物车 store：页面只读这里的状态，所有增删改都通过 cartService 同步后端。
export const useCartStore = defineStore('cart', () => {
  const items = ref<CartItem[]>([])
  const loading = ref(false)
  const mutating = ref(false)

  const checkedItems = computed(() => items.value.filter((item) => item.checked))

  const exactCheckedTotalPrice = computed(() => {
    return Number(
      checkedItems.value.reduce((sum, item) => sum + item.price * item.quantity, 0).toFixed(2),
    )
  })

  const cartGroupedByMerchant = computed<CartMerchantGroup[]>(() => {
    const grouped = new Map<string, CartMerchantGroup>()

    // 购物车页面按商家分组展示，所以这里把扁平接口数据整理成商家分组。
    items.value.forEach((item) => {
      if (!grouped.has(item.merchantId)) {
        grouped.set(item.merchantId, {
          merchantId: item.merchantId,
          merchantName: item.merchantName,
          items: [],
        })
      }

      grouped.get(item.merchantId)?.items.push(item)
    })

    return Array.from(grouped.values())
  })

  const selectedGroups = computed<CartMerchantGroup[]>(() => {
    return cartGroupedByMerchant.value
      .map((group) => ({
        ...group,
        items: group.items.filter((item) => item.checked),
      }))
      .filter((group) => group.items.length > 0)
  })

  async function loadCart(force = false) {
    // 非强制刷新时复用已有购物车数据，避免页面反复切换产生多余请求。
    if (loading.value || (items.value.length > 0 && !force)) {
      return items.value
    }

    loading.value = true
    try {
      items.value = await cartService.getCartItems()
      return items.value
    } finally {
      loading.value = false
    }
  }

  async function syncCart(task: () => Promise<CartItem[]>) {
    // 所有购物车写操作都返回后端最新购物车列表，前端直接整体替换，减少局部状态不一致。
    mutating.value = true
    try {
      items.value = await task()
      return items.value
    } finally {
      mutating.value = false
    }
  }

  async function addSkuToCart(skuId: string) {
    return syncCart(() => cartService.addCartItem(skuId))
  }

  async function addToCart(product: Product | ProductCard | ProductSku) {
    // 页面可能传 SPU、SKU 或商品卡片，这里统一解析成后端购物车需要的 skuId。
    if ('skuId' in product && product.skuId) {
      return addSkuToCart(product.skuId)
    }
    if ('productId' in product && product.productId) {
      return addSkuToCart(product.productId)
    }
    if ('id' in product) {
      return addSkuToCart(product.id)
    }
    throw new Error('商品信息不完整')
  }

  async function updateQuantity(productId: string, delta: number) {
    const current = items.value.find((item) => item.id === productId)
    if (!current) {
      throw new Error('购物车商品不存在')
    }

    return syncCart(() => cartService.updateCartItemQuantity(productId, current.quantity + delta))
  }

  async function setItemChecked(productId: string, checked: boolean) {
    return syncCart(() => cartService.setCartItemChecked(productId, checked))
  }

  async function toggleMerchantChecked(merchantId: string, checked: boolean) {
    return syncCart(() => cartService.setMerchantCartChecked(merchantId, checked))
  }

  async function toggleAllChecked(checked: boolean) {
    return syncCart(() => cartService.setAllCartChecked(checked))
  }

  async function clearChecked() {
    return syncCart(() => cartService.clearCheckedCartItems())
  }

  function getMerchantTotal(merchantId: string) {
    return Number(
      items.value
        .filter((item) => item.merchantId === merchantId)
        .reduce((sum, item) => sum + item.price * item.quantity, 0)
        .toFixed(2),
    )
  }

  function reset() {
    items.value = []
  }

  return {
    items,
    loading,
    mutating,
    checkedItems,
    exactCheckedTotalPrice,
    cartGroupedByMerchant,
    selectedGroups,
    loadCart,
    addSkuToCart,
    addToCart,
    updateQuantity,
    setItemChecked,
    toggleMerchantChecked,
    toggleAllChecked,
    clearChecked,
    getMerchantTotal,
    reset,
  }
})
