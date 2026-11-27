import { fireEvent, render, screen, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { CourseDetailResponse, UserResponse } from '@eduhub/api-client';
import { useAuth, type AuthContextValue } from '../auth/AuthContext';
import { useCourseDetail, type UseCourseDetailResult } from '../hooks/useCourseDetail';
import { CourseDetailPage } from './CourseDetailPage';

vi.mock('../hooks/useCourseDetail', () => ({
  useCourseDetail: vi.fn(),
}));
vi.mock('../auth/AuthContext', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../auth/AuthContext')>()),
  useAuth: vi.fn(),
}));

const mockedUseCourseDetail = vi.mocked(useCourseDetail);
const mockedUseAuth = vi.mocked(useAuth);

const base: UseCourseDetailResult = { data: null, loading: false, error: null, notFound: false, reload: vi.fn() };

const COURSE: CourseDetailResponse = {
  id: 1,
  title: 'Java 기초',
  category: '개발',
  description: '자바 입문 과정',
  status: 'OPEN',
  sessions: [
    { id: 10, startDate: '2026-12-01', place: '본사 3층', capacity: 20, remainingSeats: 15, status: 'OPEN' },
    { id: 11, startDate: '2026-06-01', place: '연수원', capacity: 30, remainingSeats: 0, status: 'CLOSED' },
  ],
};

function givenUser(user: UserResponse | null) {
  mockedUseAuth.mockReturnValue({ user, initializing: false, expired: false } as AuthContextValue);
}

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/" element={<div>목록 화면</div>} />
        <Route path="/courses/:courseId" element={<CourseDetailPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('CourseDetailPage', () => {
  beforeEach(() => {
    mockedUseCourseDetail.mockReset();
    givenUser(null);
  });

  it('URL의 id로 훅을 부른다', () => {
    mockedUseCourseDetail.mockReturnValue({ ...base, loading: true });

    renderAt('/courses/7');

    expect(mockedUseCourseDetail).toHaveBeenCalledWith(7);
  });

  it('과정 정보와 회차 목록을 보여 준다', () => {
    mockedUseCourseDetail.mockReturnValue({ ...base, data: COURSE });

    renderAt('/courses/1');

    expect(screen.getByRole('heading', { name: 'Java 기초' })).toBeInTheDocument();
    expect(screen.getByText('분류: 개발')).toBeInTheDocument();
    expect(screen.getByText('자바 입문 과정')).toBeInTheDocument();

    const rows = within(screen.getByRole('table', { name: '회차 목록' })).getAllByRole('row');
    expect(rows).toHaveLength(3); // 헤더 + 회차 2개
    expect(within(rows[1]).getByText('2026-12-01')).toBeInTheDocument();
    expect(within(rows[1]).getByText('본사 3층')).toBeInTheDocument();
    expect(within(rows[1]).getByText('15/20')).toBeInTheDocument();
    expect(within(rows[1]).getByText('모집 중')).toBeInTheDocument();
    expect(within(rows[2]).getByText('종료')).toBeInTheDocument();
  });

  it('운영 중 과정의 모집 중 회차에만 신청 버튼을 두고, 로그인 전이면 로그인 후 신청을 보여 준다', () => {
    mockedUseCourseDetail.mockReturnValue({ ...base, data: COURSE });

    renderAt('/courses/1');

    const rows = within(screen.getByRole('table', { name: '회차 목록' })).getAllByRole('row');
    expect(within(rows[1]).getByRole('link', { name: '로그인 후 신청' })).toHaveAttribute('href', '/login');
    expect(within(rows[2]).queryByRole('link', { name: '로그인 후 신청' })).not.toBeInTheDocument();
  });

  it('종료된 과정이면 종료 표시를 하고 신청 버튼을 두지 않는다', () => {
    mockedUseCourseDetail.mockReturnValue({ ...base, data: { ...COURSE, status: 'CLOSED' } });

    renderAt('/courses/1');

    expect(screen.getByText('종료된 과정입니다. 신청할 수 없습니다.')).toBeInTheDocument();
    expect(screen.queryByRole('columnheader', { name: '신청' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: '로그인 후 신청' })).not.toBeInTheDocument();
  });

  it('관리자에게는 과정 수정 버튼과 회차별 명단 링크를 보여 준다', () => {
    givenUser({ id: 3, employeeNo: 'A0001', name: '관리자', role: 'ADMIN' });
    mockedUseCourseDetail.mockReturnValue({ ...base, data: COURSE });

    renderAt('/courses/1');

    expect(screen.getByRole('link', { name: '과정 수정' })).toHaveAttribute('href', '/admin/courses/1');
    const rosterLinks = screen.getAllByRole('link', { name: '명단' });
    expect(rosterLinks.map((a) => a.getAttribute('href'))).toEqual([
      '/admin/sessions/10/roster',
      '/admin/sessions/11/roster',
    ]);
  });

  it('사원에게는 관리 버튼을 보여 주지 않는다', () => {
    givenUser({ id: 1, employeeNo: 'E0001', name: '사원1', role: 'EMPLOYEE' });
    mockedUseCourseDetail.mockReturnValue({ ...base, data: COURSE });

    renderAt('/courses/1');

    expect(screen.queryByRole('link', { name: '과정 수정' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: '명단' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: '신청' })).toBeInTheDocument();
  });

  it('회차가 없으면 안내를 보여 준다', () => {
    mockedUseCourseDetail.mockReturnValue({
      ...base,
      data: { id: 1, title: 'Java 기초', category: '개발', status: 'OPEN', sessions: [] },
    });

    renderAt('/courses/1');

    expect(screen.getByText('등록된 회차가 없습니다.')).toBeInTheDocument();
  });

  it('불러오는 중에는 로딩 표시를 보여 준다', () => {
    mockedUseCourseDetail.mockReturnValue({ ...base, loading: true });

    renderAt('/courses/1');

    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });

  it('과정이 없으면 찾을 수 없다고 안내한다', () => {
    mockedUseCourseDetail.mockReturnValue({ ...base, notFound: true });

    renderAt('/courses/999');

    expect(screen.getByRole('alert')).toHaveTextContent('과정을 찾을 수 없습니다.');
  });

  it('에러가 나면 메시지를 보여 준다', () => {
    mockedUseCourseDetail.mockReturnValue({ ...base, error: new Error('Network Error') });

    renderAt('/courses/1');

    expect(screen.getByRole('alert')).toHaveTextContent('과정 정보를 불러오지 못했습니다. (Network Error)');
  });

  it('다시 불러오기에 실패해도 보여 주던 과정과 회차는 남기고 경고만 보여 준다', () => {
    mockedUseCourseDetail.mockReturnValue({ ...base, data: COURSE, error: new Error('Network Error') });

    renderAt('/courses/1');

    expect(screen.getByRole('heading', { name: 'Java 기초' })).toBeInTheDocument();
    expect(screen.getByRole('table', { name: '회차 목록' })).toBeInTheDocument();
    expect(screen.getByText(/최신 정보를 불러오지 못했습니다/)).toBeInTheDocument();
  });

  it('목록으로 버튼을 누르면 목록으로 돌아간다', () => {
    mockedUseCourseDetail.mockReturnValue({ ...base, notFound: true });

    renderAt('/courses/999');
    fireEvent.click(screen.getByRole('button', { name: '목록으로' }));

    expect(screen.getByText('목록 화면')).toBeInTheDocument();
  });
});
