package com.example.eduhub.enrollment.service;

import com.example.eduhub.course.domain.CourseSession;
import com.example.eduhub.course.exception.SessionClosedException;
import com.example.eduhub.course.exception.SessionNotFoundException;
import com.example.eduhub.course.repository.CourseSessionRepository;
import com.example.eduhub.enrollment.domain.Enrollment;
import com.example.eduhub.enrollment.domain.EnrollmentStatus;
import com.example.eduhub.enrollment.dto.EnrollmentResponse;
import com.example.eduhub.enrollment.exception.CancelDeadlinePassedException;
import com.example.eduhub.enrollment.exception.DuplicateEnrollmentException;
import com.example.eduhub.enrollment.exception.EnrollmentNotFoundException;
import com.example.eduhub.enrollment.exception.SessionFullException;
import com.example.eduhub.enrollment.repository.EnrollmentConstraints;
import com.example.eduhub.enrollment.repository.EnrollmentRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 수강 신청 (FR-04, NFR-01, D-02).
 *
 * <p>규칙: 회차 행에 쓰기 락을 걸고 확정 수를 센 뒤 저장까지 한 트랜잭션에서 한다. 확정 수가 정원보다 적으면 확정, 정원이 찼으면 대기(정원의 50% 내림까지),
 * 대기도 차면 409 SESSION_FULL이다. 같은 회차에 이미 신청했으면 409 ENROLLMENT_DUPLICATE다. 종료된 회차는 409 SESSION_CLOSED,
 * 없는 회차는 404 SESSION_NOT_FOUND다. 시작일이 지난 회차도 막지 않는다. 대기 순번(FR-05, D-04)은 락 안에서 센 기존 대기 수 + 1이다.
 * D-04는 같은 회차 대기 중 신청 id 오름차순이고, id는 락을 잡은 뒤 저장할 때 발급되므로 새 대기는 항상 그 순서의 맨 뒤다.
 *
 * <p>취소 규칙(FR-06): 본인 신청만 취소할 수 있고 남의 신청이나 없는 신청은 똑같이 404다(404 우선). 회차 락을 잡은 뒤 종료 회차(409
 * SESSION_CLOSED), 상태(이미 취소 등), 마감(D-01: 오늘(Asia/Seoul)이 시작일 1일 전까지) 순서로 검사하고, 모두 통과해야 상태를 바꾼다. 취소한
 * 뒤에는 신청 상태와 관계없이 빈 자리만큼 대기 승급을 한다.
 *
 * <p>승급 규칙: 대기 중 신청 id가 가장 작은 신청(D-04 1순위)부터 확정 수가 정원보다 적은 동안 반복해 확정한다. 신청할 때도 판정 전에 먼저 승급해서 새 신청이
 * 대기자를 앞지르지 않게 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EnrollmentCommandService {

  private final CourseSessionRepository courseSessionRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final Clock clock;

  private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

  public EnrollmentResponse enroll(Long sessionId, Long employeeId) {
    CourseSession session =
        courseSessionRepository
            .findByIdForUpdate(sessionId)
            .orElseThrow(() -> new SessionNotFoundException(sessionId));
    if (session.isClosed()) {
      throw new SessionClosedException(sessionId);
    }
    if (enrollmentRepository.existsBySessionIdAndEmployeeIdAndStatusNot(
        sessionId, employeeId, EnrollmentStatus.CANCELLED)) {
      throw new DuplicateEnrollmentException(sessionId, employeeId);
    }
    fillSeatsFromWaiting(session);
    Decision decision = decide(session);
    Enrollment saved =
        save(Enrollment.create(session, employeeId, decision.status), sessionId, employeeId);
    if (decision.status == EnrollmentStatus.WAITING) {
      return EnrollmentResponse.waiting(saved, Math.toIntExact(decision.waitingBefore + 1));
    }
    return EnrollmentResponse.from(saved);
  }

  /** 내 신청을 취소한다. 취소 뒤에는 상태와 관계없이 빈 자리만큼 대기 순서대로 승급한다 (FR-06). */
  public EnrollmentResponse cancel(Long enrollmentId, Long employeeId) {
    // 락 전에는 회차 ID 값만 읽는다. 엔티티를 읽으면 1차 캐시 때문에 락 뒤 최신 상태를 못 본다.
    Long sessionId =
        enrollmentRepository
            .findSessionIdByIdAndEmployeeId(enrollmentId, employeeId)
            .orElseThrow(() -> new EnrollmentNotFoundException(enrollmentId));
    CourseSession session =
        courseSessionRepository
            .findByIdForUpdate(sessionId)
            .orElseThrow(() -> new SessionNotFoundException(sessionId));
    Enrollment enrollment =
        enrollmentRepository
            .findByIdAndEmployeeId(enrollmentId, employeeId)
            .orElseThrow(() -> new EnrollmentNotFoundException(enrollmentId));
    if (session.isClosed()) {
      throw new SessionClosedException(sessionId);
    }
    enrollment.checkCancellable();
    checkDeadline(session);
    enrollment.cancel();
    // 승급 판단의 확정 수 집계가 방금 취소를 반영하도록 먼저 flush한다(자동 flush에 기대지 않는다).
    enrollmentRepository.flush();
    fillSeatsFromWaiting(session);
    return EnrollmentResponse.from(enrollment);
  }

  private void checkDeadline(CourseSession session) {
    LocalDate today = LocalDate.now(clock.withZone(BUSINESS_ZONE));
    if (today.isAfter(session.getStartDate().minusDays(1))) {
      throw new CancelDeadlinePassedException(session.getStartDate());
    }
  }

  /**
   * 확정 수가 정원보다 적은 동안 대기 1순위(D-04, 신청 id 최소)를 확정한다. 호출 전에 회차 행 락을 잡고 있어야 한다. 한 명 확정할 때마다 flush해서 다음
   * 조회와 이어지는 집계가 바뀐 상태를 보게 한다.
   */
  private void fillSeatsFromWaiting(CourseSession session) {
    Long id = session.getId();
    long confirmed = countSeatsTaken(id);
    while (confirmed < session.getCapacity()) {
      Optional<Enrollment> next =
          enrollmentRepository.findFirstBySessionIdAndStatusOrderByIdAsc(
              id, EnrollmentStatus.WAITING);
      if (!next.isPresent()) {
        break;
      }
      next.get().confirmFromWaiting();
      // 다음 WAITING 조회가 방금 확정한 행을 건너뛰도록 자동 flush에 기대지 않고 명시적으로 반영한다
      enrollmentRepository.flush();
      confirmed++;
    }
  }

  /** 정원을 차지하는 상태. FR-08이 수료(COMPLETED)를 추가하면 여기서 함께 센다. */
  private long countSeatsTaken(Long sessionId) {
    return enrollmentRepository.countBySessionIdAndStatus(sessionId, EnrollmentStatus.CONFIRMED);
  }

  private Decision decide(CourseSession session) {
    Long id = session.getId();
    long confirmed = countSeatsTaken(id);
    if (confirmed < session.getCapacity()) {
      return new Decision(EnrollmentStatus.CONFIRMED, 0);
    }
    long waiting = enrollmentRepository.countBySessionIdAndStatus(id, EnrollmentStatus.WAITING);
    if (waiting < session.getCapacity() / 2) {
      return new Decision(EnrollmentStatus.WAITING, waiting);
    }
    throw new SessionFullException(id);
  }

  private static final class Decision {
    private final EnrollmentStatus status;
    private final long waitingBefore;

    private Decision(EnrollmentStatus status, long waitingBefore) {
      this.status = status;
      this.waitingBefore = waitingBefore;
    }
  }

  private Enrollment save(Enrollment enrollment, Long sessionId, Long employeeId) {
    try {
      return enrollmentRepository.saveAndFlush(enrollment);
    } catch (DataIntegrityViolationException e) {
      if (EnrollmentConstraints.isActiveUniqueViolation(e)) {
        throw new DuplicateEnrollmentException(sessionId, employeeId);
      }
      throw e;
    }
  }
}
