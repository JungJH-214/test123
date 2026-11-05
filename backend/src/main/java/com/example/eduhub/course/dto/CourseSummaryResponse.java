package com.example.eduhub.course.dto;

import com.example.eduhub.course.domain.Course;
import com.example.eduhub.course.domain.CourseStatus;

/** 과정 목록의 한 줄 (FR-01). */
public final class CourseSummaryResponse {

  private final Long id;
  private final String title;
  private final String category;
  private final CourseStatus status;

  private CourseSummaryResponse(Long id, String title, String category, CourseStatus status) {
    this.id = id;
    this.title = title;
    this.category = category;
    this.status = status;
  }

  public static CourseSummaryResponse from(Course course) {
    return new CourseSummaryResponse(
        course.getId(), course.getTitle(), course.getCategory(), course.getStatus());
  }

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getCategory() {
    return category;
  }

  public CourseStatus getStatus() {
    return status;
  }
}
