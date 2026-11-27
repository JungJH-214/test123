import { UserResponseRoleEnum } from '@eduhub/api-client';
import { AppBar, Box, Button, Toolbar, Typography } from '@mui/material';
import { Link as RouterLink, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { LOGIN_PATH, LoginLocationState, useAuth } from './auth/AuthContext';
import { LoginPage } from './auth/LoginPage';
import { RequireAuth } from './auth/RequireAuth';
import { AdminRoutes } from './pages/admin/AdminApp';
import { CourseDetailPage } from './pages/CourseDetailPage';
import { CourseListPage } from './pages/CourseListPage';
import { MyEnrollmentsPage } from './enrollment/MyEnrollmentsPage';

export const MY_ENROLLMENTS_PATH = '/my/enrollments';

export function App() {
  return (
    <>
      <TopBar />
      <Routes>
        <Route path="/" element={<CourseListPage />} />
        <Route path="/courses/:courseId" element={<CourseDetailPage />} />
        <Route path={LOGIN_PATH} element={<LoginPage />} />
        <Route
          path={MY_ENROLLMENTS_PATH}
          element={
            <RequireAuth>
              <MyEnrollmentsPage />
            </RequireAuth>
          }
        />
        <Route
          path="/admin/*"
          element={
            <RequireAuth admin>
              <AdminRoutes />
            </RequireAuth>
          }
        />
      </Routes>
    </>
  );
}

/** 상단 바. 로그인 상태와 권한에 따라 메뉴가 바뀐다 (NFR-03). */
function TopBar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  // 로그인 버튼을 누르면 지금 화면으로 돌아오게 한다. 로그인 화면에서는 기존 state를 그대로 둔다.
  const loginState: LoginLocationState | undefined =
    location.pathname === LOGIN_PATH ? (location.state as LoginLocationState | null) ?? undefined : { from: location };

  return (
    <AppBar position="static">
      <Toolbar sx={{ gap: 1 }}>
        <Typography variant="h6" component="div" sx={{ mr: 2 }}>
          EduHub
        </Typography>
        <Button color="inherit" component={RouterLink} to="/">
          교육과정
        </Button>
        {user && (
          <Button color="inherit" component={RouterLink} to={MY_ENROLLMENTS_PATH}>
            내 신청 내역
          </Button>
        )}
        {user?.role === UserResponseRoleEnum.Admin && (
          <Button color="inherit" component={RouterLink} to="/admin">
            과정 등록
          </Button>
        )}
        <Box sx={{ flexGrow: 1 }} />
        {user ? (
          <>
            <Typography variant="body2">{user.name}</Typography>
            <Button color="inherit" onClick={handleLogout}>
              로그아웃
            </Button>
          </>
        ) : (
          <Button color="inherit" component={RouterLink} to={LOGIN_PATH} state={loginState}>
            로그인
          </Button>
        )}
      </Toolbar>
    </AppBar>
  );
}
