package com.example.eduhub.enrollment.exception;

import com.example.eduhub.common.error.ConflictException;
import java.time.LocalDate;

/** 취소 마감(회차 시작 1일 전, D-01)이 지났을 때 던진다. */
public class CancelDeadlinePassedException extends ConflictException {

  public CancelDeadlinePassedException(LocalDate startDate) {
    super(
        "CANCEL_DEADLINE_PASSED", "취소 마감이 지났습니다. 회차 시작 1일 전까지 취소할 수 있습니다 (시작일 " + startDate + ")");
  }
}
