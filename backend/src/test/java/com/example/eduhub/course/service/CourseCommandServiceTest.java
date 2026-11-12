package com.example.eduhub.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.eduhub.course.domain.Course;
import com.example.eduhub.course.domain.CourseSession;
import com.example.eduhub.course.domain.CourseStatus;
import com.example.eduhub.course.domain.SessionStatus;
import com.example.eduhub.course.dto.CourseDetailResponse;
import com.example.eduhub.course.dto.CourseRequest;
import com.example.eduhub.course.dto.SessionRequest;
import com.example.eduhub.course.dto.SessionResponse;
import com.example.eduhub.course.exception.CourseClosedException;
import com.example.eduhub.course.exception.CourseNotFoundException;
import com.example.eduhub.course.exception.SessionClosedException;
import com.example.eduhub.course.exception.SessionNotFoundException;
import com.example.eduhub.course.repository.CourseRepository;
import com.example.eduhub.course.repository.CourseSessionRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CourseCommandServiceTest {

  private static final LocalDate DATE = LocalDate.of(2026, 12, 1);

  @Mock private CourseRepository courseRepository;
  @Mock private CourseSessionRepository courseSessionRepository;
  @InjectMocks private CourseCommandService service;

  // ---- 과정 ----

  @Test
  void createCourse_savesOpenCourse() {
    given(courseRepository.save(any(Course.class)))
        .willAnswer(invocation -> withId(invocation.getArgument(0), 1L));

    CourseDetailResponse response = service.createCourse(new CourseRequest("Java 기초", "개발", "입문"));

    assertThat(response.getId()).isEqualTo(1L);
    assertThat(response.getTitle()).isEqualTo("Java 기초");
    assertThat(response.getCategory()).isEqualTo("개발");
    assertThat(response.getDescription()).isEqualTo("입문");
    assertThat(response.getStatus()).isEqualTo(CourseStatus.OPEN);
    assertThat(response.getSessions()).isEmpty();
  }

  @Test
  void updateCourse_changesValues() {
    Course course = course(1L);
    given(courseRepository.findById(1L)).willReturn(Optional.of(course));
    given(courseSessionRepository.findByCourseIdOrderByStartDateAsc(1L))
        .willReturn(List.of(session(course, 10L)));

    CourseDetailResponse response =
        service.updateCourse(1L, new CourseRequest("Java 심화", "공통", null));

    assertThat(course.getTitle()).isEqualTo("Java 심화");
    assertThat(course.getCategory()).isEqualTo("공통");
    assertThat(course.getDescription()).isNull();
    assertThat(response.getTitle()).isEqualTo("Java 심화");
    assertThat(response.getSessions()).hasSize(1);
  }

  @Test
  void updateCourse_notFound() {
    given(courseRepository.findById(9L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> service.updateCourse(9L, new CourseRequest("a", "b", null)))
        .isInstanceOf(CourseNotFoundException.class);
  }

  @Test
  void updateCourse_closedCourse_conflict() {
    Course course = course(1L);
    course.close();
    given(courseRepository.findById(1L)).willReturn(Optional.of(course));

    assertThatThrownBy(() -> service.updateCourse(1L, new CourseRequest("새 제목", "개발", null)))
        .isInstanceOf(CourseClosedException.class);
    assertThat(course.getTitle()).isEqualTo("Java 기초");
  }

  @Test
  void closeCourse_closesCourseAndAllSessions() {
    Course course = course(1L);
    CourseSession first = session(course, 10L);
    CourseSession second = session(course, 11L);
    given(courseRepository.findById(1L)).willReturn(Optional.of(course));
    given(courseSessionRepository.findByCourseIdOrderByStartDateAsc(1L))
        .willReturn(List.of(first, second));

    CourseDetailResponse response = service.closeCourse(1L);

    assertThat(course.getStatus()).isEqualTo(CourseStatus.CLOSED);
    assertThat(first.getStatus()).isEqualTo(SessionStatus.CLOSED);
    assertThat(second.getStatus()).isEqualTo(SessionStatus.CLOSED);
    assertThat(response.getStatus()).isEqualTo(CourseStatus.CLOSED);
    assertThat(response.getSessions())
        .extracting(SessionResponse::getStatus)
        .containsOnly(SessionStatus.CLOSED);
  }

  @Test
  void closeCourse_alreadyClosed_isIdempotent() {
    Course course = course(1L);
    course.close();
    given(courseRepository.findById(1L)).willReturn(Optional.of(course));
    given(courseSessionRepository.findByCourseIdOrderByStartDateAsc(1L)).willReturn(List.of());

    CourseDetailResponse response = service.closeCourse(1L);

    assertThat(response.getStatus()).isEqualTo(CourseStatus.CLOSED);
  }

  @Test
  void closeCourse_notFound() {
    given(courseRepository.findById(9L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> service.closeCourse(9L)).isInstanceOf(CourseNotFoundException.class);
  }

  // ---- 회차 ----

  @Test
  void createSession_savesOpenSession() {
    Course course = course(1L);
    given(courseRepository.findById(1L)).willReturn(Optional.of(course));
    given(courseSessionRepository.save(any(CourseSession.class)))
        .willAnswer(invocation -> withId(invocation.getArgument(0), 10L));

    SessionResponse response = service.createSession(1L, new SessionRequest(DATE, "본사 3층", 20));

    assertThat(response.getId()).isEqualTo(10L);
    assertThat(response.getStartDate()).isEqualTo(DATE);
    assertThat(response.getPlace()).isEqualTo("본사 3층");
    assertThat(response.getCapacity()).isEqualTo(20);
    assertThat(response.getStatus()).isEqualTo(SessionStatus.OPEN);
  }

  @Test
  void createSession_pastDate_allowed() {
    given(courseRepository.findById(1L)).willReturn(Optional.of(course(1L)));
    given(courseSessionRepository.save(any(CourseSession.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    LocalDate past = LocalDate.of(2020, 1, 1);

    SessionResponse response = service.createSession(1L, new SessionRequest(past, "본사", 5));

    assertThat(response.getStartDate()).isEqualTo(past);
  }

  @Test
  void createSession_courseNotFound() {
    given(courseRepository.findById(9L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> service.createSession(9L, new SessionRequest(DATE, "본사", 5)))
        .isInstanceOf(CourseNotFoundException.class);
  }

  @Test
  void createSession_closedCourse_conflict() {
    Course course = course(1L);
    course.close();
    given(courseRepository.findById(1L)).willReturn(Optional.of(course));

    assertThatThrownBy(() -> service.createSession(1L, new SessionRequest(DATE, "본사", 5)))
        .isInstanceOf(CourseClosedException.class);
    verify(courseSessionRepository, never()).save(any());
  }

  @Test
  void updateSession_changesValues() {
    CourseSession session = session(course(1L), 10L);
    givenSession(1L, 10L, session);
    LocalDate newDate = DATE.plusDays(7);

    SessionResponse response =
        service.updateSession(1L, 10L, new SessionRequest(newDate, "온라인", 50));

    assertThat(session.getStartDate()).isEqualTo(newDate);
    assertThat(session.getPlace()).isEqualTo("온라인");
    assertThat(session.getCapacity()).isEqualTo(50);
    assertThat(response.getCapacity()).isEqualTo(50);
    assertThat(response.getRemainingSeats()).isEqualTo(50);
  }

  @Test
  void updateSession_closedSession_conflict() {
    CourseSession session = session(course(1L), 10L);
    session.close();
    givenSession(1L, 10L, session);

    assertThatThrownBy(() -> service.updateSession(1L, 10L, new SessionRequest(DATE, "온라인", 50)))
        .isInstanceOf(SessionClosedException.class);
    assertThat(session.getCapacity()).isEqualTo(20);
  }

  @Test
  void updateSession_courseNotFound() {
    given(courseRepository.existsById(9L)).willReturn(false);

    assertThatThrownBy(() -> service.updateSession(9L, 10L, new SessionRequest(DATE, "a", 1)))
        .isInstanceOf(CourseNotFoundException.class);
  }

  @Test
  void updateSession_sessionOfOtherCourse_notFound() {
    given(courseRepository.existsById(1L)).willReturn(true);
    given(courseSessionRepository.findByIdAndCourseId(99L, 1L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> service.updateSession(1L, 99L, new SessionRequest(DATE, "a", 1)))
        .isInstanceOf(SessionNotFoundException.class);
  }

  @Test
  void closeSession_closes() {
    CourseSession session = session(course(1L), 10L);
    givenSession(1L, 10L, session);

    SessionResponse response = service.closeSession(1L, 10L);

    assertThat(session.getStatus()).isEqualTo(SessionStatus.CLOSED);
    assertThat(response.getStatus()).isEqualTo(SessionStatus.CLOSED);
  }

  @Test
  void closeSession_alreadyClosed_isIdempotent() {
    CourseSession session = session(course(1L), 10L);
    session.close();
    givenSession(1L, 10L, session);

    assertThat(service.closeSession(1L, 10L).getStatus()).isEqualTo(SessionStatus.CLOSED);
  }

  @Test
  void closeSession_notFound() {
    given(courseRepository.existsById(1L)).willReturn(true);
    given(courseSessionRepository.findByIdAndCourseId(99L, 1L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> service.closeSession(1L, 99L))
        .isInstanceOf(SessionNotFoundException.class);
  }

  private void givenSession(Long courseId, Long sessionId, CourseSession session) {
    given(courseRepository.existsById(courseId)).willReturn(true);
    given(courseSessionRepository.findByIdAndCourseId(sessionId, courseId))
        .willReturn(Optional.of(session));
  }

  private static Course course(Long id) {
    return withId(Course.create("Java 기초", "개발", "입문"), id);
  }

  private static CourseSession session(Course course, Long id) {
    return withId(CourseSession.create(course, DATE, "본사 3층", 20), id);
  }

  private static <T> T withId(T entity, Long id) {
    ReflectionTestUtils.setField(entity, "id", id);
    return entity;
  }
}
