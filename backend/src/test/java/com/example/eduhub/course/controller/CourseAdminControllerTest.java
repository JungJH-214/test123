package com.example.eduhub.course.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.eduhub.course.domain.Course;
import com.example.eduhub.course.domain.CourseSession;
import com.example.eduhub.course.dto.CourseDetailResponse;
import com.example.eduhub.course.dto.SessionResponse;
import com.example.eduhub.course.exception.CourseClosedException;
import com.example.eduhub.course.exception.CourseNotFoundException;
import com.example.eduhub.course.exception.SessionClosedException;
import com.example.eduhub.course.exception.SessionNotFoundException;
import com.example.eduhub.course.service.CourseCommandService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(CourseAdminController.class)
class CourseAdminControllerTest {

  private static final String COURSE_JSON =
      "{\"title\":\"Java 기초\",\"category\":\"개발\",\"description\":\"입문\"}";
  private static final String SESSION_JSON =
      "{\"startDate\":\"2026-12-01\",\"place\":\"본사 3층\",\"capacity\":20}";

  @Autowired private MockMvc mockMvc;
  @MockBean private CourseCommandService courseCommandService;

  // ---- 성공 ----

  @Test
  void createCourse_returns201() throws Exception {
    given(courseCommandService.createCourse(any()))
        .willReturn(CourseDetailResponse.of(Course.create("Java 기초", "개발", "입문"), List.of()));

    mockMvc
        .perform(json(post("/api/v1/admin/courses"), COURSE_JSON))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("Java 기초"))
        .andExpect(jsonPath("$.status").value("OPEN"));
  }

  @Test
  void updateCourse_returns200() throws Exception {
    given(courseCommandService.updateCourse(eq(1L), any()))
        .willReturn(CourseDetailResponse.of(Course.create("Java 기초", "개발", "입문"), List.of()));

    mockMvc
        .perform(json(put("/api/v1/admin/courses/1"), COURSE_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.category").value("개발"));
  }

  @Test
  void closeCourse_returns200() throws Exception {
    Course course = Course.create("Java 기초", "개발", null);
    course.close();
    given(courseCommandService.closeCourse(1L))
        .willReturn(CourseDetailResponse.of(course, List.of()));

    mockMvc
        .perform(post("/api/v1/admin/courses/1/close"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CLOSED"));
  }

  @Test
  void createSession_returns201() throws Exception {
    given(courseCommandService.createSession(eq(1L), any())).willReturn(sessionResponse());

    mockMvc
        .perform(json(post("/api/v1/admin/courses/1/sessions"), SESSION_JSON))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.startDate").value("2026-12-01"))
        .andExpect(jsonPath("$.capacity").value(20));
  }

  @Test
  void updateSession_returns200() throws Exception {
    given(courseCommandService.updateSession(eq(1L), eq(10L), any())).willReturn(sessionResponse());

    mockMvc
        .perform(json(put("/api/v1/admin/courses/1/sessions/10"), SESSION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.place").value("본사 3층"));
  }

  @Test
  void closeSession_returns200() throws Exception {
    given(courseCommandService.closeSession(1L, 10L)).willReturn(sessionResponse());

    mockMvc.perform(post("/api/v1/admin/courses/1/sessions/10/close")).andExpect(status().isOk());
  }

  // ---- 400 ----

  @Test
  void createCourse_blankTitleAndCategory_returns400() throws Exception {
    mockMvc
        .perform(json(post("/api/v1/admin/courses"), "{\"title\":\" \",\"category\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.message").value("category: 필수 입력입니다, title: 필수 입력입니다"));
    verifyNoInteractions(courseCommandService);
  }

  @Test
  void createCourse_titleTooLong_returns400() throws Exception {
    String longTitle = "a".repeat(201);
    mockMvc
        .perform(
            json(
                post("/api/v1/admin/courses"),
                "{\"title\":\"" + longTitle + "\",\"category\":\"개발\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("title: 200자 이하로 입력하세요"));
  }

  @Test
  void createSession_zeroCapacity_returns400() throws Exception {
    mockMvc
        .perform(
            json(
                post("/api/v1/admin/courses/1/sessions"),
                "{\"startDate\":\"2026-12-01\",\"place\":\"본사\",\"capacity\":0}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.message").value("capacity: 1 이상이어야 합니다"));
  }

  @Test
  void createSession_missingFields_returns400() throws Exception {
    mockMvc
        .perform(json(post("/api/v1/admin/courses/1/sessions"), "{}"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("capacity: 필수 입력입니다, place: 필수 입력입니다, startDate: 필수 입력입니다"));
  }

  @Test
  void createSession_badDateFormat_returns400() throws Exception {
    mockMvc
        .perform(
            json(
                post("/api/v1/admin/courses/1/sessions"),
                "{\"startDate\":\"2026/12/01\",\"place\":\"본사\",\"capacity\":1}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
  }

  // ---- 404, 409 ----

  @Test
  void updateCourse_notFound_returns404() throws Exception {
    given(courseCommandService.updateCourse(eq(9L), any()))
        .willThrow(new CourseNotFoundException(9L));

    mockMvc
        .perform(json(put("/api/v1/admin/courses/9"), COURSE_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("COURSE_NOT_FOUND"));
  }

  @Test
  void updateCourse_closed_returns409() throws Exception {
    given(courseCommandService.updateCourse(eq(1L), any()))
        .willThrow(new CourseClosedException(1L));

    mockMvc
        .perform(json(put("/api/v1/admin/courses/1"), COURSE_JSON))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("COURSE_CLOSED"));
  }

  @Test
  void createSession_closedCourse_returns409() throws Exception {
    given(courseCommandService.createSession(eq(1L), any()))
        .willThrow(new CourseClosedException(1L));

    mockMvc
        .perform(json(post("/api/v1/admin/courses/1/sessions"), SESSION_JSON))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("COURSE_CLOSED"));
  }

  @Test
  void updateSession_closed_returns409() throws Exception {
    given(courseCommandService.updateSession(eq(1L), eq(10L), any()))
        .willThrow(new SessionClosedException(10L));

    mockMvc
        .perform(json(put("/api/v1/admin/courses/1/sessions/10"), SESSION_JSON))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("SESSION_CLOSED"));
  }

  @Test
  void closeSession_notFound_returns404() throws Exception {
    given(courseCommandService.closeSession(1L, 99L)).willThrow(new SessionNotFoundException(99L));

    mockMvc
        .perform(post("/api/v1/admin/courses/1/sessions/99/close"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
  }

  private static MockHttpServletRequestBuilder json(
      MockHttpServletRequestBuilder builder, String body) {
    return builder.contentType(MediaType.APPLICATION_JSON).content(body);
  }

  private static SessionResponse sessionResponse() {
    return SessionResponse.from(
        CourseSession.create(
            Course.create("Java 기초", "개발", null), LocalDate.of(2026, 12, 1), "본사 3층", 20));
  }
}
