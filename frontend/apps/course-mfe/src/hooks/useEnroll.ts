import { useCallback, useState } from 'react';
import { enrollmentApi } from '../api/clients';
import { toErrorMessage, toErrorResponse } from '../api/errorMessage';

/** 신청 결과. 대기면 신청 시점의 대기 순번(FR-05)을 담는다. */
export type EnrollOutcome = { status: 'CONFIRMED' } | { status: 'WAITING'; waitingNumber: number | null };

export interface UseEnrollResult {
  submitting: boolean;
  outcome: EnrollOutcome | null;
  error: string | null;
  enroll: () => Promise<void>;
}

/** 서버 오류 code별 사용자 메시지. 없는 code는 서버 메시지를 그대로 쓴다. */
const ERROR_MESSAGES: Record<string, string> = {
  ENROLLMENT_DUPLICATE: '이미 신청한 회차입니다',
  SESSION_FULL: '정원과 대기 인원이 모두 찼습니다',
  SESSION_CLOSED: '종료된 회차입니다',
};

/** 회차 수강 신청 (FR-04, FR-05). 신청자는 서버가 토큰에서 정한다. */
export function useEnroll(sessionId: number, onEnrolled?: () => void): UseEnrollResult {
  const [submitting, setSubmitting] = useState(false);
  const [outcome, setOutcome] = useState<EnrollOutcome | null>(null);
  const [error, setError] = useState<string | null>(null);

  const enroll = useCallback(async () => {
    setSubmitting(true);
    setError(null);
    try {
      const { data } = await enrollmentApi.enroll({ sessionId });
      setOutcome(
        data.status === 'WAITING'
          ? { status: 'WAITING', waitingNumber: data.waitingNumber ?? null }
          : { status: 'CONFIRMED' },
      );
      onEnrolled?.();
    } catch (e) {
      const code = toErrorResponse(e)?.code;
      setError((code && ERROR_MESSAGES[code]) ?? toErrorMessage(e));
    } finally {
      setSubmitting(false);
    }
  }, [sessionId, onEnrolled]);

  return { submitting, outcome, error, enroll };
}
