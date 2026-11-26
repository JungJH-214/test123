package com.example.eduhub.enrollment.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.eduhub.course.domain.Course;
import com.example.eduhub.course.domain.CourseSession;
import com.example.eduhub.enrollment.domain.Enrollment;
import com.example.eduhub.enrollment.domain.EnrollmentStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EnrollmentResponseTest {

  @Test
  void waiting_confirmedEnrollment_throws() {
    Enrollment enrollment = enrollment(EnrollmentStatus.CONFIRMED);

    assertThatThrownBy(() -> EnrollmentResponse.waiting(enrollment, 1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void waiting_zero_throws() {
    Enrollment enrollment = enrollment(EnrollmentStatus.WAITING);

    assertThatThrownBy(() -> EnrollmentResponse.waiting(enrollment, 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void waiting_waitingEnrollment_hasNumber() {
    EnrollmentResponse response =
        EnrollmentResponse.waiting(enrollment(EnrollmentStatus.WAITING), 3);

    assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.WAITING);
    assertThat(response.getWaitingNumber()).isEqualTo(3);
  }

  @Test
  void from_waitingNumberNull() {
    EnrollmentResponse response = EnrollmentResponse.from(enrollment(EnrollmentStatus.WAITING));

    assertThat(response.getWaitingNumber()).isNull();
  }

  private static Enrollment enrollment(EnrollmentStatus status) {
    Course course = Course.create("Java 기초", "개발", "입문");
    CourseSession session = CourseSession.create(course, LocalDate.of(2026, 12, 1), "본사", 10);
    ReflectionTestUtils.setField(session, "id", 1L);
    return Enrollment.create(session, 7L, status);
  }
}
