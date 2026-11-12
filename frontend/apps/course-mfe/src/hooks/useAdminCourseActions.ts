import type {
  CourseDetailResponse,
  CourseRequest,
  SessionRequest,
  SessionResponse,
} from '@eduhub/api-client';
import { useCallback, useMemo, useState } from 'react';
import { courseAdminApi } from '../api/courseApi';
import { toErrorMessage } from '../api/errorMessage';

/** 실패하면 서버 메시지를 담아 던진다. 화면은 message를 그대로 보여 주면 된다. */
export class AdminActionError extends Error {}

export interface AdminCourseActions {
  createCourse: (request: CourseRequest) => Promise<CourseDetailResponse>;
  updateCourse: (courseId: number, request: CourseRequest) => Promise<CourseDetailResponse>;
  closeCourse: (courseId: number) => Promise<CourseDetailResponse>;
  createSession: (courseId: number, request: SessionRequest) => Promise<SessionResponse>;
  updateSession: (
    courseId: number,
    sessionId: number,
    request: SessionRequest,
  ) => Promise<SessionResponse>;
  closeSession: (courseId: number, sessionId: number) => Promise<SessionResponse>;
  /** 요청이 진행 중이면 true. 버튼을 막는 데 쓴다. */
  pending: boolean;
}

/** 과정·회차 등록, 수정, 종료 (FR-03). */
export function useAdminCourseActions(): AdminCourseActions {
  const [pendingCount, setPendingCount] = useState(0);

  const run = useCallback(async <T,>(call: () => Promise<{ data: T }>): Promise<T> => {
    setPendingCount((n) => n + 1);
    try {
      return (await call()).data;
    } catch (e) {
      throw new AdminActionError(toErrorMessage(e));
    } finally {
      setPendingCount((n) => n - 1);
    }
  }, []);

  const actions = useMemo(
    () => ({
      createCourse: (courseRequest: CourseRequest) =>
        run(() => courseAdminApi.createCourse({ courseRequest })),
      updateCourse: (courseId: number, courseRequest: CourseRequest) =>
        run(() => courseAdminApi.updateCourse({ courseId, courseRequest })),
      closeCourse: (courseId: number) => run(() => courseAdminApi.closeCourse({ courseId })),
      createSession: (courseId: number, sessionRequest: SessionRequest) =>
        run(() => courseAdminApi.createSession({ courseId, sessionRequest })),
      updateSession: (courseId: number, sessionId: number, sessionRequest: SessionRequest) =>
        run(() => courseAdminApi.updateSession({ courseId, sessionId, sessionRequest })),
      closeSession: (courseId: number, sessionId: number) =>
        run(() => courseAdminApi.closeSession({ courseId, sessionId })),
    }),
    [run],
  );

  return { ...actions, pending: pendingCount > 0 };
}
