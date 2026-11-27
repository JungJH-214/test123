package com.example.eduhub.enrollment.repository;

import com.example.eduhub.enrollment.domain.Enrollment;
import com.example.eduhub.enrollment.domain.EnrollmentStatus;
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
      "select new com.example.eduhub.enrollment.repository.RosterRow("
          + "e.id, e.employeeId, emp.employeeNo, emp.name, e.status, e.createdAt) "
          + "from Enrollment e left join Employee emp on emp.id = e.employeeId "
          + "where e.session.id = :sessionId and e.status in ("
          + "com.example.eduhub.enrollment.domain.EnrollmentStatus.CONFIRMED, "
          + "com.example.eduhub.enrollment.domain.EnrollmentStatus.COMPLETED, "
          + "com.example.eduhub.enrollment.domain.EnrollmentStatus.WAITING) "
          + "order by e.id")
  List<RosterRow> findRosterRows(@Param("sessionId") Long sessionId);

  /**
   * 임직원의 신청 내역을 최근 신청순(신청 일시, 같으면 id 내림차순)으로 쿼리 하나로 조회한다. status가 null이면 전체.
   *
   * <p>각 행의 precedingWaiting은 같은 회차의 {@code WAITING} 신청 중 id가 더 작은 신청의 수다. id는 회차 락을 잡은 뒤 발급되어 같은
   * 회차에서 실제 신청 순서와 같다 (D-04). 대기 순번은 {@code precedingWaiting + 1}이며 status가 {@code WAITING}인 행에서만
   * 의미가 있다. 임직원 단위 조회이므로 회차 단위 재사용에는 맞지 않는다.
   */
  @Query(
      "select new com.example.eduhub.enrollment.repository.MyEnrollmentRow("
          + " e.id, s.id, c.title, s.startDate, s.place, e.status, e.createdAt,"
          + " (select count(w) from Enrollment w"
          + "   where w.session = s"
          + "     and w.status = com.example.eduhub.enrollment.domain.EnrollmentStatus.WAITING"
          + "     and w.id < e.id)"
          + " )"
          + " from Enrollment e"
          + "   join e.session s"
          + "   join s.course c"
          + " where e.employeeId = :employeeId"
          + "   and (:status is null or e.status = :status)"
          + " order by e.createdAt desc, e.id desc")
  List<MyEnrollmentRow> findMyEnrollments(
      @Param("employeeId") Long employeeId, @Param("status") EnrollmentStatus status);
}
