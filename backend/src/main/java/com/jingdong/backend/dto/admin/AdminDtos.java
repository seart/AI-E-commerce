package com.jingdong.backend.dto.admin;

import com.jingdong.backend.dto.order.OrderDtos.OrderResponse;
import com.jingdong.backend.store.DatabaseStore.UserRecord;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class AdminDtos {
  private AdminDtos() {}

  public record MerchantAdminResponse(
      String id,
      String name,
      int sales,
      BigDecimal minOrderPrice,
      BigDecimal deliveryFee,
      int deliveryMinutes,
      String description,
      String notice,
      BigDecimal rating,
      String logoBackground,
      String logoText,
      String status,
      int sortOrder
  ) {}

  public record MerchantUpsertRequest(
      String id,
      String name,
      Integer sales,
      BigDecimal minOrderPrice,
      BigDecimal deliveryFee,
      Integer deliveryMinutes,
      String description,
      String notice,
      BigDecimal rating,
      String logoBackground,
      String logoText,
      String status,
      Integer sortOrder
  ) {}

  public record ProductUpsertRequest(
      String id,
      String merchantId,
      String categoryId,
      String name,
      Integer sales,
      BigDecimal price,
      BigDecimal originalPrice,
      String imageText,
      String unit,
      String description,
      Integer stock,
      String status,
      Integer sortOrder
  ) {}

  public record ProductAdminResponse(
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
      String status,
      int sortOrder
  ) {}

  public record CategoryAdminResponse(
      String id,
      String name,
      String icon,
      String parentId,
      int level,
      String type,
      String status,
      int sortOrder
  ) {}

  public record CategoryUpsertRequest(
      String id,
      String name,
      String icon,
      String parentId,
      Integer level,
      String type,
      String status,
      Integer sortOrder
  ) {}

  public record BrandAdminResponse(
      String id,
      String name,
      String logo,
      String description,
      String status,
      int sortOrder
  ) {}

  public record BrandUpsertRequest(
      String id,
      String name,
      String logo,
      String description,
      String status,
      Integer sortOrder
  ) {}

  public record SpecOptionAdminResponse(
      String id,
      String groupId,
      String name,
      String status,
      int sortOrder
  ) {}

  public record SpecGroupAdminResponse(
      String id,
      String name,
      String status,
      int sortOrder,
      List<SpecOptionAdminResponse> options
  ) {}

  public record SpecGroupUpsertRequest(
      String id,
      String name,
      String status,
      Integer sortOrder
  ) {}

  public record SpecOptionUpsertRequest(
      String id,
      String name,
      String status,
      Integer sortOrder
  ) {}

  public record SkuSpecRequest(
      String groupId,
      String groupName,
      String optionId,
      String optionName
  ) {}

  public record ProductSkuAdminResponse(
      String skuId,
      String productId,
      String spuId,
      String skuCode,
      List<SkuSpecRequest> specs,
      String specText,
      BigDecimal price,
      BigDecimal originalPrice,
      String unit,
      int stock,
      String status
  ) {}

  public record ProductSkuUpsertRequest(
      String skuId,
      String skuCode,
      List<SkuSpecRequest> specs,
      BigDecimal price,
      BigDecimal originalPrice,
      String unit,
      Integer stock,
      String status
  ) {}

  public record ProductSpuAdminResponse(
      String id,
      String merchantId,
      String merchantName,
      String categoryId,
      String categoryName,
      String brandId,
      String brandName,
      String name,
      String subtitle,
      String mainImage,
      String detail,
      List<String> detailImages,
      String status,
      int sortOrder,
      int skuCount,
      int totalStock,
      BigDecimal minPrice,
      BigDecimal maxPrice,
      List<ProductSkuAdminResponse> skus
  ) {}

  public record ProductSpuUpsertRequest(
      String id,
      String merchantId,
      String categoryId,
      String brandId,
      String name,
      String subtitle,
      String mainImage,
      String detail,
      List<String> detailImages,
      String status,
      Integer sortOrder,
      List<ProductSkuUpsertRequest> skus
  ) {}

  public record StatusUpdateRequest(String status) {}

  public record OrderStatusRequest(String status, String reason) {}

  public record UserStatusRequest(String status) {}

  public record AuditLogResponse(
      String id,
      String actorId,
      String actorRole,
      String action,
      String targetType,
      String targetId,
      String detail,
      String requestId,
      String createdAt
  ) {}

  public record DashboardResponse(Map<String, Object> summary) {}

  public record AdminSnapshotResponse(
      Map<String, Object> dashboard,
      List<MerchantAdminResponse> merchants,
      List<ProductAdminResponse> products,
      List<OrderResponse> orders,
      List<UserRecord> users,
      List<AuditLogResponse> auditLogs
  ) {}
}
