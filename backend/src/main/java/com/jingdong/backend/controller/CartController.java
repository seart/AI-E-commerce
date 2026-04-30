package com.jingdong.backend.controller;

import com.jingdong.backend.api.ApiResponse;
import com.jingdong.backend.auth.UserContext;
import com.jingdong.backend.dto.cart.CartDtos.AddCartItemRequest;
import com.jingdong.backend.dto.cart.CartDtos.CartItemResponse;
import com.jingdong.backend.dto.cart.CartDtos.CheckedRequest;
import com.jingdong.backend.dto.cart.CartDtos.UpdateCartQuantityRequest;
import com.jingdong.backend.service.CartService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cart")
public class CartController {
  private final CartService cartService;

  public CartController(CartService cartService) {
    this.cartService = cartService;
  }

  @GetMapping("/items")
  public ApiResponse<List<CartItemResponse>> items() {
    return ApiResponse.success(cartService.getCartItems(UserContext.userId()));
  }

  @PostMapping("/items")
  public ApiResponse<List<CartItemResponse>> addItem(
      @Valid @RequestBody AddCartItemRequest request
  ) {
    return ApiResponse.success(cartService.addCartItem(UserContext.userId(), request.purchasableId()));
  }

  @PatchMapping("/items/{productId}")
  public ApiResponse<List<CartItemResponse>> updateQuantity(
      @PathVariable String productId,
      @Valid @RequestBody UpdateCartQuantityRequest request
  ) {
    return ApiResponse.success(cartService.updateCartQuantity(
        UserContext.userId(),
        productId,
        request.quantity()
    ));
  }

  @PatchMapping("/items/{productId}/checked")
  public ApiResponse<List<CartItemResponse>> setItemChecked(
      @PathVariable String productId,
      @Valid @RequestBody CheckedRequest request
  ) {
    return ApiResponse.success(cartService.setItemChecked(
        UserContext.userId(),
        productId,
        request.checked()
    ));
  }

  @PatchMapping("/merchants/{merchantId}/checked")
  public ApiResponse<List<CartItemResponse>> setMerchantChecked(
      @PathVariable String merchantId,
      @Valid @RequestBody CheckedRequest request
  ) {
    return ApiResponse.success(cartService.setMerchantChecked(
        UserContext.userId(),
        merchantId,
        request.checked()
    ));
  }

  @PatchMapping("/checked")
  public ApiResponse<List<CartItemResponse>> setAllChecked(
      @Valid @RequestBody CheckedRequest request
  ) {
    return ApiResponse.success(cartService.setAllChecked(UserContext.userId(), request.checked()));
  }

  @DeleteMapping("/checked")
  public ApiResponse<List<CartItemResponse>> clearChecked() {
    return ApiResponse.success(cartService.clearChecked(UserContext.userId()));
  }
}
