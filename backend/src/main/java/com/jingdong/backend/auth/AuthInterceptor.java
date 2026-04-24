package com.jingdong.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jingdong.backend.api.ApiResponse;
import com.jingdong.backend.api.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final JwtTokenService jwtTokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(
            JwtTokenService jwtTokenService,
            TokenBlacklistService tokenBlacklistService,
            ObjectMapper objectMapper
    ) {
        this.jwtTokenService = jwtTokenService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            writeUnauthorized(response);
            return false;
        }

        String token = authorization.substring("Bearer ".length()).trim();
        try {
            if (tokenBlacklistService.isBlacklisted(token)) {
                writeUnauthorized(response);
                return false;
            }
            UserContext.setUserId(jwtTokenService.parseUserId(token));
            return true;
        } catch (Exception exception) {
            UserContext.clear();
            writeUnauthorized(response);
            return false;
        }
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        UserContext.clear();
    }

    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(ErrorCode.UNAUTHORIZED.httpStatus().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.failure(ErrorCode.UNAUTHORIZED.code(), ErrorCode.UNAUTHORIZED.message())
        ));
    }
}
