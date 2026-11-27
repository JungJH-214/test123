import type { ErrorResponse } from '@eduhub/api-client';

/** API 에러에서 서버 ErrorResponse를 꺼낸다. 없으면 null. */
export function toErrorResponse(e: unknown): ErrorResponse | null {
  return (e as { response?: { data?: ErrorResponse } } | null)?.response?.data ?? null;
}

/** API 에러에서 사용자에게 보여 줄 메시지를 꺼낸다. 서버 ErrorResponse.message가 있으면 그것을 쓴다. */
export function toErrorMessage(e: unknown): string {
  const data = toErrorResponse(e);
  if (data?.message) {
    return data.message;
  }
  return e instanceof Error ? e.message : String(e);
}
