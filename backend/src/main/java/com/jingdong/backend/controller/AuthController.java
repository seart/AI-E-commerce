package com.jingdong.backend.controller;

import com.jingdong.backend.api.ApiResponse;
import com.jingdong.backend.dto.auth.AuthDtos.LoginRequest;
import com.jingdong.backend.dto.auth.AuthDtos.RegisterRequest;
import com.jingdong.backend.dto.auth.AuthDtos.SuccessResponse;
import com.jingdong.backend.dto.auth.AuthDtos.UserSessionResponse;
import com.jingdong.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public ApiResponse<UserSessionResponse> login(@Valid @RequestBody LoginRequest request) {
    return ApiResponse.success(authService.login(request));
  }

  @PostMapping("/register")
  public ApiResponse<SuccessResponse> register(@Valid @RequestBody RegisterRequest request) {
    return ApiResponse.success(authService.register(request));
  }

  @PostMapping("/logout")
  public ApiResponse<SuccessResponse> logout(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
  ) {
    return ApiResponse.success(authService.logout(authorization));
  }
}
