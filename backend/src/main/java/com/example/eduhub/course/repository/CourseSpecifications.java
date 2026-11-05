package com.example.eduhub.course.repository;

import com.example.eduhub.course.domain.Course;
import org.springframework.data.jpa.domain.Specification;

/** 과정 목록 검색 조건 (FR-01). 값이 비어 있으면 null을 돌려 그 조건을 건너뛴다. */
public final class CourseSpecifications {

  private static final char LIKE_ESCAPE = '\\';

  private CourseSpecifications() {}

  /** 제목 부분 일치, 대소문자 무시. */
  public static Specification<Course> titleContains(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return null;
    }
    String pattern = "%" + escapeLike(keyword.trim().toLowerCase()) + "%";
    return (root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern, LIKE_ESCAPE);
  }

  /** 분류 정확히 일치. */
  public static Specification<Course> categoryEquals(String category) {
    if (category == null || category.isBlank()) {
      return null;
    }
    return (root, query, cb) -> cb.equal(root.get("category"), category.trim());
  }

  private static String escapeLike(String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
