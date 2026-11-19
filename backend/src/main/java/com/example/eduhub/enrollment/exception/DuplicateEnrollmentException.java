package com.example.eduhub.enrollment.exception;

import com.example.eduhub.common.error.ConflictException;

/** 같은 회차에 취소되지 않은 신청이 이미 있다 (FR-04). */
public class DuplicateEnrollmentException extends ConflictException {

  public DuplicateEnrollmentException(Long sessionId, Long employeeId) {
    super("ENROLLMENT_DUPLICATE", "이미 신청한 회차입니다: " + sessionId);
  }
}
