# 설계명세: FR-07 내 신청 내역 조회

- 상태: 승인됨
- 범위: 로그인한 임직원이 자기 수강 신청 내역을 상태별로 조회하는 백엔드 API
- 범위 밖: 화면, FR-08(회차별 명단·수료 처리), 취소(`CANCELLED`, FR-06), 수료 상태, 페이지 나누기
- 관련: `docs/requirements.md` FR-05, FR-07, NFR-02, NFR-03, NFR-04, D-04(이번에 추가)
- 선행 작업: NFR-03(JWT 인증, `CurrentUser`). main에 병합된 뒤 구현을 시작한다 (개발자 결정)

## 1. 요구사항 해석
| ID | 원문 | 이번 작업에서 정한 것 |
|---|---|---|
| FR-07 | 내 신청 내역(확정, 대기, 취소, 수료)을 조회한다 | 지금 있는 상태는 `CONFIRMED`, `WAITING`뿐이다. FR-07은 enum에 값을 추가하지 않는다. 수료(`COMPLETED`)는 FR-08이, 취소는 FR-06이 추가하며, 추가되면 이 API에도 코드 변경 없이 나온다 (`?status=COMPLETED` 포함). 상태 필터를 생략하면 전체를 돌려준다 |
| FR-05 | 대기 순번을 알려 준다 | 순번은 저장하지 않고 조회할 때 계산한다. 이 API에서는 `WAITING`일 때만 값이 있다 |
| NFR-02 | N+1 금지 | 과정, 회차, 대기 순번을 SQL 1개로 가져온다. 테스트로 SQL 수를 확인한다 |
| NFR-03 | 인증 | 임직원 ID는 요청 파라미터가 아니라 JWT에서 꺼낸다: `CurrentUser.from(jwt).getEmployeeId()` |
| D-04 (신규) | — | 대기 순번은 같은 회차의 `WAITING` 신청 사이에서 신청 일시(`created_at`) 오름차순, 같으면 `id` 오름차순으로 정한다 (개발자 승인) |

## 2. API
`GET /api/me/enrollments?status={CONFIRMED|WAITING}`

- 경로는 NFR-03의 `GET /api/me`에 맞춘다. 팀 스킬의 `/api/v1/...` 규칙과 다르지만, 로그인 사용자 기준 리소스는 `/api/me` 아래에 둔다
- `status`는 선택이다. 생략하면 전체를 돌려준다
- 정렬: 신청 일시 내림차순, 같으면 `id` 내림차순 (최근 신청순)
- 페이지는 나누지 않고 목록 전체를 돌려준다. 한 사람의 신청 건수가 적기 때문이다

### 응답 `200 OK`: `MyEnrollmentResponse[]`
| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | Long | 신청 ID |
| `sessionId` | Long | 회차 ID |
| `courseTitle` | String | 과정 제목 |
| `startDate` | LocalDate | 회차 시작일 |
| `place` | String | 회차 장소 |
| `status` | EnrollmentStatus | 신청 상태 |
| `waitingNumber` | Integer (nullable) | 대기 순번. `WAITING`일 때만 값이 있고, 그 밖에는 `null` |
| `createdAt` | LocalDateTime | 신청 일시 |

### 에러
| 상태 | code | 조건 | 처리 위치 |
|---|---|---|---|
| 400 | `INVALID_REQUEST` | `status` 값이 enum에 없음 (예: `?status=FOO`) | 기존 `EnrollmentExceptionHandler`의 `MethodArgumentTypeMismatchException` 처리 |
| 401 | (NFR-03 형식) | 토큰 없음·만료 | NFR-03의 보안 설정 |

다른 사람의 신청은 쿼리 조건(`employee_id`)으로 처음부터 걸러지므로 403은 없다.

## 3. 쿼리 설계

### 방식: JPQL 상관 서브쿼리 (네이티브 쿼리를 쓰지 않는다)
```
select new com.example.eduhub.enrollment.repository.MyEnrollmentRow(
         e.id, s.id, c.title, s.startDate, s.place, e.status, e.createdAt,
         (select count(w) from Enrollment w
           where w.session = s
             and w.status = com.example.eduhub.enrollment.domain.EnrollmentStatus.WAITING
             and (w.createdAt < e.createdAt
                  or (w.createdAt = e.createdAt and w.id < e.id))))
from Enrollment e
  join e.session s
  join s.course c
where e.employeeId = :employeeId
  and (:status is null or e.status = :status)
order by e.createdAt desc, e.id desc
```
- **대기 순번:** 서브쿼리 결과(나보다 앞선 대기 수)에 1을 더한다. 상태가 `WAITING`일 때만 값을 넣는다. 서브쿼리는 확정 건에서도 실행되지만, 한 사람의 신청 수가 적어 부담이 작다
- **N+1 없음:** 엔티티가 아니라 행 클래스(`MyEnrollmentRow`)로 바로 받는다. 지연 로딩이 일어나지 않아 SQL은 1개다

### 네이티브 쿼리 + `ROW_NUMBER()`를 쓰지 않는 이유
- Hibernate 5.6의 JPQL에는 윈도 함수가 없어 네이티브 쿼리가 필요하다
- 순번은 내 신청으로 거르기 전에 회차 전체의 대기자 사이에서 매겨야 한다. 그래서 파생 테이블이나 CTE가 필요하다
- 네이티브 결과는 H2와 PostgreSQL이 날짜·enum 타입을 다르게 돌려준다. 두 DB에서 같게 동작하는지 따로 확인해야 한다
- JPQL은 두 DB에서 같은 SQL로 바뀌고, 엔티티 이름이 바뀌면 컴파일 단계에서 오류가 난다

