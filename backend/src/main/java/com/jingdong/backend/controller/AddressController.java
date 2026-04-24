package com.jingdong.backend.controller;

import com.jingdong.backend.api.ApiResponse;
import com.jingdong.backend.auth.UserContext;
import com.jingdong.backend.dto.address.AddressDtos.AddressRequest;
import com.jingdong.backend.dto.address.AddressDtos.AddressResponse;
import com.jingdong.backend.service.AddressService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/addresses")
public class AddressController {
  private final AddressService addressService;

  public AddressController(AddressService addressService) {
    this.addressService = addressService;
  }

  @GetMapping
  public ApiResponse<List<AddressResponse>> addresses() {
    return ApiResponse.success(addressService.getAddresses(UserContext.userId()));
  }

  @PostMapping
  public ApiResponse<AddressResponse> create(@Valid @RequestBody AddressRequest request) {
    return ApiResponse.success(addressService.createAddress(UserContext.userId(), request));
  }

  @PutMapping("/{addressId}")
  public ApiResponse<AddressResponse> update(
      @PathVariable String addressId,
      @Valid @RequestBody AddressRequest request
  ) {
    return ApiResponse.success(addressService.updateAddress(
        UserContext.userId(),
        addressId,
        request
    ));
  }
}
