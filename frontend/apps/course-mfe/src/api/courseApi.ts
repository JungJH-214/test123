import { Configuration, CourseAdminApi, CourseApi } from '@eduhub/api-client';

// API 인스턴스는 여기서만 만든다. basePath ''는 같은 출처의 /api로 요청한다 (개발 중에는 Vite 프록시).
const configuration = new Configuration({ basePath: '' });

export const courseApi = new CourseApi(configuration);

/** 관리자용 과정·회차 등록, 수정, 종료 (FR-03). */
export const courseAdminApi = new CourseAdminApi(configuration);