### 인덱스: `V5__enrollment_employee_index.sql` (공통 `db/migration`)
```sql
-- 내 신청 내역 조회 (FR-07)
CREATE INDEX idx_enrollment_employee_created ON enrollment (employee_id, created_at);
```
- 지금 있는 인덱스는 `(session_id, status)`와 `(session_id, employee_id)` 부분 유일 인덱스다. 둘 다 `session_id`가 앞에 있어서 `employee_id` 조건에는 쓰이지 않는다
- 서브쿼리는 기존 `idx_enrollment_session_status`를 쓴다
- V4는 NFR-03의 `V4__create_employee.sql`이다. 그래서 V5를 쓴다. FR-08과 번호가 겹치지 않게 조율한다

## 4. 구성
| 계층 | 클래스 | 내용 |
|---|---|---|
| Repository | `EnrollmentRepository.findMyEnrollments(Long employeeId, EnrollmentStatus status)` | 3장의 JPQL. `List<MyEnrollmentRow>`를 반환한다 |
| Repository | `MyEnrollmentRow` (새로 추가) | JPQL 생성자 식을 위한 행 클래스. public 생성자와 getter를 둔다. `precedingWaiting`은 `Long`이다 |
| Service | `MyEnrollmentQueryService` (새로 추가) | `@Transactional(readOnly = true)`. `List<MyEnrollmentResponse> findMine(Long employeeId, EnrollmentStatus status)` |
| DTO | `MyEnrollmentResponse` (새로 추가) | `final` 클래스. `from(MyEnrollmentRow)`에서 `WAITING`이면 `precedingWaiting + 1`, 아니면 `null`을 넣는다 |
| Controller | `EnrollmentController.myEnrollments(...)` | `@GetMapping("/api/me/enrollments")`. 클래스의 `@RequestMapping("/api/v1/sessions")` 때문에 메서드 경로가 이어 붙으므로, 클래스 매핑은 메서드 쪽으로 옮긴다 (5장) |

- 의존 방향: `enrollment → auth`(`CurrentUser`)가 새로 생긴다. NFR-03이 이미 `EnrollmentController`에서 같은 의존을 만든다

## 5. 컨트롤러 경로 처리
`EnrollmentController`는 클래스에 `@RequestMapping("/api/v1/sessions")`가 있다. 그대로 두면 새 메서드 경로가 `/api/v1/sessions/api/me/enrollments`가 된다. 그래서 클래스의 `@RequestMapping`을 지우고 기존 신청 메서드를 `@PostMapping("/api/v1/sessions/{sessionId}/enrollments")`로 바꾼다. 기존 API 경로는 바뀌지 않는다.

## 6. 테스트
| 종류 | 클래스 | 시나리오 |
|---|---|---|
| Repository (pgtest) | `EnrollmentRepositoryTest`에 추가 | 내 것만 나옴 / 상태 필터(`WAITING`, `CONFIRMED`, `null`=전체) / 같은 회차 대기 3명의 순번 0·1·2(앞선 수) / 다른 회차 대기자는 세지 않음 / `created_at`이 같으면 id 순 / 최근 신청순 정렬 / SQL 1개 |
| Service (Mockito) | `MyEnrollmentQueryServiceTest` | `WAITING`은 순번 = 앞선 수 + 1, `CONFIRMED`는 `null` / 상태 인자를 그대로 넘김 |
| Controller (`@WebMvcTest`) | `EnrollmentControllerTest`에 추가 | 200과 `jsonPath` / JWT의 임직원 ID를 서비스에 넘김 / `status` 생략 시 `null` 전달 / `?status=FOO` → 400 `INVALID_REQUEST` / 토큰 없음 → 401 |
| H2 | `EduhubApplicationTests` | V5 적용과 `ddl-auto: validate` |

### SQL 1개 검증: Hibernate `Statistics`
- 의존성을 추가하지 않는다 (datasource-proxy를 쓰지 않는다)
- 테스트 클래스에만 `@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")`를 켠다. 프로필 yml은 바꾸지 않는다
- `em.flush()`, `em.clear()`, `statistics.clear()` 뒤에 조회하고, `getPrepareStatementCount()`가 1인지 확인한다

## 7. 위험과 대안
- **`:status is null` 바인딩:** PostgreSQL은 타입을 모르는 null 파라미터를 거부할 수 있다. pgtest에서 `status = null` 경우를 반드시 실행한다. 실패하면 상태 있는/없는 메서드 두 개로 나누고, 서비스가 하나를 골라 부른다 (그래도 SQL은 1개다)
- **생성자 식 안의 서브쿼리:** Hibernate 5.6은 select 절 서브쿼리를 지원한다. 생성자 식에서 실패하면 인터페이스 프로젝션(`as` 별칭)으로 바꾼다
- **병합 충돌:** NFR-03과 FR-08이 `EnrollmentController`, `EnrollmentControllerTest`, `EnrollmentRepository`를 함께 바꾼다. NFR-03 병합 뒤에 시작해 충돌을 줄인다
