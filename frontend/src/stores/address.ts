import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { addressService } from '@/services/address'
import type { Address, AddressInput } from '@/types/domain'

// 地址 store：负责地址列表缓存和默认地址计算。
export const useAddressStore = defineStore('address', () => {
  const addresses = ref<Address[]>([])
  const loading = ref(false)

  const defaultAddress = computed(() => {
    // 优先使用后端标记的默认地址，没有默认时取第一条。
    return addresses.value.find((item) => item.isDefault) ?? addresses.value[0] ?? null
  })

  async function loadAddresses(force = false) {
    // 地址不频繁变化，默认复用缓存；新增/编辑后再强制刷新。
    if (loading.value || (addresses.value.length > 0 && !force)) {
      return addresses.value
    }

    loading.value = true
    try {
      addresses.value = await addressService.getAddresses()
      return addresses.value
    } finally {
      loading.value = false
    }
  }

  async function addAddress(payload: AddressInput) {
    await addressService.createAddress(payload)
    return loadAddresses(true)
  }

  async function updateAddress(addressId: string, payload: AddressInput) {
    await addressService.updateAddress(addressId, payload)
    return loadAddresses(true)
  }

  function getAddressById(addressId: string) {
    return addresses.value.find((item) => item.id === addressId) ?? null
  }

  function reset() {
    addresses.value = []
  }

  return {
    addresses,
    defaultAddress,
    loading,
    loadAddresses,
    addAddress,
    updateAddress,
    getAddressById,
    reset,
  }
})
