package com.jingdong.backend.auth;

import com.jingdong.backend.api.ErrorCode;
import com.jingdong.backend.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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
    CurrentUserPrincipal principal = principal();
    if (principal != null) {
      return principal.userId();
    }
    String userId = CURRENT_USER_ID.get();
    if (userId == null || userId.isBlank()) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    return userId;
  }

  public static String role() {
    CurrentUserPrincipal principal = principal();
    if (principal != null) {
      return principal.role();
    }
    String role = CURRENT_ROLE.get();
    if (role == null || role.isBlank()) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    return role;
  }

  public static boolean isAdminOperator() {
    CurrentUserPrincipal principal = principal();
    if (principal != null) {
      return "ADMIN".equals(principal.role()) || "OPERATOR".equals(principal.role());
    }
    String role = CURRENT_ROLE.get();
    return "ADMIN".equals(role) || "OPERATOR".equals(role);
  }

  public static void clear() {
    CURRENT_USER_ID.remove();
    CURRENT_ROLE.remove();
  }

  private static CurrentUserPrincipal principal() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUserPrincipal principal)) {
      return null;
    }
    return principal;
  }
}
