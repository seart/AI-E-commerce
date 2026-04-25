package com.jingdong.backend.auth;

import com.jingdong.backend.api.ErrorCode;
import com.jingdong.backend.exception.BusinessException;

public final class UserContext {
  private static final ThreadLocal<String> CURRENT_USER_ID = new ThreadLocal<>();
  private static final ThreadLocal<String> CURRENT_ROLE = new ThreadLocal<>();

  private UserContext() {}

  public static void setUserId(String userId) {
    CURRENT_USER_ID.set(userId);
  }

  public static void set(String userId, String role) {
    CURRENT_USER_ID.set(userId);
    CURRENT_ROLE.set(role);
  }

  public static String userId() {
    String userId = CURRENT_USER_ID.get();
    if (userId == null || userId.isBlank()) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    return userId;
  }

  public static String role() {
    String role = CURRENT_ROLE.get();
    if (role == null || role.isBlank()) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    return role;
  }

  public static boolean isAdminOperator() {
    String role = CURRENT_ROLE.get();
    return "ADMIN".equals(role) || "OPERATOR".equals(role);
  }

  public static void clear() {
    CURRENT_USER_ID.remove();
    CURRENT_ROLE.remove();
  }
}
