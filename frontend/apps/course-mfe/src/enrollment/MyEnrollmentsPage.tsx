import type { MyEnrollmentResponse } from '@eduhub/api-client';
import { MyEnrollmentsStatusEnum } from '@eduhub/api-client';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material';
import { useState } from 'react';
import { useCancelEnrollment } from '../hooks/useCancelEnrollment';
import { useMyEnrollments } from '../hooks/useMyEnrollments';
import { ConfirmDialog } from '../pages/admin/ConfirmDialog';

type Filter = 'ALL' | MyEnrollmentsStatusEnum;

const FILTERS: { value: Filter; label: string }[] = [
  { value: 'ALL', label: '전체' },
  { value: MyEnrollmentsStatusEnum.Confirmed, label: '확정' },
  { value: MyEnrollmentsStatusEnum.Waiting, label: '대기' },
  { value: MyEnrollmentsStatusEnum.Cancelled, label: '취소' },
  { value: MyEnrollmentsStatusEnum.Completed, label: '수료' },
];

const STATUS_CHIP: Record<string, { label: string; color: 'primary' | 'warning' | 'default' | 'success' }> = {
  CONFIRMED: { label: '확정', color: 'primary' },
  WAITING: { label: '대기', color: 'warning' },
  CANCELLED: { label: '취소', color: 'default' },
  COMPLETED: { label: '수료', color: 'success' },
};

function statusLabel(item: MyEnrollmentResponse): string {
  if (item.status === 'WAITING' && item.waitingNumber != null) {
    return `대기 ${item.waitingNumber}번`;
  }
  return STATUS_CHIP[item.status ?? '']?.label ?? item.status ?? '';
}

function isCancellable(item: MyEnrollmentResponse): boolean {
  return item.status === 'CONFIRMED' || item.status === 'WAITING';
}

/**
 * 내 신청 내역 (FR-07)과 취소 (FR-06).
 * 상태 필터로 조회하고, 확정·대기 신청은 확인을 받은 뒤 취소하고 목록을 다시 불러온다.
 */
export function MyEnrollmentsPage() {
  const [filter, setFilter] = useState<Filter>('ALL');
  const { data, loading, error, reload } = useMyEnrollments(filter === 'ALL' ? undefined : filter);
  const { submitting, error: cancelError, cancel, clearError } = useCancelEnrollment();
  const [target, setTarget] = useState<MyEnrollmentResponse | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const handleConfirmCancel = async () => {
    if (target?.id == null) {
      return;
    }
    const ok = await cancel(target.id);
    setTarget(null);
    if (ok) {
      setNotice('신청을 취소했습니다.');
    }
    // 실패해도 다시 불러온다. 다른 곳에서 이미 취소·수료된 신청이면 목록이 실제 상태로 바뀐다.
    reload();
  };

  const items = data ?? [];

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h5" component="h1" gutterBottom>
        내 신청 내역
      </Typography>

      <ToggleButtonGroup
        size="small"
        exclusive
        value={filter}
        onChange={(_, value: Filter | null) => {
          if (value) {
            setFilter(value);
          }
        }}
        aria-label="상태 필터"
        sx={{ mb: 2 }}
      >
        {FILTERS.map((f) => (
          <ToggleButton key={f.value} value={f.value}>
            {f.label}
          </ToggleButton>
        ))}
      </ToggleButtonGroup>

      {notice && (
        <Alert severity="success" sx={{ mb: 2 }} onClose={() => setNotice(null)}>
          {notice}
        </Alert>
      )}
      {cancelError && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={clearError}>
          {cancelError}
        </Alert>
      )}

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          신청 내역을 불러오지 못했습니다. ({error.message})
        </Alert>
      )}
      {loading && !data ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress />
        </Box>
      ) : !data ? null : (
        <Paper>
          <TableContainer>
            <Table size="small" aria-label="신청 내역">
              <TableHead>
                <TableRow>
                  <TableCell>과정</TableCell>
                  <TableCell>시작일</TableCell>
                  <TableCell>장소</TableCell>
                  <TableCell>상태</TableCell>
                  <TableCell />
                </TableRow>
              </TableHead>
              <TableBody>
                {items.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={5} align="center">
                      신청 내역이 없습니다.
                    </TableCell>
                  </TableRow>
                ) : (
                  items.map((item) => (
                    <TableRow key={item.id}>
                      <TableCell>{item.courseTitle}</TableCell>
                      <TableCell>{item.startDate}</TableCell>
                      <TableCell>{item.place}</TableCell>
                      <TableCell>
                        <Chip
                          size="small"
                          label={statusLabel(item)}
                          color={STATUS_CHIP[item.status ?? '']?.color ?? 'default'}
                          variant={item.status === 'CANCELLED' ? 'outlined' : 'filled'}
                        />
                      </TableCell>
                      <TableCell align="right">
                        {isCancellable(item) && (
                          <Button
                            size="small"
                            color="error"
                            disabled={submitting}
                            onClick={() => {
                              clearError();
                              setNotice(null);
                              setTarget(item);
                            }}
                          >
                            취소
                          </Button>
                        )}
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </Paper>
      )}

      <ConfirmDialog
        open={target != null}
        title="신청 취소"
        message={
          target
            ? `${target.courseTitle} (${target.startDate}) 신청을 취소할까요? 취소한 신청은 되돌릴 수 없습니다.`
            : ''
        }
        confirmLabel="신청 취소"
        disabled={submitting}
        onCancel={() => {
          if (!submitting) {
            setTarget(null);
          }
        }}
        onConfirm={() => void handleConfirmCancel()}
      />
    </Box>
  );
}
