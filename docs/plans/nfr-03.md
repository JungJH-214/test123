# 구현계획: NFR-03 인증 기반 (JWT 로그인, 경로별 권한)

- 설계명세: `docs/design/auth.md` (승인됨)
- 브랜치: `feature/nfr-03-auth` (worktree `.claude/worktrees/nfr-03-auth`)
- 상태: 승인됨, T1~T5 구현 완료, 최종 검토 지적 반영
- 공통 규칙: `backend/CLAUDE.md`, `.claude/skills/eduhub-api/SKILL.md`를 따른다. 명령은 모두 `backend/`에서 실행한다. Java 11·Boot 2.7(Spring Security 5.7)이므로 `javax.*`를 쓰고 `record`는 쓰지 않는다. `WebSecurityConfigurerAdapter`는 쓰지 않고 `SecurityFilterChain` 빈으로 설정한다.
- JDK: 이 PC에서는 `JAVA_HOME="/c/Program Files/Zulu/zulu-11" ./gradlew ...`로 실행한다.
- 패키지 기준: `com.example.eduhub.auth.{config, controller, domain, dto, exception, repository, service}`, 그리고 `auth/CurrentUser`, `auth/DevAccountInitializer`. 테스트도 같은 패키지 구조에 둔다.
- 금지: 직접 만든 필터(`OncePerRequestFilter` 등), 코드·설정 파일에 실제 비밀값, 기존 V1~V3 수정.
- 범위 밖 (넣지 않는다): 회원가입, 비밀번호 변경, 리프레시 토큰, 취소 API(FR-06), FR-07·08, 프론트 화면·API 클라이언트 재생성.

## 작업 순서
T1 → T2 → T3 → T4 → T5. 앞 작업이 끝나고 reviewer 검토를 통과해야 다음 작업을 시작한다.

- T2에서 보안 의존성을 넣는 순간 기존 `@WebMvcTest` 4개가 깨지므로, 의존성·`SecurityConfig`·기존 테스트 수정을 T2 한 작업에 묶는다.

---

## T1. 마이그레이션 + Employee 엔티티·Repository

### 변경 파일
| 구분 | 파일 |
|---|---|
| 추가 | `src/main/resources/db/migration/V4__create_employee.sql` |
| 추가 | `src/main/java/com/example/eduhub/auth/package-info.java` |
| 추가 | `src/main/java/com/example/eduhub/auth/domain/Role.java` |
| 추가 | `src/main/java/com/example/eduhub/auth/domain/Employee.java` |
| 추가 | `src/main/java/com/example/eduhub/auth/repository/EmployeeRepository.java` |
| 추가 | `src/test/java/com/example/eduhub/auth/repository/EmployeeRepositoryTest.java` |

### 내용
1. `V4__create_employee.sql`: 설계명세 5장의 SQL을 그대로 쓴다. 첫 줄 주석은 `-- 임직원 계정 (NFR-03)`.
2. `package-info.java`: `/** 인증과 임직원 계정 (NFR-03). */` (기존 `course/package-info.java` 형식)
3. `Role`: `public enum Role { EMPLOYEE, ADMIN }`
4. `Employee` (`Course` 형식)
   - `@Entity @Table(name = "employee") @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)`. `@Setter` 없음.
   - 필드: `id`(IDENTITY), `employeeNo`(`@Column(name = "employee_no", nullable = false, length = 20)`), `name`(50), `passwordHash`(`password_hash`, 100), `role`(`@Enumerated(STRING)`, 20)
   - `public static Employee create(String employeeNo, String name, String passwordHash, Role role)`
5. `EmployeeRepository extends JpaRepository<Employee, Long>`: `Optional<Employee> findByEmployeeNo(String employeeNo)`, `boolean existsByEmployeeNo(String employeeNo)`

### 테스트 (`@DataJpaTest`, `@AutoConfigureTestDatabase(replace = NONE)`, `@ActiveProfiles("pgtest")`, `em.flush()`·`em.clear()` 뒤 조회)
- `findByEmployeeNo_returnsEmployee()` / `findByEmployeeNo_unknown_returnsEmpty()`
- `existsByEmployeeNo_trueAndFalse()`
- `save_duplicateEmployeeNo_throwsDataIntegrityViolation()` (`saveAndFlush`)

