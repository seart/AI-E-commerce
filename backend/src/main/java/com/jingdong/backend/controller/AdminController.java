package com.jingdong.backend.controller;

import com.jingdong.backend.api.ApiResponse;
import com.jingdong.backend.dto.admin.AdminDtos.AuditLogResponse;
import com.jingdong.backend.dto.admin.AdminDtos.BrandAdminResponse;
import com.jingdong.backend.dto.admin.AdminDtos.BrandUpsertRequest;
import com.jingdong.backend.dto.admin.AdminDtos.CategoryAdminResponse;
import com.jingdong.backend.dto.admin.AdminDtos.CategoryUpsertRequest;
import com.jingdong.backend.dto.admin.AdminDtos.DashboardResponse;
import com.jingdong.backend.dto.admin.AdminDtos.MerchantAdminResponse;
import com.jingdong.backend.dto.admin.AdminDtos.MerchantUpsertRequest;
import com.jingdong.backend.dto.admin.AdminDtos.OrderStatusRequest;
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
import com.jingdong.backend.dto.admin.AdminDtos.StatusUpdateRequest;
import com.jingdong.backend.dto.admin.AdminDtos.UserStatusRequest;
import com.jingdong.backend.dto.inventory.InventoryDtos.InventoryAccountResponse;
import com.jingdong.backend.dto.inventory.InventoryDtos.InventoryAdjustRequest;
import com.jingdong.backend.dto.inventory.InventoryDtos.InventoryTransactionResponse;
import com.jingdong.backend.dto.order.OrderDtos.OrderResponse;
import com.jingdong.backend.service.AdminService;
import com.jingdong.backend.store.DatabaseStore.UserRecord;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {
  private final AdminService adminService;

  public AdminController(AdminService adminService) {
    this.adminService = adminService;
  }

  @GetMapping("/dashboard/summary")
  public ApiResponse<DashboardResponse> dashboard() {
    return ApiResponse.success(new DashboardResponse(adminService.dashboard()));
  }

  @GetMapping("/merchants")
  public ApiResponse<List<MerchantAdminResponse>> merchants() {
    return ApiResponse.success(adminService.merchants());
  }

  @PostMapping("/merchants")
  public ApiResponse<MerchantAdminResponse> createMerchant(@RequestBody MerchantUpsertRequest request) {
    return ApiResponse.success(adminService.saveMerchant(request));
  }

  @PutMapping("/merchants/{merchantId}")
  public ApiResponse<MerchantAdminResponse> updateMerchant(
      @PathVariable String merchantId,
      @RequestBody MerchantUpsertRequest request
  ) {
    return ApiResponse.success(adminService.saveMerchant(new MerchantUpsertRequest(
        merchantId,
        request.name(),
        request.sales(),
        request.minOrderPrice(),
        request.deliveryFee(),
        request.deliveryMinutes(),
        request.description(),
        request.notice(),
        request.rating(),
        request.logoBackground(),
        request.logoText(),
        request.status(),
        request.sortOrder()
    )));
  }

  @GetMapping("/products")
  public ApiResponse<List<ProductAdminResponse>> products() {
    return ApiResponse.success(adminService.products());
  }

  @PostMapping("/products")
  public ApiResponse<ProductAdminResponse> createProduct(@RequestBody ProductUpsertRequest request) {
    return ApiResponse.success(adminService.saveProduct(request));
  }

  @PutMapping("/products/{productId}")
  public ApiResponse<ProductAdminResponse> updateProduct(
      @PathVariable String productId,
      @RequestBody ProductUpsertRequest request
  ) {
    return ApiResponse.success(adminService.saveProduct(new ProductUpsertRequest(
        productId,
        request.merchantId(),
        request.categoryId(),
        request.name(),
        request.sales(),
        request.price(),
        request.originalPrice(),
        request.imageText(),
        request.unit(),
        request.description(),
        request.stock(),
        request.status(),
        request.sortOrder()
    )));
  }

  @GetMapping("/inventory/accounts")
  public ApiResponse<List<InventoryAccountResponse>> inventoryAccounts(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Boolean lowStockOnly
  ) {
    return ApiResponse.success(adminService.inventoryAccounts(keyword, lowStockOnly));
  }

  @GetMapping("/inventory/transactions")
  public ApiResponse<List<InventoryTransactionResponse>> inventoryTransactions(
      @RequestParam(required = false) String skuId,
      @RequestParam(required = false) String orderId,
      @RequestParam(required = false) String bizType,
      @RequestParam(required = false) Integer limit
  ) {
    return ApiResponse.success(adminService.inventoryTransactions(skuId, orderId, bizType, limit));
  }

  @PostMapping("/inventory/accounts/{skuId}/adjust")
  public ApiResponse<InventoryAccountResponse> adjustInventory(
      @PathVariable String skuId,
      @RequestBody InventoryAdjustRequest request
  ) {
    return ApiResponse.success(adminService.adjustInventory(skuId, request));
  }

  @GetMapping("/categories")
  public ApiResponse<List<CategoryAdminResponse>> categories() {
    return ApiResponse.success(adminService.categories());
  }

  @PostMapping("/categories")
  public ApiResponse<CategoryAdminResponse> createCategory(@RequestBody CategoryUpsertRequest request) {
    return ApiResponse.success(adminService.saveCategory(request));
  }

  @PutMapping("/categories/{categoryId}")
  public ApiResponse<CategoryAdminResponse> updateCategory(
      @PathVariable String categoryId,
      @RequestBody CategoryUpsertRequest request
  ) {
    return ApiResponse.success(adminService.saveCategory(new CategoryUpsertRequest(
        categoryId,
        request.name(),
        request.icon(),
        request.parentId(),
        request.level(),
        request.type(),
        request.status(),
        request.sortOrder()
    )));
  }

  @PatchMapping("/categories/{categoryId}/status")
  public ApiResponse<CategoryAdminResponse> updateCategoryStatus(
      @PathVariable String categoryId,
      @RequestBody StatusUpdateRequest request
  ) {
    return ApiResponse.success(adminService.updateCategoryStatus(categoryId, request.status()));
  }

  @GetMapping("/brands")
  public ApiResponse<List<BrandAdminResponse>> brands() {
    return ApiResponse.success(adminService.brands());
  }

  @PostMapping("/brands")
  public ApiResponse<BrandAdminResponse> createBrand(@RequestBody BrandUpsertRequest request) {
    return ApiResponse.success(adminService.saveBrand(request));
  }

  @PutMapping("/brands/{brandId}")
  public ApiResponse<BrandAdminResponse> updateBrand(
      @PathVariable String brandId,
      @RequestBody BrandUpsertRequest request
  ) {
    return ApiResponse.success(adminService.saveBrand(new BrandUpsertRequest(
        brandId,
        request.name(),
        request.logo(),
        request.description(),
        request.status(),
        request.sortOrder()
    )));
  }

  @PatchMapping("/brands/{brandId}/status")
  public ApiResponse<BrandAdminResponse> updateBrandStatus(
      @PathVariable String brandId,
      @RequestBody StatusUpdateRequest request
  ) {
    return ApiResponse.success(adminService.updateBrandStatus(brandId, request.status()));
  }

  @GetMapping("/spec-groups")
  public ApiResponse<List<SpecGroupAdminResponse>> specGroups() {
    return ApiResponse.success(adminService.specGroups());
  }

  @PostMapping("/spec-groups")
  public ApiResponse<SpecGroupAdminResponse> createSpecGroup(@RequestBody SpecGroupUpsertRequest request) {
    return ApiResponse.success(adminService.saveSpecGroup(request));
  }

  @PutMapping("/spec-groups/{groupId}")
  public ApiResponse<SpecGroupAdminResponse> updateSpecGroup(
      @PathVariable String groupId,
      @RequestBody SpecGroupUpsertRequest request
  ) {
    return ApiResponse.success(adminService.saveSpecGroup(new SpecGroupUpsertRequest(
        groupId,
        request.name(),
        request.status(),
        request.sortOrder()
    )));
  }

  @PostMapping("/spec-groups/{groupId}/options")
  public ApiResponse<SpecOptionAdminResponse> createSpecOption(
      @PathVariable String groupId,
      @RequestBody SpecOptionUpsertRequest request
  ) {
    return ApiResponse.success(adminService.saveSpecOption(groupId, request));
  }

  @PutMapping("/spec-options/{optionId}")
  public ApiResponse<SpecOptionAdminResponse> updateSpecOption(
      @PathVariable String optionId,
      @RequestBody SpecOptionUpsertRequest request
  ) {
    return ApiResponse.success(adminService.updateSpecOption(optionId, request));
  }

  @PatchMapping("/spec-options/{optionId}/status")
  public ApiResponse<SpecOptionAdminResponse> updateSpecOptionStatus(
      @PathVariable String optionId,
      @RequestBody StatusUpdateRequest request
  ) {
    return ApiResponse.success(adminService.updateSpecOptionStatus(optionId, request.status()));
  }

  @GetMapping("/product-spus")
  public ApiResponse<List<ProductSpuAdminResponse>> productSpus() {
    return ApiResponse.success(adminService.productSpus());
  }

  @GetMapping("/product-spus/{spuId}")
  public ApiResponse<ProductSpuAdminResponse> productSpuDetail(@PathVariable String spuId) {
    return ApiResponse.success(adminService.productSpuDetail(spuId));
  }

  @PostMapping("/product-spus")
  public ApiResponse<ProductSpuAdminResponse> createProductSpu(@RequestBody ProductSpuUpsertRequest request) {
    return ApiResponse.success(adminService.saveProductSpu(request));
  }

  @PutMapping("/product-spus/{spuId}")
  public ApiResponse<ProductSpuAdminResponse> updateProductSpu(
      @PathVariable String spuId,
      @RequestBody ProductSpuUpsertRequest request
  ) {
    return ApiResponse.success(adminService.saveProductSpu(new ProductSpuUpsertRequest(
        spuId,
        request.merchantId(),
        request.categoryId(),
        request.brandId(),
        request.name(),
        request.subtitle(),
        request.mainImage(),
        request.detail(),
        request.detailImages(),
        request.status(),
        request.sortOrder(),
        request.skus()
    )));
  }

  @PatchMapping("/product-spus/{spuId}/status")
  public ApiResponse<ProductSpuAdminResponse> updateProductSpuStatus(
      @PathVariable String spuId,
      @RequestBody StatusUpdateRequest request
  ) {
    return ApiResponse.success(adminService.updateProductSpuStatus(spuId, request.status()));
  }

  @PostMapping("/product-spus/{spuId}/skus")
  public ApiResponse<ProductSkuAdminResponse> createProductSku(
      @PathVariable String spuId,
      @RequestBody ProductSkuUpsertRequest request
  ) {
    return ApiResponse.success(adminService.saveProductSku(spuId, request));
  }

  @PutMapping("/product-spus/{spuId}/skus/{skuId}")
  public ApiResponse<ProductSkuAdminResponse> updateProductSku(
      @PathVariable String spuId,
      @PathVariable String skuId,
      @RequestBody ProductSkuUpsertRequest request
  ) {
    return ApiResponse.success(adminService.saveProductSku(spuId, new ProductSkuUpsertRequest(
        skuId,
        request.skuCode(),
        request.specs(),
        request.price(),
        request.originalPrice(),
        request.unit(),
        request.stock(),
        request.status()
    )));
  }

  @PatchMapping("/product-spus/{spuId}/skus/{skuId}/status")
  public ApiResponse<ProductSkuAdminResponse> updateProductSkuStatus(
      @PathVariable String spuId,
      @PathVariable String skuId,
      @RequestBody StatusUpdateRequest request
  ) {
    return ApiResponse.success(adminService.updateProductSkuStatus(spuId, skuId, request.status()));
  }

  @GetMapping("/orders")
  public ApiResponse<List<OrderResponse>> orders() {
    return ApiResponse.success(adminService.orders());
  }

  @PatchMapping("/orders/{orderId}/status")
  public ApiResponse<OrderResponse> updateOrderStatus(
      @PathVariable String orderId,
      @RequestBody OrderStatusRequest request
  ) {
    return ApiResponse.success(adminService.updateOrderStatus(orderId, request.status(), request.reason()));
  }

  @GetMapping("/users")
  public ApiResponse<List<UserRecord>> users() {
    return ApiResponse.success(adminService.users());
  }

  @PatchMapping("/users/{userId}/status")
  public ApiResponse<UserRecord> updateUserStatus(
      @PathVariable String userId,
      @RequestBody UserStatusRequest request
  ) {
    return ApiResponse.success(adminService.updateUserStatus(userId, request.status()));
  }

  @GetMapping("/audit-logs")
  public ApiResponse<List<AuditLogResponse>> auditLogs(
      @RequestParam(required = false) String actorId,
      @RequestParam(required = false) String action,
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to
  ) {
    return ApiResponse.success(adminService.auditLogs(actorId, action, from, to));
  }
}
