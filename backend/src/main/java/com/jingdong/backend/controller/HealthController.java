package com.jingdong.backend.controller;

import com.jingdong.backend.api.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
  @GetMapping("/health")
  public ApiResponse<Map<String, String>> health() {
    return ApiResponse.success(Map.of("status", "UP"));
  }
}
