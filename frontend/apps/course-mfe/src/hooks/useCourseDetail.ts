import type { CourseDetailResponse } from '@eduhub/api-client';
import { useEffect, useState } from 'react';
import { courseApi } from '../api/courseApi';

export interface UseCourseDetailResult {
  data: CourseDetailResponse | null;
  loading: boolean;
  /** 404 이외의 실패. */
  error: Error | null;
  /** 과정이 없거나(404) id가 올바르지 않을 때 true. */
  notFound: boolean;
}

function isNotFound(e: unknown): boolean {
  // axios 에러는 response.status를 가진다. 화면 앱에 axios 직접 의존을 두지 않으려고 형태만 확인한다.
  return (e as { response?: { status?: number } } | null)?.response?.status === 404;
}

/** 과정 상세와 회차 목록 조회 (FR-02). */
export function useCourseDetail(id: number): UseCourseDetailResult {
  const [result, setResult] = useState<UseCourseDetailResult>({
    data: null,
    loading: true,
    error: null,
    notFound: false,
  });

  useEffect(() => {
    if (!Number.isInteger(id) || id <= 0) {
      setResult({ data: null, loading: false, error: null, notFound: true });
      return;
    }

    let cancelled = false;
    setResult({ data: null, loading: true, error: null, notFound: false });

    courseApi
      .getCourse({ id })
      .then((response) => {
        if (!cancelled) {
          setResult({ data: response.data, loading: false, error: null, notFound: false });
        }
      })
      .catch((e: unknown) => {
        if (cancelled) {
          return;
        }
        if (isNotFound(e)) {
          setResult({ data: null, loading: false, error: null, notFound: true });
        } else {
          setResult({
            data: null,
            loading: false,
            error: e instanceof Error ? e : new Error(String(e)),
            notFound: false,
          });
        }
      });

    return () => {
      cancelled = true;
    };
  }, [id]);

  return result;
}
