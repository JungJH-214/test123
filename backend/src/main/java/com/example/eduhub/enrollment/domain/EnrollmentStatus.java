package com.example.eduhub.enrollment.domain;

/** 수강 신청 상태. */
public enum EnrollmentStatus {
  CONFIRMED,
  WAITING,
  /** 수료 처리된 확정자 (FR-08). 수료자도 정원을 차지한다 (D-05). 취소할 수 없다. */
  COMPLETED,
  /** 취소됨 (FR-06). 중복 신청 판정과 부분 유일 인덱스에서 제외된다. */
  CANCELLED
}
