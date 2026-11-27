import { Alert, Box, Button, CircularProgress, Paper, Stack, TextField, Typography } from '@mui/material';
import { isAxiosError } from 'axios';
import { FormEvent, useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { toErrorMessage } from '../api/errorMessage';
import { LOGIN_PATH, LoginLocationState, useAuth } from './AuthContext';

export const INVALID_CREDENTIALS_MESSAGE = '사번 또는 비밀번호가 올바르지 않습니다.';

/**
 * 로그인 후 돌아갈 경로. 이 앱 안의 경로(`/`로 시작하고 `//`가 아닌 것)만 받고, 로그인 화면 자신이나
 * 그 밖의 값이면 과정 목록(`/`)으로 보낸다.
 */
export function redirectTarget(from: LoginLocationState['from']): string {
  if (!from || !from.pathname.startsWith('/') || from.pathname.startsWith('//') || from.pathname === LOGIN_PATH) {
    return '/';
  }
  return `${from.pathname}${from.search}${from.hash}`;
}

/** 로그인 화면 (NFR-03). 로그인하면 원래 가려던 화면(state.from)으로, 없으면 과정 목록으로 간다. */
export function LoginPage() {
  const { user, initializing, expired, login } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  const [employeeNo, setEmployeeNo] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const target = redirectTarget((location.state as LoginLocationState | null)?.from);

  // 저장된 토큰을 확인하는 중에는 폼을 보이지 않는다. 확인 응답과 새 로그인이 겹치지 않게 한다.
  if (initializing) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
        <CircularProgress aria-label="로그인 확인 중" />
      </Box>
    );
  }

  if (user && !submitting) {
    return <Navigate to={target} replace />;
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await login(employeeNo.trim(), password);
      navigate(target, { replace: true });
    } catch (e) {
      setError(isAxiosError(e) && e.response?.status === 401 ? INVALID_CREDENTIALS_MESSAGE : toErrorMessage(e));
      setSubmitting(false);
    }
  };

  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
      <Paper sx={{ p: 4, width: '100%', maxWidth: 400 }}>
        <Typography variant="h5" component="h1" gutterBottom>
          로그인
        </Typography>
        {expired && (
          <Alert severity="info" sx={{ mb: 2 }}>
            로그인이 만료되었습니다. 다시 로그인해 주세요.
          </Alert>
        )}
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}
        <Stack component="form" spacing={2} onSubmit={handleSubmit}>
          <TextField
            label="사번"
            value={employeeNo}
            onChange={(e) => setEmployeeNo(e.target.value)}
            autoComplete="username"
            required
            autoFocus
          />
          <TextField
            label="비밀번호"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            required
          />
          <Button type="submit" variant="contained" disabled={submitting}>
            로그인
          </Button>
        </Stack>
      </Paper>
    </Box>
  );
}
