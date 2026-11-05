import { renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { courseApi } from '../api/courseApi';
import { useCourses } from './useCourses';

vi.mock('../api/courseApi', () => ({
  courseApi: { getCourses: vi.fn() },
}));

const getCourses = vi.mocked(courseApi.getCourses);

const page = {
  content: [{ id: 1, title: 'Java 기초', category: '개발', status: 'OPEN' as const }],
  page: 0,
  size: 20,
  totalElements: 1,
};

describe('useCourses', () => {
  beforeEach(() => {
    getCourses.mockReset();
  });

  it('목록을 불러온다', async () => {
    getCourses.mockResolvedValue({ data: page } as Awaited<ReturnType<typeof courseApi.getCourses>>);

    const { result } = renderHook(() => useCourses({ page: 0, size: 20 }));

    expect(result.current.loading).toBe(true);
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.data).toEqual(page);
    expect(result.current.error).toBeNull();
  });

  it('입력값을 그대로 API에 넘긴다', async () => {
    getCourses.mockResolvedValue({ data: page } as Awaited<ReturnType<typeof courseApi.getCourses>>);

    renderHook(() => useCourses({ keyword: 'java', category: '개발', page: 2, size: 10 }));

    await waitFor(() =>
      expect(getCourses).toHaveBeenCalledWith({ keyword: 'java', category: '개발', page: 2, size: 10 }),
    );
  });

  it('실패하면 error를 채운다', async () => {
    getCourses.mockRejectedValue(new Error('Network Error'));

    const { result } = renderHook(() => useCourses({ page: 0, size: 20 }));

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.error?.message).toBe('Network Error');
    expect(result.current.data).toBeNull();
  });
});