### 완료 명령
```bash
./gradlew spotlessApply
./gradlew test --tests "com.example.eduhub.EduhubApplicationTests" --tests "com.example.eduhub.auth.repository.*"
```

---

## T2. 보안 설정 (의존성, JWT 인코더·디코더, 필터 체인, 기존 테스트 정리)

### 변경 파일
| 구분 | 파일 |
|---|---|
| 수정 | `build.gradle.kts` |
| 수정 | `src/main/resources/application.yml` |
| 수정 | `.env.example` (저장소 루트) |
| 추가 | `src/test/resources/config/application.yml` |
| 추가 | `src/main/java/com/example/eduhub/auth/config/JwtProperties.java` |
| 추가 | `src/main/java/com/example/eduhub/auth/config/SecurityConfig.java` |
| 추가 | `src/main/java/com/example/eduhub/auth/config/JsonSecurityErrorHandler.java` |
| 수정 | `src/main/java/com/example/eduhub/common/config/OpenApiConfig.java` |
| 수정 | `src/main/java/com/example/eduhub/common/health/HealthController.java` (`@SecurityRequirements()`만) |
| 수정 | `src/main/java/com/example/eduhub/course/controller/CourseController.java` (`@SecurityRequirements()`만) |
| 수정 | `src/test/java/com/example/eduhub/common/health/HealthControllerTest.java` |
| 수정 | `src/test/java/com/example/eduhub/course/controller/CourseControllerTest.java` |
| 수정 | `src/test/java/com/example/eduhub/course/controller/CourseAdminControllerTest.java` |
| 수정 | `src/test/java/com/example/eduhub/enrollment/controller/EnrollmentControllerTest.java` |

### 내용
1. `build.gradle.kts`: `implementation("org.springframework.boot:spring-boot-starter-security")`, `implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")`, `testImplementation("org.springframework.security:spring-security-test")`. 버전은 쓰지 않는다(BOM).
2. `application.yml`에 추가 (다른 줄은 건드리지 않는다)
   ```yaml
   eduhub:
     jwt:
       secret: ${EDUHUB_JWT_SECRET}
       issuer: eduhub
       expires-in: 60m
   ```
3. `.env.example`에 추가: `# JWT 서명 키 (HS256, 32바이트 이상)` / `EDUHUB_JWT_SECRET=` / `# dev 프로필 개발용 계정 3개의 공통 비밀번호` / `EDUHUB_DEV_PASSWORD=`. 값은 쓰지 않는다.
4. `src/test/resources/config/application.yml`: `eduhub.jwt.secret: test-only-jwt-secret-0123456789abcdef` (32바이트 이상). 첫 줄 주석: `# 테스트 전용 JWT 키. 테스트 클래스패스에만 있다 (NFR-03)`. 다른 설정은 넣지 않는다.
5. `JwtProperties`: `@ConfigurationProperties("eduhub.jwt") @Validated`, 필드 `@NotBlank String secret`, `@NotBlank String issuer`, `@NotNull Duration expiresIn`. Boot 2.7이므로 `@ConstructorBinding` 생성자 바인딩(불변) 또는 getter/setter 중 생성자 바인딩을 쓴다. `secretKey()` 메서드가 `new SecretKeySpec(secret.getBytes(UTF_8), "HmacSHA256")`을 돌려주고, 32바이트 미만이면 `IllegalStateException("EDUHUB_JWT_SECRET은 32바이트 이상이어야 합니다")`.
6. `SecurityConfig` (`@Configuration @EnableConfigurationProperties(JwtProperties.class)`)
   - `SecurityFilterChain`: 설계명세 4장 코드 그대로.
   - `JwtDecoder`: `NimbusJwtDecoder.withSecretKey(props.secretKey()).macAlgorithm(MacAlgorithm.HS256).build()` 후 `setJwtValidator(JwtValidators.createDefaultWithIssuer(props.getIssuer()))`.
   - `JwtEncoder`: `new NimbusJwtEncoder(new ImmutableSecret<>(props.secretKey()))`.
   - `PasswordEncoder`: `new BCryptPasswordEncoder()`.
   - `Clock`: `Clock.systemUTC()` (T3의 `JwtIssuer`가 쓴다).
   - `JwtAuthenticationConverter`: `JwtGrantedAuthoritiesConverter`에 `setAuthoritiesClaimName("role")`, `setAuthorityPrefix("ROLE_")`.
