package com.example.eduhub.enrollment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.eduhub.course.domain.Course;
import com.example.eduhub.course.domain.CourseSession;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class EnrollmentTest {

  private Enrollment enrollment(EnrollmentStatus status) {
    Course course = Course.create("Java 기초", "개발", "입문");
    CourseSession session = CourseSession.create(course, LocalDate.of(2026, 12, 1), "본사", 10);
    return Enrollment.create(session, 7L, status);
  }

  @Test
  void complete_confirmed_becomesCompleted() {
    Enrollment enrollment = enrollment(EnrollmentStatus.CONFIRMED);

    enrollment.complete();

    assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
  }

  @Test
  void complete_waiting_throwsIllegalState() {
    Enrollment enrollment = enrollment(EnrollmentStatus.WAITING);

    assertThatThrownBy(enrollment::complete).isInstanceOf(IllegalStateException.class);
    assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.WAITING);
  }

  @Test
  void complete_alreadyCompleted_throwsIllegalState() {
    Enrollment enrollment = enrollment(EnrollmentStatus.COMPLETED);

    assertThatThrownBy(enrollment::complete).isInstanceOf(IllegalStateException.class);
  }
}
