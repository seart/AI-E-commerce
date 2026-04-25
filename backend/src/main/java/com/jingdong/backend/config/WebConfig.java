package com.jingdong.backend.config;

import com.jingdong.backend.auth.AuthInterceptor;
import com.jingdong.backend.auth.AdminInterceptor;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  private final AuthInterceptor authInterceptor;
  private final AdminInterceptor adminInterceptor;
  private final List<String> allowedOrigins;

  public WebConfig(
      AuthInterceptor authInterceptor,
      AdminInterceptor adminInterceptor,
      CorsProperties corsProperties
  ) {
    this.authInterceptor = authInterceptor;
    this.adminInterceptor = adminInterceptor;
    this.allowedOrigins = corsProperties.getAllowedOrigins();
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(authInterceptor)
        .addPathPatterns("/**")
        .excludePathPatterns(
            "/auth/login",
            "/auth/register",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/static/**",
            "/health",
            "/payments/notify/**",
            "/error"
        );
    registry.addInterceptor(adminInterceptor)
        .addPathPatterns("/admin/**");
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
        .allowedOrigins(allowedOrigins.toArray(String[]::new))
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true);
  }
}
