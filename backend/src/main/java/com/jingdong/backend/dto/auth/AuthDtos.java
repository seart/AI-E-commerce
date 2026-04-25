package com.jingdong.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
  private AuthDtos() {}

  public record LoginRequest(
      @NotBlank(message = "手机号不能为空")
      @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
      String mobile,

      @NotBlank(message = "密码不能为空")
      String password
  ) {}

  public record RegisterRequest(
      @NotBlank(message = "手机号不能为空")
      @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
      String mobile,

      @NotBlank(message = "密码不能为空")
      @Size(min = 6, message = "密码长度至少 6 位")
      String password,

      @NotBlank(message = "确认密码不能为空")
      String confirmPassword
  ) {}

  public record AuthUserResponse(
      String id,
      String mobile,
      String nickname,
      String memberLevel,
      String role,
      String status
  ) {}

  public record UserSessionResponse(
      String accessToken,
      String refreshToken,
      String expiresAt,
      AuthUserResponse user
  ) {}

  public record SuccessResponse(boolean success) {}
}
