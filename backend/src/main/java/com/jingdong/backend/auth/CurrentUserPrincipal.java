package com.jingdong.backend.auth;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record CurrentUserPrincipal(
    String userId,
    String mobile,
    String role,
    String status
) {
  public Collection<? extends GrantedAuthority> authorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role));
  }
}

