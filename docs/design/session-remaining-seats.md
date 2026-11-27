# 설계명세: 회차 남은 자리 계산과 명단 API 태그 수정 (FR-02, FR-08 결함)

- 상태: 승인됨 (2026-11-27, Q1 방안 A, Q2 `Roster`, Q4 관리자 API도 함께 고침)
- 구현계획: `docs/plans/session-remaining-seats.md`
- 브랜치: `fix/session-remaining-seats` (main `be0136b`에서 시작)
- 관련: FR-02, FR-04, FR-06, FR-08, D-05, NFR-02, NFR-04
- 범위 밖: 프론트 API 클라이언트 재생성과 화면(프론트 작업에서 한다), 과정 목록 API

## 1. 결함
| # | 현상 | 원인 |
|---|---|---|
| B1 | `GET /api/v1/courses/{id}`의 `sessions[].remainingSeats`가 신청·수료 뒤에도 정원과 같다 | `SessionResponse.from(CourseSession)`이 FR-02 당시 "신청이 없다"는 전제로 `capacity`를 그대로 넣는다 |
| B2 | 명단·수료 API(`/api/admin/sessions/{sessionId}/roster`, `/completions`)의 생성 클라이언트가 `DefaultApi`로 나온다 | `@Tag`는 있지만 이름이 한글 `관리자-수강`이다. openapi-generator(typescript-axios)는 태그 이름으로 클래스 이름을 만드는데, 한글을 식별자로 바꾸지 못해 `DefaultApi`로 대체한다. 다른 컨트롤러 태그는 모두 영문(`Course`, `CourseAdmin`, `Enrollment`, `Auth`, `Health`)이다 |

## 2. B1 — 남은 자리 계산
### 2.1 규칙
- 회차별 `remainingSeats = max(0, capacity − 정원을 차지한 수)`
- 정원을 차지한 수 = 그 회차의 `CONFIRMED` + `COMPLETED` 신청 수 (D-05). `EnrollmentCommandService.countSeatsTaken`과 같은 기준이다. `WAITING`, `CANCELLED`는 넣지 않는다
- 회차 상태(OPEN/CLOSED)와 관계없이 같은 식으로 계산한다
- 표시용 조회이므로 락을 잡지 않는다. 신청 판정은 지금처럼 `EnrollmentCommandService`가 회차 락 안에서 다시 센다 (조회 직후 값이 바뀔 수 있음은 허용)
- 응답 형식(필드 이름·타입)은 바뀌지 않는다. 프론트 클라이언트 재생성이 필요 없다

### 2.2 집계 위치 (Q1 — 개발자 결정 필요)
`course → enrollment` 의존은 금지다(`backend/CLAUDE.md`, `course/package-info.java`). 방안 셋을 비교한다.

| 방안 | 내용 | 장점 | 단점 |
|---|---|---|---|
| **A. 포트 역전 (권장)** | course에 인터페이스 `SessionSeatCounter`(`Map<Long, Long> countSeatsTaken(Collection<Long> sessionIds)`)를 두고, enrollment가 구현한다(`enrollment → course`는 허용). `CourseQueryService`는 인터페이스만 주입받는다 | 의존 방향 유지. D-05 기준(`SEAT_STATUSES`)이 enrollment 한 곳에 남아 `countSeatsTaken`과 어긋날 일이 없다. course 단위 테스트는 인터페이스를 가짜로 바꿔 끼우면 된다 | 파일이 하나(인터페이스) + 구현 하나 늘어난다. 실행 시 enrollment 구현 빈이 없으면 course 상세가 뜨지 않는다(같은 애플리케이션이라 실제로는 문제없음) |
| B. course 리포지토리의 집계 쿼리 | `CourseSessionRepository`에 회차별 집계를 직접 쓴다. JPQL로 `Enrollment` 엔티티와 `EnrollmentStatus` 상수를 문자열로 참조하거나, 네이티브 SQL로 `enrollment` 테이블과 `'CONFIRMED'`, `'COMPLETED'` 문자열을 쓴다 | 파일 추가가 가장 적다 | import는 없어도 course가 enrollment 테이블·상태를 안다(숨은 의존). D-05 기준이 두 곳에 생겨 한쪽만 바뀌면 남은 자리와 신청 판정이 어긋난다 |
| C. 정원 차지 수 컬럼 | `course_session.seats_taken` 컬럼을 두고 신청·취소·승급·수료 때 락 안에서 갱신한다 | 조회가 가장 싸다 | Flyway 변경, 갱신 지점 4곳, 기존 데이터 보정, 카운터와 실제 행이 어긋날 위험. 결함 수정치고 범위가 크다 |

### 2.3 방안 A 상세
- `course/service/SessionSeatCounter.java` (인터페이스, 신규)
  - `Map<Long, Long> countSeatsTaken(Collection<Long> sessionIds)` — 회차 ID → 정원을 차지한 수. 신청이 없는 회차는 맵에 없을 수 있다(호출 쪽이 0으로 본다). 빈 컬렉션이면 쿼리 없이 빈 맵
