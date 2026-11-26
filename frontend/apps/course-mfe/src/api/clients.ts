import { AuthApi, Configuration } from '@eduhub/api-client';
import { http } from './http';

/**
 * 생성 API 클라이언트 공통 설정. basePath ''는 같은 출처의 /api로 요청한다 (개발 중에는 Vite 프록시).
 * 생성된 *Api는 src/api 안에서만 `new XxxApi(apiConfiguration, undefined, http)`로 만든다.
 * 과정 API는 courseApi.ts, 나머지는 이 파일에 둔다.
 */
export const apiConfiguration = new Configuration({ basePath: '' });

/** 로그인과 현재 사용자 확인 (NFR-03). */
export const authApi = new AuthApi(apiConfiguration, undefined, http);

/**
 * GET /api/me. 백엔드 스펙이 `@AuthenticationPrincipal Jwt`를 쿼리 파라미터로 노출해 생성 메서드가 jwt를 요구한다.
 * 빈 객체를 넘기면 쿼리가 붙지 않는다. 백엔드에서 그 파라미터를 숨기고 재생성하면 이 우회를 지운다.
 */
export function fetchMe() {
  return authApi.me({ jwt: {} });
}
