package com.example.eduhub.enrollment.service;

import com.example.eduhub.common.time.BusinessTime;
import com.example.eduhub.course.domain.CourseSession;
import com.example.eduhub.course.exception.SessionNotFoundException;
import com.example.eduhub.course.repository.CourseSessionRepository;
import com.example.eduhub.enrollment.domain.Enrollment;
import com.example.eduhub.enrollment.domain.EnrollmentStatus;
import com.example.eduhub.enrollment.dto.CompletionFailure;
import com.example.eduhub.enrollment.dto.CompletionFailureReason;
import com.example.eduhub.enrollment.dto.CompletionResponse;
import com.example.eduhub.enrollment.dto.RosterEntry;
import com.example.eduhub.enrollment.dto.RosterResponse;
import com.example.eduhub.enrollment.exception.SessionNotStartedException;
import com.example.eduhub.enrollment.repository.EnrollmentRepository;
import com.example.eduhub.enrollment.repository.RosterRow;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회차 명단 조회와 수료 처리 (FR-08).
 *
 * <p>명단 규칙: 없는 회차는 404 SESSION_NOT_FOUND다. 명단은 한 번의 조회(신청 ID 오름차순)로 읽어 확정자(CONFIRMED, COMPLETED)와
 * 대기자(WAITING)로 나눈다. 대기 순번은 같은 회차 대기자 사이에서 신청 ID 오름차순으로 1부터 매긴다(D-04). 락은 걸지 않는다.
 *
 * <p>수료 처리 규칙: 회차 락을 먼저 잡고(없는 회차는 404), Asia/Seoul 기준 오늘이 시작일보다 이르면 409 SESSION_NOT_STARTED다(당일과
 * 종료된 회차는 허용). 요청의 중복 ID는 처음 순서대로 한 번만 처리한다. 확정(CONFIRMED)만 수료(COMPLETED)로 바꾸고, 신청이 없으면
 * NOT_ENROLLED, 대기면 WAITING, 취소면 CANCELLED, 이미 수료면 ALREADY_COMPLETED로 실패 처리하되 나머지는 계속 처리한다. 결과는 요청
 * 순서를 따르고 저장은 변경 감지에 맡긴다.
 *
 * <p>같은 임직원의 신청이 여러 건인 경우는 FR-06(CANCELLED) 이후에만 생긴다(취소 건과 재신청 건). 그때 최신 신청을 쓰도록 ID가 큰 쪽을 남긴다. 알 수
 * 없는 그 밖의 상태 분기(NOT_ENROLLED)는 지금 enum으로는 도달하지 않는 방어 코드다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RosterService {

  private final CourseSessionRepository courseSessionRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final Clock clock;

  public RosterResponse getRoster(Long sessionId) {
    CourseSession session =
        courseSessionRepository
            .findById(sessionId)
            .orElseThrow(() -> new SessionNotFoundException(sessionId));
    String courseTitle = session.getCourse().getTitle();

    List<RosterEntry> confirmed = new ArrayList<>();
    List<RosterEntry> waiting = new ArrayList<>();
    for (RosterRow row : enrollmentRepository.findRosterRows(sessionId)) {
      if (row.getStatus() == EnrollmentStatus.WAITING) {
        waiting.add(RosterEntry.waiting(row, waiting.size() + 1));
      } else {
        confirmed.add(RosterEntry.confirmed(row));
      }
    }
    return RosterResponse.of(
        session.getId(),
        courseTitle,
        session.getStartDate(),
        session.getCapacity(),
        confirmed,
        waiting);
  }

  @Transactional
  public CompletionResponse complete(Long sessionId, List<Long> employeeIds) {
    CourseSession session =
        courseSessionRepository
            .findByIdForUpdate(sessionId)
            .orElseThrow(() -> new SessionNotFoundException(sessionId));
    LocalDate today = LocalDate.now(clock.withZone(BusinessTime.ZONE));
    if (today.isBefore(session.getStartDate())) {
      throw new SessionNotStartedException(sessionId);
    }

    LinkedHashSet<Long> requested = new LinkedHashSet<>(employeeIds);
    Map<Long, Enrollment> byEmployee =
        enrollmentRepository.findBySessionIdAndEmployeeIdIn(sessionId, requested).stream()
            .collect(
                Collectors.toMap(
                    Enrollment::getEmployeeId,
                    Function.identity(),
                    (a, b) -> a.getId() > b.getId() ? a : b));

    List<Long> completed = new ArrayList<>();
    List<CompletionFailure> failed = new ArrayList<>();
    for (Long employeeId : requested) {
      Enrollment enrollment = byEmployee.get(employeeId);
      if (enrollment == null) {
        failed.add(new CompletionFailure(employeeId, CompletionFailureReason.NOT_ENROLLED));
      } else if (enrollment.getStatus() == EnrollmentStatus.WAITING) {
        failed.add(new CompletionFailure(employeeId, CompletionFailureReason.WAITING));
      } else if (enrollment.getStatus() == EnrollmentStatus.COMPLETED) {
        failed.add(new CompletionFailure(employeeId, CompletionFailureReason.ALREADY_COMPLETED));
      } else if (enrollment.getStatus() == EnrollmentStatus.CANCELLED) {
        failed.add(new CompletionFailure(employeeId, CompletionFailureReason.CANCELLED));
      } else if (enrollment.getStatus() == EnrollmentStatus.CONFIRMED) {
        enrollment.complete();
        completed.add(employeeId);
      } else {
        failed.add(new CompletionFailure(employeeId, CompletionFailureReason.NOT_ENROLLED));
      }
    }
    return new CompletionResponse(completed, failed);
  }
}
