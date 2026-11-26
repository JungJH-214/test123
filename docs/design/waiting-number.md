# 설계명세: 신청 응답의 대기 순번 (FR-05)

- 상태: 승인됨 (Q1~Q3 추천안 채택)
- 범위: 수강 신청(`POST /api/v1/sessions/{sessionId}/enrollments`) 201 응답에 대기 순번 `waitingNumber`를 추가한다
- 범위 밖: 대기 등록 자체(FR-04에서 구현됨), 취소와 대기 승급(FR-06), 내 신청 내역의 순번 조회(FR-07), 화면
- 관련: `docs/requirements.md` FR-05, NFR-01, NFR-04, D-02, D-04, `docs/design/enrollment.md`(FR-04), `docs/design/my-enrollments.md`(FR-07, 별도 브랜치)

## 1. 요구사항 정리
| ID | 원문 | 이번 작업에서 정한 것 |
|---|---|---|
| FR-05 | 정원이 차면 대기자로 등록되고 대기 순번을 알려 준다 | 대기 등록과 대기 상한(정원의 50%, D-02)은 FR-04에 이미 있다. 남은 것은 "순번을 알려 준다"이다. 신청 응답에 `waitingNumber`를 넣는다 |
| D-04 | 대기 순번은 같은 회차의 대기 신청 사이에서 신청 일시가 빠른 순, 같으면 신청 ID가 작은 순 | 신청 시점의 순번은 "같은 회차의 기존 `WAITING` 수 + 1"이다. 근거는 4절 |
| NFR-01 | 동시에 신청해도 정합성 유지 | 동시에 대기로 들어온 신청들의 순번이 겹치거나 빠지지 않는다(1..N 연속). 기존 회차 행 락 안에서 계산한다 |
| NFR-04 | OpenAPI 문서 | 응답 스키마에 필드 설명을 단다. 아래 Q2(인증 파라미터 숨김)도 여기에 해당한다 |

### 질문 (승인 시 답 필요, 각 항목의 추천안으로 썼다)
- **Q1. 필드 이름:** `waitingNumber`(Integer, nullable). FR-07 설계(`MyEnrollmentResponse.waitingNumber`)와 같은 이름을 쓴다. **추천: 그대로**
- **Q2. OpenAPI 인증 파라미터:** `EnrollmentController.enroll`과 `MeController.me`의 `@AuthenticationPrincipal Jwt jwt`가 OpenAPI에 요청 파라미터(`jwt`)로 노출되어, 생성된 프론트 클라이언트가 `jwt` 인자를 요구한다(프론트는 `fetchMe()`에서 빈 객체로 우회 중). 두 곳에 `@Parameter(hidden = true)`를 붙여 이번에 같이 고칠까? **추천: 같이 고친다** (한 줄씩이고, FR-05 화면 작업이 이 API를 바로 쓴다)
- **Q3. 순번의 의미:** 응답의 순번은 신청한 순간의 값이다. 이후 취소·승급(FR-06)으로 바뀐 순번은 FR-07 조회 API로 본다. 신청 응답이 나중에 바뀐 값을 보장하지 않는다는 것을 API 설명에 적는다. **추천: 그대로**

## 2. API

### POST `/api/v1/sessions/{sessionId}/enrollments` — 수강 신청 (응답 변경)
요청, 상태 코드, 오류 코드는 FR-04와 같다. 응답 `EnrollmentResponse`에 필드 하나를 추가한다.

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | Long | 신청 ID (기존) |
| `sessionId` | Long | 회차 ID (기존) |
| `employeeId` | Long | 신청자 사원 ID (기존) |
| `status` | EnrollmentStatus | `CONFIRMED` 또는 `WAITING` (기존) |
| `createdAt` | LocalDateTime | 신청 일시 (기존) |
| **`waitingNumber`** | Integer, nullable | **추가.** `status`가 `WAITING`이면 1부터 시작하는 대기 순번, `CONFIRMED`면 `null`. 신청 시점의 값이다 |

