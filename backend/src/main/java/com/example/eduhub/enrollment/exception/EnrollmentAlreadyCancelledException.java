package com.example.eduhub.enrollment.exception;

import com.example.eduhub.common.error.ConflictException;

/** 이미 취소한 신청을 다시 취소할 때 던진다. */
public class EnrollmentAlreadyCancelledException extends ConflictException {

  public EnrollmentAlreadyCancelledException(Long enrollmentId) {
    super("ENROLLMENT_ALREADY_CANCELLED", "이미 취소한 신청입니다: " + enrollmentId);
  }
}