7. `JsonSecurityErrorHandler` (`@Component`, `AuthenticationEntryPoint`와 `AccessDeniedHandler` 구현. 필터가 아니다)
   - 401: `ErrorResponse.of("UNAUTHORIZED", "인증이 필요합니다.")`, 403: `ErrorResponse.of("FORBIDDEN", "권한이 없습니다.")`
   - `Content-Type: application/json;charset=UTF-8`, `ObjectMapper`로 쓴다. 401에는 `WWW-Authenticate: Bearer` 헤더를 붙인다.
8. `OpenApiConfig`: `Components`에 `bearerAuth`(`SecurityScheme.Type.HTTP`, `scheme("bearer")`, `bearerFormat("JWT")`)를 넣고 `addSecurityItem(new SecurityRequirement().addList("bearerAuth"))`.
9. `HealthController`, `CourseController` 클래스에 `@SecurityRequirements()`를 붙여 Swagger에서 공개 API로 보이게 한다. 다른 줄은 건드리지 않는다.
10. 기존 `@WebMvcTest` 4개: 클래스에 `@Import(SecurityConfig.class)`를 붙인다. `JsonSecurityErrorHandler`는 `@Component`라 `@WebMvcTest`가 스캔하지 않으므로 함께 `@Import`한다.
    - `HealthControllerTest`, `CourseControllerTest`: 요청은 그대로(토큰 없이 200).
    - `CourseAdminControllerTest`: 모든 요청에 `.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))`. 추가: `createCourse_noToken_returns401()`(code `UNAUTHORIZED`), `createCourse_employee_returns403()`(code `FORBIDDEN`), 둘 다 서비스 호출 없음(`verifyNoInteractions`).
    - `EnrollmentControllerTest`: 지금은 헤더를 그대로 두고 모든 요청에 `.with(jwt())`만 붙인다. 헤더 제거는 T4.

### 테스트
- 위 4개 테스트 클래스가 모두 통과해야 한다.
- `EduhubApplicationTests`가 테스트 키로 컨텍스트를 띄운다.

### 완료 명령
```bash
./gradlew spotlessApply
./gradlew test --tests "com.example.eduhub.EduhubApplicationTests" --tests "*ControllerTest"
```

---

## T3. 로그인·내 정보 API + CurrentUser

### 변경 파일
| 구분 | 파일 |
|---|---|
| 추가 | `src/main/java/com/example/eduhub/common/error/UnauthorizedException.java` |
| 수정 | `src/main/java/com/example/eduhub/common/error/GlobalExceptionHandler.java` |
| 추가 | `src/main/java/com/example/eduhub/auth/exception/InvalidCredentialsException.java` |
| 추가 | `src/main/java/com/example/eduhub/auth/CurrentUser.java` |
| 추가 | `src/main/java/com/example/eduhub/auth/service/JwtIssuer.java` |
| 추가 | `src/main/java/com/example/eduhub/auth/service/AuthService.java` |
| 추가 | `src/main/java/com/example/eduhub/auth/dto/LoginRequest.java`, `LoginResponse.java`, `UserResponse.java` |
| 추가 | `src/main/java/com/example/eduhub/auth/controller/AuthController.java`, `MeController.java` |
| 추가 | `src/test/java/com/example/eduhub/auth/CurrentUserTest.java` |
| 추가 | `src/test/java/com/example/eduhub/auth/service/JwtIssuerTest.java`, `AuthServiceTest.java` |
| 추가 | `src/test/java/com/example/eduhub/auth/controller/AuthControllerTest.java` |

