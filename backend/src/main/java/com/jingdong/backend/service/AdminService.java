package com.jingdong.backend.service;

import com.jingdong.backend.api.ErrorCode;
import com.jingdong.backend.auth.UserContext;
import com.jingdong.backend.dto.admin.AdminDtos.AuditLogResponse;
import com.jingdong.backend.dto.admin.AdminDtos.BrandAdminResponse;
import com.jingdong.backend.dto.admin.AdminDtos.BrandUpsertRequest;
import com.jingdong.backend.dto.admin.AdminDtos.CategoryAdminResponse;
import com.jingdong.backend.dto.admin.AdminDtos.CategoryUpsertRequest;
import com.jingdong.backend.dto.admin.AdminDtos.MerchantAdminResponse;
import com.jingdong.backend.dto.admin.AdminDtos.MerchantUpsertRequest;
import com.jingdong.backend.dto.admin.AdminDtos.ProductAdminResponse;
import com.jingdong.backend.dto.admin.AdminDtos.ProductSkuAdminResponse;
import com.jingdong.backend.dto.admin.AdminDtos.ProductSkuUpsertRequest;
import com.jingdong.backend.dto.admin.AdminDtos.ProductSpuAdminResponse;
import com.jingdong.backend.dto.admin.AdminDtos.ProductSpuUpsertRequest;
import com.jingdong.backend.dto.admin.AdminDtos.ProductUpsertRequest;
import com.jingdong.backend.dto.admin.AdminDtos.SpecGroupAdminResponse;
import com.jingdong.backend.dto.admin.AdminDtos.SpecGroupUpsertRequest;
import com.jingdong.backend.dto.admin.AdminDtos.SpecOptionAdminResponse;
import com.jingdong.backend.dto.admin.AdminDtos.SpecOptionUpsertRequest;
import com.jingdong.backend.dto.inventory.InventoryDtos.InventoryAccountResponse;
import com.jingdong.backend.dto.inventory.InventoryDtos.InventoryAdjustRequest;
import com.jingdong.backend.dto.inventory.InventoryDtos.InventoryTransactionResponse;
import com.jingdong.backend.dto.order.OrderDtos.OrderResponse;
import com.jingdong.backend.entity.DataEntities.AuditLogEntity;
import com.jingdong.backend.entity.DataEntities.MerchantEntity;
import com.jingdong.backend.exception.BusinessException;
import com.jingdong.backend.store.DatabaseStore;
import com.jingdong.backend.store.DatabaseStore.UserRecord;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminService {
  private final DatabaseStore store;
  private final AuditLogService auditLogService;
  private final ProductCenterService productCenterService;
  private final InventoryService inventoryService;

  public AdminService(
      DatabaseStore store,
      AuditLogService auditLogService,
      ProductCenterService productCenterService,
      InventoryService inventoryService
  ) {
    this.store = store;
    this.auditLogService = auditLogService;
    this.productCenterService = productCenterService;
    this.inventoryService = inventoryService;
  }

  public Map<String, Object> dashboard() {
    return store.dashboardSummary();
  }

  public List<MerchantAdminResponse> merchants() {
    return store.adminMerchants().stream().map(this::toMerchantResponse).toList();
  }

  public MerchantAdminResponse saveMerchant(MerchantUpsertRequest request) {
    MerchantEntity merchant = new MerchantEntity();
    merchant.setId(request.id());
    merchant.setName(value(request.name(), "未命名商家"));
    merchant.setSales(value(request.sales(), 0));
    merchant.setMinOrderPrice(value(request.minOrderPrice(), BigDecimal.ZERO));
    merchant.setDeliveryFee(value(request.deliveryFee(), BigDecimal.ZERO));
    merchant.setDeliveryMinutes(value(request.deliveryMinutes(), 30));
    merchant.setTagsJson("[]");
    merchant.setDescription(value(request.description(), "后台新增商家"));
    merchant.setNotice(value(request.notice(), "欢迎选购"));
    merchant.setRating(value(request.rating(), new BigDecimal("5.0")));
    merchant.setLogoBackground(value(request.logoBackground(), "linear-gradient(180deg, #2f6fed 0%, #194ec8 100%)"));
    merchant.setLogoText(value(request.logoText(), merchant.getName().substring(0, Math.min(2, merchant.getName().length()))));
    merchant.setStatus(value(request.status(), "ACTIVE"));
    merchant.setSortOrder(value(request.sortOrder(), 100));
    MerchantAdminResponse response = toMerchantResponse(store.saveMerchant(merchant));
    auditLogService.record("ADMIN_SAVE_MERCHANT", "MERCHANT", response.id(), response.name());
    return response;
  }

  public List<ProductAdminResponse> products() {
    return store.adminProducts();
  }

  public ProductAdminResponse saveProduct(ProductUpsertRequest request) {
    return productCenterService.saveLegacyProduct(request);
  }

  public List<InventoryAccountResponse> inventoryAccounts(String keyword, Boolean lowStockOnly) {
    return inventoryService.accounts(keyword, lowStockOnly);
  }

  public List<InventoryTransactionResponse> inventoryTransactions(
      String skuId,
      String orderId,
      String bizType,
      Integer limit
  ) {
    return inventoryService.transactions(skuId, orderId, bizType, limit);
  }

  public InventoryAccountResponse adjustInventory(String skuId, InventoryAdjustRequest request) {
    InventoryAccountResponse response = inventoryService.adjustAvailable(
        skuId,
        request.delta() == null ? 0 : request.delta(),
        request.reason()
    );
    auditLogService.record("ADMIN_ADJUST_INVENTORY", "SKU", skuId, request.reason());
    return response;
  }

  public List<CategoryAdminResponse> categories() {
    return productCenterService.categories();
  }

  public CategoryAdminResponse saveCategory(CategoryUpsertRequest request) {
    return productCenterService.saveCategory(request);
  }

  public CategoryAdminResponse updateCategoryStatus(String categoryId, String status) {
    return productCenterService.updateCategoryStatus(categoryId, status);
  }

  public List<BrandAdminResponse> brands() {
    return productCenterService.brands();
  }

  public BrandAdminResponse saveBrand(BrandUpsertRequest request) {
    return productCenterService.saveBrand(request);
  }

  public BrandAdminResponse updateBrandStatus(String brandId, String status) {
    return productCenterService.updateBrandStatus(brandId, status);
  }

  public List<SpecGroupAdminResponse> specGroups() {
    return productCenterService.specGroups();
  }

  public SpecGroupAdminResponse saveSpecGroup(SpecGroupUpsertRequest request) {
    return productCenterService.saveSpecGroup(request);
  }

  public SpecOptionAdminResponse saveSpecOption(String groupId, SpecOptionUpsertRequest request) {
    return productCenterService.saveSpecOption(groupId, request);
  }

  public SpecOptionAdminResponse updateSpecOption(String optionId, SpecOptionUpsertRequest request) {
    return productCenterService.updateSpecOption(optionId, request);
  }

  public SpecOptionAdminResponse updateSpecOptionStatus(String optionId, String status) {
    return productCenterService.updateSpecOptionStatus(optionId, status);
  }

  public List<ProductSpuAdminResponse> productSpus() {
    return productCenterService.productSpus();
  }

  public ProductSpuAdminResponse productSpuDetail(String spuId) {
    return productCenterService.productSpuDetail(spuId);
  }

  public ProductSpuAdminResponse saveProductSpu(ProductSpuUpsertRequest request) {
    return productCenterService.saveProductSpu(request);
  }

  public ProductSpuAdminResponse updateProductSpuStatus(String spuId, String status) {
    return productCenterService.updateProductSpuStatus(spuId, status);
  }

  public ProductSkuAdminResponse saveProductSku(String spuId, ProductSkuUpsertRequest request) {
    return productCenterService.saveProductSku(spuId, request);
  }

  public ProductSkuAdminResponse updateProductSkuStatus(String spuId, String skuId, String status) {
    return productCenterService.updateProductSkuStatus(spuId, skuId, status);
  }

  public List<OrderResponse> orders() {
    return store.adminOrders();
  }

  public OrderResponse updateOrderStatus(String orderId, String status, String reason) {
    OrderResponse order = store.adminUpdateOrderStatus(orderId, status, reason);
    auditLogService.record("ADMIN_UPDATE_ORDER_STATUS", "ORDER", orderId, status);
    return order;
  }

  public List<UserRecord> users() {
    return store.adminUsers();
  }

  public UserRecord updateUserStatus(String userId, String status) {
    if ("DISABLED".equals(status) && userId.equals(UserContext.userId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    UserRecord user = store.updateUserStatus(userId, status);
    auditLogService.record("ADMIN_UPDATE_USER_STATUS", "USER", userId, status);
    return user;
  }

  public List<AuditLogResponse> auditLogs(String actorId, String action, String from, String to) {
    return store.auditLogs(actorId, action, auditTime(from), auditTime(to)).stream()
        .map(this::toAuditLogResponse)
        .toList();
  }

  private MerchantAdminResponse toMerchantResponse(MerchantEntity merchant) {
    return new MerchantAdminResponse(
        merchant.getId(),
        merchant.getName(),
        merchant.getSales(),
        merchant.getMinOrderPrice(),
        merchant.getDeliveryFee(),
        merchant.getDeliveryMinutes(),
        merchant.getDescription(),
        merchant.getNotice(),
        merchant.getRating(),
        merchant.getLogoBackground(),
        merchant.getLogoText(),
        merchant.getStatus(),
        merchant.getSortOrder()
    );
  }

  private AuditLogResponse toAuditLogResponse(AuditLogEntity log) {
    return new AuditLogResponse(
        log.getId(),
        log.getActorId(),
        log.getActorRole(),
        log.getAction(),
        log.getTargetType(),
        log.getTargetId(),
        log.getDetail(),
        log.getRequestId(),
        log.getCreatedAt() == null ? null : log.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toString()
    );
  }

  private <T> T value(T value, T fallback) {
    return value == null ? fallback : value;
  }

  private LocalDateTime auditTime(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
  }
}
