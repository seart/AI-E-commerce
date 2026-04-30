import { ref } from 'vue'
import { defineStore } from 'pinia'
import { STORAGE_KEYS } from '@/constants/storage'
import { catalogService } from '@/services/catalog'
import type { HomePageData, Merchant, MerchantDetail, ProductCard, ProductDetail } from '@/types/domain'
import { loadJson, saveJson } from '@/utils/storage'

const popularKeywords = ['矿泉水', '榴莲', '气泡水', '咖啡', '三文鱼', '苹果']

export const useCatalogStore = defineStore('catalog', () => {
  const home = ref<HomePageData | null>(null)
  const merchants = ref<Merchant[]>([])
  const products = ref<ProductCard[]>([])
  const merchantDetails = ref<Record<string, MerchantDetail>>({})
  const productDetails = ref<Record<string, ProductDetail>>({})
  const homeLoading = ref(false)
  const searchLoading = ref(false)
  const detailLoading = ref(false)
  const productDetailLoading = ref(false)
  const searchHistory = ref<string[]>(loadJson<string[]>(STORAGE_KEYS.searchHistory, []))

  async function loadHomePage(force = false) {
    if (homeLoading.value || (home.value && !force)) {
      return home.value
    }

    homeLoading.value = true
    try {
      home.value = await catalogService.getHomePage()
      return home.value
    } finally {
      homeLoading.value = false
    }
  }

  async function searchMerchants(keyword: string) {
    searchLoading.value = true
    try {
      merchants.value = await catalogService.searchMerchants(keyword)
      return merchants.value
    } finally {
      searchLoading.value = false
    }
  }

  async function searchProducts(keyword: string, filters: { merchantId?: string; categoryId?: string; brandId?: string } = {}) {
    searchLoading.value = true
    try {
      products.value = await catalogService.searchProducts(keyword, filters)
      return products.value
    } finally {
      searchLoading.value = false
    }
  }

  async function searchAll(keyword: string) {
    searchLoading.value = true
    try {
      const [merchantRows, productRows] = await Promise.all([
        catalogService.searchMerchants(keyword),
        catalogService.searchProducts(keyword),
      ])
      merchants.value = merchantRows
      products.value = productRows
      return { merchants: merchantRows, products: productRows }
    } finally {
      searchLoading.value = false
    }
  }

  async function loadMerchantDetail(merchantId: string, force = false) {
    if (detailLoading.value) {
      return merchantDetails.value[merchantId] ?? null
    }
    if (merchantDetails.value[merchantId] && !force) {
      return merchantDetails.value[merchantId]
    }

    detailLoading.value = true
    try {
      const detail = await catalogService.getMerchantDetail(merchantId)
      merchantDetails.value = {
        ...merchantDetails.value,
        [merchantId]: detail,
      }
      return detail
    } finally {
      detailLoading.value = false
    }
  }

  async function loadProductDetail(productId: string, force = false) {
    if (productDetailLoading.value) {
      return productDetails.value[productId] ?? null
    }
    if (productDetails.value[productId] && !force) {
      return productDetails.value[productId]
    }

    productDetailLoading.value = true
    try {
      const detail = await catalogService.getProductDetail(productId)
      productDetails.value = {
        ...productDetails.value,
        [productId]: detail,
        [detail.product.spuId]: detail,
        ...(detail.product.skuId ? { [detail.product.skuId]: detail } : {}),
      }
      return detail
    } finally {
      productDetailLoading.value = false
    }
  }

  function addSearchHistory(keyword: string) {
    const normalized = keyword.trim()
    if (!normalized) {
      return
    }

    searchHistory.value = [normalized, ...searchHistory.value.filter((item) => item !== normalized)]
      .slice(0, 8)
    saveJson(STORAGE_KEYS.searchHistory, searchHistory.value)
  }

  function clearSearchHistory() {
    searchHistory.value = []
    saveJson(STORAGE_KEYS.searchHistory, searchHistory.value)
  }

  return {
    home,
    merchants,
    products,
    merchantDetails,
    productDetails,
    homeLoading,
    searchLoading,
    detailLoading,
    productDetailLoading,
    popularKeywords,
    searchHistory,
    loadHomePage,
    searchMerchants,
    searchProducts,
    searchAll,
    loadMerchantDetail,
    loadProductDetail,
    addSearchHistory,
    clearSearchHistory,
  }
})
