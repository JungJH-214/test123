import type { CourseDetailResponse } from '@eduhub/api-client';
import { useCallback, useEffect, useState } from 'react';
import { courseApi } from '../api/courseApi';

export interface UseCourseDetailResult {
  data: CourseDetailResponse | null;
  loading: boolean;
  /** 404 이외의 실패. */
  error: Error | null;
  /** 과정이 없거나(404) id가 올바르지 않을 때 true. */
  notFound: boolean;
  /** 같은 과정을 다시 불러온다(신청 뒤 남은 자리 갱신 등). 이미 보여 주던 내용은 그대로 두고 바꾼다. */
  reload: () => void;
}

type State = Omit<UseCourseDetailResult, 'reload'>;

function isNotFound(e: unknown): boolean {
  // axios 에러는 response.status를 가진다. 화면 앱에 axios 직접 의존을 두지 않으려고 형태만 확인한다.
  return (e as { response?: { status?: number } } | null)?.response?.status === 404;
}

/** 과정 상세와 회차 목록 조회 (FR-02). */
export function useCourseDetail(id: number): UseCourseDetailResult {
  const [result, setResult] = useState<State>({
    data: null,
    loading: true,
    error: null,
    notFound: false,
  });
  const [version, setVersion] = useState(0);
  const reload = useCallback(() => setVersion((v) => v + 1), []);

  useEffect(() => {
    if (!Number.isInteger(id) || id <= 0) {
      setResult({ data: null, loading: false, error: null, notFound: true });
      return;
    }

    let cancelled = false;
    // 같은 과정을 다시 불러올 때는 화면을 비우지 않는다(로딩 표시로 바꾸지 않음).
    setResult((prev) =>
      prev.data?.id === id
        ? { ...prev, error: null }
        : { data: null, loading: true, error: null, notFound: false },
    );

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
          return;
        }
        const error = e instanceof Error ? e : new Error(String(e));
        // 다시 불러오기가 실패해도 이미 보여 주던 내용은 남기고 오류만 알린다(신청 직후 화면이 사라지지 않게).
        setResult((prev) =>
          prev.data?.id === id
            ? { ...prev, loading: false, error }
            : { data: null, loading: false, error, notFound: false },
        );
      });

    return () => {
      cancelled = true;
    };
  }, [id, version]);

  return { ...result, reload };
}
