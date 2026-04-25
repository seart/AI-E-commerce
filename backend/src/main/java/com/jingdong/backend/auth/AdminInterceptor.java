package com.jingdong.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jingdong.backend.api.ApiResponse;
import com.jingdong.backend.api.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminInterceptor implements HandlerInterceptor {
  private final ObjectMapper objectMapper;

  public AdminInterceptor(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler
  ) throws Exception {
    if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || UserContext.isAdminOperator()) {
      return true;
    }

    response.setStatus(ErrorCode.FORBIDDEN.httpStatus().value());
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.getWriter().write(objectMapper.writeValueAsString(
        ApiResponse.failure(ErrorCode.FORBIDDEN.code(), ErrorCode.FORBIDDEN.message())
    ));
    return false;
  }
}
