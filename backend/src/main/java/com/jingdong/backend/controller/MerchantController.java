package com.jingdong.backend.controller;

import com.jingdong.backend.api.ApiResponse;
import com.jingdong.backend.dto.merchant.MerchantDtos.MerchantDetailResponse;
import com.jingdong.backend.dto.merchant.MerchantDtos.MerchantResponse;
import com.jingdong.backend.service.CatalogService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/merchants")
public class MerchantController {
  private final CatalogService catalogService;

  public MerchantController(CatalogService catalogService) {
    this.catalogService = catalogService;
  }

  @GetMapping("/search")
  public ApiResponse<List<MerchantResponse>> search(
      @RequestParam(name = "keyword", required = false, defaultValue = "") String keyword
  ) {
    return ApiResponse.success(catalogService.searchMerchants(keyword));
  }

  @GetMapping("/{merchantId}")
  public ApiResponse<MerchantDetailResponse> detail(@PathVariable String merchantId) {
    return ApiResponse.success(catalogService.merchantDetail(merchantId));
  }
}
