package com.example.eduhub.enrollment.domain;

import com.example.eduhub.course.domain.CourseSession;
import com.example.eduhub.enrollment.exception.EnrollmentAlreadyCancelledException;
import com.example.eduhub.enrollment.exception.EnrollmentNotCancellableException;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 회차 수강 신청. 같은 회차에 취소되지 않은 신청은 임직원당 하나다 (FR-04). */
@Entity
@Table(name = "enrollment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Enrollment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "session_id", nullable = false)
  private CourseSession session;

  @Column(name = "employee_id", nullable = false)
  private Long employeeId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EnrollmentStatus status;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  private Enrollment(CourseSession session, Long employeeId, EnrollmentStatus status) {
    this.session = session;
    this.employeeId = employeeId;
    this.status = status;
    this.createdAt = LocalDateTime.now();
  }

  public static Enrollment create(CourseSession session, Long employeeId, EnrollmentStatus status) {
    return new Enrollment(session, employeeId, status);
  }

  /** 취소할 수 있는 상태인지 검사한다. 이미 취소됐거나 확정·대기가 아니면 예외. */
  public void checkCancellable() {
    if (status == EnrollmentStatus.CANCELLED) {
      throw new EnrollmentAlreadyCancelledException(id);
    }
    if (status != EnrollmentStatus.CONFIRMED && status != EnrollmentStatus.WAITING) {
      throw new EnrollmentNotCancellableException(id, status);
    }
  }

  /** 신청을 취소한다 (FR-06). 검사를 통과해야만 상태가 바뀐다. */
  public void cancel() {
    checkCancellable();
    this.status = EnrollmentStatus.CANCELLED;
  }

  /** 대기 신청을 확정으로 승급한다 (FR-06). 대기가 아니면 호출 오류다. */
  public void confirmFromWaiting() {
    if (status != EnrollmentStatus.WAITING) {
      throw new IllegalStateException("대기 신청만 확정할 수 있습니다: " + id);
    }
    this.status = EnrollmentStatus.CONFIRMED;
  }

  public boolean isConfirmed() {
    return status == EnrollmentStatus.CONFIRMED;
  }
}
