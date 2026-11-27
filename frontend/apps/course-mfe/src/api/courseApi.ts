import { CourseAdminApi, CourseApi } from '@eduhub/api-client';
import { apiConfiguration } from './clients';
import { http } from './http';

// 과정 API 인스턴스는 여기서만 만든다. 모든 요청은 토큰을 붙이는 http 인스턴스를 쓴다 (NFR-03).
export const courseApi = new CourseApi(apiConfiguration, undefined, http);

/** 관리자용 과정·회차 등록, 수정, 종료 (FR-03). */
export const courseAdminApi = new CourseAdminApi(apiConfiguration, undefined, http);
