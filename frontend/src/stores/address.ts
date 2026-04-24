import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { addressService } from '@/services/address'
import type { Address, AddressInput } from '@/types/domain'

export const useAddressStore = defineStore('address', () => {
  const addresses = ref<Address[]>([])
  const loading = ref(false)

  const defaultAddress = computed(() => {
    return addresses.value.find((item) => item.isDefault) ?? addresses.value[0] ?? null
  })

  async function loadAddresses(force = false) {
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
