package com.jingdong.backend.dto.merchant;

import java.math.BigDecimal;
import java.util.List;

public final class MerchantDtos {
  private MerchantDtos() {}

  public record MerchantCategoryResponse(String id, String name) {}

  public record MerchantResponse(
      String id,
      String name,
      int monthlySales,
      BigDecimal minOrderAmount,
      BigDecimal deliveryFee,
      int etaMinutes,
      List<String> tags,
      String description,
      String notice,
      BigDecimal rating,
      String heroColor,
      String logoText,
      List<MerchantCategoryResponse> categories
  ) {}

  public record ProductResponse(
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
      int stock
  ) {}

  public record MerchantDetailResponse(
      MerchantResponse merchant,
      List<ProductResponse> products
  ) {}
}
