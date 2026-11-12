package com.example.eduhub.enrollment.service;

import com.example.eduhub.course.domain.CourseSession;
import com.example.eduhub.course.exception.SessionClosedException;
import com.example.eduhub.course.exception.SessionNotFoundException;
import com.example.eduhub.course.repository.CourseSessionRepository;
import com.example.eduhub.enrollment.domain.Enrollment;
import com.example.eduhub.enrollment.domain.EnrollmentStatus;
import com.example.eduhub.enrollment.dto.EnrollmentResponse;
import com.example.eduhub.enrollment.exception.DuplicateEnrollmentException;
import com.example.eduhub.enrollment.exception.SessionFullException;
import com.example.eduhub.enrollment.repository.EnrollmentConstraints;
import com.example.eduhub.enrollment.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 수강 신청 (FR-04, NFR-01, D-02).
 *
 * <p>규칙: 회차 행에 쓰기 락을 걸고 확정 수를 센 뒤 저장까지 한 트랜잭션에서 한다. 확정 수가 정원보다 적으면 확정, 정원이 찼으면 대기(정원의 50% 내림까지),
 * 대기도 차면 409 SESSION_FULL이다. 같은 회차에 이미 신청했으면 409 ENROLLMENT_DUPLICATE다. 종료된 회차는 409 SESSION_CLOSED,
 * 없는 회차는 404 SESSION_NOT_FOUND다. 시작일이 지난 회차도 막지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EnrollmentCommandService {

  private final CourseSessionRepository courseSessionRepository;
  private final EnrollmentRepository enrollmentRepository;

  public EnrollmentResponse enroll(Long sessionId, Long employeeId) {
    CourseSession session =
        courseSessionRepository
            .findByIdForUpdate(sessionId)
            .orElseThrow(() -> new SessionNotFoundException(sessionId));
    if (session.isClosed()) {
      throw new SessionClosedException(sessionId);
    }
    if (enrollmentRepository.existsBySessionIdAndEmployeeId(sessionId, employeeId)) {
      throw new DuplicateEnrollmentException(sessionId, employeeId);
    }
    EnrollmentStatus status = decideStatus(session);
    Enrollment saved = save(Enrollment.create(session, employeeId, status), sessionId, employeeId);
    return EnrollmentResponse.from(saved);
  }

  private EnrollmentStatus decideStatus(CourseSession session) {
    Long id = session.getId();
    long confirmed = enrollmentRepository.countBySessionIdAndStatus(id, EnrollmentStatus.CONFIRMED);
    if (confirmed < session.getCapacity()) {
      return EnrollmentStatus.CONFIRMED;
    }
    long waiting = enrollmentRepository.countBySessionIdAndStatus(id, EnrollmentStatus.WAITING);
    if (waiting < session.getCapacity() / 2) {
      return EnrollmentStatus.WAITING;
    }
    throw new SessionFullException(id);
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
