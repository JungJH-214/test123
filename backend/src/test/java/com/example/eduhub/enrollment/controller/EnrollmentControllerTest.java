package com.example.eduhub.enrollment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.eduhub.auth.config.JsonSecurityErrorHandler;
import com.example.eduhub.auth.config.SecurityConfig;
import com.example.eduhub.course.domain.Course;
import com.example.eduhub.course.domain.CourseSession;
import com.example.eduhub.course.exception.SessionClosedException;
import com.example.eduhub.course.exception.SessionNotFoundException;
import com.example.eduhub.enrollment.domain.Enrollment;
import com.example.eduhub.enrollment.domain.EnrollmentStatus;
import com.example.eduhub.enrollment.dto.EnrollmentResponse;
import com.example.eduhub.enrollment.exception.DuplicateEnrollmentException;
import com.example.eduhub.enrollment.exception.SessionFullException;
import com.example.eduhub.enrollment.service.EnrollmentCommandService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@Import({SecurityConfig.class, JsonSecurityErrorHandler.class})
@WebMvcTest(EnrollmentController.class)
class EnrollmentControllerTest {

  private static final String URL = "/api/v1/sessions/1/enrollments";

  @Autowired private MockMvc mockMvc;
  @MockBean private EnrollmentCommandService enrollmentCommandService;

  // ---- 성공 ----

  @Test
  void enroll_returns201() throws Exception {
    given(enrollmentCommandService.enroll(1L, 7L)).willReturn(response(EnrollmentStatus.CONFIRMED));

    mockMvc
        .perform(post(URL).with(employee()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("CONFIRMED"))
        .andExpect(jsonPath("$.employeeId").value(7))
        .andExpect(jsonPath("$", hasKey("waitingNumber")))
        .andExpect(jsonPath("$.waitingNumber").isEmpty());
  }

  @Test
  void enroll_passesPathAndTokenSubjectToService() throws Exception {
    given(enrollmentCommandService.enroll(1L, 7L)).willReturn(response(EnrollmentStatus.CONFIRMED));

    mockMvc.perform(post(URL).with(employee())).andExpect(status().isCreated());

    ArgumentCaptor<Long> sessionId = ArgumentCaptor.forClass(Long.class);
    ArgumentCaptor<Long> employeeId = ArgumentCaptor.forClass(Long.class);
    verify(enrollmentCommandService).enroll(sessionId.capture(), employeeId.capture());
    assertThat(sessionId.getValue()).isEqualTo(1L);
    assertThat(employeeId.getValue()).isEqualTo(7L);
  }

  @Test
  void enroll_ignoresHeaderAndQueryEmployeeId_usesTokenSubject() throws Exception {
    given(enrollmentCommandService.enroll(1L, 7L)).willReturn(response(EnrollmentStatus.CONFIRMED));

    mockMvc
        .perform(post(URL).param("employeeId", "99").header("X-Employee-Id", 99).with(employee()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.employeeId").value(7));

    verify(enrollmentCommandService).enroll(1L, 7L);
    verify(enrollmentCommandService, never()).enroll(anyLong(), eq(99L));
  }

  @Test
  void enroll_waiting_returns201WithWaiting() throws Exception {
    given(enrollmentCommandService.enroll(1L, 7L)).willReturn(waitingResponse(3));

    mockMvc
        .perform(post(URL).with(employee()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("WAITING"))
        .andExpect(jsonPath("$.waitingNumber").value(3));
  }

  // ---- 401, 400 ----

  @Test
  void enroll_noToken_returns401() throws Exception {
    mockMvc
        .perform(post(URL))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

    verifyNoInteractions(enrollmentCommandService);
  }

  @Test
  void enroll_nonNumericSessionId_returns400InvalidRequest() throws Exception {
    mockMvc
        .perform(post("/api/v1/sessions/abc/enrollments").with(employee()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

    verifyNoInteractions(enrollmentCommandService);
  }

  // ---- 404, 409 ----

  @Test
  void enroll_unknownSession_returns404() throws Exception {
    given(enrollmentCommandService.enroll(1L, 7L)).willThrow(new SessionNotFoundException(1L));

    mockMvc
        .perform(post(URL).with(employee()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
  }

  @Test
  void enroll_closedSession_returns409() throws Exception {
    given(enrollmentCommandService.enroll(1L, 7L)).willThrow(new SessionClosedException(1L));

    mockMvc
        .perform(post(URL).with(employee()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("SESSION_CLOSED"));
  }

  @Test
  void enroll_duplicate_returns409() throws Exception {
    given(enrollmentCommandService.enroll(1L, 7L))
        .willThrow(new DuplicateEnrollmentException(1L, 7L));

    mockMvc
        .perform(post(URL).with(employee()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ENROLLMENT_DUPLICATE"));
  }

  @Test
  void enroll_full_returns409() throws Exception {
    given(enrollmentCommandService.enroll(1L, 7L)).willThrow(new SessionFullException(1L));

    mockMvc
        .perform(post(URL).with(employee()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("SESSION_FULL"));
  }

  private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor employee() {
    return jwt().jwt(j -> j.subject("7").claim("role", "EMPLOYEE"));
  }

  private EnrollmentResponse response(EnrollmentStatus status) {
    return EnrollmentResponse.from(enrollment(status));
  }

  private EnrollmentResponse waitingResponse(int waitingNumber) {
    return EnrollmentResponse.waiting(enrollment(EnrollmentStatus.WAITING), waitingNumber);
  }

  private Enrollment enrollment(EnrollmentStatus status) {
    Course course = Course.create("Java 기초", "개발", "입문");
    CourseSession session = CourseSession.create(course, LocalDate.of(2026, 12, 1), "본사", 10);
    ReflectionTestUtils.setField(session, "id", 1L);
    Enrollment enrollment = Enrollment.create(session, 7L, status);
    ReflectionTestUtils.setField(enrollment, "id", 100L);
    return enrollment;
  }
}