- `enrollment/repository/EnrollmentRepository` — 집계 쿼리 추가 (쿼리 1개, `group by`)
  ```
  select e.session.id as sessionId, count(e) as seatsTaken
  from Enrollment e
  where e.session.id in :sessionIds and e.status in :statuses
  group by e.session.id
  ```
  결과는 인터페이스 projection `SessionSeatCountRow(getSessionId, getSeatsTaken)`
- `enrollment/service/EnrollmentSeatCounter.java` (`@Component`, `SessionSeatCounter` 구현, 신규, `@Transactional(readOnly = true)`) — `SEAT_STATUSES`는 `EnrollmentCommandService`와 같은 상수를 쓴다. 상수를 `EnrollmentStatus.SEAT_STATUSES`(정적 불변 목록)로 옮겨 두 곳이 함께 쓴다
- `CourseQueryService.getDetail` — 회차 목록을 읽은 뒤 ID 목록으로 `countSeatsTaken`을 한 번 부르고, `SessionResponse.of(session, seatsTaken)`으로 만든다
- `SessionResponse` — `from(CourseSession)`을 `of(CourseSession, long seatsTaken)`으로 바꾼다. `remainingSeats = (int) Math.max(0, capacity − seatsTaken)`
- `CourseCommandService`(관리자 API)도 같은 `SessionResponse`를 돌려주므로 같은 결함이 있다. 함께 고친다 (Q4)
  - 회차 수정(`updateSession`, 정원 변경 포함)·회차 종료(`closeSession`): 회차 하나의 ID로 `countSeatsTaken`을 부른다
  - 과정 수정·종료(`toDetail`): 회차 목록 전체 ID로 한 번 부른다 (N+1 없음)
  - 회차 생성(`createSession`): 신청이 있을 수 없으므로 쿼리 없이 `seatsTaken = 0`
- `course/package-info.java` — "course는 enrollment에 의존하지 않는다. 정원 차지 수는 `SessionSeatCounter` 포트로 받는다"를 덧붙인다
- 공개 경로 `GET /api/v1/courses/**`는 토큰을 읽지 않는다. 컨트롤러·보안 설정은 바꾸지 않는다

### 2.4 SQL 수 (NFR-02)
`getDetail` 한 번에 SQL 3개: 과정 1 + 회차 목록 1 + 정원 차지 수 집계 1. 회차 수와 무관하다.

## 3. B2 — 명단 API 태그
- `AdminEnrollmentController`의 `@Tag(name = "관리자-수강")`을 `@Tag(name = "Roster", description = "회차 명단 조회와 수료 처리 (관리자)")`로 바꾼다 (Q2). 생성 클라이언트는 `RosterApi`가 된다
- `docs/design/roster.md` 1장 제목의 태그 이름도 고친다
- `OpenApiDocsTest`에 두 경로(`roster` GET, `completions` POST)의 `tags`가 `["Roster"]`인지 확인하는 테스트를 추가한다. 회귀 방지로 "모든 operation의 태그가 ASCII 식별자(`^[A-Za-z][A-Za-z0-9]*$`)"인지도 확인한다

## 4. 테스트 시나리오
| # | 테스트 | 종류 | 확인 |
|---|---|---|---|
| T1 | `SessionResponseTest` | 단위 | 정원 10, 차지 3 → 7 / 차지 0 → 10 / 차지 12(정원 축소 등) → 0 |
| T2 | `EnrollmentRepositoryTest` 집계 쿼리 | pgtest | 회차 둘에 CONFIRMED·COMPLETED·WAITING·CANCELLED를 섞어 넣고, 회차별로 CONFIRMED+COMPLETED만 세는지. 신청 없는 회차는 결과에 없음 |
| T3 | `EnrollmentSeatCounterTest` | 단위(Mockito) 또는 pgtest | 빈 ID 목록이면 리포지토리를 부르지 않고 빈 맵. 행을 맵으로 바꾼다 |
| T4 | `CourseQueryServiceTest` | `@SpringBootTest`(test 프로필) | 회차 3개에 신청을 넣고 상세 조회 → 회차마다 남은 자리가 맞다. Hibernate `Statistics`로 `getDetail`의 `PrepareStatementCount`가 3이며 회차 수와 무관(회차 1개일 때도 3) |
| T5 | `CourseControllerTest` | `@WebMvcTest` | 기존 `remainingSeats` 단언을 서비스 목 기준으로 유지 (서비스가 계산한 값이 그대로 나감) |
| T6 | `OpenApiDocsTest` | `@SpringBootTest` | 3장의 태그 확인 2건 |

## 5. 질문
- **Q1.** 집계 위치를 방안 A(포트 역전)로 해도 될까? (권장 A)
- **Q2.** 태그 이름을 `Roster`로 할까? (대안: 기존 `CourseAdmin`과 맞춘 `EnrollmentAdmin`)
- **Q4.** 관리자 API(회차 수정·종료, 과정 수정·종료) 응답의 `remainingSeats`도 함께 고칠까? (권장: 함께. 같은 DTO라 그대로 두면 관리 화면에 틀린 값이 나간다)
- **Q3.** pgtest에 워크트리 루트 `.env`가 필요하다. 지금 이 워크트리에 없으므로 `C:\test\.env`를 `C:\test\.claude\worktrees\vibrant-swirles-2b7614\.env`로 복사해 주기 바란다
