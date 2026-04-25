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
