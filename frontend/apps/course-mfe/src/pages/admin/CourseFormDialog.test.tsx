import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { CourseFormDialog } from './CourseFormDialog';

function fillTitle(value: string) {
  fireEvent.change(screen.getByRole('textbox', { name: /과정명/ }), { target: { value } });
}

function chooseCategory(value: string) {
  fireEvent.mouseDown(screen.getByRole('combobox', { name: /분류/ }));
  fireEvent.click(within(screen.getByRole('listbox')).getByText(value));
}

describe('CourseFormDialog', () => {
  it('필수값이 비어 있으면 제출하지 않고 오류를 보여 준다', async () => {
    const onSubmit = vi.fn();
    render(<CourseFormDialog open onClose={vi.fn()} onSubmit={onSubmit} />);

    fireEvent.click(screen.getByRole('button', { name: '저장' }));

    expect(await screen.findAllByText('필수 입력입니다')).toHaveLength(2);
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('입력값을 다듬어 제출한다', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<CourseFormDialog open onClose={vi.fn()} onSubmit={onSubmit} />);

    fillTitle('  Java 기초  ');
    chooseCategory('개발');
    fireEvent.click(screen.getByRole('button', { name: '저장' }));

    await waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith({ title: 'Java 기초', category: '개발', description: undefined }),
    );
  });

  it('서버가 거절하면 메시지를 보여 준다', async () => {
    const onSubmit = vi.fn().mockRejectedValue(new Error('종료된 과정입니다: 1'));
    render(
      <CourseFormDialog
        open
        initial={{ title: 'Java 기초', category: '개발', description: '' }}
        onClose={vi.fn()}
        onSubmit={onSubmit}
      />,
    );

    expect(screen.getByText('과정 수정')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '저장' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('종료된 과정입니다: 1');
  });
});
