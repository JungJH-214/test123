# 구현계획: 회차 남은 자리 계산과 명단 API 태그 수정

- 상태: 승인됨 (2026-11-27). 설계명세는 `docs/design/session-remaining-seats.md`
- 브랜치: `fix/session-remaining-seats` (main `be0136b`)
- 규모: 파일 약 15개(테스트 포함), course·enrollment 두 도메인과 여러 계층(CLAUDE.md 규칙 5)
- 공통: 작업마다 implementer가 구현하고 reviewer가 검토한다. 모든 작업이 끝나면 critical-reviewer가 최종 검토한다
- 명령: `backend/`에서 `JAVA_HOME="C:\Program Files\Zulu\zulu-11" ./gradlew ...`. pgtest(`@DataJpaTest` + `replace = NONE`)는 워크트리 루트 `.env`가 필요하다

## 작업 1. 명단 API 태그 (B2)
- `enrollment/controller/AdminEnrollmentController`: `@Tag(name = "Roster", description = "회차 명단 조회와 수료 처리 (관리자)")`
- `OpenApiDocsTest`
  - `apiDocs_roster_taggedRoster`: `$.paths['/api/admin/sessions/{sessionId}/roster'].get.tags`와 `$.paths['/api/admin/sessions/{sessionId}/completions'].post.tags`가 `["Roster"]`
  - `apiDocs_allTags_areAsciiIdentifiers`: 문서의 모든 operation `tags` 값이 `^[A-Za-z][A-Za-z0-9]*$`에 맞는다 (경로가 하나 이상 있는지도 먼저 확인)
- `docs/design/roster.md` 1장 제목의 `@Tag("관리자-수강")` → `@Tag("Roster")`
- **완료:** `./gradlew test --tests "com.example.eduhub.common.config.OpenApiDocsTest"`

## 작업 2. 정원 차지 수 포트와 enrollment 구현 (B1)
- `enrollment/domain/EnrollmentStatus`: `public static final List<EnrollmentStatus> SEAT_STATUSES = Collections.unmodifiableList(Arrays.asList(CONFIRMED, COMPLETED))` + Javadoc(D-05). `EnrollmentCommandService`의 private 상수를 지우고 이것을 쓴다(동작 변화 없음)
- `course/service/SessionSeatCounter` (인터페이스, 신규): `Map<Long, Long> countSeatsTaken(Collection<Long> sessionIds)`. Javadoc: 회차 ID → 정원을 차지한 수(D-05), 신청 없는 회차는 맵에 없을 수 있음, 빈 입력이면 빈 맵. 구현은 enrollment가 한다(의존 역전)
- `enrollment/repository/SessionSeatCountRow` (인터페이스 projection, 신규): `Long getSessionId()`, `long getSeatsTaken()`
- `enrollment/repository/EnrollmentRepository.countSeatsTakenBySessionIds(Collection<Long> sessionIds, Collection<EnrollmentStatus> statuses)`:
  `select e.session.id as sessionId, count(e) as seatsTaken from Enrollment e where e.session.id in :sessionIds and e.status in :statuses group by e.session.id`
- `enrollment/service/EnrollmentSeatCounter` (신규, `@Component`, `@Transactional(readOnly = true)`, `SessionSeatCounter` 구현): 빈 입력이면 리포지토리를 부르지 않고 `Collections.emptyMap()`. 아니면 `SEAT_STATUSES`로 조회해 맵으로 바꾼다
- 테스트
  - `EnrollmentRepositoryTest`(pgtest)에 `countSeatsTakenBySessionIds_countsConfirmedAndCompletedPerSession`: 회차 A(CONFIRMED 2, COMPLETED 1, WAITING 1, CANCELLED 1), 회차 B(WAITING 1만), 회차 C(신청 없음) → A=3만 있고 B·C는 없음. SQL 1개(`Statistics`)
  - `EnrollmentSeatCounterTest`(Mockito): 빈 입력 → 빈 맵·리포지토리 호출 없음 / 행 둘 → 맵 둘, `SEAT_STATUSES`를 넘김
- **완료:** `./gradlew test --tests "com.example.eduhub.enrollment.*"`

## 작업 3. course 응답에 반영 (B1)
- `course/dto/SessionResponse`: `from(CourseSession)` 삭제, `of(CourseSession session, long seatsTaken)` 추가. `remainingSeats = (int) Math.max(0L, capacity - seatsTaken)`. Javadoc 갱신(D-05)
- `course/service/CourseQueryService.getDetail`: 회차 목록 → ID 목록 → `sessionSeatCounter.countSeatsTaken` 한 번 → `SessionResponse.of(s, counts.getOrDefault(s.getId(), 0L))`
- `course/service/CourseCommandService`: 생성자에 `SessionSeatCounter` 주입
  - `createSession`: `SessionResponse.of(session, 0)` (쿼리 없음)
  - `updateSession`, `closeSession`: `countSeatsTaken(singletonList(id))`
  - `toDetail`(과정 수정·종료): 회차 ID 전체로 한 번. static에서 인스턴스 메서드로 바꾼다
- `course/package-info.java`: 정원 차지 수는 `SessionSeatCounter` 포트로 받는다는 문장 추가
- `backend/CLAUDE.md` 의존 방향 규칙에 "course가 enrollment 정보를 써야 하면 course에 인터페이스(포트)를 두고 enrollment가 구현한다 (예: `SessionSeatCounter`)" 한 줄 추가
- 테스트
  - `SessionResponseTest`(신규, 단위): 정원 10·차지 3 → 7 / 차지 0 → 10 / 차지 12 → 0
  - `CourseQueryServiceTest`(신규, `@SpringBootTest` + `@ActiveProfiles("test")` + `@Transactional`, `hibernate.generate_statistics=true`): 과정 하나에 회차 3개, 신청(Enrollment 엔티티 직접 persist: 회차1 CONFIRMED 2+COMPLETED 1, 회차2 WAITING 1+CANCELLED 1, 회차3 없음, 정원 3인 회차에 4개 같은 초과 케이스 포함) → 남은 자리가 회차마다 맞다. flush·clear 후 `Statistics.getPrepareStatementCount() == 3`. 회차 1개 과정에서도 3
  - `CourseCommandServiceTest`(Mockito): `SessionSeatCounter` 목 추가. `updateSession`·`closeSession`·과정 종료 응답의 `remainingSeats`가 목 값으로 계산되는지, `createSession`은 카운터를 부르지 않는지
  - `CourseControllerTest`, `CourseAdminControllerTest`: `SessionResponse.from(...)` → `of(..., 0)` 등으로 컴파일만 맞춘다. 기존 단언 유지
- **완료:** `./gradlew spotlessApply` 후 `./gradlew spotlessCheck test` 전체 통과

## 작업 순서와 위험
- 순서: 1 → 2 → 3 (3은 2의 인터페이스에 의존)
- 위험
  - `SessionSeatCounter` 구현 빈이 enrollment에 있으므로 course만 띄우는 슬라이스 테스트(`@WebMvcTest`)는 서비스를 목으로 바꾸므로 영향 없음. `@DataJpaTest`는 서비스 빈을 띄우지 않아 영향 없음
  - JPQL `in :sessionIds`에 빈 목록을 넘기면 DB에 따라 오류 → 구현에서 빈 입력을 먼저 거른다
  - 표시용 조회라 락 없음. 신청 판정의 정합성은 기존 락이 보장한다
  - 응답 형식은 그대로라 프론트 재생성 불필요(B2 태그 변경은 프론트 작업에서 재생성)
