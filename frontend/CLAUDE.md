# frontend/CLAUDE.md

npm workspaces다. 화면은 `apps/*`(지금은 `course-mfe`), 생성된 API 클라이언트는 `packages/api-client`에 있다.

- **UI 컴포넌트:** 화면 컴포넌트는 MUI로 만든다
- **API 호출:** 백엔드 OpenAPI 스펙에서 생성한 Axios 클라이언트로만 호출한다
- **데이터 조회 훅:** 서버 데이터 조회는 커스텀 훅(`src/hooks/useXxx.ts`)으로 감싼다
- **화면과 API 분리:** 화면 컴포넌트는 API를 직접 부르지 않고 훅을 쓴다

## 참고
- **API 인스턴스:** `CourseApi` 인스턴스는 `apps/course-mfe/src/api/courseApi.ts` 한 곳에서만 만든다. `basePath: ''`이고, 개발 중에는 Vite 프록시가 `/api` 요청을 `localhost:8080`으로 넘긴다
- **생성 코드:** `packages/api-client/src/generated`는 생성 코드라 직접 고치지 않는다. 백엔드 API가 바뀌면 `npm run generate:api`로 다시 만든다. 생성기는 Java 11 이상에서 돌고, 버전은 `frontend/openapitools.json`에 고정한다
