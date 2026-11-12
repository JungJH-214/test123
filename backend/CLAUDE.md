# backend/CLAUDE.md

Spring Boot API 서버. 명령은 모두 `backend/`에서 실행한다.

## 명령
**JDK 지정:** Gradle은 Java 11로 실행해야 한다. 시스템 `JAVA_HOME`이 JDK 8을 가리키므로, 개인 설정 `.claude/settings.local.json`의 `env.JAVA_HOME`에 JDK 11 경로를 넣는다. 그러면 명령 앞에 `JAVA_HOME=`을 붙이지 않아도 되고, `.claude/settings.json`의 허용 규칙(`./gradlew test` 등)에도 그대로 맞는다.

| 목적 | 명령 |
|---|---|
| 테스트 전체 | `./gradlew test` |
| 단일 테스트 | `./gradlew test --tests "com.example.eduhub.common.health.HealthControllerTest"` |
| 포맷 검사 / 정리 | `./gradlew spotlessCheck` / `./gradlew spotlessApply` (`test`에 포함되지 않음) |
| 로컬 실행 | `./gradlew bootRun` (dev 프로필, 로컬 PostgreSQL 필요) |

## 계층 규칙
- **계층 순서:** Controller → Service → Repository. Controller에서 Repository를 직접 호출하지 않는다
- **패키지:** 도메인 우선 구조 `com.example.eduhub.{course, enrollment, common}`을 쓴다
- **의존 방향:** `enrollment → course`만 허용한다. `common`은 도메인 패키지에 의존하지 않는다

## JPA 규칙
- **연관관계:** 연관관계는 `fetch = FetchType.LAZY`로 둔다
- **API 응답:** 엔티티를 API 응답으로 그대로 반환하지 않는다. DTO로 바꿔 반환한다
- **OSIV:** `open-in-view: false`다. 지연 로딩은 서비스 계층의 트랜잭션 안에서 끝낸다
- **스키마 변경:** 스키마는 Flyway로만 바꾼다. `ddl-auto: validate`이므로 엔티티를 바꾸면 `src/main/resources/db/migration/V{n}__*.sql`을 함께 추가한다

## OpenAPI 규칙
- **문서화:** 모든 API는 springdoc으로 OpenAPI 문서에 노출한다 (NFR-04). 컨트롤러에는 `@Tag`, 메서드에는 `@Operation`을 붙인다 (`HealthController` 참고)
- **클라이언트 생성:** 프론트 클라이언트는 이 스펙에서 생성한다. 응답 형식을 바꾸면 프론트에 영향이 간다
- **확인 주소:** `/swagger-ui.html`, `/v3/api-docs`

## 기술 스택 제약 (요구사항과 다름)
요구사항은 Java 21 + Boot 3이지만, 실제 구성은 **Java 11 + Spring Boot 2.7.18**이다.
- **패키지 이름:** `jakarta.*`가 아니라 `javax.*`를 쓴다
- **문법:** `record`, `switch` 식, 텍스트 블록을 쓸 수 없다. DTO는 일반 클래스로 만든다
- **에러 응답:** `ProblemDetail`이 없다. 공통 에러 응답은 직접 만들어야 한다
- **라이브러리 버전:** springdoc은 1.x(`springdoc-openapi-ui`)다. Flyway 8.5는 PostgreSQL 16에서 "지원 미검증" 경고를 내지만 동작한다

## 프로필
- **dev (기본):** 로컬 PostgreSQL(`localhost:5432/eduhub`)에 접속한다. 루트 `.env`를 `spring.config.import`로 읽고, 실제 환경 변수가 있으면 그 값이 우선한다
- **test:** H2(PostgreSQL 모드)를 쓴다. `@SpringBootTest`에는 `@ActiveProfiles("test")`를 붙인다
- **pgtest:** 리포지토리 테스트(`@DataJpaTest` + `replace = NONE`)는 로컬 PostgreSQL의 `eduhub_test`를 쓴다. 그래서 `./gradlew test`를 돌리려면 로컬 PG와 루트 `.env`가 있어야 한다
- **H2 한계:** H2는 락 동작이 PostgreSQL과 다르다. 동시성 테스트(NFR-01)는 Docker가 준비되면 Testcontainers로 옮긴다

## Windows 환경 주의
- **PowerShell 인코딩:** PowerShell 5.1은 BOM 없는 UTF-8 `.ps1`을 CP949로 읽는다. 한글 주석이 다음 줄을 삼키므로, 스크립트는 ASCII로 쓰거나 BOM을 붙인다
- **한글 경로:** 사용자 경로에 한글이 있어 일부 설치 프로그램이 `%TEMP%`를 읽지 못한다
- **로컬 DB:** PostgreSQL 서비스 이름은 `postgresql-x64-16`이다. psql 인증 정보는 `%APPDATA%\postgresql\pgpass.conf`에 있다
