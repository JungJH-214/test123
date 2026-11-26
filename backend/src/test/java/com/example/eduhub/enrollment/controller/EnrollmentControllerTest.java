package com.example.eduhub.enrollment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.example.eduhub.enrollment.dto.MyEnrollmentResponse;
import com.example.eduhub.enrollment.exception.DuplicateEnrollmentException;
import com.example.eduhub.enrollment.exception.SessionFullException;
import com.example.eduhub.enrollment.repository.MyEnrollmentRow;
import com.example.eduhub.enrollment.service.EnrollmentCommandService;
import com.example.eduhub.enrollment.service.MyEnrollmentQueryService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
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
  @MockBean private MyEnrollmentQueryService myEnrollmentQueryService;

  // ---- 성공 ----

  @Test
  void enroll_returns201() throws Exception {
    given(enrollmentCommandService.enroll(1L, 7L)).willReturn(response(EnrollmentStatus.CONFIRMED));

    mockMvc
        .perform(post(URL).with(employee()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("CONFIRMED"))
        .andExpect(jsonPath("$.employeeId").value(7));
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
    given(enrollmentCommandService.enroll(1L, 7L)).willReturn(response(EnrollmentStatus.WAITING));

    mockMvc
        .perform(post(URL).with(employee()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("WAITING"));
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

  // ---- 내 신청 내역 (FR-07) ----

  private static final String ME_URL = "/api/me/enrollments";

  @Test
  void myEnrollments_returns200WithFields() throws Exception {
    given(myEnrollmentQueryService.findMine(7L, null))
        .willReturn(Collections.singletonList(myResponse(EnrollmentStatus.WAITING, 2L)));

    mockMvc
        .perform(get(ME_URL).with(employee()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].courseTitle").value("Java 기초"))
        .andExpect(jsonPath("$[0].waitingNumber").value(3))
        .andExpect(jsonPath("$[0].status").value("WAITING"));
  }

  @Test
  void myEnrollments_usesEmployeeIdFromJwt() throws Exception {
    given(myEnrollmentQueryService.findMine(7L, null)).willReturn(Collections.emptyList());

    mockMvc
        .perform(get(ME_URL).with(employee()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());

    verify(myEnrollmentQueryService).findMine(7L, null);
  }

  @Test
  void myEnrollments_ignoresEmployeeIdQueryParam() throws Exception {
    given(myEnrollmentQueryService.findMine(7L, null)).willReturn(Collections.emptyList());

    mockMvc
        .perform(get(ME_URL).param("employeeId", "99").header("X-Employee-Id", 99).with(employee()))
        .andExpect(status().isOk());

    verify(myEnrollmentQueryService).findMine(7L, null);
    verify(myEnrollmentQueryService, never()).findMine(eq(99L), any());
  }

  @Test
  void myEnrollments_withStatus_passesStatus() throws Exception {
    given(myEnrollmentQueryService.findMine(7L, EnrollmentStatus.WAITING))
        .willReturn(Collections.emptyList());

    mockMvc
        .perform(get(ME_URL).param("status", "WAITING").with(employee()))
        .andExpect(status().isOk());

    verify(myEnrollmentQueryService).findMine(7L, EnrollmentStatus.WAITING);
  }

  @Test
  void myEnrollments_invalidStatus_returns400InvalidRequest() throws Exception {
    mockMvc
        .perform(get(ME_URL).param("status", "FOO").with(employee()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

    verifyNoInteractions(myEnrollmentQueryService);
  }

  @Test
  void myEnrollments_noToken_returns401() throws Exception {
    mockMvc
        .perform(get(ME_URL))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

    verifyNoInteractions(myEnrollmentQueryService);
  }

  private static MyEnrollmentResponse myResponse(EnrollmentStatus status, long preceding) {
    return MyEnrollmentResponse.from(
        new MyEnrollmentRow(
            100L,
            1L,
            "Java 기초",
            LocalDate.of(2026, 12, 1),
            "본사",
            status,
            LocalDateTime.of(2026, 11, 1, 9, 0),
            preceding));
  }

  private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor employee() {
    return jwt().jwt(j -> j.subject("7").claim("role", "EMPLOYEE"));
  }

  private EnrollmentResponse response(EnrollmentStatus status) {
    Course course = Course.create("Java 기초", "개발", "입문");
    CourseSession session = CourseSession.create(course, LocalDate.of(2026, 12, 1), "본사", 10);
    ReflectionTestUtils.setField(session, "id", 1L);
    Enrollment enrollment = Enrollment.create(session, 7L, status);
    ReflectionTestUtils.setField(enrollment, "id", 100L);
    return EnrollmentResponse.from(enrollment);
  }
}
