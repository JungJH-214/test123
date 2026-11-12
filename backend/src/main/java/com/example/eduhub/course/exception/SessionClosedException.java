package com.example.eduhub.course.exception;

import com.example.eduhub.common.error.ConflictException;

/** 종료된 회차는 수정할 수 없다 (FR-03). */
public class SessionClosedException extends ConflictException {

  public SessionClosedException(Long sessionId) {
    super("SESSION_CLOSED", "종료된 회차입니다: " + sessionId);
  }
}
