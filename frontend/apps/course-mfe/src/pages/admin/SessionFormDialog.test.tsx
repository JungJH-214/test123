import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { SessionFormDialog } from './SessionFormDialog';

describe('SessionFormDialog', () => {
  it('정원이 1보다 작으면 제출하지 않는다', async () => {
    const onSubmit = vi.fn();
    render(<SessionFormDialog open onClose={vi.fn()} onSubmit={onSubmit} />);

    fireEvent.change(screen.getByLabelText(/시작일/), { target: { value: '2026-12-01' } });
    fireEvent.change(screen.getByRole('textbox', { name: /장소/ }), { target: { value: '본사' } });
    fireEvent.change(screen.getByRole('spinbutton', { name: /정원/ }), { target: { value: '0' } });
    fireEvent.click(screen.getByRole('button', { name: '저장' }));

    expect(await screen.findByText('1 이상의 정수를 입력하세요')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('지난 날짜도 숫자 정원과 함께 제출한다', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<SessionFormDialog open onClose={vi.fn()} onSubmit={onSubmit} />);

    fireEvent.change(screen.getByLabelText(/시작일/), { target: { value: '2020-01-01' } });
    fireEvent.change(screen.getByRole('textbox', { name: /장소/ }), { target: { value: ' 본사 3층 ' } });
    fireEvent.change(screen.getByRole('spinbutton', { name: /정원/ }), { target: { value: '20' } });
    fireEvent.click(screen.getByRole('button', { name: '저장' }));

    await waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith({ startDate: '2020-01-01', place: '본사 3층', capacity: 20 }),
    );
  });

  it('수정이면 기존 값을 채운다', () => {
    render(
      <SessionFormDialog
        open
        initial={{ startDate: '2026-12-01', place: '본사', capacity: 15 }}
        onClose={vi.fn()}
        onSubmit={vi.fn()}
      />,
    );

    expect(screen.getByText('회차 수정')).toBeInTheDocument();
    expect(screen.getByLabelText(/시작일/)).toHaveValue('2026-12-01');
    expect(screen.getByRole('spinbutton', { name: /정원/ })).toHaveValue(15);
  });
});
