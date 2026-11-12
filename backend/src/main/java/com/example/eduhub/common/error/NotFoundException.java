package com.example.eduhub.common.error;

/** 요청한 리소스가 없을 때 던진다. {@link GlobalExceptionHandler}가 404로 바꾼다. */
public class NotFoundException extends RuntimeException {

  private final String code;

  public NotFoundException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
