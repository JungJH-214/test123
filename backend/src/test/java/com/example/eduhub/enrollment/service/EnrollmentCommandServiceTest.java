package com.example.eduhub.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.eduhub.course.domain.Course;
import com.example.eduhub.course.domain.CourseSession;
import com.example.eduhub.course.exception.SessionClosedException;
import com.example.eduhub.course.exception.SessionNotFoundException;
import com.example.eduhub.course.repository.CourseSessionRepository;
import com.example.eduhub.enrollment.domain.Enrollment;
import com.example.eduhub.enrollment.domain.EnrollmentStatus;
import com.example.eduhub.enrollment.dto.EnrollmentResponse;
import com.example.eduhub.enrollment.exception.CancelDeadlinePassedException;
import com.example.eduhub.enrollment.exception.DuplicateEnrollmentException;
import com.example.eduhub.enrollment.exception.EnrollmentAlreadyCancelledException;
import com.example.eduhub.enrollment.exception.EnrollmentNotFoundException;
import com.example.eduhub.enrollment.exception.SessionFullException;
import com.example.eduhub.enrollment.repository.EnrollmentRepository;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EnrollmentCommandServiceTest {

  private static final Long SESSION_ID = 1L;
  private static final Long EMPLOYEE_ID = 7L;

  @Mock private CourseSessionRepository courseSessionRepository;
  @Mock private EnrollmentRepository enrollmentRepository;
  private EnrollmentCommandService service;

  @BeforeEach
  void setUp() {
    service = serviceAt("2026-11-27T00:00:00Z");
  }

  private EnrollmentCommandService serviceAt(String instant) {
    return new EnrollmentCommandService(
        courseSessionRepository,
        enrollmentRepository,
        Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
  }

  @Test
  void enroll_seatAvailable_confirmed() {
    CourseSession session = givenSession(10);
    givenCounts(9, 0);
    givenSaveReturnsArgument();

    EnrollmentResponse response = service.enroll(SESSION_ID, EMPLOYEE_ID);

    ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
    verify(enrollmentRepository).saveAndFlush(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
    assertThat(captor.getValue().getEmployeeId()).isEqualTo(EMPLOYEE_ID);
    assertThat(captor.getValue().getSession()).isSameAs(session);
    assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
    assertThat(response.getSessionId()).isEqualTo(SESSION_ID);
    assertThat(response.getWaitingNumber()).isNull();
    verify(enrollmentRepository, never())
        .countBySessionIdAndStatus(SESSION_ID, EnrollmentStatus.WAITING);
  }

  @Test
  void enroll_capacityFull_waiting() {
    givenSession(10);
    givenCounts(10, 4);
    givenSaveReturnsArgument();

    EnrollmentResponse response = service.enroll(SESSION_ID, EMPLOYEE_ID);

    assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.WAITING);
    assertThat(response.getWaitingNumber()).isEqualTo(5);
  }

  @Test
  void enroll_capacityFullNoWaiting_waitingNumberOne() {
    givenSession(10);
    givenCounts(10, 0);
    givenSaveReturnsArgument();

    EnrollmentResponse response = service.enroll(SESSION_ID, EMPLOYEE_ID);

    assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.WAITING);
    assertThat(response.getWaitingNumber()).isEqualTo(1);
  }

  @Test
  void enroll_capacityAndWaitingFull_throwsSessionFull() {
    givenSession(10);
    givenCounts(10, 5);

    assertThatThrownBy(() -> service.enroll(SESSION_ID, EMPLOYEE_ID))
        .isInstanceOf(SessionFullException.class);
    verify(enrollmentRepository, never()).saveAndFlush(any());
  }

  @Test
  void enroll_oddCapacity_waitingLimitRoundsDown() {
    givenSession(3);
    givenCounts(3, 1);

    assertThatThrownBy(() -> service.enroll(SESSION_ID, EMPLOYEE_ID))
        .isInstanceOf(SessionFullException.class);
    verify(enrollmentRepository, never()).saveAndFlush(any());
  }

  @Test
  void enroll_confirmedPlusCompletedOneBelowCapacity_confirmed() {
    givenSession(2);
    givenCounts(1, 0);
    givenSaveReturnsArgument();

    EnrollmentResponse response = service.enroll(SESSION_ID, EMPLOYEE_ID);

    assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
    assertThat(response.getWaitingNumber()).isNull();
  }

  @Test
  void enroll_confirmedPlusCompletedFillCapacity_waiting() {
    givenSession(2);
    // 확정 1명 + 수료 1명 = 합계 2명이 정원을 채운다 (D-05)
    givenCounts(2, 0);
    givenSaveReturnsArgument();

    EnrollmentResponse response = service.enroll(SESSION_ID, EMPLOYEE_ID);

    assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.WAITING);
    assertThat(response.getWaitingNumber()).isEqualTo(1);
  }

  @Test
  void enroll_unknownSession_throwsNotFound() {
    given(courseSessionRepository.findByIdForUpdate(SESSION_ID)).willReturn(Optional.empty());

    assertThatThrownBy(() -> service.enroll(SESSION_ID, EMPLOYEE_ID))
        .isInstanceOf(SessionNotFoundException.class);
    verify(enrollmentRepository, never()).saveAndFlush(any());
  }

  @Test
  void enroll_closedSession_throwsSessionClosed() {
    CourseSession session = givenSession(10);
    session.close();

    assertThatThrownBy(() -> service.enroll(SESSION_ID, EMPLOYEE_ID))
        .isInstanceOf(SessionClosedException.class);
    verify(enrollmentRepository, never()).saveAndFlush(any());
  }

  @Test
  void enroll_alreadyEnrolled_throwsDuplicate() {
    givenSession(10);
    given(
            enrollmentRepository.existsBySessionIdAndEmployeeIdAndStatusNot(
                SESSION_ID, EMPLOYEE_ID, EnrollmentStatus.CANCELLED))
        .willReturn(true);

    assertThatThrownBy(() -> service.enroll(SESSION_ID, EMPLOYEE_ID))
        .isInstanceOf(DuplicateEnrollmentException.class);
    verify(enrollmentRepository, never()).saveAndFlush(any());
  }

  @Test
  void enroll_uniqueViolationOnSave_throwsDuplicate() {
    givenSession(10);
    givenCounts(0, 0);
    given(enrollmentRepository.saveAndFlush(any(Enrollment.class)))
        .willThrow(violation("ux_enrollment_session_employee_active"));

    assertThatThrownBy(() -> service.enroll(SESSION_ID, EMPLOYEE_ID))
        .isInstanceOf(DuplicateEnrollmentException.class);
  }

  @Test
  void enroll_uniqueViolationWithoutConstraintName_sqlState23505_throwsDuplicate() {
    givenSession(10);
    givenCounts(0, 0);
    given(enrollmentRepository.saveAndFlush(any(Enrollment.class)))
        .willThrow(
            new DataIntegrityViolationException(
                "dup",
                new ConstraintViolationException(
                    "dup", new SQLException("중복된 키 값", "23505"), null)));

    assertThatThrownBy(() -> service.enroll(SESSION_ID, EMPLOYEE_ID))
        .isInstanceOf(DuplicateEnrollmentException.class);
  }

  @Test
  void enroll_otherIntegrityViolation_rethrows() {
    givenSession(10);
    givenCounts(0, 0);
    given(enrollmentRepository.saveAndFlush(any(Enrollment.class)))
        .willThrow(violation("fk_enrollment_session"));

    assertThatThrownBy(() -> service.enroll(SESSION_ID, EMPLOYEE_ID))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void enroll_usesLockingQuery() {
    givenSession(10);
    givenCounts(0, 0);
    givenSaveReturnsArgument();

    service.enroll(SESSION_ID, EMPLOYEE_ID);

    verify(courseSessionRepository).findByIdForUpdate(SESSION_ID);
    verify(courseSessionRepository, never()).findById(any());
  }

  // ---- FR-06 취소·승급 ----

  private static final Long ENROLLMENT_ID = 100L;

  @Test
  void cancel_notOwnedOrMissing_throwsNotFound() {
    given(enrollmentRepository.findSessionIdByIdAndEmployeeId(ENROLLMENT_ID, EMPLOYEE_ID))
        .willReturn(Optional.empty());

    assertThatThrownBy(() -> service.cancel(ENROLLMENT_ID, EMPLOYEE_ID))
        .isInstanceOf(EnrollmentNotFoundException.class);
    verify(courseSessionRepository, never()).findByIdForUpdate(any());
    verify(enrollmentRepository, never()).findByIdAndEmployeeId(any(), any());
  }

  @Test
  void cancel_confirmedWithWaiting_promotesFirstWaiting() {
    CourseSession session = givenSession(10);
    Enrollment mine = givenEnrollment(session, EnrollmentStatus.CONFIRMED);
    Enrollment waiting = Enrollment.create(session, 8L, EnrollmentStatus.WAITING);
    givenSeatsTaken(9L);
    given(
            enrollmentRepository.findFirstBySessionIdAndStatusOrderByIdAsc(
                SESSION_ID, EnrollmentStatus.WAITING))
        .willReturn(Optional.of(waiting));

    EnrollmentResponse response = service.cancel(ENROLLMENT_ID, EMPLOYEE_ID);

    assertThat(mine.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
    assertThat(waiting.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
    assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
    assertThat(response.getWaitingNumber()).isNull();
    verify(enrollmentRepository, times(1))
        .findFirstBySessionIdAndStatusOrderByIdAsc(SESSION_ID, EnrollmentStatus.WAITING);
    verify(enrollmentRepository, times(2)).flush(); // 취소 직후 1회 + 승급 1회
    // 락 전에는 회차 ID만 읽고, 락 뒤에 신청 엔티티를 읽는다
    InOrder order = inOrder(enrollmentRepository, courseSessionRepository);
    order.verify(enrollmentRepository).findSessionIdByIdAndEmployeeId(ENROLLMENT_ID, EMPLOYEE_ID);
    order.verify(courseSessionRepository).findByIdForUpdate(SESSION_ID);
    order.verify(enrollmentRepository).findByIdAndEmployeeId(ENROLLMENT_ID, EMPLOYEE_ID);
    verify(courseSessionRepository, never()).findById(any());
  }

  @Test
  void cancel_confirmedNoWaiting_noPromotion() {
    CourseSession session = givenSession(10);
    Enrollment mine = givenEnrollment(session, EnrollmentStatus.CONFIRMED);
    givenSeatsTaken(9L);
    given(
            enrollmentRepository.findFirstBySessionIdAndStatusOrderByIdAsc(
                SESSION_ID, EnrollmentStatus.WAITING))
        .willReturn(Optional.empty());

    service.cancel(ENROLLMENT_ID, EMPLOYEE_ID);

    assertThat(mine.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
    verify(enrollmentRepository, times(1)).flush(); // 취소 직후만, 승급 없음
  }

  @Test
  void cancel_waiting_countsButNoPromotionWhenFull() {
    CourseSession session = givenSession(10);
    Enrollment mine = givenEnrollment(session, EnrollmentStatus.WAITING);
    givenSeatsTaken(10L);

    service.cancel(ENROLLMENT_ID, EMPLOYEE_ID);

    assertThat(mine.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
    verify(enrollmentRepository)
        .countBySessionIdAndStatusIn(eq(SESSION_ID), argThat(isSeatStatuses()));
    verify(enrollmentRepository, never()).findFirstBySessionIdAndStatusOrderByIdAsc(any(), any());
    verify(enrollmentRepository, times(1)).flush(); // 취소 직후만, 승급 없음
  }

  @Test
  void cancel_waitingWithFreeSeat_promotesFirstRemainingWaiting() {
    // 정원 7, 확정 6, 대기 3 -> 대기 1명 취소 -> 남은 대기 1순위 확정
    CourseSession session = givenSession(7);
    Enrollment mine = givenEnrollment(session, EnrollmentStatus.WAITING);
    Enrollment first = Enrollment.create(session, 31L, EnrollmentStatus.WAITING);
    givenSeatsTaken(6L);
    given(
            enrollmentRepository.findFirstBySessionIdAndStatusOrderByIdAsc(
                SESSION_ID, EnrollmentStatus.WAITING))
        .willReturn(Optional.of(first));

    service.cancel(ENROLLMENT_ID, EMPLOYEE_ID);

    assertThat(mine.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
    assertThat(first.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
    verify(enrollmentRepository, times(1))
        .findFirstBySessionIdAndStatusOrderByIdAsc(SESSION_ID, EnrollmentStatus.WAITING);
    verify(enrollmentRepository, times(2)).flush(); // 취소 직후 1회 + 승급 1회
  }

  @Test
  void cancel_dayBeforeStart_succeeds() {
    // 시작일 2026-12-01, 서울 2026-11-30 23:59:59
    service = serviceAt("2026-11-30T14:59:59Z");
    CourseSession session = givenSession(10);
    Enrollment mine = givenEnrollment(session, EnrollmentStatus.WAITING);

    service.cancel(ENROLLMENT_ID, EMPLOYEE_ID);

    assertThat(mine.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
  }

  @Test
  void cancel_onStartDate_throwsDeadlinePassed_noStateChange() {
    // 서울 2026-12-01 00:00:00
    service = serviceAt("2026-11-30T15:00:00Z");
    CourseSession session = givenSession(10);
    Enrollment mine = givenEnrollment(session, EnrollmentStatus.CONFIRMED);

    assertThatThrownBy(() -> service.cancel(ENROLLMENT_ID, EMPLOYEE_ID))
        .isInstanceOf(CancelDeadlinePassedException.class);
    assertThat(mine.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
    verify(enrollmentRepository, never()).findFirstBySessionIdAndStatusOrderByIdAsc(any(), any());
  }

  @Test
  void cancel_usesSeoulDate() {
    // UTC 2026-12-09T15:30Z = 서울 12-10 00:30
    service = serviceAt("2026-12-09T15:30:00Z");
    CourseSession session = givenSession(10, LocalDate.of(2026, 12, 11));
    Enrollment mine = givenEnrollment(session, EnrollmentStatus.WAITING);

    service.cancel(ENROLLMENT_ID, EMPLOYEE_ID);

    assertThat(mine.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
  }

  @Test
  void cancel_usesSeoulDate_startOnSeoulToday_throwsDeadlinePassed() {
    service = serviceAt("2026-12-09T15:30:00Z");
    CourseSession session = givenSession(10, LocalDate.of(2026, 12, 10));
    Enrollment mine = givenEnrollment(session, EnrollmentStatus.WAITING);

    assertThatThrownBy(() -> service.cancel(ENROLLMENT_ID, EMPLOYEE_ID))
        .isInstanceOf(CancelDeadlinePassedException.class);
    assertThat(mine.getStatus()).isEqualTo(EnrollmentStatus.WAITING);
  }

  @Test
  void cancel_closedSession_throwsSessionClosed() {
    CourseSession session = givenSession(10);
    session.close();
    Enrollment mine = givenEnrollment(session, EnrollmentStatus.CONFIRMED);

    assertThatThrownBy(() -> service.cancel(ENROLLMENT_ID, EMPLOYEE_ID))
        .isInstanceOf(SessionClosedException.class);
    assertThat(mine.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
  }

  @Test
  void cancel_alreadyCancelled_throwsAlreadyCancelled() {
    CourseSession session = givenSession(10);
    givenEnrollment(session, EnrollmentStatus.CANCELLED);

    assertThatThrownBy(() -> service.cancel(ENROLLMENT_ID, EMPLOYEE_ID))
        .isInstanceOf(EnrollmentAlreadyCancelledException.class);
    verify(enrollmentRepository, never()).findFirstBySessionIdAndStatusOrderByIdAsc(any(), any());
  }

  @Test
  void cancel_alreadyCancelledAndDeadlinePassed_throwsAlreadyCancelled() {
    service = serviceAt("2026-11-30T15:00:00Z");
    CourseSession session = givenSession(10);
    Enrollment mine = givenEnrollment(session, EnrollmentStatus.CANCELLED);

    assertThatThrownBy(() -> service.cancel(ENROLLMENT_ID, EMPLOYEE_ID))
        .isInstanceOf(EnrollmentAlreadyCancelledException.class);
    assertThat(mine.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
  }

  @Test
  void cancel_closedSessionAndAlreadyCancelled_throwsSessionClosed() {
    CourseSession session = givenSession(10);
    session.close();
    Enrollment mine = givenEnrollment(session, EnrollmentStatus.CANCELLED);

    assertThatThrownBy(() -> service.cancel(ENROLLMENT_ID, EMPLOYEE_ID))
        .isInstanceOf(SessionClosedException.class);
    assertThat(mine.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
  }

  @Test
  void cancel_closedSessionAndDeadlinePassed_throwsSessionClosed() {
    service = serviceAt("2026-11-30T15:00:00Z");
    CourseSession session = givenSession(10);
    session.close();
    Enrollment mine = givenEnrollment(session, EnrollmentStatus.CONFIRMED);

    assertThatThrownBy(() -> service.cancel(ENROLLMENT_ID, EMPLOYEE_ID))
        .isInstanceOf(SessionClosedException.class);
    assertThat(mine.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
  }

  @Test
  void cancel_confirmedOverCapacity_noPromotion() {
    CourseSession session = givenSession(8);
    Enrollment mine = givenEnrollment(session, EnrollmentStatus.CONFIRMED);
    // 취소 뒤에도 확정 9명(정원 8)
    givenSeatsTaken(9L);

    service.cancel(ENROLLMENT_ID, EMPLOYEE_ID);

    assertThat(mine.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
    verify(enrollmentRepository, never()).findFirstBySessionIdAndStatusOrderByIdAsc(any(), any());
  }

  @Test
  void enroll_freeSeatsWithWaiting_promotesWaitingFirst() {
    CourseSession session = givenSession(10);
    Enrollment w1 = Enrollment.create(session, 21L, EnrollmentStatus.WAITING);
    Enrollment w2 = Enrollment.create(session, 22L, EnrollmentStatus.WAITING);
    // 승급 전 확정 8, 승급(flush) 뒤 판정 시점에는 10
    givenSeatsTaken(8L, 10L);
    given(
            enrollmentRepository.findFirstBySessionIdAndStatusOrderByIdAsc(
                SESSION_ID, EnrollmentStatus.WAITING))
        .willReturn(Optional.of(w1), Optional.of(w2));
    given(enrollmentRepository.countBySessionIdAndStatus(SESSION_ID, EnrollmentStatus.WAITING))
        .willReturn(0L);
    givenSaveReturnsArgument();
    // 각 flush 시점에 승급(상태 전이)이 몇 명까지 끝났는지 기록한다
    List<EnrollmentStatus> atFlush = new ArrayList<>();
    doAnswer(
            invocation -> {
              atFlush.add(w1.getStatus());
              atFlush.add(w2.getStatus());
              return null;
            })
        .when(enrollmentRepository)
        .flush();

    EnrollmentResponse response = service.enroll(SESSION_ID, EMPLOYEE_ID);

    assertThat(w1.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
    assertThat(w2.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
    // 1번째 flush: w1만 확정, 2번째 flush: w1·w2 모두 확정
    assertThat(atFlush)
        .containsExactly(
            EnrollmentStatus.CONFIRMED,
            EnrollmentStatus.WAITING,
            EnrollmentStatus.CONFIRMED,
            EnrollmentStatus.CONFIRMED);
    assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.WAITING);
    assertThat(response.getWaitingNumber()).isEqualTo(1);
    verify(enrollmentRepository, times(2)).flush();
    // 첫 확정 집계 -> (승급 조회 -> flush) x 2 -> decide의 두 번째 확정 집계
    InOrder order = inOrder(enrollmentRepository);
    order
        .verify(enrollmentRepository)
        .countBySessionIdAndStatusIn(eq(SESSION_ID), argThat(isSeatStatuses()));
    order
        .verify(enrollmentRepository)
        .findFirstBySessionIdAndStatusOrderByIdAsc(SESSION_ID, EnrollmentStatus.WAITING);
    order.verify(enrollmentRepository).flush();
    order
        .verify(enrollmentRepository)
        .findFirstBySessionIdAndStatusOrderByIdAsc(SESSION_ID, EnrollmentStatus.WAITING);
    order.verify(enrollmentRepository).flush();
    order
        .verify(enrollmentRepository)
        .countBySessionIdAndStatusIn(eq(SESSION_ID), argThat(isSeatStatuses()));
  }

  @Test
  void enroll_seatsFilledByCompleted_noPromotionAndNewApplicantWaits() {
    // 정원 10, 확정 7 + 수료 3 = 10 (D-05). 대기 3명이 있어도 승급하지 않는다.
    givenSession(10);
    givenSeatsTaken(10L);
    given(enrollmentRepository.countBySessionIdAndStatus(SESSION_ID, EnrollmentStatus.WAITING))
        .willReturn(3L);
    givenSaveReturnsArgument();

    EnrollmentResponse response = service.enroll(SESSION_ID, EMPLOYEE_ID);

    verify(enrollmentRepository, never()).findFirstBySessionIdAndStatusOrderByIdAsc(any(), any());
    verify(enrollmentRepository, never()).flush();
    assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.WAITING);
    assertThat(response.getWaitingNumber()).isEqualTo(4);
  }

  @Test
  void enroll_seatsFilledByCompletedAndWaitingFull_throwsSessionFull_noPromotion() {
    givenSession(10);
    givenSeatsTaken(10L);
    given(enrollmentRepository.countBySessionIdAndStatus(SESSION_ID, EnrollmentStatus.WAITING))
        .willReturn(5L);

    assertThatThrownBy(() -> service.enroll(SESSION_ID, EMPLOYEE_ID))
        .isInstanceOf(SessionFullException.class);
    verify(enrollmentRepository, never()).findFirstBySessionIdAndStatusOrderByIdAsc(any(), any());
    verify(enrollmentRepository, never()).saveAndFlush(any());
  }

  @Test
  void enroll_afterCancel_notDuplicate() {
    givenSession(10);
    givenCounts(0, 0);
    givenSaveReturnsArgument();

    EnrollmentResponse response = service.enroll(SESSION_ID, EMPLOYEE_ID);

    assertThat(response.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
    verify(enrollmentRepository)
        .existsBySessionIdAndEmployeeIdAndStatusNot(
            SESSION_ID, EMPLOYEE_ID, EnrollmentStatus.CANCELLED);
    verify(enrollmentRepository).saveAndFlush(any(Enrollment.class));
  }

  private Enrollment givenEnrollment(CourseSession session, EnrollmentStatus status) {
    Enrollment enrollment = Enrollment.create(session, EMPLOYEE_ID, status);
    ReflectionTestUtils.setField(enrollment, "id", ENROLLMENT_ID);
    given(enrollmentRepository.findSessionIdByIdAndEmployeeId(ENROLLMENT_ID, EMPLOYEE_ID))
        .willReturn(Optional.of(SESSION_ID));
    given(enrollmentRepository.findByIdAndEmployeeId(ENROLLMENT_ID, EMPLOYEE_ID))
        .willReturn(Optional.of(enrollment));
    return enrollment;
  }

  private CourseSession givenSession(int capacity) {
    return givenSession(capacity, LocalDate.of(2026, 12, 1));
  }

  private CourseSession givenSession(int capacity, LocalDate startDate) {
    Course course = Course.create("Java 기초", "개발", "입문");
    CourseSession session = CourseSession.create(course, startDate, "본사", capacity);
    ReflectionTestUtils.setField(session, "id", SESSION_ID);
    given(courseSessionRepository.findByIdForUpdate(SESSION_ID)).willReturn(Optional.of(session));
    return session;
  }

  private void givenSeatsTaken(Long first, Long... rest) {
    given(
            enrollmentRepository.countBySessionIdAndStatusIn(
                eq(SESSION_ID), argThat(isSeatStatuses())))
        .willReturn(first, rest);
  }

  private void givenCounts(long confirmed, long waiting) {
    given(
            enrollmentRepository.countBySessionIdAndStatusIn(
                eq(SESSION_ID), argThat(isSeatStatuses())))
        .willReturn(confirmed);
    lenient()
        .when(enrollmentRepository.countBySessionIdAndStatus(SESSION_ID, EnrollmentStatus.WAITING))
        .thenReturn(waiting);
  }

  /** CONFIRMED와 COMPLETED를 순서와 컬렉션 타입에 상관없이 정확히 포함하는지 본다. */
  private static ArgumentMatcher<Collection<EnrollmentStatus>> isSeatStatuses() {
    return statuses ->
        statuses != null
            && statuses.size() == 2
            && new HashSet<>(statuses)
                .equals(EnumSet.of(EnrollmentStatus.CONFIRMED, EnrollmentStatus.COMPLETED));
  }

  private void givenSaveReturnsArgument() {
    given(enrollmentRepository.saveAndFlush(any(Enrollment.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
  }

  private static DataIntegrityViolationException violation(String constraintName) {
    return new DataIntegrityViolationException(
        "dup", new ConstraintViolationException("dup", null, constraintName));
  }
}
