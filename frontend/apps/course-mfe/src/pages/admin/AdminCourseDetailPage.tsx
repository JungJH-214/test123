import type { CourseRequest, SessionRequest, SessionResponse } from '@eduhub/api-client';
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
import { useState } from 'react';
import { useAdminCourse } from '../../hooks/useAdminCourse';
import { useAdminCourseActions } from '../../hooks/useAdminCourseActions';
import { ConfirmDialog } from './ConfirmDialog';
import { CourseFormDialog } from './CourseFormDialog';
import { SessionFormDialog } from './SessionFormDialog';
import { STATUS_LABELS } from './statusLabels';

type SessionDialog = { mode: 'create' } | { mode: 'edit'; session: SessionResponse; initial: SessionRequest };
type Confirm = { kind: 'course' } | { kind: 'session'; session: SessionResponse };

/**
 * 관리자 과정 상세 (FR-03). 과정 수정·종료와 회차 등록·수정·종료를 한다.
 * 종료된 과정·회차는 수정할 수 없고, 과정을 종료하면 회차도 모두 종료된다.
 */
export function AdminCourseDetailPage({ courseId }: { courseId: number }) {
  const { data: course, loading, error, reload } = useAdminCourse(courseId);
  const actions = useAdminCourseActions();
  const [editingCourse, setEditingCourse] = useState<CourseRequest | null>(null);
  const [sessionDialog, setSessionDialog] = useState<SessionDialog | null>(null);
  const [confirm, setConfirm] = useState<Confirm | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  if (loading && !course) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error || !course) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error">과정을 불러오지 못했습니다. ({error})</Alert>
        <Link href="#/admin">목록으로</Link>
      </Box>
    );
  }

  const courseClosed = course.status === 'CLOSED';
  const sessions = course.sessions ?? [];

  const handleUpdateCourse = async (request: CourseRequest) => {
    await actions.updateCourse(courseId, request);
    setEditingCourse(null);
    reload();
  };

  const handleSessionSubmit = async (request: SessionRequest) => {
    if (sessionDialog?.mode === 'edit') {
      await actions.updateSession(courseId, sessionDialog.session.id!, request);
    } else {
      await actions.createSession(courseId, request);
    }
    setSessionDialog(null);
    reload();
  };

  const handleConfirm = async () => {
    if (!confirm) return;
    setActionError(null);
    try {
      if (confirm.kind === 'course') {
        await actions.closeCourse(courseId);
      } else {
        await actions.closeSession(courseId, confirm.session.id!);
      }
    } catch (e) {
      setActionError(e instanceof Error ? e.message : String(e));
    } finally {
      setConfirm(null);
      reload();
    }
  };

  return (
    <Box sx={{ p: 3 }}>
      <Link href="#/admin">← 과정 관리</Link>

      <Stack direction="row" spacing={1} alignItems="center" sx={{ mt: 1, mb: 1 }}>
        <Typography variant="h5" component="h1">
          {course.title}
        </Typography>
        <Chip
          size="small"
          label={STATUS_LABELS[course.status ?? ''] ?? course.status}
          color={courseClosed ? 'default' : 'success'}
        />
      </Stack>
      <Typography color="text.secondary">분류: {course.category}</Typography>
      {course.description && (
        <Typography sx={{ mt: 1, whiteSpace: 'pre-wrap' }}>{course.description}</Typography>
      )}

      <Stack direction="row" spacing={1} sx={{ my: 2 }}>
        <Button
          variant="outlined"
          disabled={courseClosed}
          onClick={() =>
            setEditingCourse({
              title: course.title ?? '',
              category: course.category ?? '',
              description: course.description ?? '',
            })
          }
        >
          과정 수정
        </Button>
        <Button
          variant="outlined"
          color="error"
          disabled={courseClosed}
          onClick={() => setConfirm({ kind: 'course' })}
        >
          과정 종료
        </Button>
      </Stack>

      {courseClosed && (
        <Alert severity="info" sx={{ mb: 2 }}>
          종료된 과정은 수정하거나 회차를 추가할 수 없습니다.
        </Alert>
      )}
      {actionError && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setActionError(null)}>
          {actionError}
        </Alert>
      )}

      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
        <Typography variant="h6" component="h2">
          회차
        </Typography>
        <Button
          variant="contained"
          size="small"
          disabled={courseClosed}
          onClick={() => setSessionDialog({ mode: 'create' })}
        >
          회차 등록
        </Button>
      </Stack>

      <Paper>
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>시작일</TableCell>
                <TableCell>장소</TableCell>
                <TableCell align="right">정원</TableCell>
                <TableCell>상태</TableCell>
                <TableCell align="right">관리</TableCell>
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
                sessions.map((session) => {
                  const sessionClosed = session.status === 'CLOSED';
                  return (
                    <TableRow key={session.id}>
                      <TableCell>{session.startDate}</TableCell>
                      <TableCell>{session.place}</TableCell>
                      <TableCell align="right">{session.capacity}</TableCell>
                      <TableCell>{STATUS_LABELS[session.status ?? ''] ?? session.status}</TableCell>
                      <TableCell align="right">
                        <Button
                          size="small"
                          disabled={sessionClosed}
                          onClick={() =>
                            setSessionDialog({
                              mode: 'edit',
                              session,
                              initial: {
                                startDate: session.startDate ?? '',
                                place: session.place ?? '',
                                capacity: session.capacity ?? 1,
                              },
                            })
                          }
                        >
                          수정
                        </Button>
                        <Button
                          size="small"
                          color="error"
                          disabled={sessionClosed}
                          onClick={() => setConfirm({ kind: 'session', session })}
                        >
                          종료
                        </Button>
                      </TableCell>
                    </TableRow>
                  );
                })
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      <CourseFormDialog
        open={editingCourse !== null}
        initial={editingCourse ?? undefined}
        onClose={() => setEditingCourse(null)}
        onSubmit={handleUpdateCourse}
      />
      <SessionFormDialog
        open={sessionDialog !== null}
        initial={sessionDialog?.mode === 'edit' ? sessionDialog.initial : undefined}
        onClose={() => setSessionDialog(null)}
        onSubmit={handleSessionSubmit}
      />
      <ConfirmDialog
        open={confirm !== null}
        title={confirm?.kind === 'course' ? '과정 종료' : '회차 종료'}
        message={
          confirm?.kind === 'course'
            ? '과정을 종료하면 모든 회차도 함께 종료되고 되돌릴 수 없습니다. 종료할까요?'
            : `${confirm?.kind === 'session' ? confirm.session.startDate : ''} 회차를 종료할까요? 되돌릴 수 없습니다.`
        }
        confirmLabel="종료"
        disabled={actions.pending}
        onCancel={() => setConfirm(null)}
        onConfirm={handleConfirm}
      />
    </Box>
  );
}
