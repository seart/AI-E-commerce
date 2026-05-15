package com.jingdong.backend.controller;

import com.jingdong.backend.api.ApiResponse;
import com.jingdong.backend.dto.product.ProductDtos.ProductCardResponse;
import com.jingdong.backend.dto.product.ProductDtos.ProductDetailResponse;
import com.jingdong.backend.service.ProductCenterService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class ProductController {
  // 用户端商品搜索和商品详情接口。
  private final ProductCenterService productCenterService;

  public ProductController(ProductCenterService productCenterService) {
    this.productCenterService = productCenterService;
  }

  @GetMapping("/search")
  public ApiResponse<List<ProductCardResponse>> search(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String merchantId,
      @RequestParam(required = false) String categoryId,
      @RequestParam(required = false) String brandId
  ) {
    return ApiResponse.success(productCenterService.searchProducts(keyword, merchantId, categoryId, brandId));
  }

  @GetMapping("/{productId}")
  public ApiResponse<ProductDetailResponse> detail(@PathVariable String productId) {
    return ApiResponse.success(productCenterService.productDetail(productId));
  }
}
