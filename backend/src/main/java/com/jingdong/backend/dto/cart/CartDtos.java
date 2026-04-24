package com.jingdong.backend.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public final class CartDtos {
  private CartDtos() {}

  public record AddCartItemRequest(
      @NotBlank(message = "商品 ID 不能为空")
      String productId
  ) {}

  public record UpdateCartQuantityRequest(
      @NotNull(message = "商品数量不能为空")
      @Min(value = 0, message = "商品数量不能小于 0")
      Integer quantity
  ) {}

  public record CheckedRequest(
      @NotNull(message = "勾选状态不能为空")
      Boolean checked
  ) {}

  public record CartItemResponse(
      String id,
      String merchantId,
      String merchantName,
      String categoryId,
      String name,
      int sales,
      BigDecimal price,
      BigDecimal originalPrice,
      String imageText,
      String unit,
      String description,
      int stock,
      int quantity,
      boolean checked
  ) {}
}
