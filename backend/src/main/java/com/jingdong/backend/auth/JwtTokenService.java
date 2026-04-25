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
    return createToken(userId, expirationMinutes, "access");
  }

  public String createRefreshToken(String userId) {
    return createToken(userId, refreshExpirationMinutes, "refresh");
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

  private Claims parseClaims(String token) {
    try {
      String[] parts = token.split("\\.");
      if (parts.length != 3) {
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
      }

      String signedContent = parts[0] + "." + parts[1];
      String expectedSignature = sign(signedContent);
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

      return new Claims(userId, Instant.ofEpochSecond(expiresAt));
    } catch (BusinessException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
  }

  private String createToken(String userId, long minutes, String tokenType) {
    try {
      Map<String, Object> header = new LinkedHashMap<>();
      header.put("alg", "HS256");
      header.put("typ", "JWT");

      Instant now = Instant.now();
      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("sub", userId);
      payload.put("typ", tokenType);
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

  private record Claims(String userId, Instant expiresAt) {}
}
