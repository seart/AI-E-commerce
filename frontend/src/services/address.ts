import { request } from '@/services/request'
import { mockServer } from '@/services/mock/server'
import type { Address, AddressInput } from '@/types/domain'

// 收货地址接口：结算页和地址管理页都会通过这里读写后端地址数据。
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
