package com.example.eduhub.enrollment.repository;

import com.example.eduhub.enrollment.domain.EnrollmentStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 내 신청 내역 조회 결과 한 행. precedingWaiting은 같은 회차에서 나보다 앞선 대기 수다 (FR-07, D-04).
 *
 * <p>대기 순번 계산 규칙 (D-04): 순번은 저장하지 않고 조회할 때 계산한다. 같은 회차의 {@code WAITING} 신청 사이에서 신청 ID 오름차순으로 정한다.
 * precedingWaiting은 이 기준으로 나보다 앞선 대기 신청 수이므로, 대기 순번은 {@code precedingWaiting + 1}(1부터)이다. 이 행의
 * status가 {@code WAITING}이 아니면 순번은 의미가 없다. 다른 회차의 대기자와 확정 신청은 세지 않는다.
 */
public final class MyEnrollmentRow {

  private final Long id;
  private final Long sessionId;
  private final String courseTitle;
  private final LocalDate startDate;
  private final String place;
  private final EnrollmentStatus status;
  private final LocalDateTime createdAt;
  private final Long precedingWaiting;

  public MyEnrollmentRow(
      Long id,
      Long sessionId,
      String courseTitle,
      LocalDate startDate,
      String place,
      EnrollmentStatus status,
      LocalDateTime createdAt,
      Long precedingWaiting) {
    this.id = id;
    this.sessionId = sessionId;
    this.courseTitle = courseTitle;
    this.startDate = startDate;
    this.place = place;
    this.status = status;
    this.createdAt = createdAt;
    this.precedingWaiting = precedingWaiting;
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

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public Long getPrecedingWaiting() {
    return precedingWaiting;
  }
}
