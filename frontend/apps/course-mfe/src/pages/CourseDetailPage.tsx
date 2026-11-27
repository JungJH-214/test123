import { UserResponseRoleEnum } from '@eduhub/api-client';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Link,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { EnrollButton } from '../enrollment/EnrollButton';
import { useCourseDetail } from '../hooks/useCourseDetail';

const COURSE_STATUS_LABELS: Record<string, string> = {
  OPEN: '운영 중',
  CLOSED: '종료',
};

const SESSION_STATUS_LABELS: Record<string, string> = {
  OPEN: '모집 중',
  CLOSED: '종료',
};

/** 관리 화면 경로. 명단 화면은 Step 16(FR-08 화면)에서 만든다. */
export const adminCoursePath = (courseId: number) => `/admin/courses/${courseId}`;
export const adminRosterPath = (sessionId: number) => `/admin/sessions/${sessionId}/roster`;

/**
 * 과정 상세와 회차 목록 화면 (FR-02). 서버 데이터는 useCourseDetail로만 가져온다.
 * 회차마다 신청 버튼(FR-04·05)을 두고, 종료된 과정에는 두지 않는다. 관리자에게는 과정 수정과 회차별 명단 링크를 보여 준다.
 */
export function CourseDetailPage() {
  const { courseId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { data, loading, error, notFound, reload } = useCourseDetail(Number(courseId));
  const isAdmin = user?.role === UserResponseRoleEnum.Admin;

  const backButton = (
    <Button variant="outlined" onClick={() => navigate('/')} sx={{ mb: 2 }}>
      목록으로
    </Button>
  );

  if (loading) {
    return (
      <Box sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress />
        </Box>
      </Box>
    );
  }

  if (notFound) {
    return (
      <Box sx={{ p: 3 }}>
        {backButton}
        <Alert severity="info">과정을 찾을 수 없습니다.</Alert>
      </Box>
    );
  }

  if (!data) {
    return (
      <Box sx={{ p: 3 }}>
        {backButton}
        <Alert severity="error">과정 정보를 불러오지 못했습니다. ({error?.message ?? '응답 없음'})</Alert>
      </Box>
    );
  }

  const sessions = data.sessions ?? [];
  const courseClosed = data.status === 'CLOSED';

  return (
    <Box sx={{ p: 3 }}>
      <Stack direction="row" spacing={1} sx={{ mb: 2 }}>
        {backButton}
        {isAdmin && data.id != null && (
          <Button variant="outlined" component={RouterLink} to={adminCoursePath(data.id)} sx={{ mb: 2 }}>
            과정 수정
          </Button>
        )}
      </Stack>

      <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
        <Typography variant="h5" component="h1">
          {data.title}
        </Typography>
        <Chip
          size="small"
          label={COURSE_STATUS_LABELS[data.status ?? ''] ?? data.status}
          color={data.status === 'OPEN' ? 'primary' : 'default'}
        />
      </Stack>
      <Typography variant="body2" color="text.secondary" gutterBottom>
        분류: {data.category}
      </Typography>
      {error && (
        <Alert severity="warning" sx={{ my: 2 }}>
          최신 정보를 불러오지 못했습니다. 잠시 뒤 다시 확인해 주세요. ({error.message})
        </Alert>
      )}
      {courseClosed && (
        <Alert severity="info" sx={{ my: 2 }}>
          종료된 과정입니다. 신청할 수 없습니다.
        </Alert>
      )}
      {data.description && (
        <Typography variant="body1" sx={{ mt: 2, mb: 3, whiteSpace: 'pre-line' }}>
          {data.description}
        </Typography>
      )}

      <Typography variant="h6" component="h2" gutterBottom>
        회차
      </Typography>
      <Paper>
        <TableContainer>
          <Table size="small" aria-label="회차 목록">
            <TableHead>
              <TableRow>
                <TableCell>시작일</TableCell>
                <TableCell>장소</TableCell>
                <TableCell align="right">남은 자리/정원</TableCell>
                <TableCell>상태</TableCell>
                {!courseClosed && <TableCell>신청</TableCell>}
                {isAdmin && <TableCell>관리</TableCell>}
              </TableRow>
            </TableHead>
            <TableBody>
              {sessions.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={4 + (courseClosed ? 0 : 1) + (isAdmin ? 1 : 0)} align="center">
                    등록된 회차가 없습니다.
                  </TableCell>
                </TableRow>
              ) : (
                sessions.map((session) => (
                  <TableRow key={session.id}>
                    <TableCell>{session.startDate}</TableCell>
                    <TableCell>{session.place}</TableCell>
                    <TableCell align="right">
                      {session.remainingSeats}/{session.capacity}
                    </TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        label={SESSION_STATUS_LABELS[session.status ?? ''] ?? session.status}
                        color={session.status === 'OPEN' ? 'success' : 'default'}
                        variant="outlined"
                      />
                    </TableCell>
                    {!courseClosed && (
                      <TableCell>
                        {session.status === 'OPEN' && session.id != null ? (
                          <EnrollButton sessionId={session.id} onEnrolled={reload} />
                        ) : (
                          '-'
                        )}
                      </TableCell>
                    )}
                    {isAdmin && (
                      <TableCell>
                        {session.id != null && (
                          <Link component={RouterLink} to={adminRosterPath(session.id)}>
                            명단
                          </Link>
                        )}
                      </TableCell>
                    )}
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>
    </Box>
  );
}
