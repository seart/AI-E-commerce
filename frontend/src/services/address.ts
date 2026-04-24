import { request } from '@/services/request'
import { mockServer } from '@/services/mock/server'
import type { Address, AddressInput } from '@/types/domain'

export const addressService = {
  getAddresses() {
    return request<Address[]>(
      {
        method: 'get',
        url: '/addresses',
      },
      () => mockServer.getAddresses(),
    )
  },

  createAddress(payload: AddressInput) {
    return request<Address>(
      {
        method: 'post',
        url: '/addresses',
        data: payload,
      },
      () => mockServer.createAddress(payload),
    )
  },

  updateAddress(addressId: string, payload: AddressInput) {
    return request<Address>(
      {
        method: 'put',
        url: `/addresses/${addressId}`,
        data: payload,
      },
      () => mockServer.updateAddress(addressId, payload),
    )
  },
}
