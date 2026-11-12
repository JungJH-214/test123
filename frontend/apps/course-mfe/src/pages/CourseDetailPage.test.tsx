import { fireEvent, render, screen, within } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useCourseDetail, type UseCourseDetailResult } from '../hooks/useCourseDetail';
import { CourseDetailPage } from './CourseDetailPage';

vi.mock('../hooks/useCourseDetail', () => ({
  useCourseDetail: vi.fn(),
}));

const mockedUseCourseDetail = vi.mocked(useCourseDetail);

const base: UseCourseDetailResult = { data: null, loading: false, error: null, notFound: false };

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/" element={<div>목록 화면</div>} />
        <Route path="/courses/:id" element={<CourseDetailPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('CourseDetailPage', () => {
  beforeEach(() => {
    mockedUseCourseDetail.mockReset();
  });

  it('URL의 id로 훅을 부른다', () => {
    mockedUseCourseDetail.mockReturnValue({ ...base, loading: true });

    renderAt('/courses/7');

    expect(mockedUseCourseDetail).toHaveBeenCalledWith(7);
  });

  it('과정 정보와 회차 목록을 보여 준다', () => {
    mockedUseCourseDetail.mockReturnValue({
      ...base,
      data: {
        id: 1,
        title: 'Java 기초',
        category: '개발',
        description: '자바 입문 과정',
        status: 'OPEN',
        sessions: [
          { id: 10, startDate: '2026-12-01', place: '본사 3층', capacity: 20, remainingSeats: 15, status: 'OPEN' },
          { id: 11, startDate: '2026-06-01', place: '연수원', capacity: 30, remainingSeats: 0, status: 'CLOSED' },
        ],
      },
    });

    renderAt('/courses/1');

    expect(screen.getByRole('heading', { name: 'Java 기초' })).toBeInTheDocument();
    expect(screen.getByText('분류: 개발')).toBeInTheDocument();
    expect(screen.getByText('자바 입문 과정')).toBeInTheDocument();

    const rows = within(screen.getByRole('table', { name: '회차 목록' })).getAllByRole('row');
    expect(rows).toHaveLength(3); // 헤더 + 회차 2개
    expect(within(rows[1]).getByText('2026-12-01')).toBeInTheDocument();
    expect(within(rows[1]).getByText('본사 3층')).toBeInTheDocument();
    expect(within(rows[1]).getByText('20')).toBeInTheDocument();
    expect(within(rows[1]).getByText('15')).toBeInTheDocument();
    expect(within(rows[1]).getByText('모집 중')).toBeInTheDocument();
    expect(within(rows[2]).getByText('종료')).toBeInTheDocument();
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

  it('목록으로 버튼을 누르면 목록으로 돌아간다', () => {
    mockedUseCourseDetail.mockReturnValue({ ...base, notFound: true });

    renderAt('/courses/999');
    fireEvent.click(screen.getByRole('button', { name: '목록으로' }));

    expect(screen.getByText('목록 화면')).toBeInTheDocument();
  });
});
