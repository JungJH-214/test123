package com.example.eduhub.enrollment.domain;

import com.example.eduhub.course.domain.CourseSession;
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

  /** 수료 처리한다. 확정(CONFIRMED) 상태에서만 가능하다 (FR-08). */
  public void complete() {
    if (status != EnrollmentStatus.CONFIRMED) {
      throw new IllegalStateException("확정 상태만 수료 처리할 수 있습니다. 현재 상태: " + status);
    }
    this.status = EnrollmentStatus.COMPLETED;
  }
}
