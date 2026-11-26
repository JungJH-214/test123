# 설계명세: 인증 기반 (NFR-03)

- 상태: 승인됨 (Q1·Q2 추천안 채택), 구현 중 바뀐 점 반영(최종 검토)
- 범위: 개발용 로그인으로 JWT 발급, 모든 API에서 토큰으로 사용자 확인, 경로별 권한, FR-04 신청자 헤더(`X-Employee-Id`) 제거
- 범위 밖: 회원가입, 비밀번호 변경, 리프레시 토큰, 신청 취소(FR-06), FR-07·08 API, 프론트 로그인 화면
- 관련: `docs/requirements.md` NFR-03, NFR-04, FR-03, FR-04, `docs/design/enrollment.md`

## 1. 요구사항 정리
| ID | 내용 | 이 명세에서 다루는 부분 |
|---|---|---|
| NFR-03 | 로그인한 사용자만 신청 가능 | 신청 API에 인증 필수. 신청자는 토큰의 `sub`(사원 ID) |
| NFR-03 | 본인 신청만 취소 가능 | 취소 API(FR-06)가 아직 없다. `CurrentUser`만 준비하고, 검사는 FR-06에서 한다 (결정 2) |
| NFR-03 | 비밀 정보는 저장소에 두지 않음 | JWT 키는 환경 변수 `EDUHUB_JWT_SECRET`. 테스트 키는 테스트 클래스패스에만 둔다 |
| FR-03 | 관리자는 과정과 회차를 등록·수정·종료한다 | `/api/v1/admin/**`에 ADMIN 권한 |
| NFR-04 | 모든 API는 OpenAPI 문서가 있다 | 새 API에 `@Tag`/`@Operation`. Swagger UI에 Bearer 인증 추가 |

