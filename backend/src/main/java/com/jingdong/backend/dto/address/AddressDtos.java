package com.jingdong.backend.dto.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public final class AddressDtos {
  private AddressDtos() {}

  public record AddressRequest(
      @NotBlank(message = "城市不能为空")
      String city,

      @NotBlank(message = "区县不能为空")
      String district,

      @NotBlank(message = "街道不能为空")
      String street,

      @NotBlank(message = "详细地址不能为空")
      String detail,

      @NotBlank(message = "收货人不能为空")
      String contactName,

      @NotBlank(message = "手机号不能为空")
      @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
      String phone,

      @NotBlank(message = "地址标签不能为空")
      String tag,

      @NotNull(message = "默认地址标记不能为空")
      Boolean isDefault
  ) {}

  public record AddressResponse(
      String id,
      String city,
      String district,
      String street,
      String detail,
      String contactName,
      String phone,
      String tag,
      boolean isDefault
  ) {}
}
