import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useParams } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useCourses } from '../hooks/useCourses';
import { CourseListPage } from './CourseListPage';

function DetailStub() {
  const { id } = useParams();
  return <div>상세 화면 {id}</div>;
}

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/']}>
      <Routes>
        <Route path="/" element={<CourseListPage />} />
        <Route path="/courses/:id" element={<DetailStub />} />
      </Routes>
    </MemoryRouter>,
  );
}

vi.mock('../hooks/useCourses', () => ({
  useCourses: vi.fn(),
}));

const mockedUseCourses = vi.mocked(useCourses);

describe('CourseListPage', () => {
  beforeEach(() => {
    mockedUseCourses.mockReset();
  });

  it('목록을 테이블에 보여 준다', () => {
    mockedUseCourses.mockReturnValue({
      data: {
        content: [
          { id: 1, title: 'Java 기초', category: '개발', status: 'OPEN' },
          { id: 2, title: '코칭 대화법', category: '리더십', status: 'CLOSED' },
        ],
        page: 0,
        size: 20,
        totalElements: 2,
      },
      loading: false,
      error: null,
    });

    renderPage();

    expect(screen.getByText('Java 기초')).toBeInTheDocument();
    expect(screen.getByText('코칭 대화법')).toBeInTheDocument();
    expect(screen.getByText('종료')).toBeInTheDocument();
  });

  it('행을 누르면 과정 상세로 이동한다', () => {
    mockedUseCourses.mockReturnValue({
      data: {
        content: [{ id: 5, title: 'Java 기초', category: '개발', status: 'OPEN' }],
        page: 0,
        size: 20,
        totalElements: 1,
      },
      loading: false,
      error: null,
    });

    renderPage();
    fireEvent.click(screen.getByText('Java 기초'));

    expect(screen.getByText('상세 화면 5')).toBeInTheDocument();
  });

  it('에러가 나면 메시지를 보여 준다', () => {
    mockedUseCourses.mockReturnValue({
      data: null,
      loading: false,
      error: new Error('Network Error'),
    });

    renderPage();

    expect(screen.getByRole('alert')).toHaveTextContent('과정 목록을 불러오지 못했습니다.');
  });

  it('불러오는 중에는 로딩 표시를 보여 준다', () => {
    mockedUseCourses.mockReturnValue({ data: null, loading: true, error: null });

    renderPage();

    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });
});
