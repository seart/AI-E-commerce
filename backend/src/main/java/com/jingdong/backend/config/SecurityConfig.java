package com.jingdong.backend.config;

import com.jingdong.backend.auth.JwtAuthenticationFilter;
import com.jingdong.backend.auth.SecurityErrorHandler;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final SecurityErrorHandler securityErrorHandler;
  private final CorsProperties corsProperties;

  public SecurityConfig(
      JwtAuthenticationFilter jwtAuthenticationFilter,
      SecurityErrorHandler securityErrorHandler,
      CorsProperties corsProperties
  ) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.securityErrorHandler = securityErrorHandler;
    this.corsProperties = corsProperties;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    // 后端采用无状态 JWT 鉴权：关闭 session、表单登录和 HTTP Basic，只保留 Bearer Token。
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint((request, response, exception) ->
                securityErrorHandler.writeUnauthorized(response))
            .accessDeniedHandler((request, response, exception) ->
                securityErrorHandler.writeForbidden(response))
        )
        .authorizeHttpRequests(authorize -> authorize
            // 登录、注册、文档、静态资源、支付回调等接口允许匿名访问。
            .requestMatchers(
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
            ).permitAll()
            // 后台接口统一放在 /admin/** 下，只允许 ADMIN / OPERATOR 访问。
            .requestMatchers("/admin/**").hasAnyRole("ADMIN", "OPERATOR")
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    // CORS 白名单来自 application.yml，方便本地/测试/生产按环境覆盖。
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public UserDetailsService userDetailsService() {
    // 项目不使用 Spring Security 默认用户名密码登录，用户身份由 JwtAuthenticationFilter 注入。
    return username -> {
      throw new UsernameNotFoundException(username);
    };
  }
}
