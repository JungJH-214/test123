package com.example.eduhub.enrollment.exception;

import com.example.eduhub.common.error.NotFoundException;

/** 신청이 없거나 다른 사원의 신청이다. 존재 여부를 드러내지 않으려고 메시지를 같게 한다. */
public class EnrollmentNotFoundException extends NotFoundException {

  public EnrollmentNotFoundException(Long enrollmentId) {
    super("ENROLLMENT_NOT_FOUND", "신청을 찾을 수 없습니다: " + enrollmentId);
  }
}
