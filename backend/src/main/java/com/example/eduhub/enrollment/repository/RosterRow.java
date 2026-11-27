package com.example.eduhub.enrollment.repository;

import com.example.eduhub.enrollment.domain.EnrollmentStatus;
import java.time.LocalDateTime;

/** 명단 조회 쿼리의 결과 한 줄 (JPQL constructor expression용). employee 행이 없으면 사번과 이름이 null이다. */
public final class RosterRow {

  private final Long enrollmentId;
  private final Long employeeId;
  private final String employeeNo;
  private final String name;
  private final EnrollmentStatus status;
  private final LocalDateTime createdAt;

  public RosterRow(
      Long enrollmentId,
      Long employeeId,
      String employeeNo,
      String name,
      EnrollmentStatus status,
      LocalDateTime createdAt) {
    this.enrollmentId = enrollmentId;
    this.employeeId = employeeId;
    this.employeeNo = employeeNo;
    this.name = name;
    this.status = status;
    this.createdAt = createdAt;
  }

  public Long getEnrollmentId() {
    return enrollmentId;
  }

  public Long getEmployeeId() {
    return employeeId;
  }

  public String getEmployeeNo() {
    return employeeNo;
  }

  public String getName() {
    return name;
  }

  public EnrollmentStatus getStatus() {
    return status;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
