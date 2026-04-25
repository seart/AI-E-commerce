package com.jingdong.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jingdong.backend.api.ApiResponse;
import com.jingdong.backend.api.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class SecurityErrorHandler {
  private final ObjectMapper objectMapper;

  public SecurityErrorHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public void writeUnauthorized(HttpServletResponse response) throws java.io.IOException {
    write(response, ErrorCode.UNAUTHORIZED);
  }

  public void writeForbidden(HttpServletResponse response) throws java.io.IOException {
    write(response, ErrorCode.FORBIDDEN);
  }

  private void write(HttpServletResponse response, ErrorCode errorCode) throws java.io.IOException {
    response.setStatus(errorCode.httpStatus().value());
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.getWriter().write(objectMapper.writeValueAsString(
        ApiResponse.failure(errorCode.code(), errorCode.message())
    ));
  }
}

