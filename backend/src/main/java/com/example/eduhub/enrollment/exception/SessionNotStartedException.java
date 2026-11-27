package com.example.eduhub.enrollment.exception;

import com.example.eduhub.common.error.ConflictException;

/** 회차 시작일 전에는 수료 처리할 수 없다 (FR-08). */
public class SessionNotStartedException extends ConflictException {

  public SessionNotStartedException(Long sessionId) {
    super("SESSION_NOT_STARTED", "시작 전인 회차는 수료 처리할 수 없습니다: " + sessionId);
  }
}
