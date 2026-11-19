package com.example.eduhub.enrollment.dto;

import com.example.eduhub.enrollment.domain.Enrollment;
import com.example.eduhub.enrollment.domain.EnrollmentStatus;
import java.time.LocalDateTime;

/** 수강 신청 결과 (FR-04). */
public final class EnrollmentResponse {

  private final Long id;
  private final Long sessionId;
  private final Long employeeId;
  private final EnrollmentStatus status;
  private final LocalDateTime createdAt;

  private EnrollmentResponse(
      Long id, Long sessionId, Long employeeId, EnrollmentStatus status, LocalDateTime createdAt) {
    this.id = id;
    this.sessionId = sessionId;
    this.employeeId = employeeId;
    this.status = status;
    this.createdAt = createdAt;
  }

  public static EnrollmentResponse from(Enrollment enrollment) {
    return new EnrollmentResponse(
        enrollment.getId(),
        enrollment.getSession().getId(),
        enrollment.getEmployeeId(),
        enrollment.getStatus(),
        enrollment.getCreatedAt());
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
}
