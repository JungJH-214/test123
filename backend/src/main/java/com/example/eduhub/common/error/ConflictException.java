package com.example.eduhub.common.error;

/** 현재 상태에서 할 수 없는 요청일 때 던진다. {@link GlobalExceptionHandler}가 409로 바꾼다. */
public class ConflictException extends RuntimeException {

  private final String code;

  public ConflictException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
