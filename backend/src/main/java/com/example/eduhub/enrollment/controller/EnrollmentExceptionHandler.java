package com.example.eduhub.enrollment.controller;

import com.example.eduhub.common.error.ErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** 수강 신청 전용 예외 처리. 경로 값 형식 오류를 400 INVALID_REQUEST로 바꾼다. */
@RestControllerAdvice(assignableTypes = EnrollmentController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EnrollmentExceptionHandler {

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException e) {
    return ErrorResponse.of("INVALID_REQUEST", "요청 값의 형식이 올바르지 않습니다: " + e.getName());
  }
}
