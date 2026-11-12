import { act, renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { courseAdminApi } from '../api/courseApi';
import { AdminActionError, useAdminCourseActions } from './useAdminCourseActions';

vi.mock('../api/courseApi', () => ({
  courseAdminApi: {
    createCourse: vi.fn(),
    updateCourse: vi.fn(),
    closeCourse: vi.fn(),
    createSession: vi.fn(),
    updateSession: vi.fn(),
    closeSession: vi.fn(),
  },
}));

const api = vi.mocked(courseAdminApi);
type Resp<K extends keyof typeof courseAdminApi> = Awaited<ReturnType<(typeof courseAdminApi)[K]>>;

describe('useAdminCourseActions', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('과정 등록 요청을 보내고 응답 본문을 돌려준다', async () => {
    const created = { id: 7, title: 'Java 기초', category: '개발', status: 'OPEN' as const, sessions: [] };
    api.createCourse.mockResolvedValue({ data: created } as unknown as Resp<'createCourse'>);
    const { result } = renderHook(() => useAdminCourseActions());

    let response;
    await act(async () => {
      response = await result.current.createCourse({ title: 'Java 기초', category: '개발' });
    });

    expect(api.createCourse).toHaveBeenCalledWith({
      courseRequest: { title: 'Java 기초', category: '개발' },
    });
    expect(response).toEqual(created);
    expect(result.current.pending).toBe(false);
  });

  it('회차 수정과 종료에 과정 ID와 회차 ID를 넘긴다', async () => {
    api.updateSession.mockResolvedValue({ data: {} } as Resp<'updateSession'>);
    api.closeSession.mockResolvedValue({ data: {} } as Resp<'closeSession'>);
    const { result } = renderHook(() => useAdminCourseActions());
    const request = { startDate: '2026-12-01', place: '본사', capacity: 10 };

    await act(async () => {
      await result.current.updateSession(1, 10, request);
      await result.current.closeSession(1, 10);
    });

    expect(api.updateSession).toHaveBeenCalledWith({ courseId: 1, sessionId: 10, sessionRequest: request });
    expect(api.closeSession).toHaveBeenCalledWith({ courseId: 1, sessionId: 10 });
  });

  it('실패하면 서버 메시지를 담은 AdminActionError를 던진다', async () => {
    api.updateCourse.mockRejectedValue(
      Object.assign(new Error('Request failed with status code 409'), {
        response: { data: { code: 'COURSE_CLOSED', message: '종료된 과정입니다: 1' } },
      }),
    );
    const { result } = renderHook(() => useAdminCourseActions());

    let caught: unknown;
    await act(async () => {
      await result.current.updateCourse(1, { title: 'a', category: '개발' }).catch((e) => {
        caught = e;
      });
    });

    expect(caught).toBeInstanceOf(AdminActionError);
    expect((caught as Error).message).toBe('종료된 과정입니다: 1');
    expect(result.current.pending).toBe(false);
  });
});
