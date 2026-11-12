package com.example.eduhub.course.exception;

import com.example.eduhub.common.error.NotFoundException;

public class SessionNotFoundException extends NotFoundException {

  public SessionNotFoundException(Long sessionId) {
    super("SESSION_NOT_FOUND", "회차를 찾을 수 없습니다: " + sessionId);
  }
}
