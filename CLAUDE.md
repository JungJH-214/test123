# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

EduHub: 사내 교육과정 신청시스템. 현재는 `backend/`(Spring Boot)만 있고, 프론트엔드(React + Module Federation)는 아직 없다.

## 팀 규칙
- 요구사항은 docs/requirements.md를 기준으로 한다 (결정 사항 D-xx 포함)
- 백엔드 빌드·테스트: cd backend && ./gradlew test
- 계층 규칙: Controller → Service → Repository. Controller에서 Repository 직접 호출 금지
- 엔티티를 API 응답으로 그대로 반환하지 않는다 (DTO 사용)
- 비밀 정보는 코드와 설정 파일에 쓰지 않는다. 환경 변수와 .env(커밋 금지)만 사용

## 명령어 (backend/ 에서 실행)
- **JDK:** Gradle은 Java 11로 실행해야 한다. 시스템 `JAVA_HOME`은 JDK 8을 가리키므로, Bash에서는 명령 앞에 `JAVA_HOME="/c/Program Files/Zulu/zulu-11"`을 붙인다.

| 목적 | 명령 |
|---|---|
| 테스트 전체 | `./gradlew test` |
| 단일 테스트 | `./gradlew test --tests "com.example.eduhub.common.health.HealthControllerTest"` (메서드는 `...HealthControllerTest.health_returnsUp`) |
| 포맷 검사 / 자동 정리 | `./gradlew spotlessCheck` / `./gradlew spotlessApply` (google-java-format, `test`에는 포함되지 않음) |
| 로컬 실행 | `./gradlew bootRun` (dev 프로필, 로컬 PostgreSQL 필요) |

로컬 실행 중 확인 주소: `/api/health`, `/swagger-ui.html`, `/v3/api-docs`

## 기술 스택: 요구사항과 다름
`docs/requirements.md`는 Java 21 + Spring Boot 3을 요구하지만, 이 PC에는 Java 11까지만 있어서 **Java 11 + Spring Boot 2.7.18**로 구성했다. 코드를 쓸 때 다음에 주의한다.
- `jakarta.*`가 아니라 `javax.*`를 쓴다 (persistence, validation)
- Java 11이라 `record`, `switch` 식, 텍스트 블록을 쓸 수 없다. DTO는 일반 클래스로 만든다
- `ProblemDetail`이 없으므로 공통 에러 응답은 직접 만들어야 한다
- springdoc은 1.x(`springdoc-openapi-ui`)다
- Flyway 8.5는 PostgreSQL 16에서 "지원 미검증" 경고를 내지만 동작한다

## 구조
- **패키지:** 도메인 우선 구조 `com.example.eduhub.{course, enrollment, common}`. 각 도메인 안에 계층을 둔다.
- **의존 방향:** `enrollment → course`만 허용한다. `common`은 도메인 패키지에 의존하지 않는다. 각 패키지의 `package-info.java`에 책임과 규칙이 적혀 있다.
- **스키마는 Flyway로만 관리한다:** `ddl-auto: validate`이므로, 엔티티를 추가하거나 바꾸면 `src/main/resources/db/migration/V{n}__*.sql`을 함께 만들어야 컨텍스트가 뜬다.
- **프로필:**
  - 기본 프로필은 `dev`다. 로컬 PostgreSQL(`localhost:5432/eduhub`)에 접속하고, 저장소 루트의 `.env`를 `spring.config.import`로 읽는다. 실제 환경 변수가 있으면 그 값이 우선한다.
  - `test` 프로필은 H2(PostgreSQL 모드)를 쓴다. `@SpringBootTest`에는 `@ActiveProfiles("test")`를 붙인다.
  - H2는 락 동작이 PostgreSQL과 다르다. 동시성 테스트(NFR-01)는 Docker가 준비되면 Testcontainers로 옮길 예정이다.
- **OSIV:** `open-in-view: false`다. 지연 로딩은 서비스 계층의 트랜잭션 안에서 끝내야 한다.

## Windows 환경 주의
- **PowerShell 스크립트:** PowerShell 5.1은 BOM 없는 UTF-8 `.ps1`을 CP949로 읽는다. 한글 주석이 다음 줄을 삼켜 코드가 실행되지 않으므로, 스크립트는 ASCII로만 쓰거나 BOM을 붙인다.
- **한글 경로:** 사용자 폴더 경로에 한글이 있어 일부 설치 프로그램이 `%TEMP%`를 읽지 못한다.
- **로컬 DB:** PostgreSQL 16 서비스 이름은 `postgresql-x64-16`이다. psql 인증 정보는 `%APPDATA%\postgresql\pgpass.conf`에 있다.
