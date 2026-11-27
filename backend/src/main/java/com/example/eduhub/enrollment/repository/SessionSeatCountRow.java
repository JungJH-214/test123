package com.example.eduhub.enrollment.repository;

/** 회차별 정원을 차지한 수 집계 행. */
public interface SessionSeatCountRow {

  Long getSessionId();

  long getSeatsTaken();
}
