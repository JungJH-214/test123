package com.example.eduhub.course.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 교육과정. 상태는 {@link #close()}로만 바꾼다. */
@Entity
@Table(name = "course")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(nullable = false, length = 50)
  private String category;

  @Column(length = 2000)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CourseStatus status;

  private Course(String title, String category, String description) {
    this.title = title;
    this.category = category;
    this.description = description;
    this.status = CourseStatus.OPEN;
  }

  public static Course create(String title, String category, String description) {
    return new Course(title, category, description);
  }

  public void close() {
    this.status = CourseStatus.CLOSED;
  }
}
