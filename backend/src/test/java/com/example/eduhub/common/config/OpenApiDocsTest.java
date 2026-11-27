package com.example.eduhub.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** OpenAPI 문서 내용 검증 (NFR-04, FR-05). 인증 주입 파라미터는 문서에 나오면 안 된다. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocsTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void apiDocs_enroll_hasNoJwtParameter() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.paths['/api/v1/sessions/{sessionId}/enrollments'].post.parameters[*].name")
                .value(contains("sessionId")))
        .andExpect(
            jsonPath("$.paths['/api/v1/sessions/{sessionId}/enrollments'].post.parameters[*].name")
                .value(not(hasItem("jwt"))));
  }

  @Test
  void apiDocs_myEnrollments_onlyStatusParameter() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.paths['/api/me/enrollments'].get.parameters[*].name")
                .value(contains("status")));
  }

  @Test
  void apiDocs_cancel_onlyEnrollmentIdParameter() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.paths['/api/me/enrollments/{enrollmentId}/cancel'].post.parameters[*].name")
                .value(contains("enrollmentId")));
  }

  @Test
  void apiDocs_me_hasNoParameters() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            result -> {
              JsonNode me =
                  new ObjectMapper()
                      .readTree(result.getResponse().getContentAsString())
                      .at("/paths/~1api~1me/get");
              // 경로가 바뀌거나 빠져서 허위로 통과하지 않도록 먼저 /api/me GET이 문서에 있는지 확인한다.
              assertThat(me.isMissingNode()).as("/api/me GET이 문서에 있어야 한다").isFalse();
              JsonNode parameters = me.path("parameters");
              assertThat(parameters.isMissingNode() || parameters.size() == 0).isTrue();
            });
  }

  @Test
  void apiDocs_enrollmentResponse_hasWaitingNumber() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.components.schemas.EnrollmentResponse.properties.waitingNumber").exists());
  }
}
