import type { UserResponse } from '@eduhub/api-client';
import { AxiosError, AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { http, tokenStore, UNAUTHORIZED_EVENT } from './http';

const USER: UserResponse = { id: 1, employeeNo: 'E0001', name: '사원1', role: 'EMPLOYEE' };

/** 실제 네트워크 대신 쓰는 어댑터. 요청 설정을 기록하고 지정한 상태 코드로 응답한다. */
function fakeAdapter(status: number) {
  const seen: InternalAxiosRequestConfig[] = [];
  const adapter = async (config: InternalAxiosRequestConfig): Promise<AxiosResponse> => {
    seen.push(config);
    const response: AxiosResponse = { data: {}, status, statusText: '', headers: {}, config };
    if (status >= 400) {
      throw new AxiosError(`status ${status}`, AxiosError.ERR_BAD_REQUEST, config, null, response);
    }
    return response;
  };
  return { adapter, seen };
}

describe('http', () => {
  const onUnauthorized = vi.fn();

  beforeEach(() => {
    localStorage.clear();
    window.addEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
  });

  afterEach(() => {
    window.removeEventListener(UNAUTHORIZED_EVENT, onUnauthorized);
    onUnauthorized.mockReset();
  });

  it('토큰이 있으면 Authorization: Bearer 헤더를 붙인다', async () => {
    tokenStore.save('token-123', USER);
    const { adapter, seen } = fakeAdapter(200);

    await http.get('/api/me', { adapter });

    expect(seen[0].headers.get('Authorization')).toBe('Bearer token-123');
  });

  it('토큰이 없으면 Authorization 헤더를 붙이지 않는다', async () => {
    const { adapter, seen } = fakeAdapter(200);

    await http.get('/api/v1/courses', { adapter });

    expect(seen[0].headers.has('Authorization')).toBe(false);
  });

  it('401이면 토큰을 지우고 eduhub:unauthorized 이벤트를 보낸다', async () => {
    tokenStore.save('expired-token', USER);
    const { adapter } = fakeAdapter(401);

    await expect(http.get('/api/me', { adapter })).rejects.toMatchObject({ response: { status: 401 } });

    expect(tokenStore.getToken()).toBeNull();
    expect(tokenStore.getUser()).toBeNull();
    expect(onUnauthorized).toHaveBeenCalledTimes(1);
  });

  it('토큰 없이 보낸 요청의 401(로그인 실패 등)은 이벤트를 보내지 않는다', async () => {
    const { adapter } = fakeAdapter(401);

    await expect(http.post('/api/auth/login', {}, { adapter })).rejects.toMatchObject({ response: { status: 401 } });

    expect(onUnauthorized).not.toHaveBeenCalled();
  });

  it('응답 전에 토큰이 바뀌었으면(재로그인) 늦게 온 401이 새 토큰을 지우지 않는다', async () => {
    tokenStore.save('old-token', USER);
    const { adapter: base } = fakeAdapter(401);
    const adapter = async (config: InternalAxiosRequestConfig) => {
      tokenStore.save('new-token', USER); // 요청이 나가 있는 사이 다시 로그인했다
      return base(config);
    };

    await expect(http.get('/api/me', { adapter })).rejects.toMatchObject({ response: { status: 401 } });

    expect(tokenStore.getToken()).toBe('new-token');
    expect(onUnauthorized).not.toHaveBeenCalled();
  });

  it('응답 전에 로그아웃했으면 늦게 온 401이 이벤트를 보내지 않는다', async () => {
    tokenStore.save('old-token', USER);
    const { adapter: base } = fakeAdapter(401);
    const adapter = async (config: InternalAxiosRequestConfig) => {
      tokenStore.clear(); // 요청이 나가 있는 사이 로그아웃했다
      return base(config);
    };

    await expect(http.get('/api/me', { adapter })).rejects.toMatchObject({ response: { status: 401 } });

    expect(onUnauthorized).not.toHaveBeenCalled();
  });

  it('403이면 토큰을 그대로 두고 이벤트를 보내지 않는다', async () => {
    tokenStore.save('employee-token', USER);
    const { adapter } = fakeAdapter(403);

    await expect(http.get('/api/v1/admin/courses', { adapter })).rejects.toMatchObject({ response: { status: 403 } });

    expect(tokenStore.getToken()).toBe('employee-token');
    expect(tokenStore.getUser()).toEqual(USER);
    expect(onUnauthorized).not.toHaveBeenCalled();
  });
});
