package com.jingdong.backend.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {
  private static final String KEY_PREFIX = "auth:blacklist:";
  private static final String JTI_KEY_PREFIX = "auth:blacklist:jti:";

  private final StringRedisTemplate redisTemplate;

  public TokenBlacklistService(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public void blacklist(String token, Instant expiresAt) {
    Duration ttl = Duration.between(Instant.now(), expiresAt);
    if (ttl.isNegative() || ttl.isZero()) {
      return;
    }
    redisTemplate.opsForValue().set(key(token), "1", ttl);
  }

  public void blacklistJti(String jti, Instant expiresAt) {
    Duration ttl = Duration.between(Instant.now(), expiresAt);
    if (ttl.isNegative() || ttl.isZero()) {
      return;
    }
    redisTemplate.opsForValue().set(jtiKey(jti), "1", ttl);
  }

  public boolean isBlacklisted(String token) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(key(token)));
  }

  public boolean isJtiBlacklisted(String jti) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(jtiKey(jti)));
  }

  private String key(String token) {
    return KEY_PREFIX + fingerprint(token);
  }

  private String jtiKey(String jti) {
    return JTI_KEY_PREFIX + jti;
  }

  private String fingerprint(String token) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(token.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to fingerprint token", exception);
    }
  }
}
