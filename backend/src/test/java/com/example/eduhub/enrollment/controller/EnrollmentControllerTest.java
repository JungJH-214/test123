package com.example.eduhub.enrollment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EnrollmentController.class)
class EnrollmentControllerTest {

  private static final String URL = "/api/v1/sessions/1/enrollments";
  private static final String HEADER = "X-Employee-Id";

  @Autowired private MockMvc mockMvc;
  @MockBean private EnrollmentCommandService enrollmentCommandService;

  // ---- 성공 ----

  @Test
  void enroll_returns201() throws Exception {
    given(enrollmentCommandService.enroll(1L, 7L)).willReturn(response(EnrollmentStatus.CONFIRMED));

    mockMvc
        .perform(post(URL).header(HEADER, 7))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("CONFIRMED"))
        .andExpect(jsonPath("$.employeeId").value(7));
  }

  @Test
  void enroll_passesPathAndHeaderToService() throws Exception {
    given(enrollmentCommandService.enroll(1L, 7L)).willReturn(response(EnrollmentStatus.CONFIRMED));

    mockMvc.perform(post(URL).header(HEADER, 7)).andExpect(status().isCreated());

    ArgumentCaptor<Long> sessionId = ArgumentCaptor.forClass(Long.class);
    ArgumentCaptor<Long> employeeId = ArgumentCaptor.forClass(Long.class);
    verify(enrollmentCommandService).enroll(sessionId.capture(), employeeId.capture());
    assertThat(sessionId.getValue()).isEqualTo(1L);
    assertThat(employeeId.getValue()).isEqualTo(7L);
  }

  @Test
  void enroll_waiting_returns201WithWaiting() throws Exception {
    given(enrollmentCommandService.enroll(1L, 7L)).willReturn(response(EnrollmentStatus.WAITING));

    mockMvc
        .perform(post(URL).header(HEADER, 7))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("WAITING"));
  }

  // ---- 400 ----

  @Test
  void enroll_missingHeader_returns400() throws Exception {
    mockMvc
        .perform(post(URL))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MISSING_EMPLOYEE_ID"));

    verifyNoInteractions(enrollmentCommandService);
  }

  @Test
  void enroll_nonNumericHeader_returns400() throws Exception {
    mockMvc
        .perform(post(URL).header(HEADER, "abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MISSING_EMPLOYEE_ID"));

    verifyNoInteractions(enrollmentCommandService);
  }

  @Test
  void enroll_nonNumericSessionId_returns400InvalidRequest() throws Exception {
    mockMvc
        .perform(post("/api/v1/sessions/abc/enrollments").header(HEADER, 7))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

    verifyNoInteractions(enrollmentCommandService);
  }

  // ---- 404, 409 ----

  @Test
  void enroll_unknownSession_returns404() throws Exception {
    given(enrollmentCommandService.enroll(1L, 7L)).willThrow(new SessionNotFoundException(1L));

    mockMvc
        .perform(post(URL).header(HEADER, 7))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
  }

  @Test
  void enroll_closedSession_returns409() throws Exception {
    given(enrollmentCommandService.enroll(1L, 7L)).willThrow(new SessionClosedException(1L));

    mockMvc
        .perform(post(URL).header(HEADER, 7))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("SESSION_CLOSED"));
  }

  @Test
  void enroll_duplicate_returns409() throws Exception {
    given(enrollmentCommandService.enroll(1L, 7L))
        .willThrow(new DuplicateEnrollmentException(1L, 7L));

    mockMvc
        .perform(post(URL).header(HEADER, 7))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ENROLLMENT_DUPLICATE"));
  }

  @Test
  void enroll_full_returns409() throws Exception {
    given(enrollmentCommandService.enroll(1L, 7L)).willThrow(new SessionFullException(1L));

    mockMvc
        .perform(post(URL).header(HEADER, 7))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("SESSION_FULL"));
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
