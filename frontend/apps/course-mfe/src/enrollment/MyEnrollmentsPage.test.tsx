import type { MyEnrollmentResponse } from '@eduhub/api-client';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useCancelEnrollment } from '../hooks/useCancelEnrollment';
import { useMyEnrollments } from '../hooks/useMyEnrollments';
import { MyEnrollmentsPage } from './MyEnrollmentsPage';

vi.mock('../hooks/useMyEnrollments', () => ({ useMyEnrollments: vi.fn() }));
vi.mock('../hooks/useCancelEnrollment', () => ({ useCancelEnrollment: vi.fn() }));

const ITEMS: MyEnrollmentResponse[] = [
  { id: 1, sessionId: 10, courseTitle: 'Java 기초', startDate: '2026-12-01', place: '본사', status: 'CONFIRMED' },
  { id: 2, sessionId: 11, courseTitle: 'SQL 튜닝', startDate: '2026-12-05', place: '연수원', status: 'WAITING', waitingNumber: 2 },
  { id: 3, sessionId: 12, courseTitle: 'Kotlin 입문', startDate: '2026-11-01', place: '본사', status: 'COMPLETED' },
  { id: 4, sessionId: 13, courseTitle: '보고서 작성법', startDate: '2026-12-10', place: '본사', status: 'CANCELLED' },
];

const reload = vi.fn();
const cancel = vi.fn();

function given(items: MyEnrollmentResponse[], cancelError: string | null = null) {
  vi.mocked(useMyEnrollments).mockReturnValue({ data: items, loading: false, error: null, reload });
  vi.mocked(useCancelEnrollment).mockReturnValue({ submitting: false, error: cancelError, cancel, clearError: vi.fn() });
}

function rowOf(title: string) {
  return screen.getByRole('row', { name: new RegExp(title) });
}

describe('MyEnrollmentsPage', () => {
  beforeEach(() => {
    reload.mockReset();
    cancel.mockReset();
  });

  it('상태 칩을 보여 주고 대기는 대기 N번으로 표시한다', () => {
    given(ITEMS);
    render(<MyEnrollmentsPage />);

    expect(within(rowOf('Java 기초')).getByText('확정')).toBeInTheDocument();
    expect(within(rowOf('SQL 튜닝')).getByText('대기 2번')).toBeInTheDocument();
    expect(within(rowOf('Kotlin 입문')).getByText('수료')).toBeInTheDocument();
    expect(within(rowOf('보고서 작성법')).getByText('취소')).toBeInTheDocument();
  });

  it('확정·대기 신청에만 취소 버튼을 둔다', () => {
    given(ITEMS);
    render(<MyEnrollmentsPage />);

    expect(within(rowOf('Java 기초')).getByRole('button', { name: '취소' })).toBeInTheDocument();
    expect(within(rowOf('SQL 튜닝')).getByRole('button', { name: '취소' })).toBeInTheDocument();
    expect(within(rowOf('Kotlin 입문')).queryByRole('button', { name: '취소' })).not.toBeInTheDocument();
    expect(within(rowOf('보고서 작성법')).queryByRole('button', { name: '취소' })).not.toBeInTheDocument();
  });

  it('확인을 받은 뒤 취소하고 목록을 다시 불러온다', async () => {
    given(ITEMS);
    cancel.mockResolvedValue(true);
    render(<MyEnrollmentsPage />);

    fireEvent.click(within(rowOf('SQL 튜닝')).getByRole('button', { name: '취소' }));
    expect(cancel).not.toHaveBeenCalled();
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: '신청 취소' }));

    await waitFor(() => expect(reload).toHaveBeenCalledTimes(1));
    expect(cancel).toHaveBeenCalledWith(2);
    expect(await screen.findByText('신청을 취소했습니다.')).toBeInTheDocument();
  });

  it('취소 오류가 있으면 서버 메시지를 보여 준다', () => {
    given(ITEMS, '취소 마감이 지났습니다. 회차 시작 1일 전까지 취소할 수 있습니다');
    render(<MyEnrollmentsPage />);

    expect(screen.getByRole('alert')).toHaveTextContent('취소 마감이 지났습니다');
  });

  it('취소에 실패해도 목록을 다시 불러와 실제 상태를 보여 준다(성공 안내는 없음)', async () => {
    given(ITEMS);
    cancel.mockResolvedValue(false);
    render(<MyEnrollmentsPage />);

    fireEvent.click(within(rowOf('Java 기초')).getByRole('button', { name: '취소' }));
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: '신청 취소' }));

    await waitFor(() => expect(reload).toHaveBeenCalledTimes(1));
    expect(cancel).toHaveBeenCalledWith(1);
    expect(screen.queryByText('신청을 취소했습니다.')).not.toBeInTheDocument();
  });

  it('다시 불러오기에 실패해도 보여 주던 목록은 남기고 오류를 알린다', () => {
    vi.mocked(useMyEnrollments).mockReturnValue({
      data: ITEMS,
      loading: false,
      error: new Error('Network Error'),
      reload,
    });
    vi.mocked(useCancelEnrollment).mockReturnValue({ submitting: false, error: null, cancel, clearError: vi.fn() });
    render(<MyEnrollmentsPage />);

    expect(screen.getByRole('alert')).toHaveTextContent('신청 내역을 불러오지 못했습니다. (Network Error)');
    expect(rowOf('Java 기초')).toBeInTheDocument();
  });

  it('상태 필터를 바꾸면 그 상태로 조회한다', () => {
    given([]);
    render(<MyEnrollmentsPage />);

    expect(vi.mocked(useMyEnrollments)).toHaveBeenLastCalledWith(undefined);
    fireEvent.click(screen.getByRole('button', { name: '대기' }));
    expect(vi.mocked(useMyEnrollments)).toHaveBeenLastCalledWith('WAITING');
    fireEvent.click(screen.getByRole('button', { name: '수료' }));
    expect(vi.mocked(useMyEnrollments)).toHaveBeenLastCalledWith('COMPLETED');
    expect(screen.getByText('신청 내역이 없습니다.')).toBeInTheDocument();
  });
});
