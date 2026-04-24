package com.jingdong.backend.service;

import com.jingdong.backend.dto.order.OrderDtos.CreateOrderRequest;
import com.jingdong.backend.dto.order.OrderDtos.OrderResponse;
import com.jingdong.backend.store.DatabaseStore;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
  private final DatabaseStore store;

  public OrderService(DatabaseStore store) {
    this.store = store;
  }

  public List<OrderResponse> getOrders(String userId) {
    return store.getOrders(userId);
  }

  public OrderResponse createOrder(String userId, CreateOrderRequest request) {
    return store.createOrder(userId, request.addressId(), request.items());
  }
}
