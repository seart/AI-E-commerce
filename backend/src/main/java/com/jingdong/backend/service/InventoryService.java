package com.jingdong.backend.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jingdong.backend.api.ErrorCode;
import com.jingdong.backend.dto.inventory.InventoryDtos.InventoryAccountResponse;
import com.jingdong.backend.dto.inventory.InventoryDtos.InventoryTransactionResponse;
import com.jingdong.backend.entity.DataEntities.InventoryAccountEntity;
import com.jingdong.backend.entity.DataEntities.InventoryTransactionEntity;
import com.jingdong.backend.entity.DataEntities.MerchantEntity;
import com.jingdong.backend.entity.DataEntities.OrderItemEntity;
import com.jingdong.backend.entity.DataEntities.ProductEntity;
import com.jingdong.backend.exception.BusinessException;
import com.jingdong.backend.mapper.InventoryAccountMapper;
import com.jingdong.backend.mapper.InventoryTransactionMapper;
import com.jingdong.backend.mapper.MerchantMapper;
import com.jingdong.backend.mapper.OrderItemMapper;
import com.jingdong.backend.mapper.ProductMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
  private static final int LOW_STOCK_THRESHOLD = 5;

  private final InventoryAccountMapper inventoryAccountMapper;
  private final InventoryTransactionMapper inventoryTransactionMapper;
  private final ProductMapper productMapper;
  private final MerchantMapper merchantMapper;
  private final OrderItemMapper orderItemMapper;

  public InventoryService(
      InventoryAccountMapper inventoryAccountMapper,
      InventoryTransactionMapper inventoryTransactionMapper,
      ProductMapper productMapper,
      MerchantMapper merchantMapper,
      OrderItemMapper orderItemMapper
  ) {
    this.inventoryAccountMapper = inventoryAccountMapper;
    this.inventoryTransactionMapper = inventoryTransactionMapper;
    this.productMapper = productMapper;
    this.merchantMapper = merchantMapper;
    this.orderItemMapper = orderItemMapper;
  }

  public List<InventoryAccountResponse> accounts(String keyword, Boolean lowStockOnly) {
    ensureAllAccounts();
    String normalized = normalize(keyword);
    boolean lowOnly = Boolean.TRUE.equals(lowStockOnly);
    return inventoryAccountMapper.selectList(Wrappers.<InventoryAccountEntity>lambdaQuery()
            .orderByAsc(InventoryAccountEntity::getAvailableQuantity)
            .orderByAsc(InventoryAccountEntity::getSkuId))
        .stream()
        .map(this::toAccountResponse)
        .filter(item -> normalized.isBlank() || accountMatches(item, normalized))
        .filter(item -> !lowOnly || item.availableQuantity() <= LOW_STOCK_THRESHOLD)
        .toList();
  }

  public List<InventoryTransactionResponse> transactions(
      String skuId,
      String orderId,
      String bizType,
      Integer limit
  ) {
    int safeLimit = limit == null ? 100 : Math.min(Math.max(limit, 1), 200);
    return inventoryTransactionMapper.selectList(Wrappers.<InventoryTransactionEntity>lambdaQuery()
            .eq(skuId != null && !skuId.isBlank(), InventoryTransactionEntity::getSkuId, skuId)
            .eq(orderId != null && !orderId.isBlank(), InventoryTransactionEntity::getOrderId, orderId)
            .eq(bizType != null && !bizType.isBlank(), InventoryTransactionEntity::getBizType, bizType)
            .orderByDesc(InventoryTransactionEntity::getCreatedAt)
            .last("limit " + safeLimit))
        .stream()
        .map(this::toTransactionResponse)
        .toList();
  }

  @Transactional
  public void lockOrderStock(String orderId, String reason) {
    for (OrderItemEntity item : orderItems(orderId)) {
      applyLock(orderId, item, reason);
    }
  }

  @Transactional
  public void confirmOrderPaid(String orderId, String reason) {
    for (OrderItemEntity item : orderItems(orderId)) {
      applyConfirm(orderId, item, reason);
    }
  }

  @Transactional
  public void releaseOrderStock(String orderId, String reason) {
    for (OrderItemEntity item : orderItems(orderId)) {
      applyRelease(orderId, item, reason);
    }
  }

  @Transactional
  public void restockSoldStock(String orderId, String reason) {
    for (OrderItemEntity item : orderItems(orderId)) {
      applyRestock(orderId, item, reason);
    }
  }

  @Transactional
  public InventoryAccountResponse adjustAvailable(String skuId, int delta, String reason) {
    if (delta == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "库存调整数量不能为 0");
    }
    if (reason == null || reason.isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "库存调整原因不能为空");
    }
    InventoryAccountEntity before = ensureAccount(skuId);
    InventoryTransactionEntity transaction = beginTransaction(
        "ADMIN_ADJUST",
        uid("adjust"),
        delta > 0 ? "INCREASE" : "DECREASE",
        skuId,
        null,
        null,
        Math.abs(delta),
        reason,
        before
    );
    if (transaction == null) {
      return toAccountResponse(ensureAccount(skuId));
    }

    int quantity = Math.abs(delta);
    int updated = delta > 0
        ? increaseAvailable(skuId, quantity)
        : decreaseAvailable(skuId, quantity);
    if (updated <= 0) {
      throw new BusinessException(ErrorCode.PRODUCT_STOCK_LOW);
    }
    syncProductAvailableDelta(skuId, delta);
    InventoryAccountEntity after = ensureAccount(skuId);
    finishTransaction(transaction, after);
    return toAccountResponse(after);
  }

  @Transactional
  public void syncAvailableFromProductUpdate(String skuId, int availableQuantity, String reason) {
    if (availableQuantity < 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "库存不能小于 0");
    }
    InventoryAccountEntity before = ensureAccount(skuId);
    int current = value(before.getAvailableQuantity());
    int delta = availableQuantity - current;
    if (delta == 0) {
      return;
    }
    InventoryTransactionEntity transaction = beginTransaction(
        "PRODUCT_STOCK_SYNC",
        skuId + ":" + System.nanoTime(),
        "SYNC",
        skuId,
        null,
        null,
        Math.abs(delta),
        reason,
        before
    );
    inventoryAccountMapper.update(null, Wrappers.<InventoryAccountEntity>lambdaUpdate()
        .eq(InventoryAccountEntity::getSkuId, skuId)
        .set(InventoryAccountEntity::getAvailableQuantity, availableQuantity)
        .setSql("version = version + 1"));
    ProductEntity product = productMapper.selectById(skuId);
    if (product != null && value(product.getStock()) != availableQuantity) {
      product.setStock(availableQuantity);
      productMapper.updateById(product);
    }
    finishTransaction(transaction, ensureAccount(skuId));
  }

  private void applyLock(String orderId, OrderItemEntity item, String reason) {
    int quantity = positiveQuantity(item);
    InventoryAccountEntity before = ensureAccount(item.getProductId());
    InventoryTransactionEntity transaction = beginTransaction(
        "ORDER_LOCK",
        orderId,
        "LOCK",
        item.getProductId(),
        orderId,
        item.getId(),
        quantity,
        reason,
        before
    );
    if (transaction == null) {
      return;
    }
    int accountUpdated = inventoryAccountMapper.update(null, Wrappers.<InventoryAccountEntity>lambdaUpdate()
        .eq(InventoryAccountEntity::getSkuId, item.getProductId())
        .ge(InventoryAccountEntity::getAvailableQuantity, quantity)
        .setSql("available_quantity = available_quantity - " + quantity)
        .setSql("locked_quantity = locked_quantity + " + quantity)
        .setSql("version = version + 1"));
    int productUpdated = productMapper.update(null, Wrappers.<ProductEntity>lambdaUpdate()
        .eq(ProductEntity::getId, item.getProductId())
        .ge(ProductEntity::getStock, quantity)
        .setSql("stock = stock - " + quantity));
    if (accountUpdated <= 0 || productUpdated <= 0) {
      throw new BusinessException(ErrorCode.PRODUCT_STOCK_LOW);
    }
    finishTransaction(transaction, ensureAccount(item.getProductId()));
  }

  private void applyConfirm(String orderId, OrderItemEntity item, String reason) {
    int quantity = positiveQuantity(item);
    InventoryAccountEntity before = ensureAccount(item.getProductId());
    InventoryTransactionEntity transaction = beginTransaction(
        "PAYMENT_CONFIRM",
        orderId,
        "CONFIRM",
        item.getProductId(),
        orderId,
        item.getId(),
        quantity,
        reason,
        before
    );
    if (transaction == null) {
      return;
    }
    int updated = inventoryAccountMapper.update(null, Wrappers.<InventoryAccountEntity>lambdaUpdate()
        .eq(InventoryAccountEntity::getSkuId, item.getProductId())
        .ge(InventoryAccountEntity::getLockedQuantity, quantity)
        .setSql("locked_quantity = locked_quantity - " + quantity)
        .setSql("sold_quantity = sold_quantity + " + quantity)
        .setSql("version = version + 1"));
    if (updated <= 0) {
      if (hasTransaction("ORDER_LOCK", orderId, item.getProductId())) {
        throw new BusinessException(ErrorCode.PRODUCT_STOCK_LOW, "锁定库存不足");
      }
      inventoryAccountMapper.update(null, Wrappers.<InventoryAccountEntity>lambdaUpdate()
          .eq(InventoryAccountEntity::getSkuId, item.getProductId())
          .setSql("sold_quantity = sold_quantity + " + quantity)
          .setSql("version = version + 1"));
    }
    finishTransaction(transaction, ensureAccount(item.getProductId()));
  }

  private void applyRelease(String orderId, OrderItemEntity item, String reason) {
    int quantity = positiveQuantity(item);
    InventoryAccountEntity before = ensureAccount(item.getProductId());
    InventoryTransactionEntity transaction = beginTransaction(
        "ORDER_RELEASE",
        orderId,
        "RELEASE",
        item.getProductId(),
        orderId,
        item.getId(),
        quantity,
        reason,
        before
    );
    if (transaction == null) {
      return;
    }
    int updated = inventoryAccountMapper.update(null, Wrappers.<InventoryAccountEntity>lambdaUpdate()
        .eq(InventoryAccountEntity::getSkuId, item.getProductId())
        .ge(InventoryAccountEntity::getLockedQuantity, quantity)
        .setSql("locked_quantity = locked_quantity - " + quantity)
        .setSql("available_quantity = available_quantity + " + quantity)
        .setSql("version = version + 1"));
    if (updated <= 0) {
      if (hasTransaction("ORDER_LOCK", orderId, item.getProductId())) {
        throw new BusinessException(ErrorCode.PRODUCT_STOCK_LOW, "锁定库存不足");
      }
      increaseAvailable(item.getProductId(), quantity);
    }
    syncProductAvailableDelta(item.getProductId(), quantity);
    finishTransaction(transaction, ensureAccount(item.getProductId()));
  }

  private void applyRestock(String orderId, OrderItemEntity item, String reason) {
    int quantity = positiveQuantity(item);
    InventoryAccountEntity before = ensureAccount(item.getProductId());
    InventoryTransactionEntity transaction = beginTransaction(
        "ORDER_RESTOCK",
        orderId,
        "RESTOCK",
        item.getProductId(),
        orderId,
        item.getId(),
        quantity,
        reason,
        before
    );
    if (transaction == null) {
      return;
    }
    int updated = inventoryAccountMapper.update(null, Wrappers.<InventoryAccountEntity>lambdaUpdate()
        .eq(InventoryAccountEntity::getSkuId, item.getProductId())
        .ge(InventoryAccountEntity::getSoldQuantity, quantity)
        .setSql("sold_quantity = sold_quantity - " + quantity)
        .setSql("available_quantity = available_quantity + " + quantity)
        .setSql("version = version + 1"));
    if (updated <= 0) {
      if (hasTransaction("PAYMENT_CONFIRM", orderId, item.getProductId())) {
        throw new BusinessException(ErrorCode.PRODUCT_STOCK_LOW, "已售库存不足");
      }
      increaseAvailable(item.getProductId(), quantity);
    }
    syncProductAvailableDelta(item.getProductId(), quantity);
    finishTransaction(transaction, ensureAccount(item.getProductId()));
  }

  private InventoryAccountEntity ensureAccount(String skuId) {
    if (skuId == null || skuId.isBlank()) {
      throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
    }
    InventoryAccountEntity account = inventoryAccountMapper.selectById(skuId);
    if (account != null) {
      return account;
    }
    ProductEntity product = productMapper.selectById(skuId);
    if (product == null) {
      throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
    }
    InventoryAccountEntity next = new InventoryAccountEntity();
    next.setSkuId(skuId);
    next.setAvailableQuantity(Math.max(value(product.getStock()), 0));
    next.setLockedQuantity(0);
    next.setSoldQuantity(0);
    next.setVersion(0);
    try {
      inventoryAccountMapper.insert(next);
    } catch (DuplicateKeyException ignored) {
      // Another request created the account first; use the database row below.
    }
    return inventoryAccountMapper.selectById(skuId);
  }

  private void ensureAllAccounts() {
    productMapper.selectList(null).forEach(product -> ensureAccount(product.getId()));
  }

  private InventoryTransactionEntity beginTransaction(
      String bizType,
      String bizId,
      String direction,
      String skuId,
      String orderId,
      String orderItemId,
      int quantity,
      String reason,
      InventoryAccountEntity before
  ) {
    InventoryTransactionEntity transaction = new InventoryTransactionEntity();
    transaction.setId(uid("invtx"));
    transaction.setSkuId(skuId);
    transaction.setOrderId(orderId);
    transaction.setOrderItemId(orderItemId);
    transaction.setBizType(bizType);
    transaction.setBizId(bizId);
    transaction.setDirection(direction);
    transaction.setQuantity(quantity);
    transaction.setBeforeAvailable(value(before.getAvailableQuantity()));
    transaction.setAfterAvailable(value(before.getAvailableQuantity()));
    transaction.setBeforeLocked(value(before.getLockedQuantity()));
    transaction.setAfterLocked(value(before.getLockedQuantity()));
    transaction.setBeforeSold(value(before.getSoldQuantity()));
    transaction.setAfterSold(value(before.getSoldQuantity()));
    transaction.setReason(reason == null ? "" : reason);
    transaction.setRequestId(MDC.get("requestId"));
    transaction.setCreatedAt(LocalDateTime.now());
    try {
      inventoryTransactionMapper.insert(transaction);
      return transaction;
    } catch (DuplicateKeyException ignored) {
      return null;
    }
  }

  private void finishTransaction(InventoryTransactionEntity transaction, InventoryAccountEntity after) {
    if (transaction == null) {
      return;
    }
    transaction.setAfterAvailable(value(after.getAvailableQuantity()));
    transaction.setAfterLocked(value(after.getLockedQuantity()));
    transaction.setAfterSold(value(after.getSoldQuantity()));
    inventoryTransactionMapper.updateById(transaction);
  }

  private int increaseAvailable(String skuId, int quantity) {
    return inventoryAccountMapper.update(null, Wrappers.<InventoryAccountEntity>lambdaUpdate()
        .eq(InventoryAccountEntity::getSkuId, skuId)
        .setSql("available_quantity = available_quantity + " + quantity)
        .setSql("version = version + 1"));
  }

  private int decreaseAvailable(String skuId, int quantity) {
    return inventoryAccountMapper.update(null, Wrappers.<InventoryAccountEntity>lambdaUpdate()
        .eq(InventoryAccountEntity::getSkuId, skuId)
        .ge(InventoryAccountEntity::getAvailableQuantity, quantity)
        .setSql("available_quantity = available_quantity - " + quantity)
        .setSql("version = version + 1"));
  }

  private void syncProductAvailableDelta(String skuId, int delta) {
    if (delta == 0) {
      return;
    }
    ProductEntity product = productMapper.selectById(skuId);
    if (product == null) {
      throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
    }
    product.setStock(value(product.getStock()) + delta);
    productMapper.updateById(product);
  }

  private boolean hasTransaction(String bizType, String bizId, String skuId) {
    Long count = inventoryTransactionMapper.selectCount(Wrappers.<InventoryTransactionEntity>lambdaQuery()
        .eq(InventoryTransactionEntity::getBizType, bizType)
        .eq(InventoryTransactionEntity::getBizId, bizId)
        .eq(InventoryTransactionEntity::getSkuId, skuId));
    return count != null && count > 0;
  }

  private List<OrderItemEntity> orderItems(String orderId) {
    return orderItemMapper.selectList(Wrappers.<OrderItemEntity>lambdaQuery()
        .eq(OrderItemEntity::getOrderId, orderId));
  }

  private InventoryAccountResponse toAccountResponse(InventoryAccountEntity account) {
    ProductEntity product = productMapper.selectById(account.getSkuId());
    MerchantEntity merchant = product == null ? null : merchantMapper.selectById(product.getMerchantId());
    int available = value(account.getAvailableQuantity());
    int locked = value(account.getLockedQuantity());
    int sold = value(account.getSoldQuantity());
    return new InventoryAccountResponse(
        account.getSkuId(),
        product == null ? null : product.getSpuId(),
        product == null ? "" : product.getSkuCode(),
        product == null ? "" : product.getName(),
        product == null ? "" : product.getMerchantId(),
        merchant == null ? "" : merchant.getName(),
        product == null ? "" : product.getCategoryId(),
        product == null ? "" : product.getStatus(),
        available,
        locked,
        sold,
        available + locked,
        toInstantString(account.getUpdatedAt())
    );
  }

  private InventoryTransactionResponse toTransactionResponse(InventoryTransactionEntity transaction) {
    ProductEntity product = productMapper.selectById(transaction.getSkuId());
    return new InventoryTransactionResponse(
        transaction.getId(),
        transaction.getSkuId(),
        product == null ? "" : product.getName(),
        transaction.getOrderId(),
        transaction.getOrderItemId(),
        transaction.getBizType(),
        transaction.getBizId(),
        transaction.getDirection(),
        value(transaction.getQuantity()),
        value(transaction.getBeforeAvailable()),
        value(transaction.getAfterAvailable()),
        value(transaction.getBeforeLocked()),
        value(transaction.getAfterLocked()),
        value(transaction.getBeforeSold()),
        value(transaction.getAfterSold()),
        transaction.getReason(),
        transaction.getRequestId(),
        toInstantString(transaction.getCreatedAt())
    );
  }

  private boolean accountMatches(InventoryAccountResponse account, String keyword) {
    return (account.skuId()
        + " " + nullToBlank(account.spuId())
        + " " + account.skuCode()
        + " " + account.productName()
        + " " + account.merchantName())
        .toLowerCase(Locale.ROOT)
        .contains(keyword);
  }

  private int positiveQuantity(OrderItemEntity item) {
    int quantity = value(item.getQuantity());
    if (quantity <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "库存变更数量必须大于 0");
    }
    return quantity;
  }

  private int value(Integer value) {
    return value == null ? 0 : value;
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }

  private String nullToBlank(String value) {
    return value == null ? "" : value;
  }

  private String toInstantString(LocalDateTime dateTime) {
    return dateTime == null ? null : dateTime.atZone(ZoneId.systemDefault()).toInstant().toString();
  }

  private String uid(String prefix) {
    return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
  }
}
