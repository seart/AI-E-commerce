package com.jingdong.backend.controller;

import com.jingdong.backend.api.ApiResponse;
import com.jingdong.backend.dto.home.HomeDtos.HomeResponse;
import com.jingdong.backend.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {
  private final CatalogService catalogService;

  public HomeController(CatalogService catalogService) {
    this.catalogService = catalogService;
  }

  @GetMapping("/home")
  public ApiResponse<HomeResponse> home() {
    return ApiResponse.success(catalogService.home());
  }
}
