import { Button, Stack, Typography } from '@mui/material';
import { Link as RouterLink, useLocation } from 'react-router-dom';
import { LOGIN_PATH, LoginLocationState, useAuth } from '../auth/AuthContext';
import { useEnroll } from '../hooks/useEnroll';

interface EnrollButtonProps {
  sessionId: number;
  /** 신청에 성공하면 부른다(회차의 남은 자리를 다시 불러오는 등). */
  onEnrolled?: () => void;
}

/**
 * 회차 신청 버튼 (FR-04, FR-05).
 * - 로그인하지 않았으면 "로그인 후 신청"(로그인 화면으로, 돌아올 곳은 지금 화면)
 * - 신청 결과: 확정이면 "신청이 확정되었습니다", 대기면 "대기 N번으로 등록되었습니다"
 * - 409 중복이면 "이미 신청한 회차입니다"
 */
export function EnrollButton({ sessionId, onEnrolled }: EnrollButtonProps) {
  const { user } = useAuth();
  const location = useLocation();
  const { submitting, outcome, error, enroll } = useEnroll(sessionId, onEnrolled);

  if (!user) {
    const state: LoginLocationState = { from: location };
    return (
      <Button size="small" variant="outlined" component={RouterLink} to={LOGIN_PATH} state={state}>
        로그인 후 신청
      </Button>
    );
  }

  if (outcome) {
    return (
      <Typography variant="body2" color="success.main" role="status">
        {outcome.status === 'CONFIRMED'
          ? '신청이 확정되었습니다'
          : outcome.waitingNumber != null
            ? `대기 ${outcome.waitingNumber}번으로 등록되었습니다`
            : '대기자로 등록되었습니다'}
      </Typography>
    );
  }

  return (
    <Stack direction="row" spacing={1} alignItems="center">
      <Button size="small" variant="contained" disabled={submitting} onClick={() => void enroll()}>
        신청
      </Button>
      {error && (
        <Typography variant="body2" color="error" role="alert">
          {error}
        </Typography>
      )}
    </Stack>
  );
}
