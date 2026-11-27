package com.example.eduhub.enrollment.repository;

import com.example.eduhub.enrollment.domain.Enrollment;
import com.example.eduhub.enrollment.domain.EnrollmentStatus;
import com.example.eduhub.enrollment.dto.RosterRow;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

  boolean existsBySessionIdAndEmployeeId(Long sessionId, Long employeeId);

  long countBySessionIdAndStatus(Long sessionId, EnrollmentStatus status);

  long countBySessionIdAndStatusIn(Long sessionId, Collection<EnrollmentStatus> statuses);

  /** 회차에서 주어진 임직원들의 신청을 한 번에 읽는다 (FR-08 수료 처리). 신청이 없는 임직원은 결과에 없다. */
  List<Enrollment> findBySessionIdAndEmployeeIdIn(Long sessionId, Collection<Long> employeeIds);

  /**
   * 회차 명단 행을 신청 ID 오름차순으로 읽는다 (FR-08). CONFIRMED, COMPLETED, WAITING만 포함한다. employee 행이 없는 신청(FK
   * 없음)도 빠지지 않고 사번과 이름이 null로 나온다.
   */
  @Query(
      "select new com.example.eduhub.enrollment.dto.RosterRow("
          + "e.id, e.employeeId, emp.employeeNo, emp.name, e.status, e.createdAt) "
          + "from Enrollment e left join Employee emp on emp.id = e.employeeId "
          + "where e.session.id = :sessionId and e.status in ("
          + "com.example.eduhub.enrollment.domain.EnrollmentStatus.CONFIRMED, "
          + "com.example.eduhub.enrollment.domain.EnrollmentStatus.COMPLETED, "
          + "com.example.eduhub.enrollment.domain.EnrollmentStatus.WAITING) "
          + "order by e.id")
  List<RosterRow> findRosterRows(@Param("sessionId") Long sessionId);
}
