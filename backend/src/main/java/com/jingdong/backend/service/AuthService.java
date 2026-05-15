package com.jingdong.backend.service;

import com.jingdong.backend.api.ErrorCode;
import com.jingdong.backend.auth.JwtTokenService;
import com.jingdong.backend.auth.JwtTokenService.TokenClaims;
import com.jingdong.backend.auth.PasswordHasher;
import com.jingdong.backend.auth.TokenBlacklistService;
import com.jingdong.backend.dto.auth.AuthDtos.AuthUserResponse;
import com.jingdong.backend.dto.auth.AuthDtos.LoginRequest;
import com.jingdong.backend.dto.auth.AuthDtos.RefreshTokenRequest;
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
    return login(request, "unknown");
  }

  public UserSessionResponse login(LoginRequest request, String clientIp) {
    // 同时按手机号和 IP 限流，降低撞库/暴力登录风险。
    rateLimiterService.check("login:" + request.mobile(), 10, Duration.ofMinutes(1));
    rateLimiterService.check("login-ip:" + clientIp, 50, Duration.ofMinutes(1));
    UserRecord user = store.findUserByMobile(request.mobile())
        .filter(record -> passwordHasher.matches(request.password(), record.password()))
        .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));
    if (!"ACTIVE".equals(user.status())) {
      throw new BusinessException(ErrorCode.USER_DISABLED);
    }
    // 兼容历史明文密码：登录成功后立刻升级为 PBKDF2 哈希。
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
      // 退出登录时同时拉黑完整 token 和 jti，访问令牌过期前也不能继续使用。
      TokenClaims claims = jwtTokenService.parseClaims(token);
      tokenBlacklistService.blacklist(token, claims.expiresAt());
      tokenBlacklistService.blacklistJti(claims.jti(), claims.expiresAt());
    }
    auditLogService.record("AUTH_LOGOUT", "USER", null, "用户退出登录");
    return new SuccessResponse(true);
  }

  public UserSessionResponse refresh(RefreshTokenRequest request) {
    // 刷新令牌也要查黑名单，避免 logout 后还能继续换新 access token。
    if (tokenBlacklistService.isBlacklisted(request.refreshToken())) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    TokenClaims claims = jwtTokenService.parseRefreshToken(request.refreshToken());
    if (tokenBlacklistService.isJtiBlacklisted(claims.jti())) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    UserRecord user = store.findUserById(claims.userId())
        .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    if (!"ACTIVE".equals(user.status())) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    auditLogService.record("AUTH_REFRESH", "USER", user.id(), "刷新访问令牌");
    return session(user);
  }

  private String bearerToken(String authorization) {
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      return null;
    }
    String token = authorization.substring("Bearer ".length()).trim();
    return token.isBlank() ? null : token;
  }

  private UserSessionResponse session(UserRecord user) {
    // 登录/刷新成功统一从这里生成前端需要的会话结构。
    return new UserSessionResponse(
        jwtTokenService.createAccessToken(user.id(), user.role()),
        jwtTokenService.createRefreshToken(user.id(), user.role()),
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
