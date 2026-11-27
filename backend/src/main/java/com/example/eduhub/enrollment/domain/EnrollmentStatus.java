package com.example.eduhub.enrollment.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** 수강 신청 상태. */
public enum EnrollmentStatus {
  CONFIRMED,
  WAITING,
  /** 수료 처리된 확정자 (FR-08). 수료자도 정원을 차지한다 (D-05). 취소할 수 없다. */
  COMPLETED,
  /** 취소됨 (FR-06). 중복 신청 판정과 부분 유일 인덱스에서 제외된다. */
  CANCELLED;

  /** 정원을 차지하는 상태 (D-05). 확정자와 수료자만 차지하고 대기자와 취소자는 차지하지 않는다. 불변 목록이다. */
  public static final List<EnrollmentStatus> SEAT_STATUSES =
      Collections.unmodifiableList(Arrays.asList(CONFIRMED, COMPLETED));
}
