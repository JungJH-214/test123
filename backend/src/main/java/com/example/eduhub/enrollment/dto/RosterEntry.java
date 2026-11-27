package com.example.eduhub.enrollment.dto;

import com.example.eduhub.enrollment.domain.EnrollmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 명단의 신청 한 건 (FR-08). */
public final class RosterEntry {

  @Schema(description = "신청 ID")
  private final Long enrollmentId;

  @Schema(description = "임직원 ID")
  private final Long employeeId;

  @Schema(description = "사번. 임직원 정보가 없으면 null", nullable = true)
  private final String employeeNo;

  @Schema(description = "이름. 임직원 정보가 없으면 null", nullable = true)
  private final String name;

  @Schema(description = "신청 상태 (CONFIRMED, COMPLETED, WAITING)")
  private final EnrollmentStatus status;

  @Schema(description = "신청 시각")
  private final LocalDateTime enrolledAt;

  @Schema(description = "대기 순번(1부터, 신청 ID 오름차순). 대기자만 값이 있고 나머지는 null", nullable = true)
  private final Integer waitingNumber;

  private RosterEntry(RosterRow row, Integer waitingNumber) {
    this.enrollmentId = row.getEnrollmentId();
    this.employeeId = row.getEmployeeId();
    this.employeeNo = row.getEmployeeNo();
    this.name = row.getName();
    this.status = row.getStatus();
    this.enrolledAt = row.getCreatedAt();
    this.waitingNumber = waitingNumber;
  }

  public static RosterEntry confirmed(RosterRow row) {
    return new RosterEntry(row, null);
  }

  public static RosterEntry waiting(RosterRow row, int waitingNumber) {
    return new RosterEntry(row, waitingNumber);
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

  public LocalDateTime getEnrolledAt() {
    return enrolledAt;
  }

  public Integer getWaitingNumber() {
    return waitingNumber;
  }
}
