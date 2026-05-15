package com.jingdong.backend.controller;

import com.jingdong.backend.api.ApiResponse;
import com.jingdong.backend.auth.UserContext;
import com.jingdong.backend.dto.order.OrderDtos.CreateOrderRequest;
import com.jingdong.backend.dto.order.OrderDtos.OrderActionRequest;
import com.jingdong.backend.dto.order.OrderDtos.OrderResponse;
import com.jingdong.backend.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {
  // 用户端订单接口：创建、查询、取消和申请退款。
  private final OrderService orderService;

  public OrderController(OrderService orderService) {
    this.orderService = orderService;
  }

  @GetMapping
  public ApiResponse<List<OrderResponse>> orders() {
    return ApiResponse.success(orderService.getOrders(UserContext.userId()));
  }

  @PostMapping
  public ApiResponse<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
    return ApiResponse.success(orderService.createOrder(UserContext.userId(), request));
  }

  @PostMapping("/{orderId}/cancel")
  public ApiResponse<OrderResponse> cancel(
      @PathVariable String orderId,
      @RequestBody(required = false) OrderActionRequest request
  ) {
    String reason = request == null ? "用户取消订单" : request.reason();
    return ApiResponse.success(orderService.cancelOrder(UserContext.userId(), orderId, reason));
  }

  @PostMapping("/{orderId}/refund")
  public ApiResponse<OrderResponse> refund(
      @PathVariable String orderId,
      @RequestBody(required = false) OrderActionRequest request
  ) {
    String reason = request == null ? "用户申请退款" : request.reason();
    return ApiResponse.success(orderService.requestRefund(UserContext.userId(), orderId, reason));
  }
}
