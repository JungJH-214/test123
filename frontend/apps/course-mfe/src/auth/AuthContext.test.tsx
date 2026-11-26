import type { UserResponse } from '@eduhub/api-client';
import { act, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, useLocation } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fetchMe } from '../api/clients';
import { tokenStore, UNAUTHORIZED_EVENT } from '../api/http';
import { AuthProvider, LoginLocationState, useAuth } from './AuthContext';

vi.mock('../api/clients', () => ({
  authApi: { login: vi.fn() },
  fetchMe: vi.fn(),
}));

const USER: UserResponse = { id: 1, employeeNo: 'E0001', name: '사원1', role: 'EMPLOYEE' };

/** 지금 경로와 state.from, 로그인 상태를 화면에 찍는다. */
function Probe() {
  const location = useLocation();
  const { user, expired } = useAuth();
  const from = (location.state as LoginLocationState | null)?.from;
  return (
    <>
      <div data-testid="path">{location.pathname}</div>
      <div data-testid="from">{from ? `${from.pathname}${from.search}` : ''}</div>
      <div data-testid="user">{user?.name ?? ''}</div>
      <div data-testid="expired">{String(expired)}</div>
    </>
  );
}

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider>
        <Probe />
      </AuthProvider>
    </MemoryRouter>,
  );
}

function fireUnauthorized() {
  // 실제로는 http 인터셉터가 토큰을 지운 뒤 이벤트를 보낸다.
  tokenStore.clear();
  window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
}

describe('AuthProvider', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.mocked(fetchMe).mockReset();
  });

  it('401을 받으면 원래 경로를 state.from에 담아 로그인 화면으로 보내고 만료로 표시한다', async () => {
    tokenStore.save('token', USER);
    vi.mocked(fetchMe).mockResolvedValue({ data: USER } as Awaited<ReturnType<typeof fetchMe>>);
    renderAt('/admin/courses/3?tab=sessions');
    await waitFor(() => expect(screen.getByTestId('user')).toHaveTextContent('사원1'));

    act(() => fireUnauthorized());

    expect(screen.getByTestId('path')).toHaveTextContent('/login');
    expect(screen.getByTestId('from')).toHaveTextContent('/admin/courses/3?tab=sessions');
    expect(screen.getByTestId('user')).toHaveTextContent('');
    expect(screen.getByTestId('expired')).toHaveTextContent('true');
  });

  it('401이 두 번 와도 원래 가려던 곳을 유지한다', () => {
    renderAt('/my/enrollments');

    act(() => fireUnauthorized());
    act(() => fireUnauthorized());

    expect(screen.getByTestId('path')).toHaveTextContent('/login');
    expect(screen.getByTestId('from')).toHaveTextContent('/my/enrollments');
  });

  it('한 번에 401이 여러 번 와도 원래 가려던 곳을 유지한다', () => {
    renderAt('/my/enrollments');

    act(() => {
      fireUnauthorized();
      fireUnauthorized();
    });

    expect(screen.getByTestId('path')).toHaveTextContent('/login');
    expect(screen.getByTestId('from')).toHaveTextContent('/my/enrollments');
  });

  it('로그인 상태에서 401이 두 번 와도 원래 가려던 곳과 만료 표시를 유지한다', async () => {
    tokenStore.save('token', USER);
    vi.mocked(fetchMe).mockResolvedValue({ data: USER } as Awaited<ReturnType<typeof fetchMe>>);
    renderAt('/admin');
    await waitFor(() => expect(screen.getByTestId('user')).toHaveTextContent('사원1'));

    act(() => fireUnauthorized());
    act(() => fireUnauthorized());

    expect(screen.getByTestId('path')).toHaveTextContent('/login');
    expect(screen.getByTestId('from')).toHaveTextContent('/admin');
    expect(screen.getByTestId('expired')).toHaveTextContent('true');
  });

  it('확인 중에 다른 계정으로 로그인했으면 늦게 온 /api/me 응답을 쓰지 않는다', async () => {
    tokenStore.save('old-token', USER);
    let resolveMe: (value: Awaited<ReturnType<typeof fetchMe>>) => void = () => {};
    vi.mocked(fetchMe).mockReturnValue(new Promise((resolve) => (resolveMe = resolve)));
    renderAt('/');

    const admin: UserResponse = { id: 3, employeeNo: 'A0001', name: '관리자', role: 'ADMIN' };
    tokenStore.save('new-token', admin);
    await act(async () => resolveMe({ data: USER } as Awaited<ReturnType<typeof fetchMe>>));

    expect(tokenStore.getUser()).toEqual(admin);
  });
});
