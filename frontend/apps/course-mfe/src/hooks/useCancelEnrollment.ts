import { useCallback, useState } from 'react';
import { enrollmentApi } from '../api/clients';
import { toErrorMessage } from '../api/errorMessage';

export interface UseCancelEnrollmentResult {
  submitting: boolean;
  error: string | null;
  /** 성공하면 true. 실패하면 error에 서버 메시지(마감 지남 등)를 담고 false. */
  cancel: (enrollmentId: number) => Promise<boolean>;
  clearError: () => void;
}

/** 내 신청 취소 (FR-06). 확정 취소 시 대기 1순위 승급은 서버가 한다. */
export function useCancelEnrollment(): UseCancelEnrollmentResult {
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const cancel = useCallback(async (enrollmentId: number) => {
    setSubmitting(true);
    setError(null);
    try {
      await enrollmentApi.cancel({ enrollmentId });
      return true;
    } catch (e) {
      setError(toErrorMessage(e));
      return false;
    } finally {
      setSubmitting(false);
    }
  }, []);

  const clearError = useCallback(() => setError(null), []);

  return { submitting, error, cancel, clearError };
}
