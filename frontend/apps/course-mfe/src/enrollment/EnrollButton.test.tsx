import type { UserResponse } from '@eduhub/api-client';
import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { enrollmentApi } from '../api/clients';
import { useAuth, type AuthContextValue, type LoginLocationState } from '../auth/AuthContext';
import { EnrollButton } from './EnrollButton';

vi.mock('../api/clients', () => ({
  enrollmentApi: { enroll: vi.fn() },
}));
vi.mock('../auth/AuthContext', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../auth/AuthContext')>()),
  useAuth: vi.fn(),
}));

const enroll = vi.mocked(enrollmentApi.enroll);
const EMPLOYEE: UserResponse = { id: 1, employeeNo: 'E0001', name: '사원1', role: 'EMPLOYEE' };

function givenUser(user: UserResponse | null) {
  vi.mocked(useAuth).mockReturnValue({ user, initializing: false, expired: false } as AuthContextValue);
}

/** 로그인 화면으로 넘어갔을 때 돌아올 곳(state.from)을 찍는다. */
function LoginProbe() {
  const from = (useLocation().state as LoginLocationState | null)?.from;
  return <div>로그인 화면 from={from?.pathname}</div>;
}

function renderButton(onEnrolled?: () => void) {
  return render(
    <MemoryRouter initialEntries={['/courses/1']}>
      <Routes>
        <Route path="/courses/:courseId" element={<EnrollButton sessionId={10} onEnrolled={onEnrolled} />} />
        <Route path="/login" element={<LoginProbe />} />
      </Routes>
    </MemoryRouter>,
  );
}

function apiError(status: number, code: string, message: string) {
  return Object.assign(new Error(`status ${status}`), { response: { status, data: { code, message } } });
}

describe('EnrollButton', () => {
  beforeEach(() => {
    enroll.mockReset();
  });

  it('로그인하지 않았으면 로그인 후 신청 링크를 보여 주고, 누르면 지금 화면을 기억한 채 로그인 화면으로 간다', () => {
    givenUser(null);
    renderButton();

    fireEvent.click(screen.getByRole('link', { name: '로그인 후 신청' }));

    expect(screen.getByText('로그인 화면 from=/courses/1')).toBeInTheDocument();
    expect(enroll).not.toHaveBeenCalled();
  });

  it('대기로 등록되면 대기 순번을 보여 준다', async () => {
    givenUser(EMPLOYEE);
    enroll.mockResolvedValue({ data: { id: 5, sessionId: 10, status: 'WAITING', waitingNumber: 3 } } as Awaited<
      ReturnType<typeof enrollmentApi.enroll>
    >);
    const onEnrolled = vi.fn();
    renderButton(onEnrolled);

    fireEvent.click(screen.getByRole('button', { name: '신청' }));

    expect(await screen.findByRole('status')).toHaveTextContent('대기 3번으로 등록되었습니다');
    expect(enroll).toHaveBeenCalledWith({ sessionId: 10 });
    expect(onEnrolled).toHaveBeenCalledTimes(1);
  });

  it('확정되면 신청이 확정되었습니다를 보여 준다', async () => {
    givenUser(EMPLOYEE);
    enroll.mockResolvedValue({ data: { id: 5, sessionId: 10, status: 'CONFIRMED', waitingNumber: null } } as Awaited<
      ReturnType<typeof enrollmentApi.enroll>
    >);
    renderButton();

    fireEvent.click(screen.getByRole('button', { name: '신청' }));

    expect(await screen.findByRole('status')).toHaveTextContent('신청이 확정되었습니다');
  });

  it('409 중복이면 이미 신청한 회차입니다를 보여 주고 버튼은 그대로 둔다', async () => {
    givenUser(EMPLOYEE);
    enroll.mockRejectedValue(apiError(409, 'ENROLLMENT_DUPLICATE', '이미 신청한 회차입니다: 10'));
    const onEnrolled = vi.fn();
    renderButton(onEnrolled);

    fireEvent.click(screen.getByRole('button', { name: '신청' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('이미 신청한 회차입니다');
    expect(screen.getByRole('button', { name: '신청' })).toBeEnabled();
    expect(onEnrolled).not.toHaveBeenCalled();
  });

  it('정원과 대기가 모두 찼으면(409 SESSION_FULL) 그 안내를 보여 준다', async () => {
    givenUser(EMPLOYEE);
    enroll.mockRejectedValue(apiError(409, 'SESSION_FULL', '정원과 대기 인원이 모두 찼습니다: 10'));
    renderButton();

    fireEvent.click(screen.getByRole('button', { name: '신청' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('정원과 대기 인원이 모두 찼습니다');
  });
});
