import type { CourseDetailResponse } from '@eduhub/api-client';
import { useCallback, useEffect, useState } from 'react';
import { courseApi } from '../api/courseApi';
import { toErrorMessage } from '../api/errorMessage';

export interface UseAdminCourseResult {
  data: CourseDetailResponse | null;
  loading: boolean;
  error: string | null;
  /** 등록·수정·종료 뒤 다시 불러온다. */
  reload: () => void;
}

/** 관리 화면의 과정 상세와 회차 목록 (FR-03). 조회는 공개 상세 API를 그대로 쓴다. */
export function useAdminCourse(courseId: number): UseAdminCourseResult {
  const [version, setVersion] = useState(0);
  const [state, setState] = useState<Omit<UseAdminCourseResult, 'reload'>>({
    data: null,
    loading: true,
    error: null,
  });

  useEffect(() => {
    let cancelled = false;
    setState((prev) => ({ ...prev, loading: true, error: null }));

    courseApi
      .getCourse({ id: courseId })
      .then((response) => {
        if (!cancelled) {
          setState({ data: response.data, loading: false, error: null });
        }
      })
      .catch((e: unknown) => {
        if (!cancelled) {
          setState({ data: null, loading: false, error: toErrorMessage(e) });
        }
      });

    return () => {
      cancelled = true;
    };
  }, [courseId, version]);

  const reload = useCallback(() => setVersion((v) => v + 1), []);

  return { ...state, reload };
}
