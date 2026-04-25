package com.jingdong.backend.exception;

import com.jingdong.backend.api.ApiResponse;
import com.jingdong.backend.api.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
    ErrorCode errorCode = exception.errorCode();
    return ResponseEntity
        .status(errorCode.httpStatus())
        .body(ApiResponse.failure(errorCode.code(), exception.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidationException(
      MethodArgumentNotValidException exception
  ) {
    String message = exception.getBindingResult().getFieldErrors().stream()
        .map(error -> error.getField() + ": " + error.getDefaultMessage())
        .collect(Collectors.joining("; "));
    return ResponseEntity
        .badRequest()
        .body(ApiResponse.failure(ErrorCode.BAD_REQUEST.code(), message));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
      ConstraintViolationException exception
  ) {
    return ResponseEntity
        .badRequest()
        .body(ApiResponse.failure(ErrorCode.BAD_REQUEST.code(), exception.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
    log.error("Unhandled exception requestId={}", MDC.get("requestId"), exception);
    return ResponseEntity
        .status(ErrorCode.INTERNAL_ERROR.httpStatus())
        .body(ApiResponse.failure(ErrorCode.INTERNAL_ERROR.code(), ErrorCode.INTERNAL_ERROR.message()));
  }
}
