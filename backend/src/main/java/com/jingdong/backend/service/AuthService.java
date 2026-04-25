package com.jingdong.backend.service;

import com.jingdong.backend.api.ErrorCode;
import com.jingdong.backend.auth.JwtTokenService;
import com.jingdong.backend.auth.PasswordHasher;
import com.jingdong.backend.auth.TokenBlacklistService;
import com.jingdong.backend.dto.auth.AuthDtos.AuthUserResponse;
import com.jingdong.backend.dto.auth.AuthDtos.LoginRequest;
import com.jingdong.backend.dto.auth.AuthDtos.RegisterRequest;
import com.jingdong.backend.dto.auth.AuthDtos.SuccessResponse;
import com.jingdong.backend.dto.auth.AuthDtos.UserSessionResponse;
import com.jingdong.backend.exception.BusinessException;
import com.jingdong.backend.store.DatabaseStore;
import com.jingdong.backend.store.DatabaseStore.UserRecord;
import java.time.Duration;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  private final DatabaseStore store;
  private final JwtTokenService jwtTokenService;
  private final TokenBlacklistService tokenBlacklistService;
  private final PasswordHasher passwordHasher;
  private final RateLimiterService rateLimiterService;
  private final AuditLogService auditLogService;

  public AuthService(
      DatabaseStore store,
      JwtTokenService jwtTokenService,
      TokenBlacklistService tokenBlacklistService,
      PasswordHasher passwordHasher,
      RateLimiterService rateLimiterService,
      AuditLogService auditLogService
  ) {
    this.store = store;
    this.jwtTokenService = jwtTokenService;
    this.tokenBlacklistService = tokenBlacklistService;
    this.passwordHasher = passwordHasher;
    this.rateLimiterService = rateLimiterService;
    this.auditLogService = auditLogService;
  }

  public UserSessionResponse login(LoginRequest request) {
    rateLimiterService.check("login:" + request.mobile(), 10, Duration.ofMinutes(1));
    UserRecord user = store.findUserByMobile(request.mobile())
        .filter(record -> passwordHasher.matches(request.password(), record.password()))
        .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));
    if (!"ACTIVE".equals(user.status())) {
      throw new BusinessException(ErrorCode.USER_DISABLED);
    }
    if (!passwordHasher.isHashed(user.password())) {
      store.updatePassword(user.id(), passwordHasher.hash(request.password()));
    }
    store.recordLogin(user.id());
    auditLogService.record("AUTH_LOGIN", "USER", user.id(), "用户登录");
    return session(user);
  }

  public SuccessResponse register(RegisterRequest request) {
    if (!request.password().equals(request.confirmPassword())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "两次输入的密码不一致");
    }
    UserRecord user = store.createUser(request.mobile(), passwordHasher.hash(request.password()));
    auditLogService.record("AUTH_REGISTER", "USER", user.id(), "用户注册");
    return new SuccessResponse(true);
  }

  public SuccessResponse logout(String authorization) {
    String token = bearerToken(authorization);
    if (token != null) {
      tokenBlacklistService.blacklist(token, jwtTokenService.expiresAt(token));
    }
    auditLogService.record("AUTH_LOGOUT", "USER", null, "用户退出登录");
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
        new AuthUserResponse(
            user.id(),
            user.mobile(),
            user.nickname(),
            user.memberLevel(),
            user.role(),
            user.status()
        )
    );
  }
}
