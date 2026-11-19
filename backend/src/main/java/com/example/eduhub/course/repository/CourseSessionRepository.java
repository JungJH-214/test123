package com.example.eduhub.course.repository;

import com.example.eduhub.course.domain.CourseSession;
import java.util.List;
import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseSessionRepository extends JpaRepository<CourseSession, Long> {

  List<CourseSession> findByCourseIdOrderByStartDateAsc(Long courseId);

  /** 회차가 그 과정에 속할 때만 찾는다. */
  Optional<CourseSession> findByIdAndCourseId(Long id, Long courseId);

  /** 회차 행에 쓰기 락(SELECT ... FOR UPDATE)을 건다. 수강 신청의 정원 판정용 (NFR-01). */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from CourseSession s where s.id = :id")
  Optional<CourseSession> findByIdForUpdate(@Param("id") Long id);
}
