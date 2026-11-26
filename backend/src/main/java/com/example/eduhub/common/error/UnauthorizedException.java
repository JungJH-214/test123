package com.example.eduhub.common.error;

/** 인증에 실패했을 때 던진다. {@link GlobalExceptionHandler}가 401로 바꾼다. */
public class UnauthorizedException extends RuntimeException {

  private final String code;

  public UnauthorizedException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
