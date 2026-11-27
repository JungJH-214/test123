package com.example.eduhub.enrollment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/** 회차 명단 (FR-08). */
public final class RosterResponse {

  @Schema(description = "회차 ID")
  private final Long sessionId;

  @Schema(description = "과정 제목")
  private final String courseTitle;

  @Schema(description = "회차 시작일")
  private final LocalDate startDate;

  @Schema(description = "정원")
  private final int capacity;

  @Schema(description = "확정자(CONFIRMED, COMPLETED). 신청 ID 오름차순")
  private final List<RosterEntry> confirmed;

  @Schema(description = "대기자(WAITING). 신청 ID 오름차순이며 waitingNumber는 1부터")
  private final List<RosterEntry> waiting;

  private RosterResponse(
      Long sessionId,
      String courseTitle,
      LocalDate startDate,
      int capacity,
      List<RosterEntry> confirmed,
      List<RosterEntry> waiting) {
    this.sessionId = sessionId;
    this.courseTitle = courseTitle;
    this.startDate = startDate;
    this.capacity = capacity;
    this.confirmed = confirmed;
    this.waiting = waiting;
  }

  public static RosterResponse of(
      Long sessionId,
      String courseTitle,
      LocalDate startDate,
      int capacity,
      List<RosterEntry> confirmed,
      List<RosterEntry> waiting) {
    return new RosterResponse(sessionId, courseTitle, startDate, capacity, confirmed, waiting);
  }

  public Long getSessionId() {
    return sessionId;
  }

  public String getCourseTitle() {
    return courseTitle;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public int getCapacity() {
    return capacity;
  }

  public List<RosterEntry> getConfirmed() {
    return confirmed;
  }

  public List<RosterEntry> getWaiting() {
    return waiting;
  }
}
