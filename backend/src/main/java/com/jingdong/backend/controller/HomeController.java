package com.jingdong.backend.controller;

import com.jingdong.backend.api.ApiResponse;
import com.jingdong.backend.dto.home.HomeDtos.HomeResponse;
import com.jingdong.backend.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {
  // 用户端首页聚合接口。
  private final CatalogService catalogService;

  public HomeController(CatalogService catalogService) {
    this.catalogService = catalogService;
  }

  @GetMapping("/home")
  public ApiResponse<HomeResponse> home() {
    return ApiResponse.success(catalogService.home());
  }
}
