import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { courseApi } from '../api/courseApi';
import { useAdminCourse } from './useAdminCourse';

vi.mock('../api/courseApi', () => ({
  courseApi: { getCourse: vi.fn() },
}));

const getCourse = vi.mocked(courseApi.getCourse);
type Resp = Awaited<ReturnType<typeof courseApi.getCourse>>;

const course = { id: 1, title: 'Java 기초', category: '개발', status: 'OPEN' as const, sessions: [] };

describe('useAdminCourse', () => {
  beforeEach(() => {
    getCourse.mockReset();
  });

  it('과정 상세를 불러온다', async () => {
    getCourse.mockResolvedValue({ data: course } as unknown as Resp);

    const { result } = renderHook(() => useAdminCourse(1));

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(getCourse).toHaveBeenCalledWith({ id: 1 });
    expect(result.current.data).toEqual(course);
  });

  it('reload하면 다시 불러온다', async () => {
    getCourse.mockResolvedValue({ data: course } as unknown as Resp);
    const { result } = renderHook(() => useAdminCourse(1));
    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => result.current.reload());

    await waitFor(() => expect(getCourse).toHaveBeenCalledTimes(2));
  });

  it('404면 서버 메시지를 error에 담는다', async () => {
    getCourse.mockRejectedValue({ response: { data: { code: 'COURSE_NOT_FOUND', message: '과정을 찾을 수 없습니다: 9' } } });

    const { result } = renderHook(() => useAdminCourse(9));

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.error).toBe('과정을 찾을 수 없습니다: 9');
    expect(result.current.data).toBeNull();
  });
});
