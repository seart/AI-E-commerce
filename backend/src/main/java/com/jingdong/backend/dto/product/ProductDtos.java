package com.jingdong.backend.dto.product;

import java.math.BigDecimal;
import java.util.List;

public final class ProductDtos {
  private ProductDtos() {}

  public record BrandResponse(String id, String name, String logo, String description) {}

  public record SpecOptionResponse(String id, String groupId, String groupName, String name) {}

  public record SkuSpecResponse(String groupId, String groupName, String optionId, String optionName) {}

  public record ProductSkuResponse(
      String skuId,
      String productId,
      String spuId,
      String skuCode,
      List<SkuSpecResponse> specs,
      String specText,
      BigDecimal price,
      BigDecimal originalPrice,
      String unit,
      int stock,
      String status
  ) {}

  public record ProductCardResponse(
      String id,
      String spuId,
      String skuId,
      String merchantId,
      String merchantName,
      String categoryId,
      String brandId,
      String brandName,
      String name,
      String subtitle,
      int sales,
      BigDecimal minPrice,
      BigDecimal maxPrice,
      BigDecimal originalPrice,
      String imageText,
      String mainImage,
      String unit,
      String description,
      int stock,
      boolean singleSku
  ) {}

  public record ProductDetailResponse(
      ProductCardResponse product,
      List<ProductSkuResponse> skus,
      List<String> detailImages,
      String detail
  ) {}
}
