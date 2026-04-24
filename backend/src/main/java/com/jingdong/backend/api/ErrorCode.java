package com.jingdong.backend.api;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
  BAD_REQUEST(4001, "参数错误", HttpStatus.BAD_REQUEST),
  LOGIN_FAILED(4002, "手机号或密码错误", HttpStatus.BAD_REQUEST),
  MOBILE_EXISTS(4003, "该手机号已注册", HttpStatus.CONFLICT),
  PRODUCT_NOT_FOUND(4004, "商品不存在或已下架", HttpStatus.NOT_FOUND),
  ADDRESS_NOT_FOUND(4005, "地址不存在", HttpStatus.NOT_FOUND),
  INVALID_ADDRESS(4006, "请选择有效的收货地址", HttpStatus.BAD_REQUEST),
  UNAUTHORIZED(4010, "未登录或登录已过期", HttpStatus.UNAUTHORIZED),
  MERCHANT_NOT_FOUND(4040, "商家不存在或已下线", HttpStatus.NOT_FOUND),
  INTERNAL_ERROR(5000, "系统异常", HttpStatus.INTERNAL_SERVER_ERROR);

  private final int code;
  private final String message;
  private final HttpStatus httpStatus;

  ErrorCode(int code, String message, HttpStatus httpStatus) {
    this.code = code;
    this.message = message;
    this.httpStatus = httpStatus;
  }

  public int code() {
    return code;
  }

  public String message() {
    return message;
  }

  public HttpStatus httpStatus() {
    return httpStatus;
  }
}
