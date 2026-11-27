package com.example.eduhub.enrollment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.eduhub.course.domain.Course;
import com.example.eduhub.course.domain.CourseSession;
import com.example.eduhub.enrollment.exception.EnrollmentAlreadyCancelledException;
import com.example.eduhub.enrollment.exception.EnrollmentNotCancellableException;
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

  @Test
  void cancel_confirmed_cancelled() {
    Enrollment e = enrollment(EnrollmentStatus.CONFIRMED);

    e.cancel();

    assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
  }

  @Test
  void cancel_waiting_cancelled() {
    Enrollment e = enrollment(EnrollmentStatus.WAITING);

    e.cancel();

    assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
  }

  @Test
  void cancel_cancelled_throwsAlreadyCancelled() {
    Enrollment e = enrollment(EnrollmentStatus.CANCELLED);

    assertThatThrownBy(e::cancel)
        .isInstanceOf(EnrollmentAlreadyCancelledException.class)
        .extracting("code")
        .isEqualTo("ENROLLMENT_ALREADY_CANCELLED");
    assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
  }

  @Test
  void confirmFromWaiting_waiting_confirmed() {
    Enrollment e = enrollment(EnrollmentStatus.WAITING);

    e.confirmFromWaiting();

    assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
    assertThat(e.isConfirmed()).isTrue();
  }

  @Test
  void confirmFromWaiting_confirmed_throws() {
    Enrollment e = enrollment(EnrollmentStatus.CONFIRMED);

    assertThatThrownBy(e::confirmFromWaiting).isInstanceOf(IllegalStateException.class);
    assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
  }

  @Test
  void confirmFromWaiting_cancelled_throws() {
    Enrollment e = enrollment(EnrollmentStatus.CANCELLED);

    assertThatThrownBy(e::confirmFromWaiting).isInstanceOf(IllegalStateException.class);
    assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
  }

  @Test
  void cancel_completed_throwsNotCancellable() {
    Enrollment e = enrollment(EnrollmentStatus.COMPLETED);

    assertThatThrownBy(e::cancel)
        .isInstanceOf(EnrollmentNotCancellableException.class)
        .extracting("code")
        .isEqualTo("ENROLLMENT_NOT_CANCELLABLE");
    assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
  }
}
