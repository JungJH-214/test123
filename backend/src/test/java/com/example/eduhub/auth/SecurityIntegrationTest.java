package com.example.eduhub.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.eduhub.auth.config.JwtProperties;
import com.example.eduhub.auth.domain.Employee;
import com.example.eduhub.auth.domain.Role;
import com.example.eduhub.auth.dto.LoginRequest;
import com.example.eduhub.auth.repository.EmployeeRepository;
import com.example.eduhub.course.domain.Course;
import com.example.eduhub.course.domain.CourseSession;
import com.example.eduhub.course.dto.CourseRequest;
import com.example.eduhub.course.repository.CourseRepository;
import com.example.eduhub.course.repository.CourseSessionRepository;
import com.example.eduhub.enrollment.domain.Enrollment;
import com.example.eduhub.enrollment.domain.EnrollmentStatus;
import com.example.eduhub.enrollment.repository.EnrollmentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** 로그인 → 토큰 → 경로별 권한을 실제 필터 체인과 H2로 확인한다 (NFR-03). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

  private static final String EMP_NO = "T0001";
  private static final String ADMIN_NO = "T9001";
  private static final String PASSWORD = "test-only-password-1!";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JwtProperties jwtProperties;
  @Autowired private EmployeeRepository employeeRepository;
  @Autowired private CourseRepository courseRepository;
  @Autowired private CourseSessionRepository sessionRepository;
  @Autowired private EnrollmentRepository enrollmentRepository;

  private Employee employee;
  private CourseSession session;

  @BeforeEach
  void setUp() {
    String hash = passwordEncoder.encode(PASSWORD);
    employee = employeeRepository.save(Employee.create(EMP_NO, "테스트사원", hash, Role.EMPLOYEE));
    employeeRepository.save(Employee.create(ADMIN_NO, "테스트관리자", hash, Role.ADMIN));
    Course course = courseRepository.save(Course.create("보안 기초", "IT", "설명"));
    session =
        sessionRepository.save(CourseSession.create(course, LocalDate.now().plusDays(7), "A동", 5));
  }

  @AfterEach
  void tearDown() {
    enrollmentRepository.deleteAllInBatch();
    sessionRepository.deleteAllInBatch();
    courseRepository.deleteAllInBatch();
    employeeRepository.deleteAllInBatch();
  }

  @Test
  void login_success_returnsToken() throws Exception {
    String token = login(EMP_NO, PASSWORD);

    assertThat(token).isNotBlank();
    mockMvc
        .perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.employeeNo").value(EMP_NO))
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
  }

  @Test
  void login_wrongPassword_returns401() throws Exception {
    mockMvc
        .perform(loginRequest(EMP_NO, "wrong-password"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
        .andExpect(jsonPath("$.message").value("사번 또는 비밀번호가 올바르지 않습니다."));
  }

  @Test
  void login_unknownEmployeeNo_returns401() throws Exception {
    mockMvc
        .perform(loginRequest("NOBODY", PASSWORD))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
        .andExpect(jsonPath("$.message").value("사번 또는 비밀번호가 올바르지 않습니다."));
  }

  @Test
  void me_noToken_returns401() throws Exception {
    mockMvc
        .perform(get("/api/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  void me_forgedToken_returns401() throws Exception {
    SecretKey otherKey =
        new SecretKeySpec(
            "another-forged-key-0123456789abcdef".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    String token =
        signedToken(otherKey, String.valueOf(employee.getId()), Instant.now().plusSeconds(600));

    expectUnauthorizedMe(token);
  }

  @Test
  void me_expiredToken_returns401() throws Exception {
    Instant past = Instant.now().minus(2, ChronoUnit.HOURS);
    String token =
        signedToken(
            jwtProperties.secretKey(),
            String.valueOf(employee.getId()),
            past.minus(1, ChronoUnit.HOURS),
            past);

    expectUnauthorizedMe(token);
  }

  @Test
  void me_algNoneToken_returns401() throws Exception {
    Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
    String header = encoder.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
    long exp = Instant.now().plusSeconds(600).getEpochSecond();
    String payload =
        encoder.encodeToString(
            ("{\"iss\":\"eduhub\",\"sub\":\""
                    + employee.getId()
                    + "\",\"role\":\"ADMIN\",\"exp\":"
                    + exp
                    + "}")
                .getBytes(StandardCharsets.UTF_8));

    expectUnauthorizedMe(header + "." + payload + ".");
  }

  @Test
  void me_nonNumericSubjectToken_returns401() throws Exception {
    String token =
        signedToken(jwtProperties.secretKey(), "not-a-number", Instant.now().plusSeconds(600));

    expectUnauthorizedMe(token);
  }

  @Test
  void enroll_employeeIdIsTokenSubject() throws Exception {
    String token = login(EMP_NO, PASSWORD);

    mockMvc
        .perform(
            post("/api/v1/sessions/{id}/enrollments", session.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.employeeId").value(employee.getId()));

    List<Enrollment> saved = enrollmentRepository.findAll();
    assertThat(saved).hasSize(1);
    assertThat(saved.get(0).getEmployeeId()).isEqualTo(employee.getId());
  }

  @Test
  void cancel_otherEmployeesEnrollment_returns404AndKeepsStatus() throws Exception {
    String hash = passwordEncoder.encode(PASSWORD);
    employeeRepository.save(Employee.create("T0002", "다른사원", hash, Role.EMPLOYEE));
    Enrollment enrollmentOfA =
        enrollmentRepository.save(
            Enrollment.create(session, employee.getId(), EnrollmentStatus.CONFIRMED));

    mockMvc
        .perform(
            post("/api/me/enrollments/{id}/cancel", enrollmentOfA.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + login("T0002", PASSWORD)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ENROLLMENT_NOT_FOUND"));

    assertThat(enrollmentRepository.findById(enrollmentOfA.getId()).get().getStatus())
        .isEqualTo(EnrollmentStatus.CONFIRMED);
  }

  @Test
  void createCourse_noToken_401() throws Exception {
    mockMvc
        .perform(courseRequest(null))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  void createCourse_employee_403() throws Exception {
    mockMvc
        .perform(courseRequest(login(EMP_NO, PASSWORD)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void createCourse_admin_201() throws Exception {
    mockMvc
        .perform(courseRequest(login(ADMIN_NO, PASSWORD)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.title").value("신규 과정"));

    assertThat(courseRepository.findAll()).filteredOn(c -> "신규 과정".equals(c.getTitle())).hasSize(1);
  }

  @Test
  void adminApi_employee_returns403() throws Exception {
    mockMvc
        .perform(
            get("/api/admin/anything")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + login(EMP_NO, PASSWORD)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void adminRoster_employeeToken_returns403() throws Exception {
    mockMvc
        .perform(
            get("/api/admin/sessions/{id}/roster", session.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + login(EMP_NO, PASSWORD)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void adminRoster_adminToken_returns200() throws Exception {
    mockMvc
        .perform(
            get("/api/admin/sessions/{id}/roster", session.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + login(ADMIN_NO, PASSWORD)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sessionId").value(session.getId()));
  }

  @Test
  void getCourses_noToken_returns200() throws Exception {
    mockMvc.perform(get("/api/v1/courses")).andExpect(status().isOk());
  }

  @Test
  void login_withExpiredToken_stillReturns200() throws Exception {
    Instant past = Instant.now().minus(2, ChronoUnit.HOURS);
    String expired =
        signedToken(
            jwtProperties.secretKey(),
            String.valueOf(employee.getId()),
            past.minus(1, ChronoUnit.HOURS),
            past);

    mockMvc
        .perform(
            loginRequest(EMP_NO, PASSWORD).header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").exists());
  }

  @Test
  void getCourses_withGarbageToken_returns200() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/courses").header(HttpHeaders.AUTHORIZATION, "Bearer garbage.token.value"))
        .andExpect(status().isOk());
  }

  @Test
  void me_withGarbageToken_stillReturns401() throws Exception {
    expectUnauthorizedMe("garbage.token.value");
  }

  @Test
  void createCourse_withGarbageToken_returns401() throws Exception {
    mockMvc
        .perform(courseRequest("garbage.token.value"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  void me_wrongIssuerToken_returns401() throws Exception {
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer("other")
            .subject(String.valueOf(employee.getId()))
            .claim("role", "EMPLOYEE")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(600))
            .build();

    expectUnauthorizedMe(encode(claims));
  }

  @Test
  void me_tokenWithoutExp_returns401() throws Exception {
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(jwtProperties.getIssuer())
            .subject(String.valueOf(employee.getId()))
            .claim("role", "EMPLOYEE")
            .issuedAt(Instant.now())
            .build();

    expectUnauthorizedMe(encode(claims));
  }

  private String encode(JwtClaimsSet claims) {
    NimbusJwtEncoder encoder =
        new NimbusJwtEncoder(new ImmutableSecret<>(jwtProperties.secretKey()));
    return encoder
        .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
        .getTokenValue();
  }

  private void expectUnauthorizedMe(String token) throws Exception {
    mockMvc
        .perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  private MockHttpServletRequestBuilder loginRequest(String employeeNo, String password)
      throws Exception {
    return post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new LoginRequest(employeeNo, password)));
  }

  private String login(String employeeNo, String password) throws Exception {
    MvcResult result =
        mockMvc.perform(loginRequest(employeeNo, password)).andExpect(status().isOk()).andReturn();
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
    return body.get("accessToken").asText();
  }

  private MockHttpServletRequestBuilder courseRequest(String token) throws Exception {
    MockHttpServletRequestBuilder builder =
        post("/api/v1/admin/courses")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new CourseRequest("신규 과정", "IT", "설명")));
    if (token != null) {
      builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
    return builder;
  }

  private String signedToken(SecretKey key, String subject, Instant expiresAt) {
    return signedToken(key, subject, Instant.now(), expiresAt);
  }

  private String signedToken(SecretKey key, String subject, Instant issuedAt, Instant expiresAt) {
    NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(jwtProperties.getIssuer())
            .subject(subject)
            .claim("role", "EMPLOYEE")
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .build();
    return encoder
        .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
        .getTokenValue();
  }
}
