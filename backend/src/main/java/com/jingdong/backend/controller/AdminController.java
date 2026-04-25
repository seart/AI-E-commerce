package com.jingdong.backend.controller;

import com.jingdong.backend.api.ApiResponse;
import com.jingdong.backend.dto.admin.AdminDtos.AuditLogResponse;
import com.jingdong.backend.dto.admin.AdminDtos.DashboardResponse;
import com.jingdong.backend.dto.admin.AdminDtos.MerchantAdminResponse;
import com.jingdong.backend.dto.admin.AdminDtos.MerchantUpsertRequest;
import com.jingdong.backend.dto.admin.AdminDtos.OrderStatusRequest;
import com.jingdong.backend.dto.admin.AdminDtos.ProductAdminResponse;
import com.jingdong.backend.dto.admin.AdminDtos.ProductUpsertRequest;
import com.jingdong.backend.dto.admin.AdminDtos.UserStatusRequest;
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
