# 설계명세: 회차 명단 조회·수료 처리 (FR-08)

- 상태: 승인됨 (2026-11-26)
- 구현계획: `docs/plans/fr-08.md`
- 선행 조건: NFR-03(PR #7: JWT, `/api/admin/**` ADMIN 규칙, `Clock` 빈, V4 employee)과 FR-05(PR #9: 대기 순번, D-04)가 병합된 main `1be9e3d`에서 시작했다. 이후 FR-07(PR #10, `61f35a0`)이 먼저 병합되어 브랜치에 origin/main을 병합했다(5장)
- 범위 밖: 화면, 알림(FR-09), FR-06 취소·승급
- 관련: FR-08, D-02, D-03, NFR-03, NFR-04, NFR-05

## 0. 결정 사항 (2026-11-26 개발자 승인: 모두 권장안)
| # | 질문 | 결정 |
|---|---|---|
| Q1 | 언제, 어디서 시작하나 | 설계와 계획은 지금 쓴다. 구현은 NFR-03이 main에 병합된 뒤 main에서 `feature/fr-08-roster` 브랜치를 만들어 한다 |
| Q2 | COMPLETED도 정원에 넣나 | 넣는다. CONFIRMED와 COMPLETED를 함께 센다. `requirements.md`에 **D-05** "수료자도 정원을 차지한다"를 추가하고, FR-04 `EnrollmentCommandService`의 확정 수 집계를 고친다 |
| Q3 | 사번·이름을 어떻게 가져오나 | enrollment → auth 읽기 의존을 허용한다. Repository JPQL에서 `Employee`를 LEFT JOIN해 조회 결과(projection)로 받는다. `backend/CLAUDE.md`의 의존 방향 규칙을 고친다 |
| Q4 | 시작일 기준 | Asia/Seoul 기준 `오늘 < startDate`이면 409. 시작일 당일부터 수료 처리할 수 있다 |

## 1. API (`AdminEnrollmentController`, `@Tag("관리자-수강")`)
경로는 요구사항대로 `/api/admin`을 쓴다. 기존 관리자 API는 `/api/v1/admin`이지만, NFR-03 `SecurityConfig`는 두 경로를 모두 ADMIN으로 막는다.

### GET `/api/admin/sessions/{sessionId}/roster` — 회차 명단
`200 RosterResponse`
| 필드 | 타입 | 설명 |
|---|---|---|
| `sessionId` | Long | |
| `courseTitle` | String | 과정 제목 |
| `startDate` | LocalDate | 회차 시작일 |
| `capacity` | int | 정원 |
| `confirmed` | `RosterEntry[]` | CONFIRMED와 COMPLETED. `id` 오름차순(신청 순서) |
| `waiting` | `RosterEntry[]` | WAITING. `id` 오름차순(D-04). `waitingNumber`는 1부터 |

`RosterEntry`: `enrollmentId`, `employeeId`, `employeeNo`, `name`, `status`, `enrolledAt`, `waitingNumber`(대기자만 값이 있고, 나머지는 null)
- employee 행이 없는 신청(V4 이전 데이터, FK 없음)은 LEFT JOIN 결과라서 `employeeNo`와 `name`이 null이다. 명단에서 빠뜨리지 않는다.

### POST `/api/admin/sessions/{sessionId}/completions` — 수료 처리
요청 `CompletionRequest { employeeIds: Long[] }`. `@NotEmpty`를 붙이고, 원소에는 `@NotNull`을 붙인다.
응답 `200 CompletionResponse { completed: Long[], failed: [{employeeId, reason}] }`
- **중복 ID:** 요청 목록의 중복 ID는 처음 나온 순서를 지켜 한 번만 처리한다
- **결과 순서:** `completed`와 `failed` 모두 요청 순서를 따른다
- **부분 처리:** 한 건이 실패해도 나머지는 처리한다. 사유가 있는 실패는 예외로 만들지 않는다

| reason | 조건 |
|---|---|
| `NOT_ENROLLED` | 이 회차에 신청 내역이 없음 |
| `WAITING` | 대기 상태 |
| `ALREADY_COMPLETED` | 이미 수료 |
| `CANCELLED` | 취소됨. FR-06이 CANCELLED를 추가하면 함께 넣는다. FR-06 전까지는 NOT_ENROLLED로 처리한다. 같은 임직원의 신청이 여러 건이면 ID가 큰 쪽을 쓴다 |

### 오류
| HTTP | code | 조건 |
|---|---|---|
| 400 | `VALIDATION_FAILED` | `employeeIds`가 비었거나 없음, 원소가 null |
| 400 | `INVALID_REQUEST` | 본문이나 경로 값의 형식 오류 |
| 401 | (NFR-03 공통) | 토큰 없음 또는 잘못된 토큰 |
| 403 | (NFR-03 공통) | ADMIN이 아님 (D-03) |
| 404 | `SESSION_NOT_FOUND` | 회차가 없음. 기존 예외를 재사용한다 |
| 409 | `SESSION_NOT_STARTED` | KST 기준 오늘이 시작일보다 이름. 새 `SessionNotStartedException extends ConflictException` (enrollment/exception) |

- **종료된 회차:** 막지 않는다. 과정이 끝난 뒤에 수료 처리하는 것이 자연스럽다.

## 2. 도메인·스키마
- **상태 추가:** `EnrollmentStatus`에 `COMPLETED`를 추가한다. `status`는 VARCHAR(20)이고 CHECK 제약이 없어서 **마이그레이션이 필요 없다**.
- **부분 유일 인덱스:** 조건이 `status <> 'CANCELLED'`라서 COMPLETED도 중복 방지 대상이다. 수료한 사람은 같은 회차에 다시 신청할 수 없고, 의도한 동작이다. 인덱스는 바꾸지 않는다.
- **상태 변경 메서드:** `Enrollment.complete()`를 추가한다. CONFIRMED가 아니면 `IllegalStateException`을 던진다. 서비스가 상태를 미리 거르므로, 이 예외는 방어용이다.
- **정원 집계 (D-05):** `EnrollmentCommandService.decide`(FR-05에서 `decideStatus`가 바뀐 이름)가 확정 수를 셀 때 `countBySessionIdAndStatusIn(id, [CONFIRMED, COMPLETED])`를 쓴다. 이렇게 해야 수료 처리 뒤에도 자리가 비지 않는다. FR-06 승급 로직도 같은 기준을 써야 한다(FR-06 세션에 전달).

## 3. 동시성 (CLAUDE.md 규칙 6)
- **회차 락:** 수료 처리는 정원 수를 바꾸지 않는다. CONFIRMED에서 COMPLETED로 바뀌어도 둘 다 정원에 들어간다(D-05). 그래도 `CourseSessionRepository.findByIdForUpdate`로 **회차 행 락**을 건다. 근거는 이렇다.
  - FR-06 취소·승급과 순서를 맞춘다. 두 작업 모두 회차 락을 먼저 잡게 한다
  - 같은 신청을 취소와 수료가 동시에 바꾸면 마지막에 쓴 쪽이 이기는 lost update가 생긴다. 락이 이를 막는다
  - 같은 회차를 두 관리자가 동시에 처리해도 결과(`ALREADY_COMPLETED`)가 정해진 대로 나온다
- **처리 순서:** 회차 락 → 시작일 확인 → 신청 일괄 조회(`findBySessionIdAndEmployeeIdIn`) → 상태별 분류 → `complete()` → dirty checking
- **명단 조회:** 락 없이 `readOnly` 트랜잭션에서 한다.

## 4. 시간 기준
- **오늘 날짜:** `LocalDate.now(clock.withZone(BusinessTime.ZONE))`로 구한다. NFR-03의 `Clock` 빈은 UTC다.
- **시간대 상수:** `common/time/BusinessTime.ZONE = ZoneId.of("Asia/Seoul")`를 새로 만든다. FR-06(D-01 취소 기한)도 같은 상수를 쓰도록 공유한다.
- **테스트:** `Clock.fixed`로 경계값을 검증한다. 예: UTC 11-30 15:00 = KST 12-01 00:00, 시작일 12-01 → 허용.

## 5. FR-07과 겹치는 부분
- **겹치는 파일:** `EnrollmentRepository`, `EnrollmentStatus`, 대기 순번 계산
- **병합 결과 (2026-11-27):** FR-07이 먼저 병합되었다. FR-08 브랜치에 origin/main(`61f35a0`)을 병합하면서 `EnrollmentRepository`(양쪽 메서드 유지), `EnrollmentRepositoryTest`(import 합침), `requirements.md`(D-04 다음에 D-05)의 충돌을 정리했다. 명단 projection `RosterRow`는 FR-07의 `MyEnrollmentRow`처럼 `repository` 패키지에 둔다.
- **대기 순번 (FR-07과 합의, 2026-11-27 갱신):** FR-07의 새 D-04를 따른다. 같은 회차의 WAITING 신청 사이에서 **신청 ID 오름차순**으로 순번을 정하고, `created_at`은 쓰지 않는다. `created_at`은 앱 서버 시계라 서버가 여러 대이거나 시계가 뒤로 가면 실제 순서와 어긋난다. 반면 id는 회차 락을 잡은 뒤 발급되므로 같은 회차 안에서는 실제 신청 순서와 같다.
  - FR-07: 공유할 Java 헬퍼가 없다. `findMyEnrollments`의 JPQL 상관 서브쿼리로 순번을 센다
  - FR-08: 회차 하나만 보므로 `order by e.id`로 정렬한 뒤 서비스에서 1부터 번호를 매긴다. 기준이 같아서 두 화면의 순번이 일치한다
- **COMPLETED:** FR-08이 `EnrollmentStatus`에 추가한다. FR-07은 상태 enum을 그대로 돌려주고 `?status=`로 거르기만 하므로 코드를 바꿀 필요가 없다

## 5-1. FR-06과 합칠 때 맞출 지점 (2026-11-27)
FR-06(PR #12, `c9bfb6c`)이 먼저 병합되어 FR-08 브랜치에 origin/main을 병합하면서 아래를 반영했다. 같은 임직원의 여러 신청을 실제 DB로 검증하는 pgtest(W-3)와 동시성 pgtest(W-4)는 병합 커밋 뒤에 이어서 추가한다. 다만 `Clock` 빈을 auth `SecurityConfig`에서 common으로 옮기는 일(FR-06 검토 권장)은 이번 범위에 넣지 않았다.
- **정원 집계:** FR-06은 신청 판정(`decide`)과 승급(`fillSeatsFromWaiting`)이 함께 쓰는 private `countSeatsTaken(sessionId)`를 둔다. D-05에 따라 **이 메서드 하나**가 CONFIRMED와 COMPLETED를 함께 세게 바꾼다. `decide`만 바꾸면 승급 기준이 갈라져 정원 초과 승급이 생긴다.
- **CANCELLED 사유:** `CompletionFailureReason`에 `CANCELLED`를 추가하고, `RosterService.complete`의 "그 밖의 상태" 분기를 CANCELLED 사유로 바꾼다. 같은 임직원의 신청이 여러 건이면 ID가 큰 쪽(최신 신청)을 쓰는 규칙은 그대로 둔다. 취소 후 재신청 건이 있으면 그 건이 선택된다.
- **명단 필터:** `findRosterRows`는 CONFIRMED, COMPLETED, WAITING만 고르므로 CANCELLED는 자동으로 빠진다. 이를 검증하는 pgtest를 추가한다.
- **중복 확인:** FR-06은 `existsBySessionIdAndEmployeeId`를 지우고 `existsBySessionIdAndEmployeeIdAndStatusNot(..., CANCELLED)`로 바꿨다. FR-08은 이 메서드를 쓰지 않는다.
- **취소 제한:** FR-06의 `Enrollment.checkCancellable()`은 COMPLETED 취소를 막는다(`ENROLLMENT_NOT_CANCELLABLE`). D-05와 충돌하지 않는다.
- **락 순서:** 두 기능 모두 회차 행 락을 먼저 잡는다.
- **`EnrollmentTest` 양쪽 신규 파일 (최종 검토 W-1):** FR-06과 FR-08이 `backend/src/test/java/com/example/eduhub/enrollment/domain/EnrollmentTest.java`를 각자 새로 만들었다. 병합할 때 한쪽 테스트가 통째로 사라지지 않게 두 파일의 테스트를 합친다.
- **FR-06 테스트 스텁 (W-1):** FR-06 `EnrollmentCommandServiceTest`는 `countBySessionIdAndStatus(SESSION_ID, CONFIRMED)`를 스텁한다. `countSeatsTaken`을 `countBySessionIdAndStatusIn`으로 바꿀 때 아래 두 가지를 고친다.
  - 스텁을 FR-08의 `isSeatStatuses()` argThat 방식으로 옮긴다.
  - `verify(..., never()).countBySessionIdAndStatus(any(), any())` 같은 검증은 새 메서드 이름으로는 아무것도 걸러내지 못한 채 통과한다. 그래서 `countBySessionIdAndStatusIn`도 검증 대상에 넣는다.
- **시간대 상수 하나로 (W-1):** FR-06 `EnrollmentCommandService`의 `BUSINESS_ZONE`을 `common/time/BusinessTime.ZONE`으로 바꾼다. 지금은 D-01 취소 기한(시작 1일 전까지)과 FR-08 수료 시작(시작일 당일부터)이 겹치지 않아, 같은 CONFIRMED 건을 취소와 수료가 동시에 바꿀 수 없다. 이 성질은 두 기능이 같은 시간대를 쓸 때만 유지된다.
- **D-05 승급 회귀 테스트, pgtest (W-1):**
  - 상황: 정원 2인 회차에 COMPLETED 1명, CONFIRMED 1명, WAITING 2명이 있고, CONFIRMED 1명이 취소한다.
  - 기대: 대기자 1명만 승급하고, CONFIRMED와 COMPLETED 합계가 2다.
  - 이유: `countSeatsTaken`을 고치지 않으면 2명이 승급해 정원을 넘는다.
- **같은 임직원의 여러 신청 (W-3):** 실제 CANCELLED 행(작은 ID)과 재신청(큰 ID)을 쓰는 서비스 테스트와 pgtest로, 재신청 건이 수료되는지 검증한다.
- **동시성 증명 (W-4):** 같은 회차에 신청 다수와 수료 요청을 동시에 실행하고, CONFIRMED와 COMPLETED 합계가 정원 이하인지 보는 pgtest를 `EnrollmentConcurrencyTest`에 추가한다. 늦어도 FR-06 병합 시점에는 넣는다. 지금은 세 쓰기 경로(신청, 수료, 취소)가 모두 회차 행 락을 먼저 잡고 수료는 정원 합계를 바꾸지 않아 위험이 낮다.

## 5-2. 백로그 (FR-08 범위 밖, 최종 검토 제안)
- **D-05의 부작용 (P-4):** FR-04는 시작일이 지난 회차의 신청을 막지 않는다. 시작 뒤 수료자로 정원이 차면 새 신청은 WAITING이 되는데, 수료로는 자리가 비지 않고 D-01 때문에 취소도 할 수 없다. 그러면 이 대기는 승급되지 않는다. 시작일 이후 신청을 막을지 개발자가 정한다.
- **회차 수정에 락이 없음 (P-5):** FR-03 `CourseCommandService.updateSession`은 락 없이 `startDate`와 `capacity`를 바꾼다. 그래서 두 가지 상태가 생길 수 있다.
  - 수료 처리한 회차의 시작일을 미래로 옮기면 "시작 전인데 수료자가 있는" 상태가 된다.
  - 정원을 확정자와 수료자 합계보다 낮게 줄일 수 있다.
- **응답 필드 이름 (P-2):** `RosterResponse.confirmed`에는 수료자도 들어간다. 설계 결정으로 이름은 그대로 두고(이 명세 1장: 확정자 목록에 수료자 포함), OpenAPI 설명에 적었다. 프론트를 만들 때 이름을 바꿀지 다시 본다.

## 6. 서비스 구조
- **`RosterService`:** 클래스에 `@Transactional(readOnly = true)`를 붙인다. `getRoster`는 읽기 전용이고, `complete`에는 메서드 단위 `@Transactional`을 붙인다. 이름은 요구사항이 정했다. 스킬은 조회와 쓰기 서비스를 나누라고 하지만 여기서는 요구사항을 따른다.
- **`getRoster` 흐름:** 회차와 과정을 조회한다(없으면 404). 이어서 `EnrollmentRepository.findRosterRows(sessionId)`를 JPQL `select new RosterRow(...) from Enrollment e left join Employee emp on emp.id = e.employeeId where e.session.id = :id and e.status in (CONFIRMED, COMPLETED, WAITING) order by e.id`로 한 번 조회하고, 서비스에서 확정자와 대기자로 나눈다. N+1이 생기지 않는다.

## 7. 테스트 (완료 기준: `./gradlew test` 통과)
- **Service (Mockito):**
  - 정상: 명단 분류·순번, 수료 처리
  - 상태 오류: WAITING, NOT_ENROLLED, ALREADY_COMPLETED가 섞인 부분 실패
  - 시작일: 하루 전이면 409, 당일이면 허용, KST 경계
  - 기타: 404, 중복 ID
- **Controller (`@WebMvcTest` + `SecurityConfig` import + `jwt()`):**
  - 정상: 200 명단, 200 수료 결과
  - 400 빈 목록, 400 null 원소
  - 401 토큰 없음, 403 EMPLOYEE
  - 404, 409
- **Repository (pgtest):** 명단 조회 정렬, LEFT JOIN(employee가 없으면 null), `findBySessionIdAndEmployeeIdIn`
- **FR-04 회귀:** COMPLETED가 정원을 차지해서, 확정 2명 중 1명이 수료한 정원 2 회차에 새로 신청하면 WAITING이 되는지 확인한다
