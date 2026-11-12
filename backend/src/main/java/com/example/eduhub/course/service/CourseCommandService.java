package com.example.eduhub.course.service;

import com.example.eduhub.course.domain.Course;
import com.example.eduhub.course.domain.CourseSession;
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 과정·회차 등록, 수정, 종료 (FR-03).
 *
 * <p>규칙: 종료된 과정·회차는 수정할 수 없다(409). 종료된 과정에는 회차를 추가할 수 없다. 과정을 종료하면 회차도 모두 종료한다. 이미 종료된 것을 다시 종료하면
 * 아무것도 바꾸지 않는다. 회차 시작일은 지난 날짜도 허용한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CourseCommandService {

  private final CourseRepository courseRepository;
  private final CourseSessionRepository courseSessionRepository;

  public CourseDetailResponse createCourse(CourseRequest request) {
    Course course =
        courseRepository.save(
            Course.create(request.getTitle(), request.getCategory(), request.getDescription()));
    return CourseDetailResponse.of(course, Collections.emptyList());
  }

  public CourseDetailResponse updateCourse(Long courseId, CourseRequest request) {
    Course course = findCourse(courseId);
    if (course.isClosed()) {
      throw new CourseClosedException(courseId);
    }
    course.update(request.getTitle(), request.getCategory(), request.getDescription());
    return toDetail(course, findSessions(courseId));
  }

  public CourseDetailResponse closeCourse(Long courseId) {
    Course course = findCourse(courseId);
    List<CourseSession> sessions = findSessions(courseId);
    course.close();
    sessions.forEach(CourseSession::close);
    return toDetail(course, sessions);
  }

  public SessionResponse createSession(Long courseId, SessionRequest request) {
    Course course = findCourse(courseId);
    if (course.isClosed()) {
      throw new CourseClosedException(courseId);
    }
    CourseSession session =
        courseSessionRepository.save(
            CourseSession.create(
                course, request.getStartDate(), request.getPlace(), request.getCapacity()));
    return SessionResponse.from(session);
  }

  public SessionResponse updateSession(Long courseId, Long sessionId, SessionRequest request) {
    CourseSession session = findSession(courseId, sessionId);
    if (session.isClosed()) {
      throw new SessionClosedException(sessionId);
    }
    session.update(request.getStartDate(), request.getPlace(), request.getCapacity());
    return SessionResponse.from(session);
  }

  public SessionResponse closeSession(Long courseId, Long sessionId) {
    CourseSession session = findSession(courseId, sessionId);
    session.close();
    return SessionResponse.from(session);
  }

  private Course findCourse(Long courseId) {
    return courseRepository
        .findById(courseId)
        .orElseThrow(() -> new CourseNotFoundException(courseId));
  }

  /** 과정이 없으면 COURSE_NOT_FOUND, 회차가 없거나 다른 과정의 회차면 SESSION_NOT_FOUND. */
  private CourseSession findSession(Long courseId, Long sessionId) {
    if (!courseRepository.existsById(courseId)) {
      throw new CourseNotFoundException(courseId);
    }
    return courseSessionRepository
        .findByIdAndCourseId(sessionId, courseId)
        .orElseThrow(() -> new SessionNotFoundException(sessionId));
  }

  private List<CourseSession> findSessions(Long courseId) {
    return courseSessionRepository.findByCourseIdOrderByStartDateAsc(courseId);
  }

  private static CourseDetailResponse toDetail(Course course, List<CourseSession> sessions) {
    return CourseDetailResponse.of(
        course, sessions.stream().map(SessionResponse::from).collect(Collectors.toList()));
  }
}
