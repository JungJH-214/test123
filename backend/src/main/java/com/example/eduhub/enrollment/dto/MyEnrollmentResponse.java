package com.example.eduhub.enrollment.dto;

import com.example.eduhub.enrollment.domain.EnrollmentStatus;
import com.example.eduhub.enrollment.repository.MyEnrollmentRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 내 신청 내역 한 건 (FR-07). */
public final class MyEnrollmentResponse {

  @Schema(description = "신청 ID")
  private final Long id;

  @Schema(description = "회차 ID")
  private final Long sessionId;

  @Schema(description = "과정 제목")
  private final String courseTitle;

  @Schema(description = "회차 시작일")
  private final LocalDate startDate;

  @Schema(description = "회차 장소")
  private final String place;

  @Schema(description = "신청 상태")
  private final EnrollmentStatus status;

  @Schema(description = "대기 순번 (WAITING일 때만, 1부터)", nullable = true)
  private final Integer waitingNumber;

  @Schema(description = "신청 일시")
  private final LocalDateTime createdAt;

  private MyEnrollmentResponse(
      Long id,
      Long sessionId,
      String courseTitle,
      LocalDate startDate,
      String place,
      EnrollmentStatus status,
      Integer waitingNumber,
      LocalDateTime createdAt) {
    this.id = id;
    this.sessionId = sessionId;
    this.courseTitle = courseTitle;
    this.startDate = startDate;
    this.place = place;
    this.status = status;
    this.waitingNumber = waitingNumber;
    this.createdAt = createdAt;
  }

  public static MyEnrollmentResponse from(MyEnrollmentRow row) {
    Integer waitingNumber =
        row.getStatus() == EnrollmentStatus.WAITING ? (int) (row.getPrecedingWaiting() + 1) : null;
    return new MyEnrollmentResponse(
        row.getId(),
        row.getSessionId(),
        row.getCourseTitle(),
        row.getStartDate(),
        row.getPlace(),
        row.getStatus(),
        waitingNumber,
        row.getCreatedAt());
  }

  public Long getId() {
    return id;
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

  public String getPlace() {
    return place;
  }

  public EnrollmentStatus getStatus() {
    return status;
  }

  public Integer getWaitingNumber() {
    return waitingNumber;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
