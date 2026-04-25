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
            .requestMatchers("/admin/**").hasAnyRole("ADMIN", "OPERATOR")
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
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
    return username -> {
      throw new UsernameNotFoundException(username);
    };
  }
}