## 2. 결정 기록 (개발자 답변, 추천안 채택)
1. **employee:** 테이블이 없으므로 V4에서 새로 만든다. `enrollment.employee_id`에는 FK를 걸지 않는다 (동시성 테스트가 가짜 사원 ID 1~100을 쓴다).
2. **취소:** 취소 API와 "남의 신청 취소 404" 테스트는 이번 범위에서 뺀다. FR-06에서 한다.
3. **관리자 경로:** 실제 경로 `/api/v1/admin/**`와 요구사항의 `/api/admin/**`에 ADMIN을 건다. 요구사항 문구대로 GET이 아닌 `/api/v1/courses/**`도 ADMIN으로 둔다(지금은 해당 API 없음).
4. **추가 공개 경로:** `/api/health`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`.
5. **curl 확인 목록:** 7절.
6. **작업 위치:** worktree `.claude/worktrees/nfr-03-auth`, 브랜치 `feature/nfr-03-auth` (main에서 생성).

- **Q1. 개발용 계정 비밀번호:** 환경 변수 `EDUHUB_DEV_PASSWORD` 하나를 세 계정이 같이 쓴다. 값이 없으면 계정을 만들지 않고 경고 로그만 남긴다.
- **Q2. 의존 방향:** `backend/CLAUDE.md`의 의존 방향 규칙에 `enrollment → auth`, `auth → common`을 추가한다.

## 3. API

### POST `/api/auth/login` — 로그인 (공개)
요청 `LoginRequest`
| 필드 | 타입 | 검증 |
|---|---|---|
| `employeeNo` | String | `@NotBlank`, `@Size(max = 20)` |
| `password` | String | `@NotBlank`, `@Size(max = 100)` |

응답 `200 OK`, `LoginResponse`
| 필드 | 타입 | 설명 |
|---|---|---|
| `accessToken` | String | JWT |
| `tokenType` | String | 항상 `Bearer` |
| `expiresIn` | long | 만료까지 초 (3600) |
| `user` | `UserResponse` | 로그인한 사용자 |

- 생성은 `LoginResponse.of(accessToken, expiresIn, user)`(`tokenType`은 항상 `Bearer`).

- 사번이 없거나 비밀번호가 틀리면 둘 다 401 `INVALID_CREDENTIALS`, 같은 메시지("사번 또는 비밀번호가 올바르지 않습니다.")를 돌려준다.
- 사번이 없을 때도 미리 만든 더미 해시로 `passwordEncoder.matches`를 한 번 실행해 응답 시간 차이로 사번 존재 여부가 드러나지 않게 한다.

### GET `/api/me` — 현재 사용자 (인증 필요)
응답 `200 OK`, `UserResponse`
| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | Long | 사원 ID |
| `employeeNo` | String | 사번 |
| `name` | String | 이름 |
| `role` | String(enum) | `EMPLOYEE` 또는 `ADMIN` |

- 토큰의 `sub`로 DB에서 다시 읽는다. 토큰은 유효하지만 사원이 없으면 401 `UNAUTHORIZED`.

### POST `/api/v1/sessions/{sessionId}/enrollments` — 수강 신청 (변경)
- `X-Employee-Id` 헤더를 지운다. 신청자는 `CurrentUser.from(jwt).getEmployeeId()`.
- 400 `MISSING_EMPLOYEE_ID`는 없어진다. 토큰이 없거나 잘못되면 401 `UNAUTHORIZED`.
- `EnrollmentExceptionHandler`에서 헤더 처리 부분을 지운다. 경로 값 형식 오류(400 `INVALID_REQUEST`)는 그대로 둔다.
- `docs/design/enrollment.md`의 `TODO(NFR-03)` 부분에 "NFR-03에서 해결" 한 줄을 남긴다.

### 오류 코드 (추가분)
공통 형식은 기존 `ErrorResponse { code, message }`.

| HTTP | code | 조건 | 발생 위치 |
|---|---|---|---|
| 401 | `INVALID_CREDENTIALS` | 로그인 실패 (사번·비밀번호 구분 없음) | `auth/exception/InvalidCredentialsException extends common/error/UnauthorizedException` → `GlobalExceptionHandler` |
| 401 | `UNAUTHORIZED` | 토큰 없음, 서명 위조, 만료, 형식 오류, 토큰 사원 없음 | 보안 필터 체인의 `AuthenticationEntryPoint` (JSON 작성) |
| 403 | `FORBIDDEN` | 권한 부족 (EMPLOYEE가 관리자 API 호출) | 보안 필터 체인의 `AccessDeniedHandler` (JSON 작성) |

- `common/error/UnauthorizedException`(code 보유, `NotFoundException`과 같은 모양)을 새로 두고 `GlobalExceptionHandler`에 401 핸들러 하나를 추가한다. `common`은 `auth`에 의존하지 않는다.

## 4. 보안 설정

### 의존성 (`build.gradle.kts`)
- `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`
- 테스트: `spring-security-test`

### SecurityFilterChain (`auth/config/SecurityConfig`)
```java
// PUBLIC_PATHS = OrRequestMatcher(AntPathRequestMatcher 6개)
//   POST /api/auth/login, GET /api/v1/courses/**, /api/health, /v3/api-docs/**, /swagger-ui/**, /swagger-ui.html
http.csrf().disable()
    .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
    .authorizeHttpRequests(auth -> auth
        .requestMatchers(PUBLIC_PATHS).permitAll()
        .antMatchers("/api/v1/admin/**", "/api/admin/**").hasRole("ADMIN")
        .antMatchers("/api/v1/courses/**").hasRole("ADMIN")
        .anyRequest().authenticated())
    .oauth2ResourceServer(o -> o
        .bearerTokenResolver(request -> PUBLIC_PATHS.matches(request) ? null : defaultResolver.resolve(request))
        .jwt(j -> j.jwtAuthenticationConverter(jwtAuthenticationConverter()))
        .authenticationEntryPoint(entryPoint)
        .accessDeniedHandler(accessDeniedHandler))
    .exceptionHandling(e -> e
        .authenticationEntryPoint(entryPoint)
        .accessDeniedHandler(accessDeniedHandler));
