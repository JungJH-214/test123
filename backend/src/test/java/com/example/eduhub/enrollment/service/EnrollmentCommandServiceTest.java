package com.example.eduhub.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.eduhub.course.domain.Course;
import com.example.eduhub.course.domain.CourseSession;
import com.example.eduhub.course.exception.SessionClosedException;
import com.example.eduhub.course.exception.SessionNotFoundException;
import com.example.eduhub.course.repository.CourseSessionRepository;
import com.example.eduhub.enrollment.domain.Enrollment;
import com.example.eduhub.enrollment.domain.EnrollmentStatus;
import com.example.eduhub.enrollment.dto.EnrollmentResponse;
import com.example.eduhub.enrollment.exception.DuplicateEnrollmentException;
import com.example.eduhub.enrollment.exception.SessionFullException;
import com.example.eduhub.enrollment.repository.EnrollmentRepository;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
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
  @InjectMocks private EnrollmentCommandService service;

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
    given(enrollmentRepository.existsBySessionIdAndEmployeeId(SESSION_ID, EMPLOYEE_ID))
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

  private CourseSession givenSession(int capacity) {
    Course course = Course.create("Java 기초", "개발", "입문");
    CourseSession session = CourseSession.create(course, LocalDate.of(2026, 12, 1), "본사", capacity);
    ReflectionTestUtils.setField(session, "id", SESSION_ID);
    given(courseSessionRepository.findByIdForUpdate(SESSION_ID)).willReturn(Optional.of(session));
    return session;
  }

  private void givenCounts(long confirmed, long waiting) {
    given(enrollmentRepository.countBySessionIdAndStatus(SESSION_ID, EnrollmentStatus.CONFIRMED))
        .willReturn(confirmed);
    lenient()
        .when(enrollmentRepository.countBySessionIdAndStatus(SESSION_ID, EnrollmentStatus.WAITING))
        .thenReturn(waiting);
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