- JSON에서 `null`도 그대로 내보낸다(필드를 생략하지 않는다). 기존 Jackson 설정을 바꾸지 않는다.
- `@Schema(description = "대기 순번. WAITING일 때만 값이 있고(1부터), 신청 시점 기준이다", nullable = true)`
- `@ApiResponse(201)` 설명을 "신청 성공 (status: CONFIRMED, 또는 WAITING과 waitingNumber)"로 바꾼다.

### 오류 코드
변경 없음. (400 `INVALID_REQUEST`, 401 `UNAUTHORIZED`, 404 `SESSION_NOT_FOUND`, 409 `SESSION_CLOSED`·`ENROLLMENT_DUPLICATE`·`SESSION_FULL`)

### Q2를 채택하면 (OpenAPI만 바뀜, 동작은 같음)
- `EnrollmentController.enroll(@PathVariable Long sessionId, @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt)`
- `MeController.me(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt)`
- 결과: `/v3/api-docs`에서 두 API의 `jwt` 쿼리 파라미터와 `Jwt` 스키마 참조가 사라진다. 프론트는 재생성 후 `fetchMe()` 우회를 지운다(프론트 작업에서 한다).

## 3. 스키마 변경
없음. 순번은 저장하지 않는다(FR-07 설계와 같다). Flyway 파일을 추가하지 않는다.

- 엔티티 `Enrollment`, `EnrollmentStatus`는 바꾸지 않는다.
- `EnrollmentResponse`: 필드 `waitingNumber`(Integer)와 getter를 추가한다. 팩토리는 아래 둘로 나눈다.
  - `EnrollmentResponse.from(Enrollment enrollment)` → `waitingNumber = null` (기존 호출과 호환)
  - `EnrollmentResponse.waiting(Enrollment enrollment, int waitingNumber)` → `status`가 `WAITING`이 아니면 `IllegalArgumentException`

## 4. 트랜잭션과 동시성

### 처리 순서 (`EnrollmentCommandService.enroll`, 기존 트랜잭션 하나 그대로)
1. `course_session` 행을 `PESSIMISTIC_WRITE`로 조회 (기존)
2. 종료 여부, 중복 신청 확인 (기존)
3. 확정 수 집계 → 정원 미만이면 `CONFIRMED` (기존)
4. 정원이 찼으면 대기 수 `waiting` 집계 → `waiting < capacity / 2`면 `WAITING` (기존), 아니면 409 `SESSION_FULL` (기존)
5. 저장 (`saveAndFlush`, 기존)
6. **추가:** `WAITING`이면 `waitingNumber = waiting + 1`로 응답을 만든다. 4단계에서 센 값을 그대로 쓰고 다시 조회하지 않는다

- 4단계의 판정과 6단계의 순번이 같은 집계값에서 나오도록 `decideStatus`가 상태와 대기 수를 함께 돌려주게 바꾼다(내부 클래스 `Decision { EnrollmentStatus status; long waitingBefore; }` 등, private).

### 순번이 D-04와 같은 이유
- 같은 회차의 신청은 회차 행 락으로 한 줄로 선다(FR-04). 그래서 4~5단계 사이에 같은 회차의 다른 신청이 끼어들지 않는다.
- 락 안에서 새로 저장한 신청은 그 회차의 `WAITING` 중 `id`가 가장 크고, `created_at`도 가장 늦거나 같다. D-04 순서(신청 일시 오름차순, 같으면 id 오름차순)에서 맨 뒤다. 그래서 순번은 "기존 `WAITING` 수 + 1"이다.
- FR-07의 조회 시 계산(상관 서브쿼리)과 같은 값을 낸다. 테스트로 확인한다(5절 R2, FR-07 병합 뒤).
- **주의:** `created_at`은 `LocalDateTime.now()`(애플리케이션 시각)다. 서버가 여러 대이고 시계가 어긋나면 D-04의 `created_at` 순서와 락 순서가 어긋날 수 있다. 지금은 서버 한 대라 문제가 없다. 위험 영역으로 기록한다.

