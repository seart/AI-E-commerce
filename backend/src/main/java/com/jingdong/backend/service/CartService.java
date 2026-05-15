package com.jingdong.backend.service;

import com.jingdong.backend.dto.cart.CartDtos.CartItemResponse;
import com.jingdong.backend.store.DatabaseStore;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CartService {
  // 购物车业务层，保证所有购物车数据都按当前用户隔离。
  private final DatabaseStore store;

  public CartService(DatabaseStore store) {
    this.store = store;
  }

  public List<CartItemResponse> getCartItems(String userId) {
    return store.getCart(userId);
  }

  public List<CartItemResponse> addCartItem(String userId, String productId) {
    return store.addCartItem(userId, productId);
  }

  public List<CartItemResponse> updateCartQuantity(String userId, String productId, int quantity) {
    return store.updateCartQuantity(userId, productId, quantity);
  }

  public List<CartItemResponse> setItemChecked(String userId, String productId, boolean checked) {
    return store.setCartItemChecked(userId, productId, checked);
  }

  public List<CartItemResponse> setMerchantChecked(String userId, String merchantId, boolean checked) {
    return store.setMerchantChecked(userId, merchantId, checked);
  }

  public List<CartItemResponse> setAllChecked(String userId, boolean checked) {
    return store.setAllChecked(userId, checked);
  }

  public List<CartItemResponse> clearChecked(String userId) {
    return store.clearChecked(userId);
  }
}
