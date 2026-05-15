package com.jingdong.backend.auth;

import com.jingdong.backend.auth.JwtTokenService.TokenClaims;
import com.jingdong.backend.store.DatabaseStore;
import com.jingdong.backend.store.DatabaseStore.UserRecord;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  // 与 SecurityConfig 的匿名路径保持一致，避免公开接口被 JWT 过滤器提前拦截。
  private static final List<String> PUBLIC_PATHS = List.of(
      "/auth/login",
      "/auth/register",
      "/auth/refresh",
      "/v3/api-docs/**",
      "/swagger-ui/**",
      "/swagger-ui.html",
      "/static/**",
      "/health",
      "/payments/notify/**",
      "/error"
  );

  private final AntPathMatcher pathMatcher = new AntPathMatcher();
  private final JwtTokenService jwtTokenService;
  private final TokenBlacklistService tokenBlacklistService;
  private final DatabaseStore store;
  private final SecurityErrorHandler securityErrorHandler;

  public JwtAuthenticationFilter(
      JwtTokenService jwtTokenService,
      TokenBlacklistService tokenBlacklistService,
      DatabaseStore store,
      SecurityErrorHandler securityErrorHandler
  ) {
    this.jwtTokenService = jwtTokenService;
    this.tokenBlacklistService = tokenBlacklistService;
    this.store = store;
    this.securityErrorHandler = securityErrorHandler;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    // 预检请求不做鉴权；实际业务请求仍会按权限规则校验。
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      return true;
    }
    String path = request.getServletPath();
    return PUBLIC_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    // 没有 Bearer Token 时继续进入后续 Spring Security 规则，由规则决定是否 401。
    String authorization = request.getHeader("Authorization");
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = authorization.substring("Bearer ".length()).trim();
    if (token.isBlank()) {
      securityErrorHandler.writeUnauthorized(response);
      return;
    }

    try {
      // logout 后的 token 或 jti 会进入 Redis 黑名单，避免旧 token 被继续使用。
      if (tokenBlacklistService.isBlacklisted(token)) {
        securityErrorHandler.writeUnauthorized(response);
        return;
      }

      TokenClaims claims = jwtTokenService.parseAccessToken(token);
      if (tokenBlacklistService.isJtiBlacklisted(claims.jti())) {
        securityErrorHandler.writeUnauthorized(response);
        return;
      }

      Optional<UserRecord> user = store.findUserById(claims.userId());
      if (user.isEmpty() || !"ACTIVE".equals(user.get().status())) {
        securityErrorHandler.writeUnauthorized(response);
        return;
      }

      // 把当前用户写入 SecurityContext，后续 controller/service 可通过认证信息拿到用户身份。
      CurrentUserPrincipal principal = new CurrentUserPrincipal(
          user.get().id(),
          user.get().mobile(),
          user.get().role(),
          user.get().status()
      );
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(
              principal,
              null,
              principal.authorities()
          );
      SecurityContextHolder.getContext().setAuthentication(authentication);
      filterChain.doFilter(request, response);
    } catch (Exception exception) {
      SecurityContextHolder.clearContext();
      securityErrorHandler.writeUnauthorized(response);
    }
  }
}
