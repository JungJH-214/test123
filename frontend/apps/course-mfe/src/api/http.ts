import type { UserResponse } from '@eduhub/api-client';
import axios from 'axios';

const TOKEN_KEY = 'eduhub.accessToken';
const USER_KEY = 'eduhub.user';

/** 401을 받아 토큰을 지웠을 때 window에 보내는 이벤트. AuthContext가 듣고 로그인 화면으로 보낸다. */
export const UNAUTHORIZED_EVENT = 'eduhub:unauthorized';

/** 로그인 토큰과 사용자 정보 저장소 (NFR-03). localStorage를 쓰므로 새로고침해도 남는다. */
export const tokenStore = {
  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  },
  getUser(): UserResponse | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as UserResponse;
    } catch {
      return null;
    }
  },
  save(token: string, user: UserResponse): void {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  },
  saveUser(user: UserResponse): void {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  },
  clear(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  },
};

declare module 'axios' {
  interface InternalAxiosRequestConfig {
    /** 이 요청에 붙인 토큰. 응답이 올 때 지금 토큰과 같은지 비교한다. */
    eduhubToken?: string | null;
  }
}

/**
 * 모든 생성 API 클라이언트가 쓰는 Axios 인스턴스.
 * - 요청: 토큰이 있으면 Authorization: Bearer를 붙인다
 * - 응답: 401이면 토큰을 지우고 UNAUTHORIZED_EVENT를 보낸다. 403(권한 부족)은 로그인 상태를 그대로 둔다
 *   단, 보낸 토큰이 지금 토큰과 같을 때만 그렇게 한다. 토큰 없이 보낸 요청(로그인 실패 등)이나
 *   로그아웃·재로그인 전에 보낸 요청의 늦은 401이 새 로그인 상태를 지우지 않게 한다.
 */
export const http = axios.create();

http.interceptors.request.use((config) => {
  const token = tokenStore.getToken();
  config.eduhubToken = token;
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`);
  }
  return config;
});

http.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      const sentToken = error.config?.eduhubToken;
      if (sentToken && sentToken === tokenStore.getToken()) {
        tokenStore.clear();
        window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
      }
    }
    return Promise.reject(error);
  },
);
