# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

EduHub는 사내 교육과정 신청시스템이다.
임직원은 교육과정을 조회하고 신청하며, 정원이 차면 대기자로 등록된다. 교육 담당자는 과정과 회차를 관리한다.

## 디렉터리 지도
| 경로 | 내용 |
|---|---|
| `backend/` | Spring Boot API 서버. 작업 규칙은 `backend/CLAUDE.md` |
| `frontend/` | React 화면 (아직 코드 없음). 작업 규칙은 `frontend/CLAUDE.md` |
| `docs/requirements.md` | 요구사항(FR, NFR)과 결정 사항(D-xx) |
| `.env.example` | 로컬 환경 변수 이름. 실제 값은 루트 `.env`에 쓴다 |

## 실행 명령
| 목적 | 명령 |
|---|---|
| 백엔드 테스트 | `cd backend && ./gradlew test` |
| 백엔드 로컬 실행 | `cd backend && ./gradlew bootRun` |

프론트엔드 명령은 코드가 생기면 추가한다.

## 공통 규칙
- **요구사항 기준:** 요구사항은 `docs/requirements.md`를 기준으로 한다 (결정 사항 D-xx 포함)
- **커밋 형식:** 커밋 메시지는 Conventional Commits 형식을 따른다 (예: `feat(backend): ...`, `docs: ...`)
- **비밀 정보:** 비밀 정보는 코드와 설정 파일에 쓰지 않는다. 환경 변수와 `.env`(커밋 금지)만 사용한다
- **브랜치 이름:** `<타입>/<짧은-설명>` 형식으로 짓는다. 타입은 Conventional Commits 타입이나 `feature`를 쓴다 (예: `feature/week1-skeleton`, `chore/pr-template`)
- **main 브랜치:** `main`에 직접 커밋하지 않는다. PR로만 병합한다
