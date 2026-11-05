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

  /** 수강 신청이 아직 없으므로 남은 자리는 정원과 같다. */
  public static SessionResponse from(CourseSession session) {
    return new SessionResponse(
        session.getId(),
        session.getStartDate(),
        session.getPlace(),
        session.getCapacity(),
        session.getCapacity(),
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
