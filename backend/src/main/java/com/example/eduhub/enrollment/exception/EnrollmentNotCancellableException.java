package com.example.eduhub.enrollment.exception;

import com.example.eduhub.common.error.ConflictException;
import com.example.eduhub.enrollment.domain.EnrollmentStatus;

/** 확정·대기가 아닌 상태의 신청을 취소할 때 던진다. */
public class EnrollmentNotCancellableException extends ConflictException {

  public EnrollmentNotCancellableException(Long enrollmentId, EnrollmentStatus status) {
    super("ENROLLMENT_NOT_CANCELLABLE", "취소할 수 없는 상태입니다: " + status);
  }
}