### 내용
1. `UnauthorizedException`: `NotFoundException`과 같은 모양(`code` 필드). `GlobalExceptionHandler`에 `@ExceptionHandler(UnauthorizedException.class) @ResponseStatus(UNAUTHORIZED)` 하나 추가.
2. `InvalidCredentialsException extends UnauthorizedException`: code `INVALID_CREDENTIALS`, 메시지 "사번 또는 비밀번호가 올바르지 않습니다."
3. `CurrentUser`: `final`, 필드 `Long employeeId`, `Role role`, private 생성자, getter. `public static CurrentUser from(Jwt jwt)`: `Long.valueOf(jwt.getSubject())`, `Role.valueOf(jwt.getClaimAsString("role"))`. 숫자가 아니거나 역할이 잘못되면 `InvalidBearerTokenException("토큰의 사용자 정보가 올바르지 않습니다.")`.
4. `JwtIssuer` (`@Component`): `IssuedToken issue(Employee e)`. `JwtClaimsSet`에 `issuer`, `subject(String.valueOf(e.getId()))`, `claim("role", e.getRole().name())`, `issuedAt(now)`, `expiresAt(now + expiresIn)`. `JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)`. 반환은 토큰 문자열과 `expiresIn` 초를 담는 작은 `final` 클래스(같은 파일의 public static 중첩 클래스 `IssuedToken`).
5. `AuthService` (`@Transactional(readOnly = true)`, Javadoc에 규칙 한 단락)
   - 생성자에서 `dummyHash = passwordEncoder.encode("dummy-password-for-timing")`를 한 번 만든다.
   - `login`: `findByEmployeeNo` → 없으면 `passwordEncoder.matches(password, dummyHash)` 후 `InvalidCredentialsException`. 있으면 `matches` 실패 시 같은 예외. 성공 시 `LoginResponse.of(accessToken, expiresIn, UserResponse.from(employee))` (구현 시 `expiresIn` 인자 추가).
   - `me(Long employeeId)`: `findById` → 없으면 `new UnauthorizedException("UNAUTHORIZED", "사용자를 찾을 수 없습니다.")`.
6. DTO: 스킬 3장 형식. `LoginRequest`(`@NotBlank(message = "필수 입력입니다")`, `@Size(max = 20)`/`@Size(max = 100)`), `LoginResponse`(`accessToken`, `tokenType`="Bearer", `expiresIn`, `user`), `UserResponse.from(Employee)`(`id`, `employeeNo`, `name`, `role`). 응답 DTO에 `passwordHash`를 절대 넣지 않는다.
7. `AuthController` (`@Tag(name = "Auth")`, `@RequestMapping("/api/auth")`): `@PostMapping("/login")` 200, `@SecurityRequirements()`, `@ApiResponse` 200/400/401.
8. `MeController` (`@Tag(name = "Auth")`, `@GetMapping("/api/me")`): `@AuthenticationPrincipal Jwt jwt` → `authService.me(CurrentUser.from(jwt).getEmployeeId())`. `@ApiResponse` 200/401.

### 테스트
- `CurrentUserTest`: `from_returnsEmployeeIdAndRole()`, `from_nonNumericSubject_throws()`, `from_unknownRole_throws()` (`Jwt.withTokenValue("t").header("alg","HS256")...build()`로 만든다)
- `JwtIssuerTest`: 테스트 키로 만든 `NimbusJwtEncoder`/`NimbusJwtDecoder`와 고정 `Clock`. `issue_tokenHasSubjectRoleIssuerAndExpiry()` (exp = iat + 3600), `issue_expiresInIs3600()`
- `AuthServiceTest` (Mockito): `login_success_returnsTokenAndUser()`, `login_wrongPassword_throwsInvalidCredentials()`, `login_unknownEmployeeNo_throwsSameException_andChecksDummyHash()`, `me_returnsUser()`, `me_unknown_throwsUnauthorized()`
- `AuthControllerTest` (`@WebMvcTest({AuthController.class, MeController.class})`, `@Import({SecurityConfig.class, JsonSecurityErrorHandler.class})`, `@MockBean AuthService`): `login_returns200()`(`$.accessToken`, `$.user.employeeNo`, `$.user.passwordHash` 없음), `login_invalid_returns401()`(code `INVALID_CREDENTIALS`), `login_blank_returns400()`(`VALIDATION_FAILED`), `me_noToken_returns401()`, `me_withJwt_returns200()`(`jwt().jwt(j -> j.subject("7").claim("role","EMPLOYEE"))`, 서비스 인자 7)

### 완료 명령
```bash
./gradlew spotlessApply
./gradlew test --tests "com.example.eduhub.auth.*"
```

---

## T4. 신청자를 토큰에서 꺼내기 (FR-04 TODO 해결) + 문서

