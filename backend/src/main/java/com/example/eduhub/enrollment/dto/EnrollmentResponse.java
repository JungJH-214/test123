package com.example.eduhub.enrollment.dto;

import com.example.eduhub.enrollment.domain.Enrollment;
import com.example.eduhub.enrollment.domain.EnrollmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 수강 신청 결과 (FR-04, FR-05). */
public final class EnrollmentResponse {

  private final Long id;
  private final Long sessionId;
  private final Long employeeId;
  private final EnrollmentStatus status;
  private final LocalDateTime createdAt;

  @Schema(description = "대기 순번. WAITING일 때만 값이 있고(1부터), 신청 시점 기준이다", nullable = true)
  private final Integer waitingNumber;

  private EnrollmentResponse(
      Long id,
      Long sessionId,
      Long employeeId,
      EnrollmentStatus status,
      LocalDateTime createdAt,
      Integer waitingNumber) {
    this.id = id;
    this.sessionId = sessionId;
    this.employeeId = employeeId;
    this.status = status;
    this.createdAt = createdAt;
    this.waitingNumber = waitingNumber;
  }

  public static EnrollmentResponse from(Enrollment enrollment) {
    return new EnrollmentResponse(
        enrollment.getId(),
        enrollment.getSession().getId(),
        enrollment.getEmployeeId(),
        enrollment.getStatus(),
        enrollment.getCreatedAt(),
        null);
  }

  public static EnrollmentResponse waiting(Enrollment enrollment, int waitingNumber) {
    if (enrollment.getStatus() != EnrollmentStatus.WAITING) {
      throw new IllegalArgumentException("대기 신청에만 순번을 붙입니다");
    }
    if (waitingNumber < 1) {
      throw new IllegalArgumentException("대기 순번은 1부터입니다");
    }
    return new EnrollmentResponse(
        enrollment.getId(),
        enrollment.getSession().getId(),
        enrollment.getEmployeeId(),
        enrollment.getStatus(),
        enrollment.getCreatedAt(),
        waitingNumber);
  }

  public Long getId() {
    return id;
  }

  public Long getSessionId() {
    return sessionId;
  }

  public Long getEmployeeId() {
    return employeeId;
  }

  public EnrollmentStatus getStatus() {
    return status;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public Integer getWaitingNumber() {
    return waitingNumber;
  }
}
