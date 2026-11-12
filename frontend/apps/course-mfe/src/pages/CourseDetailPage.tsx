import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
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
import { useNavigate, useParams } from 'react-router-dom';
import { useCourseDetail } from '../hooks/useCourseDetail';

const COURSE_STATUS_LABELS: Record<string, string> = {
  OPEN: '운영 중',
  CLOSED: '종료',
};

const SESSION_STATUS_LABELS: Record<string, string> = {
  OPEN: '모집 중',
  CLOSED: '종료',
};

/** 과정 상세와 회차 목록 화면 (FR-02). 서버 데이터는 useCourseDetail로만 가져온다. */
export function CourseDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { data, loading, error, notFound } = useCourseDetail(Number(id));

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

  if (error || !data) {
    return (
      <Box sx={{ p: 3 }}>
        {backButton}
        <Alert severity="error">과정 정보를 불러오지 못했습니다. ({error?.message ?? '응답 없음'})</Alert>
      </Box>
    );
  }

  const sessions = data.sessions ?? [];

  return (
    <Box sx={{ p: 3 }}>
      {backButton}

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
                <TableCell align="right">정원</TableCell>
                <TableCell align="right">남은 자리</TableCell>
                <TableCell>상태</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {sessions.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={5} align="center">
                    등록된 회차가 없습니다.
                  </TableCell>
                </TableRow>
              ) : (
                sessions.map((session) => (
                  <TableRow key={session.id}>
                    <TableCell>{session.startDate}</TableCell>
                    <TableCell>{session.place}</TableCell>
                    <TableCell align="right">{session.capacity}</TableCell>
                    <TableCell align="right">{session.remainingSeats}</TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        label={SESSION_STATUS_LABELS[session.status ?? ''] ?? session.status}
                        color={session.status === 'OPEN' ? 'success' : 'default'}
                        variant="outlined"
                      />
                    </TableCell>
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