### 변경 파일
| 구분 | 파일 |
|---|---|
| 수정 | `src/main/java/com/example/eduhub/enrollment/controller/EnrollmentController.java` |
| 수정 | `src/main/java/com/example/eduhub/enrollment/controller/EnrollmentExceptionHandler.java` |
| 수정 | `src/main/java/com/example/eduhub/course/controller/CourseAdminController.java` (Javadoc만) |
| 수정 | `src/test/java/com/example/eduhub/enrollment/controller/EnrollmentControllerTest.java` |
| 수정 | `backend/CLAUDE.md` |
| 수정 | `docs/design/enrollment.md` |

### 내용
1. `EnrollmentController`: `EMPLOYEE_ID_HEADER` 상수와 `@RequestHeader` 인자를 지운다. `@AuthenticationPrincipal Jwt jwt`를 받아 `enrollmentCommandService.enroll(sessionId, CurrentUser.from(jwt).getEmployeeId())`. Javadoc의 `TODO(NFR-03)`를 "신청자는 토큰의 사원(NFR-03)"으로 바꾼다. `@ApiResponse` 400 설명에서 `MISSING_EMPLOYEE_ID`를 지우고 401(`UNAUTHORIZED`)을 추가한다. `@Parameter` import 정리.
2. `EnrollmentExceptionHandler`: `MissingRequestHeaderException` 처리와 헤더 분기를 지우고, `MethodArgumentTypeMismatchException` → 400 `INVALID_REQUEST`만 남긴다. Javadoc도 맞춘다.
3. `CourseAdminController`: Javadoc의 `TODO(NFR-03)` 문단을 "권한: ADMIN만 호출할 수 있다 (NFR-03, SecurityConfig)"로 바꾼다. 코드는 건드리지 않는다.
4. `EnrollmentControllerTest`: 헤더를 모두 지우고 `jwt().jwt(j -> j.subject("7").claim("role", "EMPLOYEE"))`로 바꾼다. `MISSING_EMPLOYEE_ID` 테스트 2개를 지우고 `enroll_noToken_returns401()`(서비스 호출 없음)을 추가한다. `ArgumentCaptor` 테스트는 "토큰 sub 7이 서비스 인자로 전달됨"으로 바꾼다.
5. `backend/CLAUDE.md` 계층 규칙의 의존 방향 줄: "`enrollment → course`, `enrollment → auth`, `auth → common`만 허용한다. `common`은 도메인 패키지에 의존하지 않는다". 패키지 줄에 `auth`를 추가한다.
6. `docs/design/enrollment.md`: `X-Employee-Id`·`MISSING_EMPLOYEE_ID`가 나오는 곳에 "NFR-03에서 토큰의 사원으로 바뀜 (`docs/design/auth.md`)" 한 줄을 덧붙인다. 기존 문장은 지우지 않는다(기록 유지).

### 완료 명령
```bash
./gradlew spotlessApply
./gradlew test --tests "com.example.eduhub.enrollment.controller.*" --tests "com.example.eduhub.course.controller.*"
```

---

## T5. 개발용 계정 + 통합 테스트

### 변경 파일
| 구분 | 파일 |
|---|---|
| 추가 | `src/main/java/com/example/eduhub/auth/DevAccountInitializer.java` |
| 수정 | `src/main/resources/application-dev.yml` |
| 추가 | `src/test/java/com/example/eduhub/auth/DevAccountInitializerTest.java` |
| 추가 | `src/test/java/com/example/eduhub/auth/SecurityIntegrationTest.java` |

### 내용
1. `application-dev.yml`에 `eduhub.dev-password: ${EDUHUB_DEV_PASSWORD:}` 추가 (빈 값 허용).
2. `DevAccountInitializer` (`@Component @Profile("dev")`, `ApplicationRunner`)
   - `@Value("${eduhub.dev-password:}")`. 비어 있으면 `log.warn("EDUHUB_DEV_PASSWORD가 없어 개발용 계정을 만들지 않습니다")` 후 끝.
   - 계정 `E0001`/사원1/EMPLOYEE, `E0002`/사원2/EMPLOYEE, `A0001`/관리자/ADMIN. `existsByEmployeeNo`면 건너뛰고, 아니면 BCrypt 해시로 저장. 만든 사번만 `log.info`로 남긴다(비밀번호는 로그에 쓰지 않는다).
   - 트랜잭션은 `TransactionTemplate` 대신 `EmployeeRepository.save` 단건 저장으로 충분하다(계정마다 독립).
