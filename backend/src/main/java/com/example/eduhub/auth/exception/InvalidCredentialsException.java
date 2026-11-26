package com.example.eduhub.auth.exception;

import com.example.eduhub.common.error.UnauthorizedException;

/** 사번이 없거나 비밀번호가 틀렸을 때. 둘을 구분하지 않는다. */
public class InvalidCredentialsException extends UnauthorizedException {

  public InvalidCredentialsException() {
    super("INVALID_CREDENTIALS", "사번 또는 비밀번호가 올바르지 않습니다.");
  }
}