```
- 공개 경로에서는 토큰을 읽지 않는다. `permitAll`과 `BearerTokenResolver`가 같은 `PUBLIC_PATHS` 매처를 써서 두 목록이 어긋나지 않는다. 그래서 만료·쓰레기 토큰을 붙여도 공개 API는 정상 응답한다(보호 경로는 그대로 401). `BearerTokenResolver`는 Spring 전략 인터페이스이지 필터가 아니다.
- 주의: 공개 경로에서는 토큰을 읽지 않으므로 `@AuthenticationPrincipal`이 항상 `null`이다. `GET /api/v1/courses/**` 아래에 사용자별 응답(예: 내 신청 상태)이나 로그인이 필요한 GET API를 추가하면 안 된다. 그런 API는 다른 경로에 두거나 `PUBLIC_PATHS`를 함께 고친다.
- 직접 만든 필터는 없다. 토큰 검증은 Spring의 `BearerTokenAuthenticationFilter`가 한다.
- `formLogin`, `httpBasic`은 쓰지 않는다(설정하지 않음). 기본 사용자(`UserDetailsService` 자동 설정)는 `JwtDecoder` 빈이 있으면 만들어지지 않는다.
- 권한 변환: `JwtGrantedAuthoritiesConverter`에 `authoritiesClaimName = "role"`, `authorityPrefix = "ROLE_"`.
- `entryPoint`/`accessDeniedHandler`는 `ObjectMapper`로 `ErrorResponse`를 써서 JSON(UTF-8)으로 응답한다.
- CORS: 프론트는 Vite 프록시(`/api` → 8080)로 같은 출처에서 부르므로 설정하지 않는다.

### JWT
| 항목 | 값 |
|---|---|
| 알고리즘 | HS256 |
| 키 | `eduhub.jwt.secret: ${EDUHUB_JWT_SECRET}` (UTF-8 바이트, 32바이트 이상. 짧거나 없으면 시작할 때 실패) |
| 만료 | `eduhub.jwt.expires-in: 60m` |
| 클레임 | `iss`=`eduhub`, `sub`=사원 ID(문자열), `role`=`EMPLOYEE`/`ADMIN`, `iat`, `exp` |
| 발급자 설정 | `eduhub.jwt.issuer: eduhub` (`JwtProperties.issuer`, `@NotBlank`) |
| 발급 | `NimbusJwtEncoder(new ImmutableSecret<>(key))`, `JwsHeader.with(MacAlgorithm.HS256)` |
| 검증 | `NimbusJwtDecoder.withSecretKey(key).macAlgorithm(HS256)` + 기본 검증(만료) + 발급자(`iss`) 검증 + `exp` 필수 검증(`JwtClaimValidator(EXP, Objects::nonNull)`, `DelegatingOAuth2TokenValidator`로 결합) |

- 설정 클래스 `auth/config/JwtProperties` (`@ConfigurationProperties("eduhub.jwt")`, `@Validated`, `secret`·`issuer` `@NotBlank`, `expiresIn` `Duration`).
- `.env.example`에 `EDUHUB_JWT_SECRET=`(이름만)과 `EDUHUB_DEV_PASSWORD=`를 추가한다.
- 테스트 키: `src/test/resources/config/application.yml`에만 둔다. 테스트 클래스패스에만 있어 `bootRun`과 jar에는 들어가지 않고, 모든 테스트 프로필(test, pgtest, `@WebMvcTest`)에 적용된다.

### CurrentUser (`auth/CurrentUser`)
- `final` 클래스, 필드 `employeeId`(Long), `role`(Role). `static CurrentUser from(Jwt jwt)` 하나로 만든다.
- 컨트롤러는 `@AuthenticationPrincipal Jwt jwt`를 받아 `CurrentUser.from(jwt)`를 부른다. 토큰 내용이 잘못되면 401이 되도록 `InvalidBearerTokenException`을 던진다 (정상 발급 토큰에서는 일어나지 않음).
- `sub`·`role` 누락이나 오류(숫자 아님, 없는 enum)도 모두 `InvalidBearerTokenException`(401)이다. 이 예외는 `ExceptionTranslationFilter`가 401로 바꾼다. `GlobalExceptionHandler`에 `@ExceptionHandler(Exception.class)`를 추가하면 500이 되니 주의한다.

### OpenAPI
- `OpenApiConfig`에 `bearerAuth`(HTTP bearer, JWT) 보안 스키마를 추가하고 전역 요구사항으로 건다. 공개 API(`login`, 과정 조회, health)에는 `@SecurityRequirements()`로 표시를 뺀다.

## 5. 스키마 변경

### V4: `db/migration/V4__create_employee.sql` (H2, PostgreSQL 공통)
```sql
-- 임직원 계정 (NFR-03)
CREATE TABLE employee (
    id            BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    employee_no   VARCHAR(20)  NOT NULL,
    name          VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    CONSTRAINT ux_employee_employee_no UNIQUE (employee_no),
    CONSTRAINT ck_employee_role CHECK (role IN ('EMPLOYEE', 'ADMIN'))
);
```
- 번호: 공통 V1, V2와 vendor V3 다음이므로 V4다.
- `enrollment.employee_id` FK는 걸지 않는다(결정 1). FR-07 이후 필요하면 별도 마이그레이션으로 검토한다.

### 엔티티 `auth/domain/Employee`, `auth/domain/Role`
- 필드: `id`, `employeeNo`, `name`, `passwordHash`, `role`(`@Enumerated(STRING)`). `@Getter`, `@NoArgsConstructor(access = PROTECTED)`, `@Setter` 없음.
- 생성: `Employee.create(employeeNo, name, passwordHash, role)`.
- 비밀번호 해시: `BCryptPasswordEncoder` (`PasswordEncoder` 빈, `SecurityConfig`).

### Repository `auth/repository/EmployeeRepository`
- `Optional<Employee> findByEmployeeNo(String employeeNo)`, `boolean existsByEmployeeNo(String employeeNo)`

## 6. 서비스와 트랜잭션
- `auth/service/AuthService` (`@Transactional(readOnly = true)`)
  - `LoginResponse login(LoginRequest)`: 사번 조회 → 비밀번호 확인 → `JwtIssuer.issue(employee)` → 응답
  - `UserResponse me(Long employeeId)`: 사원 조회. 없으면 `UnauthorizedException("UNAUTHORIZED")`
- `auth/service/JwtIssuer`: `JwtEncoder`, `JwtProperties`, `Clock`을 받아 토큰을 만든다. `Clock`은 만료 테스트를 위해 주입한다.
- `auth/DevAccountInitializer` (`@Profile("dev")`, `ApplicationRunner`, `@Transactional` 없음 - 계정별 단건 저장)
  - 생성자 `@Value("${eduhub.dev-password:}")`, `application-dev.yml`의 `eduhub.dev-password: ${EDUHUB_DEV_PASSWORD:}`로 받는다
  - 계정: `E0001` 사원1(EMPLOYEE), `E0002` 사원2(EMPLOYEE), `A0001` 관리자(ADMIN)
  - 사번이 이미 있으면 건너뛴다(재시작해도 안전). 동시에 두 서버가 떠서 겹치면 유일 제약이 막고, `DataIntegrityViolationException`은 잡아서 사번만 `log.info`로 남기고 계속한다.
  - 비밀번호를 바꿔도 이미 있는 계정은 바뀌지 않는다.
  - 비밀번호는 `EDUHUB_DEV_PASSWORD`. 없으면 계정을 만들지 않고 경고 로그를 남긴다 (Q1).
- 수량 변경이 없으므로 락은 쓰지 않는다. 사번 중복은 DB 유일 제약으로 보장한다.

## 7. 테스트 시나리오
| 종류 | 클래스 | 시나리오 |
|---|---|---|
| Service (Mockito) | `AuthServiceTest` | 로그인 성공 → 토큰·사용자 반환 / 비밀번호 틀림 → `InvalidCredentialsException` / 없는 사번 → 같은 예외, 더미 해시로 `matches` 호출 / `me` 성공 / `me` 사원 없음 → `UnauthorizedException` |
| 단위 | `JwtIssuerTest` | 발급한 토큰을 같은 키의 디코더로 읽으면 `sub`, `role`, `iss`, `exp`(=iat+3600) 일치 |
| 단위 | `CurrentUserTest` | `from(jwt)`가 사원 ID와 역할을 꺼냄 / `sub`가 숫자가 아니면 예외 / `role`이 없거나 잘못되면 `InvalidBearerTokenException` (`sub` 누락은 코드에서 처리하지만 단위 테스트는 없음) |
| 단위 (Mockito) | `DevAccountInitializerTest` | 비밀번호 없음 → 생성 안 함 / 3계정 생성 / 이미 있는 사번 건너뜀 / 저장 중 `DataIntegrityViolationException` → 계속 진행 |
| Controller (`@WebMvcTest` + `@Import(SecurityConfig)`) | `AuthControllerTest` | 로그인 200 `jsonPath` / 실패 401 `INVALID_CREDENTIALS` / 빈 입력 400 `VALIDATION_FAILED` / `/api/me` 토큰 없음 401, `jwt()`로 200 |
| Controller (수정) | `EnrollmentControllerTest` | 헤더 대신 `jwt().jwt(j -> j.subject("7").claim("role","EMPLOYEE"))` / 신청자 = 토큰 `sub`(`ArgumentCaptor`) / 토큰 없음 401 / 기존 404·409 유지 / `MISSING_EMPLOYEE_ID` 테스트 삭제 |
| Controller (수정) | `CourseAdminControllerTest` | 기존 테스트에 ADMIN `jwt()` 추가 / 토큰 없음 401 / EMPLOYEE 403 |
| Controller (수정) | `CourseControllerTest`, `HealthControllerTest` | `@Import(SecurityConfig)` 추가, 토큰 없이 200 (공개 확인) |
| 통합 (`@SpringBootTest` + MockMvc, test 프로필 H2) | `SecurityIntegrationTest` | 실제 로그인 성공 → 토큰으로 `/api/me` 200 / 틀린 비밀번호·없는 사번 둘 다 401 같은 code / 토큰 없음 401 / 다른 키로 서명한 위조 토큰 401 / 만료 토큰 401 / 사원 토큰으로 신청 → 응답·DB의 `employeeId` = 토큰 사원 / 과정 등록: 토큰 없음 401, 사원 403, 관리자 201 / 사원이 `/api/admin/anything` 403 / `GET /api/v1/courses` 토큰 없이 200 / 만료 토큰으로 로그인 200 / 쓰레기 토큰으로 과정 조회 200 / 쓰레기 토큰으로 `/api/me` 401 / 쓰레기 토큰으로 과정 등록 401 / `iss=other` 토큰 401 / `exp` 없는 토큰 401 |

- 기존 `EnrollmentConcurrencyTest`(pgtest)는 서비스를 직접 부르므로 바뀌지 않는다. 테스트 키가 `config/application.yml`에 있어 컨텍스트가 뜬다.

### curl 확인 (dev 프로필, `EDUHUB_JWT_SECRET`, `EDUHUB_DEV_PASSWORD` 설정 후 `bootRun`)
```bash
# 1. 로그인 성공 → 200, accessToken
curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"employeeNo":"E0001","password":"<EDUHUB_DEV_PASSWORD>"}'
# 2. 비밀번호 틀림 → 401 INVALID_CREDENTIALS
# 3. /api/me 토큰 없음 → 401, 토큰 있음 → 200
curl -s -i localhost:8080/api/me
curl -s localhost:8080/api/me -H "Authorization: Bearer $EMP"
# 4. 사원 토큰으로 신청 → 201, employeeId = 사원1 ID
curl -s -X POST localhost:8080/api/v1/sessions/1/enrollments -H "Authorization: Bearer $EMP"
# 5. 과정 등록: 사원 토큰 403, 관리자 토큰 201
curl -s -i -X POST localhost:8080/api/v1/admin/courses -H "Authorization: Bearer $EMP" \
  -H 'Content-Type: application/json' -d '{"title":"보안 기초","category":"IT","description":"x"}'
```
- 개발 DB `eduhub`에는 지금 V1만 있다. `bootRun`하면 V2~V4가 적용된다.

## 8. 위험과 영향
- **프론트:** 관리 화면(FR-03)과 신청 화면은 로그인 화면이 생길 때까지 401/403을 받는다. 과정 조회 화면은 공개라 영향이 없다. 프론트 API 클라이언트를 재생성하면 신청 API의 `xEmployeeId` 인자가 사라진다(생성 코드 변경, 이번 범위에서는 재생성하지 않음).
- **키 관리:** `EDUHUB_JWT_SECRET`이 없으면 서버가 뜨지 않는다. 의도한 동작이다(약한 기본값 금지).
- **토큰 무효화:** 리프레시·로그아웃이 없어 발급된 토큰은 60분 동안 유효하다. 역할이 바뀌어도 토큰의 `role`은 만료 전까지 그대로다.
- **프로필:** 기본 프로필이 dev라 운영 배포 시 프로필을 반드시 지정한다.
- **Swagger:** Swagger UI와 `/v3/api-docs`는 모든 프로필에서 공개다. 운영 전에 재검토한다.
- **dev 로그:** `BasicBinder` 로그를 trace에서 info로 내렸다(바인딩 파라미터에 password_hash가 찍힘). `org.hibernate.SQL: debug`는 유지한다.
