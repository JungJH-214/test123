package com.example.eduhub.course.exception;

import com.example.eduhub.common.error.ConflictException;

/** 종료된 과정은 수정하거나 회차를 추가할 수 없다 (FR-03). */
public class CourseClosedException extends ConflictException {

  public CourseClosedException(Long courseId) {
    super("COURSE_CLOSED", "종료된 과정입니다: " + courseId);
  }
}
