package com.jingdong.backend.dto.inventory;

public final class InventoryDtos {
  private InventoryDtos() {}

  public record InventoryAccountResponse(
      String skuId,
      String spuId,
      String skuCode,
      String productName,
      String merchantId,
      String merchantName,
      String categoryId,
      String status,
      int availableQuantity,
      int lockedQuantity,
      int soldQuantity,
      int totalQuantity,
      String updatedAt
  ) {}

  public record InventoryTransactionResponse(
      String id,
      String skuId,
      String productName,
      String orderId,
      String orderItemId,
      String bizType,
      String bizId,
      String direction,
      int quantity,
      int beforeAvailable,
      int afterAvailable,
      int beforeLocked,
      int afterLocked,
      int beforeSold,
      int afterSold,
      String reason,
      String requestId,
      String createdAt
  ) {}

  public record InventoryAdjustRequest(
      Integer delta,
      String reason
  ) {}
}