### 락
추가 락 없음. 기존 회차 행 락이 순번 계산까지 덮는다. 락을 거는 시점, 트랜잭션 경계를 바꾸지 않는다.

## 5. 테스트 시나리오

### 서비스 단위 (`EnrollmentCommandServiceTest`, Mockito)
| ID | 시나리오 | 기대 |
|---|---|---|
| S1 | 정원 미만 | `status=CONFIRMED`, `waitingNumber=null` |
| S2 | 정원 10, 확정 10, 대기 0 | `status=WAITING`, `waitingNumber=1` |
| S3 | 정원 10, 확정 10, 대기 4 | `status=WAITING`, `waitingNumber=5` (마지막 대기 자리) |
| S4 | 정원 10, 확정 10, 대기 5 | 409 `SESSION_FULL`, 저장하지 않음 (기존 테스트 유지) |

### 컨트롤러 (`EnrollmentControllerTest`, `@WebMvcTest`)
| ID | 시나리오 | 기대 |
|---|---|---|
| C1 | 서비스가 대기 응답 반환 | 201, `$.status == "WAITING"`, `$.waitingNumber == 3` |
| C2 | 서비스가 확정 응답 반환 | 201, `$.waitingNumber`가 존재하고 `null` (`jsonPath(...).value(nullValue())` + `exists()`) |
| C3 (Q2) | OpenAPI 문서 | `/v3/api-docs`의 신청 API와 `/api/me`에 `jwt` 파라미터가 없다. `SecurityIntegrationTest` 또는 별도 `@SpringBootTest`에서 확인 |

### 동시성 (`EnrollmentConcurrencyTest`, pgtest)
| ID | 시나리오 | 기대 |
|---|---|---|
| R1 | 정원 10, 100명 동시 신청 (기존 시나리오) | 기존 단언 유지 + 성공한 응답 중 `WAITING` 5건의 `waitingNumber`가 정확히 {1,2,3,4,5} (중복·누락 없음), `CONFIRMED` 10건은 모두 `null` |
| R2 | 확정 9명 상태에서 100명 동시 신청 (기존) | 기존 단언 유지 + 대기 5건의 순번이 {1..5} |
| R3 | 응답 순번과 D-04 순서 비교 | 대기 신청을 `created_at, id` 오름차순으로 읽은 순서와 응답 순번이 같다. (FR-07이 먼저 병합되면 그 조회 API의 `waitingNumber`와도 비교한다) |

- 동시성 테스트는 지금처럼 서비스 `enroll()`을 직접 부르고, 성공 결과(`EnrollmentResponse`)를 스레드 안전한 리스트에 모은다.

### 회귀
- 기존 `./gradlew spotlessCheck test` 전체 통과 (H2 + pgtest)

## 6. 프론트 영향 (참고, 이번 범위 밖)
- 재생성하면 `EnrollmentResponse.waitingNumber?: number | null`이 생긴다. 신청 버튼이 "대기 N번으로 등록되었습니다"를 표시할 수 있다.
- Q2를 채택하면 `EnrollmentApi.enroll({ sessionId })`, `AuthApi.me()`로 쓸 수 있고 `fetchMe()` 우회를 지운다.

## 7. 위험 영역
- **락 범위:** 순번 계산을 락 밖(저장 후 별도 조회, 다른 트랜잭션)으로 옮기면 동시 신청의 순번이 겹친다. 4단계 집계값을 그대로 써야 한다.
- **FR-06과의 관계:** 취소·승급이 들어오면 `WAITING` 수가 줄어든다. 그때도 신청은 회차 락 안에서 "현재 `WAITING` 수 + 1"이므로 D-04와 맞는다. 단, FR-06의 승급도 같은 회차 락을 잡아야 한다(FR-06 설계에서 확인).
- **시계:** 4절 주의 참고.
- **FR-07과 병합 충돌:** 둘 다 `EnrollmentRepository`/`EnrollmentControllerTest`를 건드릴 수 있다. FR-05는 Repository를 바꾸지 않는다. 먼저 병합된 쪽 위로 rebase한다.
