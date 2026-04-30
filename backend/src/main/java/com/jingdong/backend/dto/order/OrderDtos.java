package com.jingdong.backend.dto.order;

import com.jingdong.backend.dto.address.AddressDtos.AddressResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public final class OrderDtos {
  private OrderDtos() {}

  public record CheckoutItemRequest(
      String productId,

      String skuId,

      @NotNull(message = "商品数量不能为空")
      @Min(value = 1, message = "商品数量至少为 1")
      Integer quantity
  ) {
    public CheckoutItemRequest(String productId, Integer quantity) {
      this(productId, null, quantity);
    }

    public String purchasableId() {
      return skuId != null && !skuId.isBlank() ? skuId : productId;
    }
  }

  public record CreateOrderRequest(
      @NotBlank(message = "地址 ID 不能为空")
      String addressId,

      @Valid
      @NotEmpty(message = "订单商品不能为空")
      List<CheckoutItemRequest> items
  ) {}

  public record OrderActionRequest(
      String reason
  ) {}

  public record OrderLineResponse(
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
      boolean checked,
      BigDecimal amount
  ) {}

  public record OrderResponse(
      String id,
      String orderNo,
      String createdAt,
      BigDecimal totalAmount,
      String status,
      String statusText,
      String paymentStatus,
      String paymentChannel,
      String paidAt,
      String paymentExpireAt,
      String closedAt,
      List<OrderLineResponse> items,
      AddressResponse address
  ) {}
}
