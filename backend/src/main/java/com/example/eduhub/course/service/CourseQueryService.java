package com.example.eduhub.course.service;

import static com.example.eduhub.course.repository.CourseSpecifications.categoryEquals;
import static com.example.eduhub.course.repository.CourseSpecifications.titleContains;

import com.example.eduhub.common.dto.PageResponse;
import com.example.eduhub.course.domain.Course;
import com.example.eduhub.course.dto.CourseDetailResponse;
import com.example.eduhub.course.dto.CourseSummaryResponse;
import com.example.eduhub.course.dto.SessionResponse;
import com.example.eduhub.course.exception.CourseNotFoundException;
import com.example.eduhub.course.repository.CourseRepository;
import com.example.eduhub.course.repository.CourseSessionRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 과정 조회 (FR-01, FR-02). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseQueryService {

  private final CourseRepository courseRepository;
  private final CourseSessionRepository courseSessionRepository;

  public PageResponse<CourseSummaryResponse> search(
      String keyword, String category, Pageable pageable) {
    Specification<Course> spec =
        Specification.where(titleContains(keyword)).and(categoryEquals(category));
    return PageResponse.from(
        courseRepository.findAll(spec, pageable).map(CourseSummaryResponse::from));
  }

  public CourseDetailResponse getDetail(Long courseId) {
    Course course =
        courseRepository
            .findById(courseId)
            .orElseThrow(() -> new CourseNotFoundException(courseId));
    List<SessionResponse> sessions =
        courseSessionRepository.findByCourseIdOrderByStartDateAsc(courseId).stream()
            .map(SessionResponse::from)
            .collect(Collectors.toList());
    return CourseDetailResponse.of(course, sessions);
  }
}
