package com.example.eduhub.course.repository;

import com.example.eduhub.course.domain.CourseSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseSessionRepository extends JpaRepository<CourseSession, Long> {

  List<CourseSession> findByCourseIdOrderByStartDateAsc(Long courseId);

  /** 회차가 그 과정에 속할 때만 찾는다. */
  Optional<CourseSession> findByIdAndCourseId(Long id, Long courseId);
}
