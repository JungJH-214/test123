package com.example.eduhub.course.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.eduhub.common.dto.PageResponse;
import com.example.eduhub.course.domain.Course;
import com.example.eduhub.course.domain.CourseSession;
import com.example.eduhub.course.dto.CourseDetailResponse;
import com.example.eduhub.course.dto.CourseSummaryResponse;
import com.example.eduhub.course.dto.SessionResponse;
import com.example.eduhub.course.exception.CourseNotFoundException;
import com.example.eduhub.course.service.CourseQueryService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CourseController.class)
class CourseControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private CourseQueryService courseQueryService;

  @Test
  void getCourses_returnsPageResponse() throws Exception {
    CourseSummaryResponse summary =
        CourseSummaryResponse.from(Course.create("Java 기초", "개발", null));
    given(courseQueryService.search(any(), any(), any()))
        .willReturn(PageResponse.from(new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1)));

    mockMvc
        .perform(get("/api/v1/courses"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].title").value("Java 기초"))
        .andExpect(jsonPath("$.content[0].category").value("개발"))
        .andExpect(jsonPath("$.content[0].status").value("OPEN"))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void getCourses_passesQueryParamsToService() throws Exception {
    givenEmptyPage();

    mockMvc
        .perform(
            get("/api/v1/courses")
                .param("keyword", "java")
                .param("category", "개발")
                .param("page", "1")
                .param("size", "10"))
        .andExpect(status().isOk());

    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    verify(courseQueryService).search(eq("java"), eq("개발"), pageable.capture());
    assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
    assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
  }

  @Test
  void getCourses_defaultsToFirstPageOfTwenty() throws Exception {
    givenEmptyPage();

    mockMvc.perform(get("/api/v1/courses")).andExpect(status().isOk());

    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    verify(courseQueryService).search(isNull(), isNull(), pageable.capture());
    assertThat(pageable.getValue().getPageNumber()).isZero();
    assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
  }

  @Test
  void getCourse_returnsDetailWithSessions() throws Exception {
    Course course = Course.create("Java 기초", "개발", "입문 과정");
    SessionResponse session =
        SessionResponse.from(CourseSession.create(course, LocalDate.of(2026, 12, 1), "본사 3층", 20));
    given(courseQueryService.getDetail(1L))
        .willReturn(CourseDetailResponse.of(course, List.of(session)));

    mockMvc
        .perform(get("/api/v1/courses/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Java 기초"))
        .andExpect(jsonPath("$.description").value("입문 과정"))
        .andExpect(jsonPath("$.sessions[0].startDate").value("2026-12-01"))
        .andExpect(jsonPath("$.sessions[0].place").value("본사 3층"))
        .andExpect(jsonPath("$.sessions[0].capacity").value(20))
        .andExpect(jsonPath("$.sessions[0].remainingSeats").value(20));
  }

  @Test
  void getCourse_unknownId_returns404() throws Exception {
    given(courseQueryService.getDetail(999L)).willThrow(new CourseNotFoundException(999L));

    mockMvc
        .perform(get("/api/v1/courses/999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("COURSE_NOT_FOUND"));
  }

  private void givenEmptyPage() {
    given(courseQueryService.search(any(), any(), any()))
        .willReturn(PageResponse.from(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0)));
  }
}
