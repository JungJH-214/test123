package com.example.eduhub.enrollment.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.eduhub.auth.config.JsonSecurityErrorHandler;
import com.example.eduhub.auth.config.SecurityConfig;
import com.example.eduhub.course.exception.SessionNotFoundException;
import com.example.eduhub.enrollment.domain.EnrollmentStatus;
import com.example.eduhub.enrollment.dto.CompletionFailure;
import com.example.eduhub.enrollment.dto.CompletionFailureReason;
import com.example.eduhub.enrollment.dto.CompletionResponse;
import com.example.eduhub.enrollment.dto.RosterEntry;
import com.example.eduhub.enrollment.dto.RosterResponse;
import com.example.eduhub.enrollment.exception.SessionNotStartedException;
import com.example.eduhub.enrollment.repository.RosterRow;
import com.example.eduhub.enrollment.service.RosterService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@Import({SecurityConfig.class, JsonSecurityErrorHandler.class})
@WebMvcTest(AdminEnrollmentController.class)
class AdminEnrollmentControllerTest {

  private static final String URL = "/api/admin/sessions/1/roster";

  @Autowired private MockMvc mockMvc;
  @MockBean private RosterService rosterService;

  @Test
  void getRoster_admin_returns200() throws Exception {
    RosterRow confirmed =
        new RosterRow(
            10L,
            1L,
            "E-001",
            "김하나",
            EnrollmentStatus.CONFIRMED,
            LocalDateTime.of(2026, 11, 1, 9, 0));
    RosterRow waiting =
        new RosterRow(
            11L, 2L, null, null, EnrollmentStatus.WAITING, LocalDateTime.of(2026, 11, 1, 9, 1));
    given(rosterService.getRoster(1L))
        .willReturn(
            RosterResponse.of(
                1L,
                "Java 기초",
                LocalDate.of(2026, 12, 1),
                10,
                Collections.singletonList(RosterEntry.confirmed(confirmed)),
                Collections.singletonList(RosterEntry.waiting(waiting, 1))));

    mockMvc
        .perform(get(URL).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sessionId").value(1))
        .andExpect(jsonPath("$.courseTitle").value("Java 기초"))
        .andExpect(jsonPath("$.startDate").value("2026-12-01"))
        .andExpect(jsonPath("$.capacity").value(10))
        .andExpect(jsonPath("$.confirmed[0].enrollmentId").value(10))
        .andExpect(jsonPath("$.confirmed[0].employeeNo").value("E-001"))
        .andExpect(jsonPath("$.confirmed[0].name").value("김하나"))
        .andExpect(jsonPath("$.confirmed[0].status").value("CONFIRMED"))
        .andExpect(jsonPath("$.waiting[0].status").value("WAITING"))
        .andExpect(jsonPath("$.waiting[0].waitingNumber").value(1));
  }

  @Test
  void getRoster_nonNumericSessionId_returns400() throws Exception {
    mockMvc
        .perform(
            get("/api/admin/sessions/abc/roster")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    verifyNoInteractions(rosterService);
  }

  @Test
  void getRoster_noToken_returns401() throws Exception {
    mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    verifyNoInteractions(rosterService);
  }

  @Test
  void getRoster_employeeRole_returns403() throws Exception {
    mockMvc
        .perform(get(URL).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    verifyNoInteractions(rosterService);
  }

  private static final String COMPLETIONS_URL = "/api/admin/sessions/1/completions";

  private MockHttpServletRequestBuilder postCompletions(String url, String body) {
    return post(url)
        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
        .contentType(MediaType.APPLICATION_JSON)
        .content(body);
  }

  @Test
  void complete_admin_returns200WithResult() throws Exception {
    given(rosterService.complete(1L, Arrays.asList(1L, 2L, 3L)))
        .willReturn(
            new CompletionResponse(
                Collections.singletonList(1L),
                Arrays.asList(
                    new CompletionFailure(2L, CompletionFailureReason.WAITING),
                    new CompletionFailure(3L, CompletionFailureReason.NOT_ENROLLED))));

    mockMvc
        .perform(postCompletions(COMPLETIONS_URL, "{\"employeeIds\":[1,2,3]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.completed[0]").value(1))
        .andExpect(jsonPath("$.failed[0].employeeId").value(2))
        .andExpect(jsonPath("$.failed[0].reason").value("WAITING"))
        .andExpect(jsonPath("$.failed[1].employeeId").value(3))
        .andExpect(jsonPath("$.failed[1].reason").value("NOT_ENROLLED"));
  }

  @Test
  void complete_emptyList_returns400() throws Exception {
    mockMvc
        .perform(postCompletions(COMPLETIONS_URL, "{\"employeeIds\":[]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    verifyNoInteractions(rosterService);
  }

  @Test
  void complete_missingField_returns400() throws Exception {
    mockMvc
        .perform(postCompletions(COMPLETIONS_URL, "{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    verifyNoInteractions(rosterService);
  }

  @Test
  void complete_nullElement_returns400() throws Exception {
    mockMvc
        .perform(postCompletions(COMPLETIONS_URL, "{\"employeeIds\":[1,null]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    verifyNoInteractions(rosterService);
  }

  @Test
  void complete_501Ids_returns400() throws Exception {
    String ids =
        LongStream.rangeClosed(1, 501).mapToObj(String::valueOf).collect(Collectors.joining(","));

    mockMvc
        .perform(postCompletions(COMPLETIONS_URL, "{\"employeeIds\":[" + ids + "]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    verifyNoInteractions(rosterService);
  }

  @Test
  void complete_500Ids_returns200() throws Exception {
    List<Long> ids = LongStream.rangeClosed(1, 500).boxed().collect(Collectors.toList());
    given(rosterService.complete(1L, ids))
        .willReturn(new CompletionResponse(ids, Collections.emptyList()));
    String body = ids.stream().map(String::valueOf).collect(Collectors.joining(","));

    mockMvc
        .perform(postCompletions(COMPLETIONS_URL, "{\"employeeIds\":[" + body + "]}"))
        .andExpect(status().isOk());
  }

  @Test
  void complete_nonNumericElement_returns400() throws Exception {
    mockMvc
        .perform(postCompletions(COMPLETIONS_URL, "{\"employeeIds\":[\"abc\"]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    verifyNoInteractions(rosterService);
  }

  @Test
  void complete_nonNumericSessionId_returns400() throws Exception {
    mockMvc
        .perform(postCompletions("/api/admin/sessions/abc/completions", "{\"employeeIds\":[1]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    verifyNoInteractions(rosterService);
  }

  @Test
  void complete_noToken_returns401() throws Exception {
    mockMvc
        .perform(
            post(COMPLETIONS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeIds\":[1]}"))
        .andExpect(status().isUnauthorized());
    verifyNoInteractions(rosterService);
  }

  @Test
  void complete_employeeRole_returns403() throws Exception {
    mockMvc
        .perform(
            post(COMPLETIONS_URL)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeIds\":[1]}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    verifyNoInteractions(rosterService);
  }

  @Test
  void complete_sessionMissing_returns404() throws Exception {
    given(rosterService.complete(1L, Collections.singletonList(1L)))
        .willThrow(new SessionNotFoundException(1L));

    mockMvc
        .perform(postCompletions(COMPLETIONS_URL, "{\"employeeIds\":[1]}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
  }

  @Test
  void complete_sessionNotStarted_returns409() throws Exception {
    given(rosterService.complete(1L, Collections.singletonList(1L)))
        .willThrow(new SessionNotStartedException(1L));

    mockMvc
        .perform(postCompletions(COMPLETIONS_URL, "{\"employeeIds\":[1]}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("SESSION_NOT_STARTED"));
  }

  @Test
  void getRoster_sessionMissing_returns404() throws Exception {
    given(rosterService.getRoster(1L)).willThrow(new SessionNotFoundException(1L));

    mockMvc
        .perform(get(URL).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
  }
}
