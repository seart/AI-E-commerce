package com.jingdong.backend.service;

import com.jingdong.backend.api.ErrorCode;
import com.jingdong.backend.auth.JwtTokenService;
import com.jingdong.backend.auth.TokenBlacklistService;
import com.jingdong.backend.dto.auth.AuthDtos.AuthUserResponse;
import com.jingdong.backend.dto.auth.AuthDtos.LoginRequest;
import com.jingdong.backend.dto.auth.AuthDtos.RegisterRequest;
import com.jingdong.backend.dto.auth.AuthDtos.SuccessResponse;
import com.jingdong.backend.dto.auth.AuthDtos.UserSessionResponse;
import com.jingdong.backend.exception.BusinessException;
import com.jingdong.backend.store.DatabaseStore;
import com.jingdong.backend.store.DatabaseStore.UserRecord;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  private final DatabaseStore store;
  private final JwtTokenService jwtTokenService;
  private final TokenBlacklistService tokenBlacklistService;

  public AuthService(
      DatabaseStore store,
      JwtTokenService jwtTokenService,
      TokenBlacklistService tokenBlacklistService
  ) {
    this.store = store;
    this.jwtTokenService = jwtTokenService;
    this.tokenBlacklistService = tokenBlacklistService;
  }

  public UserSessionResponse login(LoginRequest request) {
    UserRecord user = store.findUserByMobile(request.mobile())
        .filter(record -> record.password().equals(request.password()))
        .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));
    return session(user);
  }

  public SuccessResponse register(RegisterRequest request) {
    if (!request.password().equals(request.confirmPassword())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "两次输入的密码不一致");
    }
    store.createUser(request.mobile(), request.password());
    return new SuccessResponse(true);
  }

  public SuccessResponse logout(String authorization) {
    String token = bearerToken(authorization);
    if (token != null) {
      tokenBlacklistService.blacklist(token, jwtTokenService.expiresAt(token));
    }
    return new SuccessResponse(true);
  }

  private String bearerToken(String authorization) {
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      return null;
    }
    String token = authorization.substring("Bearer ".length()).trim();
    return token.isBlank() ? null : token;
  }

  private UserSessionResponse session(UserRecord user) {
    return new UserSessionResponse(
        jwtTokenService.createAccessToken(user.id()),
        jwtTokenService.createRefreshToken(user.id()),
        jwtTokenService.accessTokenExpiresAt().toString(),
        new AuthUserResponse(user.id(), user.mobile(), user.nickname(), user.memberLevel())
    );
  }
}
