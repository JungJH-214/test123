package com.example.eduhub.enrollment.controller;

import com.example.eduhub.common.error.ErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** 수강 신청 전용 예외 처리. 임시 신청자 헤더(X-Employee-Id) 오류를 400으로 바꾼다 (NFR-03 전까지). */
@RestControllerAdvice(assignableTypes = EnrollmentController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EnrollmentExceptionHandler {

  @ExceptionHandler(MissingRequestHeaderException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse handleMissingHeader(MissingRequestHeaderException e) {
    return invalidEmployeeId();
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException e) {
    if (EnrollmentController.EMPLOYEE_ID_HEADER.equals(e.getName())) {
      return invalidEmployeeId();
    }
    return ErrorResponse.of("INVALID_REQUEST", "요청 값의 형식이 올바르지 않습니다: " + e.getName());
  }

  private static ErrorResponse invalidEmployeeId() {
    return ErrorResponse.of("MISSING_EMPLOYEE_ID", "신청자 임직원 ID(X-Employee-Id 헤더)가 없거나 숫자가 아닙니다.");
  }
}
