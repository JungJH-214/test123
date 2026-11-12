package com.example.eduhub.course.domain;

import java.time.LocalDate;
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

/** 과정의 회차. 값은 {@link #update}, 상태는 {@link #close()}로만 바꾼다. */
@Entity
@Table(name = "course_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseSession {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(nullable = false, length = 200)
  private String place;

  @Column(nullable = false)
  private int capacity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SessionStatus status;

  private CourseSession(Course course, LocalDate startDate, String place, int capacity) {
    this.course = course;
    this.startDate = startDate;
    this.place = place;
    this.capacity = capacity;
    this.status = SessionStatus.OPEN;
  }

  public static CourseSession create(
      Course course, LocalDate startDate, String place, int capacity) {
    return new CourseSession(course, startDate, place, capacity);
  }

  /** 시작일, 장소, 정원을 바꾼다. 종료 여부 확인은 서비스에서 한다. */
  public void update(LocalDate startDate, String place, int capacity) {
    this.startDate = startDate;
    this.place = place;
    this.capacity = capacity;
  }

  public boolean isClosed() {
    return status == SessionStatus.CLOSED;
  }

  public void close() {
    this.status = SessionStatus.CLOSED;
  }
}
