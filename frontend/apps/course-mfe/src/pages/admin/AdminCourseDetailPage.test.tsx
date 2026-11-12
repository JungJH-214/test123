import type { CourseDetailResponse } from '@eduhub/api-client';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useAdminCourse } from '../../hooks/useAdminCourse';
import { useAdminCourseActions } from '../../hooks/useAdminCourseActions';
import { AdminCourseDetailPage } from './AdminCourseDetailPage';

vi.mock('../../hooks/useAdminCourse', () => ({ useAdminCourse: vi.fn() }));
vi.mock('../../hooks/useAdminCourseActions', () => ({ useAdminCourseActions: vi.fn() }));

const mockedUseAdminCourse = vi.mocked(useAdminCourse);
const mockedUseActions = vi.mocked(useAdminCourseActions);

const openCourse: CourseDetailResponse = {
  id: 1,
  title: 'Java 기초',
  category: '개발',
  description: '입문',
  status: 'OPEN',
  sessions: [
    { id: 10, startDate: '2026-12-01', place: '본사 3층', capacity: 20, remainingSeats: 20, status: 'OPEN' },
    { id: 11, startDate: '2026-12-15', place: '온라인', capacity: 30, remainingSeats: 30, status: 'CLOSED' },
  ],
};

function setup(course: CourseDetailResponse) {
  const reload = vi.fn();
  const actions = {
    createCourse: vi.fn(),
    updateCourse: vi.fn().mockResolvedValue({}),
    closeCourse: vi.fn().mockResolvedValue({}),
    createSession: vi.fn().mockResolvedValue({}),
    updateSession: vi.fn().mockResolvedValue({}),
    closeSession: vi.fn().mockResolvedValue({}),
    pending: false,
  };
  mockedUseAdminCourse.mockReturnValue({ data: course, loading: false, error: null, reload });
  mockedUseActions.mockReturnValue(actions);
  render(
    <MemoryRouter>
      <AdminCourseDetailPage courseId={1} />
    </MemoryRouter>,
  );
  return { reload, actions };
}

describe('AdminCourseDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('과정과 회차를 보여 주고, 종료된 회차는 수정할 수 없다', () => {
    setup(openCourse);

    expect(screen.getByRole('heading', { name: 'Java 기초' })).toBeInTheDocument();
    const closedRow = screen.getByText('온라인').closest('tr')!;
    expect(within(closedRow).getByRole('button', { name: '수정' })).toBeDisabled();
    const openRow = screen.getByText('본사 3층').closest('tr')!;
    expect(within(openRow).getByRole('button', { name: '수정' })).toBeEnabled();
  });

  it('과정 종료는 확인 후 요청하고 다시 불러온다', async () => {
    const { actions, reload } = setup(openCourse);

    fireEvent.click(screen.getByRole('button', { name: '과정 종료' }));
    expect(screen.getByText(/모든 회차도 함께 종료/)).toBeInTheDocument();
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: '종료' }));

    await waitFor(() => expect(actions.closeCourse).toHaveBeenCalledWith(1));
    expect(reload).toHaveBeenCalled();
  });

  it('회차 종료 확인을 취소하면 요청하지 않는다', () => {
    const { actions } = setup(openCourse);
    const openRow = screen.getByText('본사 3층').closest('tr')!;

    fireEvent.click(within(openRow).getByRole('button', { name: '종료' }));
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: '취소' }));

    expect(actions.closeSession).not.toHaveBeenCalled();
  });

  it('회차를 수정하면 과정 ID와 회차 ID로 요청한다', async () => {
    const { actions, reload } = setup(openCourse);
    const openRow = screen.getByText('본사 3층').closest('tr')!;

    fireEvent.click(within(openRow).getByRole('button', { name: '수정' }));
    fireEvent.change(screen.getByRole('spinbutton', { name: /정원/ }), { target: { value: '25' } });
    fireEvent.click(screen.getByRole('button', { name: '저장' }));

    await waitFor(() =>
      expect(actions.updateSession).toHaveBeenCalledWith(1, 10, {
        startDate: '2026-12-01',
        place: '본사 3층',
        capacity: 25,
      }),
    );
    expect(reload).toHaveBeenCalled();
  });

  it('종료된 과정은 수정, 종료, 회차 등록 버튼을 막는다', () => {
    setup({ ...openCourse, status: 'CLOSED', sessions: [] });

    expect(screen.getByRole('button', { name: '과정 수정' })).toBeDisabled();
    expect(screen.getByRole('button', { name: '과정 종료' })).toBeDisabled();
    expect(screen.getByRole('button', { name: '회차 등록' })).toBeDisabled();
    expect(screen.getByText(/종료된 과정은 수정하거나/)).toBeInTheDocument();
  });
});
