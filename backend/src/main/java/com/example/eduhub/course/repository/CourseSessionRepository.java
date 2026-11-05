package com.example.eduhub.course.repository;

import com.example.eduhub.course.domain.CourseSession;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseSessionRepository extends JpaRepository<CourseSession, Long> {

  List<CourseSession> findByCourseIdOrderByStartDateAsc(Long courseId);
}
