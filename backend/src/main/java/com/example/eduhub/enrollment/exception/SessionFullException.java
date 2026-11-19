package com.example.eduhub.enrollment.exception;

import com.example.eduhub.common.error.ConflictException;

/** 대기는 정원의 50%(내림)까지 받는다 (D-02). */
public class SessionFullException extends ConflictException {

  public SessionFullException(Long sessionId) {
    super("SESSION_FULL", "정원과 대기 인원이 모두 찼습니다: " + sessionId);
  }
}
