package com.example.eduhub.enrollment.domain;

/**
 * 수강 신청 상태. CANCELLED는 FR-06에서 추가한다.
 *
 * <p>COMPLETED는 수료 처리된 확정자다(FR-08). 수료자도 정원을 차지한다(D-05).
 */
public enum EnrollmentStatus {
  CONFIRMED,
  WAITING,
  COMPLETED
}
