package com.jingdong.backend.service;

import com.jingdong.backend.dto.address.AddressDtos.AddressRequest;
import com.jingdong.backend.dto.address.AddressDtos.AddressResponse;
import com.jingdong.backend.store.DatabaseStore;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AddressService {
  // 收货地址业务层，当前主要把 controller 请求转给持久化门面。
  private final DatabaseStore store;

  public AddressService(DatabaseStore store) {
    this.store = store;
  }

  public List<AddressResponse> getAddresses(String userId) {
    return store.getAddresses(userId);
  }

  public AddressResponse createAddress(String userId, AddressRequest request) {
    return store.createAddress(userId, request);
  }

  public AddressResponse updateAddress(String userId, String addressId, AddressRequest request) {
    return store.updateAddress(userId, addressId, request);
  }
}
