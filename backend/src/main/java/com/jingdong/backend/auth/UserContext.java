package com.jingdong.backend.auth;

import com.jingdong.backend.api.ErrorCode;
import com.jingdong.backend.exception.BusinessException;

public final class UserContext {
  private static final ThreadLocal<String> CURRENT_USER_ID = new ThreadLocal<>();

  private UserContext() {}

  public static void setUserId(String userId) {
    CURRENT_USER_ID.set(userId);
  }

  public static String userId() {
    String userId = CURRENT_USER_ID.get();
    if (userId == null || userId.isBlank()) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    return userId;
  }

  public static void clear() {
    CURRENT_USER_ID.remove();
  }
}
