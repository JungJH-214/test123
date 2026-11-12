import { AdminCourseDetailPage } from './AdminCourseDetailPage';
import { AdminCourseListPage } from './AdminCourseListPage';

/**
 * 관리 화면 진입점 (FR-03). 경로는 해시로 고른다.
 * - #/admin → 과정 목록
 * - #/admin/courses/{id} → 과정 상세와 회차 관리
 *
 * TODO(NFR-03): 인증이 들어오면 관리자만 들어올 수 있게 막는다.
 */
export function AdminApp({ route }: { route: string }) {
  const match = /^\/admin\/courses\/(\d+)$/.exec(route);
  if (match) {
    const courseId = Number(match[1]);
    return <AdminCourseDetailPage key={courseId} courseId={courseId} />;
  }
  return <AdminCourseListPage />;
}
