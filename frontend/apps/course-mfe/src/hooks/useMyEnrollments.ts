import type { MyEnrollmentResponse, MyEnrollmentsStatusEnum } from '@eduhub/api-client';
import { useCallback, useEffect, useRef, useState } from 'react';
import { enrollmentApi } from '../api/clients';

export interface UseMyEnrollmentsResult {
  data: MyEnrollmentResponse[] | null;
  loading: boolean;
  error: Error | null;
  /** 같은 조건으로 다시 불러온다 (취소 뒤 등). */
  reload: () => void;
}

/** 내 신청 내역 (FR-07). status가 없으면 전체. */
export function useMyEnrollments(status?: MyEnrollmentsStatusEnum): UseMyEnrollmentsResult {
  const [result, setResult] = useState<Omit<UseMyEnrollmentsResult, 'reload'>>({
    data: null,
    loading: true,
    error: null,
  });
  const [version, setVersion] = useState(0);
  const reload = useCallback(() => setVersion((v) => v + 1), []);
  const lastStatus = useRef(status);

  useEffect(() => {
    let cancelled = false;
    const sameQuery = lastStatus.current === status;
    lastStatus.current = status;
    // 같은 조건으로 다시 불러올 때는 목록을 그대로 두고 바꾼다(로딩 표시로 바꾸지 않음).
    setResult((prev) =>
      sameQuery && prev.data ? { ...prev, error: null } : { data: null, loading: true, error: null },
    );

    enrollmentApi
      .myEnrollments({ status })
      .then((response) => {
        if (!cancelled) {
          setResult({ data: response.data, loading: false, error: null });
        }
      })
      .catch((e: unknown) => {
        if (!cancelled) {
          // 다시 불러오기가 실패해도 보여 주던 목록은 남긴다(필터를 바꾼 경우는 이전 목록이 맞지 않아 비운다).
          const error = e instanceof Error ? e : new Error(String(e));
          setResult((prev) => ({ data: sameQuery ? prev.data : null, loading: false, error }));
        }
      });

    return () => {
      cancelled = true;
    };
  }, [status, version]);

  return { ...result, reload };
}
