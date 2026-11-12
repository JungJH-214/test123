package com.example.eduhub.common.error;

/** 공통 에러 응답. */
public final class ErrorResponse {

  private final String code;
  private final String message;

  private ErrorResponse(String code, String message) {
    this.code = code;
    this.message = message;
  }

  public static ErrorResponse of(String code, String message) {
    return new ErrorResponse(code, message);
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }
}
