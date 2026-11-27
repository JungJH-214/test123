# 설계명세: 신청 취소와 대기 승급 (FR-06)

- 상태: 승인됨 (Q1~Q7 추천안 채택)
- 범위: 본인 신청 취소 API, 취소 시 대기 1순위 자동 확정(승급), 취소 후 재신청 허용, 빈 자리가 있을 때 신규 신청이 대기자를 앞지르지 않게 하기(Q5)
- 범위 밖: 관리자 대리 취소, 알림(메일 등), 수료(FR-08), 내 신청 조회(FR-07), 화면, 회차 수정·종료 경로의 락 보완(FR-03 후속, Q5 참고)
- 관련: `docs/requirements.md` FR-06, NFR-01, NFR-03, NFR-04, D-01, D-02, D-04(FR-07 PR에서 "신청 ID 순"으로 추가), `docs/design/enrollment.md`(FR-04 락), `docs/design/waiting-number.md`(FR-05, 7절 W1), `docs/design/my-enrollments.md`(FR-07, FR-06 관련 메모), `docs/design/auth.md`(결정 2: 남의 신청 취소 404)
- 선행: FR-05(PR #9), FR-07(PR 예정). 둘 다 `EnrollmentController`, `EnrollmentCommandService`, `EnrollmentRepository`를 건드린다. 구현은 두 PR이 main에 병합된 뒤 main 위에서 시작한다(Q6)

## 1. 요구사항 정리
| ID | 원문 | 이번 작업에서 정한 것 |
|---|---|---|
| FR-06 | 신청을 취소하면 대기 1순위가 자동으로 신청 확정된다 | 신청을 취소하면(확정·대기 상관없이), 같은 회차의 대기 중 D-04 1순위(신청 ID가 가장 작은 `WAITING`)를 `CONFIRMED`로 바꾼다. 확정 수가 정원보다 적은 동안 반복한다(정원이 줄었다가 다시 늘어난 경우까지 포함). 대기 신청을 취소해도 같은 승급을 시도하며, 빈 자리가 없으면 승급 없이 뒤 대기자의 순번만 하나씩 당겨진다(순번은 저장하지 않고 계산하므로 자동). 최종 검토 W1에서 바뀜: 취소는 상태와 관계없이 항상 승급 시도 |
| D-01 | 신청 취소는 회차 시작 1일 전까지 가능하다 | 오늘(업무 시간대 날짜)이 `startDate - 1일` 이하이면 취소 가능, 아니면 409 `CANCEL_DEADLINE_PASSED`. 즉 시작 전날 23:59:59까지 (Q1, Q2) |
| NFR-03 | 본인 신청만 취소 가능 | 신청자는 토큰의 사원(`CurrentUser`). 다른 사원의 신청이나 없는 신청은 똑같이 404 `ENROLLMENT_NOT_FOUND` (존재 여부를 드러내지 않는다, auth 설계 결정 2) |
| NFR-01 | 동시성 정합성 | 취소·승급을 신청과 같은 회차 행 락 안에서 한 트랜잭션으로 처리한다. 동시에 취소와 신청이 섞여도 확정 수가 정원을 넘지 않고, 승급 대상은 항상 D-04 1순위다 |
| D-04 | 대기 순번은 같은 회차 대기 중 신청 ID가 작은 순 | 승급 대상 선택도 같은 기준. 화면의 "대기 1번"과 실제 승급 대상이 같다 (FR-07 설계 메모) |
| FR-04 | 같은 회차에 중복 신청할 수 없다 | 취소된 신청은 중복으로 보지 않는다(V3 부분 유일 인덱스가 이미 `CANCELLED` 제외). 취소 후 재신청은 새 신청 ID로 대기 줄 맨 뒤에 선다 |

### 질문 (승인 시 답 필요, 각 항목의 추천안으로 썼다)
- **Q1. 마감 기준:** "시작 1일 전까지" = 시작 전날 하루 끝까지 취소 가능(오늘 ≤ 시작일 − 1일). 시작일 당일부터 불가. **추천: 그대로**
- **Q2. 시간대:** 날짜 판단은 `Asia/Seoul` 기준. 기존 `Clock` 빈(`SecurityConfig`, UTC)을 주입받아 `LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul")))`로 계산한다. 테스트는 고정 `Clock`으로 경계를 확인한다. **추천: 그대로** (설정값으로 빼지 않고 서비스 상수로 둔다)
- **Q3. 이미 취소한 신청을 다시 취소:** 409 `ENROLLMENT_ALREADY_CANCELLED`. (멱등 200도 가능하지만 화면 오류를 숨긴다) **추천: 409**
- **Q4. 종료된 회차의 신청 취소:** 회차가 `CLOSED`이면 409 `SESSION_CLOSED`(기존 코드 재사용), 승급하지 않는다. 종료는 운영 판단이라 이후 변경을 막는다. **추천: 그대로**
- **Q5. 빈 자리가 있는데 대기자가 있는 상태(FR-05 최종 검토 W1):** 정원 증가 등으로 이런 상태가 생기면, 지금 신규 신청은 대기자를 앞질러 확정된다. FR-06에서 신청(`enroll`)도 락을 잡은 직후 같은 승급 메서드(`fillSeatsFromWaiting`)를 먼저 실행해, 남은 대기자를 먼저 확정한 뒤 새 신청을 판정한다. 회차 수정(`CourseCommandService.updateSession`) 자체를 고치는 것은 `course → enrollment` 의존이 금지라 이번 범위에서 뺀다(FR-03 후속에서 이벤트 등으로). **추천: 신청 경로 보완은 이번에 포함, 회차 수정 경로는 범위 밖**
- **Q6. 구현 시작 시점:** FR-05(#9)와 FR-07 PR이 main에 병합된 뒤 main에서 rebase하고 시작한다. 설계·계획 문서는 지금 이 브랜치에 쓴다. **추천: 그대로**
- **Q7. 취소 응답:** 취소된 본인 신청을 `EnrollmentResponse`(status `CANCELLED`, `waitingNumber` null)로 돌려준다. 승급된 다른 사원의 정보는 응답에 넣지 않는다. **추천: 그대로**

## 2. API

### POST `/api/me/enrollments/{enrollmentId}/cancel` — 내 신청 취소 (인증 필요)
- 경로: 로그인 사용자 기준 리소스는 `/api/me` 아래(FR-07과 같다), 상태 전이는 동사 하위 경로(팀 스킬, `POST /{id}/close`와 같은 모양)
- 요청 본문 없음. 신청자는 토큰의 사원(`@Parameter(hidden = true) @AuthenticationPrincipal Jwt`, FR-05 T3과 같은 방식)
- 성공: `200 OK`, `EnrollmentResponse`

| 필드 | 값 |
|---|---|
| `id`, `sessionId`, `employeeId`, `createdAt` | 취소한 신청의 값 (그대로) |
| `status` | `CANCELLED` |
| `waitingNumber` | `null` |

### 오류 코드
| HTTP | code | 조건 | 발생 위치 |
|---|---|---|---|
| 400 | `INVALID_REQUEST` | 경로 값 형식 오류 (`/abc/cancel`) | 기존 `EnrollmentExceptionHandler` |
| 401 | `UNAUTHORIZED` | 토큰 없음·만료 | NFR-03 보안 설정 |
| 404 | `ENROLLMENT_NOT_FOUND` | 신청이 없거나 다른 사원의 신청 | 새 `EnrollmentNotFoundException extends NotFoundException` |
| 409 | `ENROLLMENT_ALREADY_CANCELLED` | 이미 취소됨 | 새 `EnrollmentAlreadyCancelledException extends ConflictException` |
| 409 | `ENROLLMENT_NOT_CANCELLABLE` | `CONFIRMED`·`WAITING`이 아닌 상태 (FR-08의 수료 등, 지금은 없음) | 새 `EnrollmentNotCancellableException extends ConflictException` |
| 409 | `CANCEL_DEADLINE_PASSED` | 오늘 > 시작일 − 1일 (D-01) | 새 `CancelDeadlinePassedException extends ConflictException` |
| 409 | `SESSION_CLOSED` | 회차가 종료됨 (Q4) | 기존 `SessionClosedException` |

- 검사 순서: 404 → 409 `SESSION_CLOSED` → 409 `ENROLLMENT_ALREADY_CANCELLED` / `ENROLLMENT_NOT_CANCELLABLE` → 409 `CANCEL_DEADLINE_PASSED`. (본인 신청이 아니면 다른 정보를 주지 않으려고 404를 맨 앞에 둔다)
- OpenAPI: 응답마다 `@ApiResponse`, 에러는 `ErrorResponse` 스키마와 code를 적는다

## 3. 엔티티·스키마 변경

### `EnrollmentStatus`
- `CANCELLED` 추가 → `CONFIRMED`, `WAITING`, `CANCELLED`. (FR-08이 `COMPLETED`를 추가한다)
- `status` 컬럼은 `VARCHAR(20)`이고 CHECK 제약이 없다. **Flyway 변경 없음.** V3 부분 유일 인덱스는 이미 `status <> 'CANCELLED'` 조건이다

### `Enrollment` (상태 변경 메서드만 추가, `@Setter` 없음)
- `checkCancellable()`: 이미 `CANCELLED`면 `EnrollmentAlreadyCancelledException`, `CONFIRMED`·`WAITING`이 아니면 `EnrollmentNotCancellableException`
- `cancel()`: `checkCancellable()`을 다시 부른 뒤 `CANCELLED`로 바꾼다 (단독 호출에도 안전)
- `confirmFromWaiting()`: `WAITING`이면 `CONFIRMED`로, 아니면 `IllegalStateException` (승급 전용)
- `isCancelled()`, `isConfirmed()` 등 조회 메서드는 필요할 때만
- 취소 일시 컬럼은 두지 않는다(요구사항에 없음). 필요해지면 FR-07/08 쪽에서 V 파일과 함께 추가

### `EnrollmentRepository` 추가
| 메서드 | 용도 |
|---|---|
| `Optional<Long> findSessionIdByIdAndEmployeeId(Long id, Long employeeId)` (`@Query("select e.session.id from Enrollment e where e.id = :id and e.employeeId = :employeeId")`) | 락 전에 회차 ID만 얻는다. 엔티티를 영속성 컨텍스트에 올리지 않는다(4절) |
| `Optional<Enrollment> findByIdAndEmployeeId(Long id, Long employeeId)` | 락 뒤에 최신 상태로 읽는다 |
| `Optional<Enrollment> findFirstBySessionIdAndStatusOrderByIdAsc(Long sessionId, EnrollmentStatus status)` | 승급 대상(D-04 1순위) |
| `boolean existsBySessionIdAndEmployeeIdAndStatusNot(Long sessionId, Long employeeId, EnrollmentStatus status)` | 신청 중복 확인을 "취소 아닌 신청" 기준으로 바꾼다(기존 `existsBySessionIdAndEmployeeId`는 취소된 신청도 중복으로 본다) |

- 인덱스: 승급 조회는 `idx_enrollment_session_status(session_id, status)`로 거른 뒤 회차당 대기 수(정원의 50% 이하)만큼 정렬한다. 새 인덱스 불필요

## 4. 트랜잭션과 동시성

### 취소 처리 순서 (`EnrollmentCommandService.cancel(Long enrollmentId, Long employeeId)`, `@Transactional` 하나)
1. `findSessionIdByIdAndEmployeeId` → 없으면 404 `ENROLLMENT_NOT_FOUND` (락 없음, 값만 읽음)
2. `courseSessionRepository.findByIdForUpdate(sessionId)` — **회차 행 쓰기 락** (신청과 같은 락)
3. `findByIdAndEmployeeId` → 락 뒤의 최신 상태. (1단계에서 엔티티를 읽지 않았으므로 1차 캐시의 오래된 값이 아니다)
4. 회차 종료면 409 `SESSION_CLOSED`
5. `enrollment.checkCancellable()` (상태 검사: 409 `ENROLLMENT_ALREADY_CANCELLED` / `ENROLLMENT_NOT_CANCELLABLE`) → 마감 검사(409 `CANCEL_DEADLINE_PASSED`) → `enrollment.cancel()`
   - 상태 검사를 마감 검사보다 먼저 하는 이유: 이미 취소한 신청에는 "마감 지남"보다 "이미 취소됨"이 정확하다
   - 상태 변경은 모든 검사가 끝난 뒤에만 한다. 예외가 나면 아무것도 바뀌지 않는다
6. `enrollmentRepository.flush()`로 취소를 먼저 반영한 뒤, 취소 전 상태와 관계없이 항상 `fillSeatsFromWaiting(session)` (최종 검토 W1: 빈 자리 + 대기자 상태가 남아 있어도 대기 취소로 해소. flush는 승급 판단의 확정 수 집계가 자동 flush에 기대지 않게 하려고 W1·W2 재검토 참고 1에서 추가)
7. 응답: `EnrollmentResponse.from(enrollment)` (status `CANCELLED`, `waitingNumber` null)
- 변경은 dirty checking으로 저장하고 커밋 시 flush한다

### 승급 `fillSeatsFromWaiting(CourseSession session)` (private, 회차 락을 이미 잡은 상태에서만 호출)
```
confirmed = count(session, CONFIRMED)
while confirmed < session.capacity:
    next = findFirstBySessionIdAndStatusOrderByIdAsc(session.id, WAITING)
    if next 없음: break
    next.confirmFromWaiting(); flush(); confirmed++
```
- 반복하는 이유: 정원이 늘어난 뒤(Q5, W1)에는 빈 자리가 둘 이상일 수 있다. 보통은 0번 또는 1번 돈다
- 한 명 확정할 때마다 `enrollmentRepository.flush()`한다. 다음 `WAITING` 조회가 방금 확정한 행을 건너뛰고 이어지는 집계가 바뀐 상태를 보게 하며, Hibernate 자동 flush에 기대지 않는다(최종 검토 W2에서 바뀜: 끝에 한 번 -> 승급마다)

### 신청 경로 보완 (Q5)
- `enroll()`의 2단계(락) 직후, 종료·중복 검사 뒤에 `fillSeatsFromWaiting(session)`을 먼저 부르고, 그다음 기존 판정(`decide`)을 한다
- 효과: 빈 자리가 있으면 기존 대기자가 먼저 확정되고, 새 신청은 남은 자리 또는 대기 맨 뒤로 간다. FR-05가 알려 준 순번이 실제 확정 순서와 맞는다
- 중복 검사는 `existsBySessionIdAndEmployeeIdAndStatusNot(..., CANCELLED)`로 바꾼다 (취소 후 재신청 허용). 부분 유일 인덱스와 같은 기준이 된다

### 락과 교착
- 락 대상은 회차 행 하나뿐이고, 신청·취소 모두 "회차 락 → 신청 행 변경" 순서다. 신청 행에 따로 락을 걸지 않는다. 그래서 신청과 취소 사이의 교착은 생기지 않는다
- 같은 신청을 두 요청이 동시에 취소하면 회차 락으로 한 줄로 서고, 두 번째는 3단계에서 `CANCELLED`를 읽어 409 `ENROLLMENT_ALREADY_CANCELLED`. 승급은 한 번만 일어난다
- 1단계(락 없음)와 2단계 사이에 신청 행이 바뀌어도 회차 ID는 바뀌지 않으므로 안전하다(신청의 회차는 변경 메서드가 없다)
- H2는 락 동작이 달라 동시성 검증은 pgtest에서 한다(기존 방침)

### FR-07·FR-05와의 정합
- FR-07 조회의 대기 순번(같은 회차에서 ID가 더 작은 `WAITING` 수 + 1)은 취소·승급 뒤 자동으로 당겨진다. `CANCELLED`는 FR-07 응답에 그대로 나온다(설계 메모와 같다)
- FR-05 신청 응답의 순번은 신청 시점 값이다. 이후 변화는 FR-07로 본다(FR-05 Q3)

## 5. 테스트 시나리오

### 도메인 단위 (`EnrollmentTest`)
| ID | 시나리오 | 기대 |
|---|---|---|
| D1 | `CONFIRMED`·`WAITING` 취소 | `CANCELLED` |
| D2 | `CANCELLED` 취소 | `EnrollmentAlreadyCancelledException` |
| D3 | `WAITING` 승급 / `CONFIRMED` 승급 | `CONFIRMED` / `IllegalStateException` |

### 서비스 단위 (`EnrollmentCommandServiceTest`, Mockito, 고정 `Clock`)
| ID | 시나리오 | 기대 |
|---|---|---|
| S1 | 없는 신청 또는 남의 신청 | 404 `ENROLLMENT_NOT_FOUND`, 락 조회 안 함 |
| S2 | 확정 취소, 대기 있음 | 취소됨, D-04 1순위 대기가 `CONFIRMED` |
| S3 | 확정 취소, 대기 없음 | 취소됨, 승급 없음 |
| S4 | 대기 취소, 확정 수 == 정원 | 취소됨, 확정 수 조회는 하되 승급 없음 (W1) |
| S4b | 대기 취소, 정원 7·확정 6·대기 3 | 남은 대기 1순위(ID 최소)가 `CONFIRMED` (W1) |
| S5 | 마감: 오늘 = 시작일 − 1일 / 오늘 = 시작일 | 성공 / 409 `CANCEL_DEADLINE_PASSED`, 상태 변경 없음 |
| S6 | 시간대: UTC 2026-12-09T15:30Z(서울 12-10 00:30), 시작일 12-11 / 12-10 | 성공 / 409 (서울 날짜 기준) |
| S7 | 종료된 회차 | 409 `SESSION_CLOSED` |
| S8 | 이미 취소된 신청 | 409 `ENROLLMENT_ALREADY_CANCELLED` |
| S9 | 정원 축소로 확정 수 > 정원인 상태에서 확정 취소 | 승급 없음 (확정 수가 여전히 정원 이상) |
| S10 | 신청 시 빈 자리 2 + 대기 2 (Q5) | 대기 2명 먼저 확정, 새 신청은 `WAITING` 순번 1 (정원·대기 상한 내) |
| S11 | 취소된 신청이 있는 사원의 재신청 | 중복 아님, 새 신청 저장 |

### 컨트롤러 (`EnrollmentControllerTest`, `@WebMvcTest`)
| ID | 시나리오 | 기대 |
|---|---|---|
| C1 | 취소 성공 | 200, `$.status == "CANCELLED"`, `$.waitingNumber` null, 서비스에 (경로 ID, 토큰 사원 ID) 전달 |
| C2 | `?employeeId=99`·`X-Employee-Id: 99`를 붙여도 | 토큰 사원 ID로만 호출 |
| C3 | 토큰 없음 / 형식 오류 / 404 / 409 각 code | 401 / 400 / 404 / 409 + code |

### 통합·동시성 (pgtest)
| ID | 시나리오 | 기대 |
|---|---|---|
| R1 | 정원 10(확정 10, 대기 5): 확정 1명 취소와 신규 50명 신청을 동시에 | 끝난 뒤 확정 수 == 10, 대기 ≤ 5, 기존 대기 1번(가장 작은 ID)이 `CONFIRMED`, 신규 신청자는 기존 대기자보다 먼저 확정되지 않음 |
| R2 | 같은 신청을 10개 스레드가 동시에 취소 | 성공 1, `ENROLLMENT_ALREADY_CANCELLED` 9, 승급 정확히 1명 |
| R3 | 확정 10명이 동시에 모두 취소 (대기 5) | 대기 5명 모두 `CONFIRMED`, 확정 수 == 5, 승급 순서가 ID 순 |
| R4 | 취소 후 재신청 (부분 유일 인덱스) | 새 신청 ID로 저장, 기존 대기자 뒤 |
| R5 | 보안 통합(`SecurityIntegrationTest`): 사원 B가 사원 A의 신청 취소 | 404, A의 신청 상태 그대로 |
| R6 | 정원 10/확정 10/대기 5에서 정원을 12로 직접 변경 후 대기 사원 15가 취소 (W1) | 사원 11·12 `CONFIRMED`, 확정 12, 대기 2(13·14) |

- 동시성 테스트의 데이터 정리는 테스트가 만든 회차 기준으로 한다(pgtest 공유 DB 문제, 별도 세션 "pgtest 테스트 DB를 worktree별로 분리"에서 다룸)

## 6. 프론트 영향 (참고, 이번 범위 밖)
- 내 신청 내역 화면에서 확정·대기 신청에 "취소" 버튼(확인 후 호출), 취소 후 목록 재조회
- 마감이 지난 신청은 버튼을 숨기거나 409 `CANCEL_DEADLINE_PASSED` 메시지를 보여 준다. 마감 계산은 서버 기준이 정답이다

## 7. 위험 영역
- **락 순서:** 취소에서 신청 엔티티를 락 전에 읽으면(1차 캐시) 락 뒤에도 오래된 상태를 본다. 1단계는 회차 ID 값만 읽는다. reviewer는 이것을 반드시 본다
- **취소 경로 승급(최종 검토 W1·W2):** 취소는 상태와 관계없이 항상 승급을 시도한다(대기 취소도 빈 자리가 있으면 승급). 승급은 한 명마다 flush하므로 연속 승급 시 쿼리가 늘지만 회차당 대기 수가 정원의 50% 이하라 허용한다
- **승급 기준 일치:** 승급 대상은 FR-07 순번 계산과 같은 D-04(ID 순)여야 한다. 다르면 화면의 1번과 실제 승급자가 달라진다
- **신청 경로 변경(Q5):** `enroll()`에 승급을 넣으면 신청 한 번에 다른 사원의 상태가 바뀔 수 있다. 응답에는 본인 신청만 담는다. 기존 FR-04·FR-05 테스트(정원·대기 상한, 순번 {1..5})가 그대로 통과해야 한다
- **회차 수정 경로(범위 밖):** `updateSession`이 회차 락 없이 정원을 바꾸는 문제(FR-04 최종 검토 경고 3, FR-05 W1)는 남는다. Q5 보완으로 "다음 신청 또는 취소 때" 대기자가 먼저 확정되므로 순번 약속은 지켜지지만, 정원을 늘린 직후 대기자가 바로 확정되지는 않는다. FR-03 후속 작업으로 남긴다
- **선행 PR 충돌:** FR-05·FR-07과 같은 파일을 바꾼다. 두 PR 병합 뒤 시작한다(Q6)
