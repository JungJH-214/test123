import { Route, Routes, useParams } from 'react-router-dom';
import { AdminCourseDetailPage } from './AdminCourseDetailPage';
import { AdminCourseListPage } from './AdminCourseListPage';

/**
 * 관리 화면 라우트 (FR-03). `/admin/*` 아래에 붙는다.
 * - /admin → 과정 목록
 * - /admin/courses/{id} → 과정 상세와 회차 관리
 *
 * TODO(NFR-03): 인증이 들어오면 관리자만 들어올 수 있게 막는다.
 */
export function AdminRoutes() {
  return (
    <Routes>
      <Route index element={<AdminCourseListPage />} />
      <Route path="courses/:id" element={<AdminCourseDetailRoute />} />
    </Routes>
  );
}

function AdminCourseDetailRoute() {
  const courseId = Number(useParams().id);
  return <AdminCourseDetailPage key={courseId} courseId={courseId} />;
}
