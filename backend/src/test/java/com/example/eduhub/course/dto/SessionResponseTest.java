package com.example.eduhub.course.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.eduhub.course.domain.Course;
import com.example.eduhub.course.domain.CourseSession;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SessionResponseTest {

  private static CourseSession session(int capacity) {
    return CourseSession.create(
        Course.create("Java 기초", "개발", null), LocalDate.of(2026, 12, 1), "본사", capacity);
  }

  @Test
  void of_subtractsSeatsTaken() {
    SessionResponse response = SessionResponse.of(session(10), 3L);

    assertThat(response.getCapacity()).isEqualTo(10);
    assertThat(response.getRemainingSeats()).isEqualTo(7);
  }

  @Test
  void of_noSeatsTaken_remainingEqualsCapacity() {
    assertThat(SessionResponse.of(session(10), 0L).getRemainingSeats()).isEqualTo(10);
  }

  @Test
  void of_seatsTakenOverCapacity_isZero() {
    assertThat(SessionResponse.of(session(10), 12L).getRemainingSeats()).isZero();
  }
}
