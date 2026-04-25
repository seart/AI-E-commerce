package com.jingdong.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestObservabilityFilter extends OncePerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(RequestObservabilityFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    String requestId = request.getHeader("X-Request-Id");
    if (requestId == null || requestId.isBlank()) {
      requestId = UUID.randomUUID().toString();
    }
    long startedAt = System.currentTimeMillis();
    MDC.put("requestId", requestId);
    response.setHeader("X-Request-Id", requestId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      long duration = System.currentTimeMillis() - startedAt;
      log.info(
          "request method={} path={} status={} durationMs={} requestId={}",
          request.getMethod(),
          request.getRequestURI(),
          response.getStatus(),
          duration,
          requestId
      );
      MDC.remove("requestId");
    }
  }
}
