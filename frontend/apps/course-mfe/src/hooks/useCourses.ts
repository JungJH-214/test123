import type { PageResponseCourseSummaryResponse } from '@eduhub/api-client';
import { useEffect, useState } from 'react';
import { courseApi } from '../api/courseApi';

export interface UseCoursesParams {
  keyword?: string;
  category?: string;
  /** 0부터 시작한다. 백엔드 Pageable과 MUI TablePagination 모두 0부터라 그대로 넘긴다. */
  page: number;
  size: number;
}

export interface UseCoursesResult {
  data: PageResponseCourseSummaryResponse | null;
  loading: boolean;
  error: Error | null;
}

/** 과정 목록 조회 (FR-01). */
export function useCourses({ keyword, category, page, size }: UseCoursesParams): UseCoursesResult {
  const [result, setResult] = useState<UseCoursesResult>({ data: null, loading: true, error: null });

  useEffect(() => {
    let cancelled = false;
    setResult((prev) => ({ ...prev, loading: true, error: null }));

    courseApi
      .getCourses({
        keyword: keyword || undefined,
        category: category || undefined,
        page,
        size,
      })
      .then((response) => {
        if (!cancelled) {
          setResult({ data: response.data, loading: false, error: null });
        }
      })
      .catch((e: unknown) => {
        if (!cancelled) {
          setResult({
            data: null,
            loading: false,
            error: e instanceof Error ? e : new Error(String(e)),
          });
        }
      });

    return () => {
      cancelled = true;
    };
  }, [keyword, category, page, size]);

  return result;
}
