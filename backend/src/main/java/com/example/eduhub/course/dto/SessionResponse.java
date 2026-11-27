package com.example.eduhub.course.dto;

import com.example.eduhub.course.domain.CourseSession;
import com.example.eduhub.course.domain.SessionStatus;
import java.time.LocalDate;

/** 과정 상세의 회차 (FR-02). */
public final class SessionResponse {

  private final Long id;
  private final LocalDate startDate;
  private final String place;
  private final int capacity;
  private final int remainingSeats;
  private final SessionStatus status;

  private SessionResponse(
      Long id,
      LocalDate startDate,
      String place,
      int capacity,
      int remainingSeats,
      SessionStatus status) {
    this.id = id;
    this.startDate = startDate;
    this.place = place;
    this.capacity = capacity;
    this.remainingSeats = remainingSeats;
    this.status = status;
  }

  /** 남은 자리는 정원에서 정원을 차지한 수(확정 + 수료, D-05)를 뺀 값이며 0 아래로 내려가지 않는다. 정원을 줄여 차지 수가 정원을 넘어도 0이다. */
  public static SessionResponse of(CourseSession session, long seatsTaken) {
    return new SessionResponse(
        session.getId(),
        session.getStartDate(),
        session.getPlace(),
        session.getCapacity(),
        (int) Math.max(0L, session.getCapacity() - seatsTaken),
        session.getStatus());
  }

  public Long getId() {
    return id;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public String getPlace() {
    return place;
  }

  public int getCapacity() {
    return capacity;
  }

  public int getRemainingSeats() {
    return remainingSeats;
  }

  public SessionStatus getStatus() {
    return status;
  }
}
