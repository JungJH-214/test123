import { CssBaseline } from '@mui/material';
import { useHashRoute } from './hooks/useHashRoute';
import { AdminApp } from './pages/admin/AdminApp';
import { CourseListPage } from './pages/CourseListPage';

export function App() {
  const route = useHashRoute();
  return (
    <>
      <CssBaseline />
      {route.startsWith('/admin') ? <AdminApp route={route} /> : <CourseListPage />}
    </>
  );
}
