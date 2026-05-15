package com.jingdong.backend.service;

import com.jingdong.backend.api.ErrorCode;
import com.jingdong.backend.exception.BusinessException;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {
  // 简易 Redis 限流服务，用于登录、下单等容易被刷的接口。
  private final StringRedisTemplate redisTemplate;

  public RateLimiterService(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public void check(String key, int limit, Duration window) {
    Long value = redisTemplate.opsForValue().increment("rate:" + key);
    if (value != null && value == 1L) {
      redisTemplate.expire("rate:" + key, window);
    }
    if (value != null && value > limit) {
      throw new BusinessException(ErrorCode.RATE_LIMITED);
    }
  }
}
