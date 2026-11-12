# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

EduHub는 사내 교육과정 신청시스템이다.
임직원은 교육과정을 조회하고 신청하며, 정원이 차면 대기자로 등록된다. 교육 담당자는 과정과 회차를 관리한다.

## 디렉터리 지도
| 경로 | 내용 |
|---|---|
| `backend/` | Spring Boot API 서버. 작업 규칙은 `backend/CLAUDE.md` |
| `frontend/` | React 화면 (npm workspaces). 작업 규칙은 `frontend/CLAUDE.md` |
| `docs/requirements.md` | 요구사항(FR, NFR)과 결정 사항(D-xx) |
| `.env.example` | 로컬 환경 변수 이름. 실제 값은 루트 `.env`에 쓴다 |

## 실행 명령
| 목적 | 명령 |
|---|---|
| 백엔드 테스트 | `cd backend && ./gradlew test` |
| 백엔드 로컬 실행 | `cd backend && ./gradlew bootRun` |
| 프론트 테스트 | `cd frontend && npm test -w apps/course-mfe` |
| 프론트 실행 (포트 3001) | `cd frontend && npm run dev -w apps/course-mfe` |
| API 클라이언트 재생성 | `cd frontend && npm run generate:api` (백엔드가 8080에 떠 있어야 함) |

## 공통 규칙
- **요구사항 기준:** 요구사항은 `docs/requirements.md`를 기준으로 한다 (결정 사항 D-xx 포함)
- **커밋 형식:** 커밋 메시지는 Conventional Commits 형식을 따른다 (예: `feat(backend): ...`, `docs: ...`)
- **비밀 정보:** 비밀 정보는 코드와 설정 파일에 쓰지 않는다. 환경 변수와 `.env`(커밋 금지)만 사용한다
- **브랜치 이름:** `<타입>/<짧은-설명>` 형식으로 짓는다. 타입은 Conventional Commits 타입이나 `feature`를 쓴다 (예: `feature/week1-skeleton`, `chore/pr-template`)
- **main 브랜치:** `main`에 직접 커밋하지 않는다. PR로만 병합한다

## 개발 진행 절차 (기능 단위)
1. 시작: /fr-start <요구사항ID> → 설계명세(docs/design/) → 개발자 승인 → 구현계획(docs/plans/) → 개발자 승인
2. 구현: 계획의 작업마다 implementer에게 맡기고, 끝나면 reviewer로 검토한다
   - 심각·경고는 implementer에게 수정시킨다. 수정·재검토는 최대 2회, 넘으면 개발자에게 보고한다
3. 최종 검토: 모든 작업이 끝나면 critical-reviewer에게 설계명세 + 전체 diff + 1차 검토 결과를 넘긴다
4. 서브에이전트를 호출할 때 model은 지정하지 않는다 (정의 파일의 model을 따른다)
5. 3개 이상 파일·여러 계층에 걸친 작업이나 diff 1,000줄 이상은 개발자에게 알리고 지시를 받는다
6. 수량 변경(정원, 대기 순번)에는 반드시 락, 중복 방지는 DB 유일 제약으로 보장한다
