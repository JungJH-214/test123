package com.example.eduhub.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.eduhub.course.domain.Course;
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
import com.example.eduhub.enrollment.dto.RosterRow;
import com.example.eduhub.enrollment.exception.SessionNotStartedException;
import com.example.eduhub.enrollment.repository.EnrollmentRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RosterServiceTest {

  private static final Long SESSION_ID = 1L;

  @Mock private CourseSessionRepository courseSessionRepository;
  @Mock private EnrollmentRepository enrollmentRepository;
  private RosterService service;

  @BeforeEach
  void setUp() {
    useClock("2026-12-01T00:00:00Z");
  }

  private void useClock(String utcInstant) {
    Clock clock = Clock.fixed(Instant.parse(utcInstant), ZoneOffset.UTC);
    service = new RosterService(courseSessionRepository, enrollmentRepository, clock);
  }

  @Test
  void getRoster_splitsConfirmedAndWaitingAndNumbersWaiting() {
    givenSession(5);
    given(enrollmentRepository.findRosterRows(SESSION_ID))
        .willReturn(
            Arrays.asList(
                row(10, 1, EnrollmentStatus.CONFIRMED),
                row(11, 2, EnrollmentStatus.WAITING),
                row(12, 3, EnrollmentStatus.COMPLETED),
                row(13, 4, EnrollmentStatus.WAITING),
                row(14, 5, EnrollmentStatus.WAITING)));

    RosterResponse response = service.getRoster(SESSION_ID);

    assertThat(response.getSessionId()).isEqualTo(SESSION_ID);
    assertThat(response.getCourseTitle()).isEqualTo("Java 기초");
    assertThat(response.getStartDate()).isEqualTo(LocalDate.of(2026, 12, 1));
    assertThat(response.getCapacity()).isEqualTo(5);
    assertThat(response.getConfirmed())
        .extracting(RosterEntry::getEnrollmentId)
        .containsExactly(10L, 12L);
    assertThat(response.getConfirmed())
        .extracting(RosterEntry::getStatus)
        .containsExactly(EnrollmentStatus.CONFIRMED, EnrollmentStatus.COMPLETED);
    assertThat(response.getConfirmed())
        .extracting(RosterEntry::getWaitingNumber)
        .containsOnlyNulls();
    assertThat(response.getWaiting())
        .extracting(RosterEntry::getEnrollmentId)
        .containsExactly(11L, 13L, 14L);
    assertThat(response.getWaiting())
        .extracting(RosterEntry::getWaitingNumber)
        .containsExactly(1, 2, 3);
  }

  @Test
  void getRoster_mapsEmployeeFieldsAndKeepsNulls() {
    givenSession(5);
    given(enrollmentRepository.findRosterRows(SESSION_ID))
        .willReturn(
            Arrays.asList(
                new RosterRow(
                    10L,
                    1L,
                    "E-001",
                    "김하나",
                    EnrollmentStatus.CONFIRMED,
                    LocalDateTime.of(2026, 11, 1, 9, 0)),
                new RosterRow(
                    11L,
                    2L,
                    null,
                    null,
                    EnrollmentStatus.CONFIRMED,
                    LocalDateTime.of(2026, 11, 1, 9, 1))));

    RosterResponse response = service.getRoster(SESSION_ID);

    RosterEntry first = response.getConfirmed().get(0);
    assertThat(first.getEmployeeId()).isEqualTo(1L);
    assertThat(first.getEmployeeNo()).isEqualTo("E-001");
    assertThat(first.getName()).isEqualTo("김하나");
    assertThat(first.getEnrolledAt()).isEqualTo(LocalDateTime.of(2026, 11, 1, 9, 0));
    RosterEntry second = response.getConfirmed().get(1);
    assertThat(second.getEmployeeNo()).isNull();
    assertThat(second.getName()).isNull();
  }

  @Test
  void getRoster_noEnrollments_returnsEmptyLists() {
    givenSession(5);
    given(enrollmentRepository.findRosterRows(SESSION_ID)).willReturn(Collections.emptyList());

    RosterResponse response = service.getRoster(SESSION_ID);

    assertThat(response.getConfirmed()).isEmpty();
    assertThat(response.getWaiting()).isEmpty();
  }

  @Test
  void getRoster_sessionMissing_throwsNotFound() {
    given(courseSessionRepository.findById(SESSION_ID)).willReturn(Optional.empty());

    assertThatThrownBy(() -> service.getRoster(SESSION_ID))
        .isInstanceOf(SessionNotFoundException.class);
    verifyNoInteractions(enrollmentRepository);
  }

  @Test
  void complete_allConfirmed_completesAllInRequestOrder() {
    givenLockedSession();
    Enrollment e1 = enrollment(1L, EnrollmentStatus.CONFIRMED);
    Enrollment e2 = enrollment(2L, EnrollmentStatus.CONFIRMED);
    given(enrollmentRepository.findBySessionIdAndEmployeeIdIn(eq(SESSION_ID), any()))
        .willReturn(Arrays.asList(e1, e2));

    CompletionResponse response = service.complete(SESSION_ID, Arrays.asList(2L, 1L));

    assertThat(response.getCompleted()).containsExactly(2L, 1L);
    assertThat(response.getFailed()).isEmpty();
    assertThat(e1.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
    assertThat(e2.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
  }

  @Test
  void complete_mixed_returnsPartialFailuresInRequestOrder() {
    givenLockedSession();
    Enrollment ok = enrollment(1L, EnrollmentStatus.CONFIRMED);
    Enrollment waiting = enrollment(3L, EnrollmentStatus.WAITING);
    Enrollment done = enrollment(4L, EnrollmentStatus.COMPLETED);
    given(enrollmentRepository.findBySessionIdAndEmployeeIdIn(eq(SESSION_ID), any()))
        .willReturn(Arrays.asList(done, ok, waiting));

    CompletionResponse response = service.complete(SESSION_ID, Arrays.asList(4L, 2L, 1L, 3L));

    assertThat(response.getCompleted()).containsExactly(1L);
    assertThat(response.getFailed())
        .extracting(CompletionFailure::getEmployeeId, CompletionFailure::getReason)
        .containsExactly(
            tuple(4L, CompletionFailureReason.ALREADY_COMPLETED),
            tuple(2L, CompletionFailureReason.NOT_ENROLLED),
            tuple(3L, CompletionFailureReason.WAITING));
    assertThat(ok.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
    assertThat(waiting.getStatus()).isEqualTo(EnrollmentStatus.WAITING);
    assertThat(done.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
  }

  @Test
  @SuppressWarnings("unchecked")
  void complete_duplicateIds_processedOnceInFirstOrder() {
    givenLockedSession();
    Enrollment e1 = enrollment(1L, EnrollmentStatus.CONFIRMED);
    given(enrollmentRepository.findBySessionIdAndEmployeeIdIn(eq(SESSION_ID), any()))
        .willReturn(Collections.singletonList(e1));

    CompletionResponse response = service.complete(SESSION_ID, Arrays.asList(2L, 1L, 2L, 1L, 1L));

    assertThat(response.getCompleted()).containsExactly(1L);
    assertThat(response.getFailed())
        .extracting(CompletionFailure::getEmployeeId)
        .containsExactly(2L);
    ArgumentCaptor<Collection<Long>> ids = ArgumentCaptor.forClass(Collection.class);
    verify(enrollmentRepository).findBySessionIdAndEmployeeIdIn(eq(SESSION_ID), ids.capture());
    assertThat(ids.getValue()).containsExactly(2L, 1L);
  }

  @Test
  void complete_dayBeforeStart_throwsNotStarted() {
    useClock("2026-11-30T00:00:00Z");
    givenLockedSession();

    assertThatThrownBy(() -> service.complete(SESSION_ID, Collections.singletonList(1L)))
        .isInstanceOf(SessionNotStartedException.class)
        .hasFieldOrPropertyWithValue("code", "SESSION_NOT_STARTED");
    verifyNoInteractions(enrollmentRepository);
  }

  @Test
  void complete_onStartDate_isAllowed() {
    useClock("2026-12-01T03:00:00Z");
    givenLockedSession();
    given(enrollmentRepository.findBySessionIdAndEmployeeIdIn(eq(SESSION_ID), any()))
        .willReturn(Collections.singletonList(enrollment(1L, EnrollmentStatus.CONFIRMED)));

    CompletionResponse response = service.complete(SESSION_ID, Collections.singletonList(1L));

    assertThat(response.getCompleted()).containsExactly(1L);
  }

  @Test
  void complete_kstMidnightBoundary_isAllowed() {
    useClock("2026-11-30T15:00:00Z");
    givenLockedSession();
    given(enrollmentRepository.findBySessionIdAndEmployeeIdIn(eq(SESSION_ID), any()))
        .willReturn(Collections.singletonList(enrollment(1L, EnrollmentStatus.CONFIRMED)));

    CompletionResponse response = service.complete(SESSION_ID, Collections.singletonList(1L));

    assertThat(response.getCompleted()).containsExactly(1L);
  }

  @Test
  void complete_oneSecondBeforeKstMidnight_throwsNotStarted() {
    useClock("2026-11-30T14:59:59Z");
    givenLockedSession();

    assertThatThrownBy(() -> service.complete(SESSION_ID, Collections.singletonList(1L)))
        .isInstanceOf(SessionNotStartedException.class);
    verifyNoInteractions(enrollmentRepository);
  }

  @Test
  void complete_sessionMissing_throwsNotFound() {
    given(courseSessionRepository.findByIdForUpdate(SESSION_ID)).willReturn(Optional.empty());

    assertThatThrownBy(() -> service.complete(SESSION_ID, Collections.singletonList(1L)))
        .isInstanceOf(SessionNotFoundException.class);
    verify(courseSessionRepository, never()).findById(any());
    verifyNoInteractions(enrollmentRepository);
  }

  @Test
  void complete_locksSessionBeforeReadingEnrollments() {
    givenLockedSession();
    given(enrollmentRepository.findBySessionIdAndEmployeeIdIn(eq(SESSION_ID), any()))
        .willReturn(Collections.singletonList(enrollment(1L, EnrollmentStatus.CONFIRMED)));

    service.complete(SESSION_ID, Collections.singletonList(1L));

    InOrder order = inOrder(courseSessionRepository, enrollmentRepository);
    order.verify(courseSessionRepository).findByIdForUpdate(SESSION_ID);
    order.verify(enrollmentRepository).findBySessionIdAndEmployeeIdIn(eq(SESSION_ID), any());
  }

  @Test
  void complete_endedSession_isAllowed() {
    useClock("2027-03-01T00:00:00Z");
    givenLockedSession();
    given(enrollmentRepository.findBySessionIdAndEmployeeIdIn(eq(SESSION_ID), any()))
        .willReturn(Collections.singletonList(enrollment(1L, EnrollmentStatus.CONFIRMED)));

    CompletionResponse response = service.complete(SESSION_ID, Collections.singletonList(1L));

    assertThat(response.getCompleted()).containsExactly(1L);
  }

  @Test
  void complete_twoEnrollmentsForSameEmployee_usesLargerId() {
    givenLockedSession();
    Enrollment older = enrollment(1L, EnrollmentStatus.COMPLETED);
    ReflectionTestUtils.setField(older, "id", 10L);
    Enrollment newer = enrollment(1L, EnrollmentStatus.CONFIRMED);
    ReflectionTestUtils.setField(newer, "id", 20L);
    given(enrollmentRepository.findBySessionIdAndEmployeeIdIn(eq(SESSION_ID), any()))
        .willReturn(Arrays.asList(newer, older));

    CompletionResponse response = service.complete(SESSION_ID, Collections.singletonList(1L));

    assertThat(response.getCompleted()).containsExactly(1L);
    assertThat(newer.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
    assertThat(older.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);

    Enrollment olderConfirmed = enrollment(2L, EnrollmentStatus.CONFIRMED);
    ReflectionTestUtils.setField(olderConfirmed, "id", 30L);
    Enrollment newerWaiting = enrollment(2L, EnrollmentStatus.WAITING);
    ReflectionTestUtils.setField(newerWaiting, "id", 40L);
    given(enrollmentRepository.findBySessionIdAndEmployeeIdIn(eq(SESSION_ID), any()))
        .willReturn(Arrays.asList(newerWaiting, olderConfirmed));
    givenLockedSession();

    CompletionResponse second = service.complete(SESSION_ID, Collections.singletonList(2L));

    assertThat(second.getCompleted()).isEmpty();
    assertThat(second.getFailed())
        .extracting(CompletionFailure::getReason)
        .containsExactly(CompletionFailureReason.WAITING);
    assertThat(olderConfirmed.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
  }

  private void givenLockedSession() {
    CourseSession session = newSession(5);
    given(courseSessionRepository.findByIdForUpdate(SESSION_ID)).willReturn(Optional.of(session));
  }

  private void givenSession(int capacity) {
    CourseSession session = newSession(capacity);
    given(courseSessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));
  }

  private static CourseSession newSession(int capacity) {
    CourseSession session =
        CourseSession.create(
            Course.create("Java 기초", "개발", null), LocalDate.of(2026, 12, 1), "본사", capacity);
    ReflectionTestUtils.setField(session, "id", SESSION_ID);
    return session;
  }

  private static Enrollment enrollment(Long employeeId, EnrollmentStatus status) {
    return Enrollment.create(null, employeeId, status);
  }

  private static RosterRow row(long enrollmentId, long employeeId, EnrollmentStatus status) {
    return new RosterRow(
        enrollmentId,
        employeeId,
        "E-" + employeeId,
        "이름" + employeeId,
        status,
        LocalDateTime.of(2026, 11, 1, 9, 0));
  }
}
