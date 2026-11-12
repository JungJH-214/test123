import { renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { courseApi } from '../api/courseApi';
import { useCourseDetail } from './useCourseDetail';

vi.mock('../api/courseApi', () => ({
  courseApi: { getCourse: vi.fn() },
}));

const getCourse = vi.mocked(courseApi.getCourse);

type GetCourseResponse = Awaited<ReturnType<typeof courseApi.getCourse>>;

const detail = {
  id: 1,
  title: 'Java 기초',
  category: '개발',
  description: '자바 입문 과정',
  status: 'OPEN' as const,
  sessions: [
    { id: 10, startDate: '2026-12-01', place: '본사 3층', capacity: 20, remainingSeats: 20, status: 'OPEN' as const },
  ],
};

function axiosError(status: number) {
  return Object.assign(new Error(`Request failed with status code ${status}`), {
    isAxiosError: true,
    response: { status },
  });
}

describe('useCourseDetail', () => {
  beforeEach(() => {
    getCourse.mockReset();
  });

  it('과정 상세를 불러온다', async () => {
    getCourse.mockResolvedValue({ data: detail } as GetCourseResponse);

    const { result } = renderHook(() => useCourseDetail(1));

    expect(result.current.loading).toBe(true);
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.data).toEqual(detail);
    expect(result.current.error).toBeNull();
    expect(result.current.notFound).toBe(false);
  });

  it('id를 그대로 API에 넘긴다', async () => {
    getCourse.mockResolvedValue({ data: detail } as GetCourseResponse);

    renderHook(() => useCourseDetail(42));

    await waitFor(() => expect(getCourse).toHaveBeenCalledWith({ id: 42 }));
  });

  it('404면 notFound를 켜고 error는 비워 둔다', async () => {
    getCourse.mockRejectedValue(axiosError(404));

    const { result } = renderHook(() => useCourseDetail(999));

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.notFound).toBe(true);
    expect(result.current.error).toBeNull();
    expect(result.current.data).toBeNull();
  });

  it('404가 아닌 실패는 error를 채운다', async () => {
    getCourse.mockRejectedValue(axiosError(500));

    const { result } = renderHook(() => useCourseDetail(1));

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.error?.message).toBe('Request failed with status code 500');
    expect(result.current.notFound).toBe(false);
  });

  it('id가 숫자가 아니면 API를 부르지 않고 notFound로 둔다', async () => {
    const { result } = renderHook(() => useCourseDetail(Number('abc')));

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.notFound).toBe(true);
    expect(getCourse).not.toHaveBeenCalled();
  });
});
