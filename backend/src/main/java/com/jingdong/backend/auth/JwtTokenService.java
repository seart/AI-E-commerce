package com.jingdong.backend.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jingdong.backend.api.ErrorCode;
import com.jingdong.backend.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {
  // 轻量 JWT 实现：只使用 HS256 签名，不引入 session 或重型 OAuth2。
  private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final ObjectMapper objectMapper;
  private final String secret;
  private final long expirationMinutes;
  private final long refreshExpirationMinutes;

  public JwtTokenService(
      ObjectMapper objectMapper,
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.expiration-minutes}") long expirationMinutes,
      @Value("${app.jwt.refresh-expiration-minutes}") long refreshExpirationMinutes
  ) {
    this.objectMapper = objectMapper;
    this.secret = secret;
    this.expirationMinutes = expirationMinutes;
    this.refreshExpirationMinutes = refreshExpirationMinutes;
  }

  public String createAccessToken(String userId) {
    return createAccessToken(userId, "CUSTOMER");
  }

  public String createAccessToken(String userId, String role) {
    return createToken(userId, role, expirationMinutes, "access");
  }

  public String createRefreshToken(String userId) {
    return createRefreshToken(userId, "CUSTOMER");
  }

  public String createRefreshToken(String userId, String role) {
    return createToken(userId, role, refreshExpirationMinutes, "refresh");
  }

  public Instant accessTokenExpiresAt() {
    return Instant.now().plusSeconds(expirationMinutes * 60);
  }

  public String parseUserId(String token) {
    return parseClaims(token).userId();
  }

  public Instant expiresAt(String token) {
    return parseClaims(token).expiresAt();
  }

  public TokenClaims parseAccessToken(String token) {
    TokenClaims claims = parseClaims(token);
    if (!"access".equals(claims.type())) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    return claims;
  }

  public TokenClaims parseRefreshToken(String token) {
    TokenClaims claims = parseClaims(token);
    if (!"refresh".equals(claims.type())) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    return claims;
  }

  public TokenClaims parseClaims(String token) {
    try {
      // JWT 必须是 header.payload.signature 三段式。
      String[] parts = token.split("\\.");
      if (parts.length != 3) {
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
      }

      String signedContent = parts[0] + "." + parts[1];
      String expectedSignature = sign(signedContent);
      // 签名比较使用常量时间比较，降低时序侧信道风险。
      if (!constantTimeEquals(expectedSignature, parts[2])) {
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
      }

      Map<String, Object> payload = objectMapper.readValue(
          URL_DECODER.decode(parts[1]),
          MAP_TYPE
      );
      long expiresAt = ((Number) payload.get("exp")).longValue();
      if (Instant.now().getEpochSecond() >= expiresAt) {
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
      }

      Object subject = payload.get("sub");
      if (!(subject instanceof String userId) || userId.isBlank()) {
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
      }

      Object typeValue = payload.get("typ");
      if (!(typeValue instanceof String type) || type.isBlank()) {
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
      }

      Object jtiValue = payload.get("jti");
      if (!(jtiValue instanceof String jti) || jti.isBlank()) {
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
      }

      Object roleValue = payload.get("role");
      String role = roleValue instanceof String roleText && !roleText.isBlank()
          ? roleText
          : "CUSTOMER";

      return new TokenClaims(userId, role, type, jti, Instant.ofEpochSecond(expiresAt));
    } catch (BusinessException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
  }

  private String createToken(String userId, String role, long minutes, String tokenType) {
    try {
      // access 和 refresh 共用生成逻辑，通过 typ 字段区分用途。
      Map<String, Object> header = new LinkedHashMap<>();
      header.put("alg", "HS256");
      header.put("typ", "JWT");

      Instant now = Instant.now();
      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("sub", userId);
      payload.put("typ", tokenType);
      payload.put("role", role);
      payload.put("iat", now.getEpochSecond());
      payload.put("exp", now.plusSeconds(minutes * 60).getEpochSecond());
      payload.put("jti", UUID.randomUUID().toString());

      String encodedHeader = encodeJson(header);
      String encodedPayload = encodeJson(payload);
      String signedContent = encodedHeader + "." + encodedPayload;

      return signedContent + "." + sign(signedContent);
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to create JWT", exception);
    }
  }

  private String encodeJson(Map<String, Object> value) throws Exception {
    return URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
  }

  private String sign(String value) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
  }

  private boolean constantTimeEquals(String left, String right) {
    byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
    byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
    if (leftBytes.length != rightBytes.length) {
      return false;
    }
    int result = 0;
    for (int i = 0; i < leftBytes.length; i++) {
      result |= leftBytes[i] ^ rightBytes[i];
    }
    return result == 0;
  }

  public record TokenClaims(
      String userId,
      String role,
      String type,
      String jti,
      Instant expiresAt
  ) {}
}