3. `SecurityIntegrationTest` (`@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")`, H2)
   - `@BeforeEach`: `EmployeeRepository`로 사원 `T0001`(EMPLOYEE), 관리자 `T9001`(ADMIN)을 BCrypt 해시로 저장. `CourseRepository`·`CourseSessionRepository`로 열린 과정과 회차 하나(정원 5) 저장. `@AfterEach`에서 enrollment → session → course → employee 순으로 지운다.
   - 시나리오 (메서드 이름 `동작_결과()`)
     - `login_success_returnsToken()`, 그 토큰으로 `/api/me` 200, `$.employeeNo` = `T0001`
     - `login_wrongPassword_returns401()`, `login_unknownEmployeeNo_returns401()`: 둘 다 code `INVALID_CREDENTIALS`, 메시지 같음
     - `me_noToken_returns401()`: code `UNAUTHORIZED`
     - `me_forgedToken_returns401()`: 다른 키(32바이트 이상)로 서명한 토큰
     - `me_expiredToken_returns401()`: 같은 키, `expiresAt`이 과거인 토큰을 `NimbusJwtEncoder`로 직접 만든다
     - `me_algNoneToken_returns401()`: 헤더 `{"alg":"none"}`, 서명 없는 토큰(`header.payload.`)을 Base64URL로 직접 만든다 (T2 검토 경고 2)
     - `me_nonNumericSubjectToken_returns401()`: 같은 키로 서명했지만 `sub`가 숫자가 아닌 토큰 → 401 JSON `UNAUTHORIZED` (컨트롤러 안에서 던진 `InvalidBearerTokenException`이 진입점까지 가는지 확인, T3 검토 참고 3)
     - `enroll_employeeIdIsTokenSubject()`: 사원 토큰으로 신청 201, 응답 `$.employeeId`와 DB 저장값이 `T0001`의 id
     - `createCourse_noToken_401()`, `createCourse_employee_403()`, `createCourse_admin_201()`
     - `adminApi_employee_returns403()`: 사원 토큰으로 `GET /api/admin/anything` → 403 `FORBIDDEN`
     - `getCourses_noToken_returns200()`
4. `DevAccountInitializerTest` (Mockito): `run_noPassword_createsNothing()`, `run_createsThreeAccounts()`(ADMIN 1, EMPLOYEE 2), `run_existingAccount_skipped()`

### 완료 명령
```bash
./gradlew spotlessApply
./gradlew test --tests "com.example.eduhub.auth.*"
```

---

## 위험 영역 (reviewer와 critical-reviewer가 중점적으로 본다)
- **권한 규칙 순서:** `GET /api/v1/courses/**` 공개 규칙이 ADMIN 규칙보다 먼저 와야 한다. `/api/v1/admin/**`가 공개 규칙에 걸리지 않는지 확인한다.
- **토큰 검증:** 디코더가 알고리즘(HS256)과 발급자, 만료를 모두 검사하는지. `alg: none` 토큰이 거절되는지.
- **비밀값:** 실제 키·비밀번호가 `src/main`, `application*.yml`, 로그에 없는지. 테스트 키는 `src/test/resources`에만 있는지.
- **로그인 실패 정보 노출:** 사번 없음과 비밀번호 틀림의 응답 code·메시지가 같고, 더미 해시 비교로 시간 차이를 줄였는지.
- **응답 DTO:** `passwordHash`가 어떤 응답에도 나가지 않는지.
- **신청자 위조:** 신청 API가 요청의 어떤 값(헤더, 본문)도 신청자로 쓰지 않고 토큰 `sub`만 쓰는지.
- **기존 동작:** 과정 조회와 health는 토큰 없이 그대로 200인지. 동시성 테스트(pgtest)가 그대로 통과하는지.

## 전체 확인 (모든 작업이 끝난 뒤)
```bash
./gradlew spotlessCheck test
```
- 로컬 PG(`eduhub_test`)와 루트 `.env`가 필요하다(pgtest).
- 그다음 critical-reviewer 최종 검토 → 개발자가 dev 프로필로 `bootRun`하고 설계명세 7장의 curl을 확인한다. `bootRun`에는 `EDUHUB_JWT_SECRET`, `EDUHUB_DEV_PASSWORD` 환경 변수가 필요하다(루트 `.env`에 넣으면 dev 프로필이 읽는다).
