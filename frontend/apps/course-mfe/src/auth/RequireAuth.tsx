import { UserResponseRoleEnum } from '@eduhub/api-client';
import { Alert, Box, CircularProgress } from '@mui/material';
import { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { LOGIN_PATH, LoginLocationState, useAuth } from './AuthContext';

interface RequireAuthProps {
  /** true면 관리자(ADMIN)만 들어올 수 있다. */
  admin?: boolean;
  children: ReactNode;
}

/** 로그인한 사용자만 보여 준다. 로그인하지 않았으면 원래 경로를 state.from에 담아 로그인 화면으로 보낸다 (NFR-03). */
export function RequireAuth({ admin = false, children }: RequireAuthProps) {
  const { user, initializing } = useAuth();
  const location = useLocation();

  if (initializing) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
        <CircularProgress aria-label="로그인 확인 중" />
      </Box>
    );
  }

  if (!user) {
    const state: LoginLocationState = { from: location };
    return <Navigate to={LOGIN_PATH} replace state={state} />;
  }

  if (admin && user.role !== UserResponseRoleEnum.Admin) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="warning">관리자만 볼 수 있는 화면입니다.</Alert>
      </Box>
    );
  }

  return <>{children}</>;
}
