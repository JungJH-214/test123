import type { SessionRequest } from '@eduhub/api-client';
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  TextField,
} from '@mui/material';
import { FormEvent, useEffect, useState } from 'react';

export interface SessionFormDialogProps {
  open: boolean;
  /** 수정이면 기존 값, 등록이면 생략한다. */
  initial?: SessionRequest;
  onClose: () => void;
  /** 실패하면 throw한다. 다이얼로그가 메시지를 보여 준다. */
  onSubmit: (request: SessionRequest) => Promise<void>;
}

interface SessionForm {
  startDate: string;
  place: string;
  capacity: string;
}

type SessionErrors = Partial<Record<keyof SessionForm, string>>;

function toForm(request?: SessionRequest): SessionForm {
  return {
    startDate: request?.startDate ?? '',
    place: request?.place ?? '',
    capacity: request ? String(request.capacity) : '',
  };
}

/** 서버 검증(SessionRequest)과 같은 규칙으로 먼저 확인한다. 지난 날짜도 허용한다. */
export function validateSession(form: SessionForm): SessionErrors {
  const errors: SessionErrors = {};
  if (!form.startDate) errors.startDate = '필수 입력입니다';
  if (!form.place.trim()) errors.place = '필수 입력입니다';
  else if (form.place.length > 200) errors.place = '200자 이하로 입력하세요';
  if (!form.capacity) errors.capacity = '필수 입력입니다';
  else if (!Number.isInteger(Number(form.capacity)) || Number(form.capacity) < 1)
    errors.capacity = '1 이상의 정수를 입력하세요';
  return errors;
}

/** 회차 등록·수정 다이얼로그 (FR-03). */
export function SessionFormDialog({ open, initial, onClose, onSubmit }: SessionFormDialogProps) {
  const [form, setForm] = useState<SessionForm>(toForm(initial));
  const [submitted, setSubmitted] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (open) {
      setForm(toForm(initial));
      setSubmitted(false);
      setServerError(null);
    }
  }, [open, initial]);

  const errors = submitted ? validateSession(form) : {};

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setSubmitted(true);
    if (Object.keys(validateSession(form)).length > 0) {
      return;
    }
    setSaving(true);
    setServerError(null);
    try {
      await onSubmit({
        startDate: form.startDate,
        place: form.place.trim(),
        capacity: Number(form.capacity),
      });
    } catch (e) {
      setServerError(e instanceof Error ? e.message : String(e));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="xs">
      <form onSubmit={handleSubmit} noValidate>
        <DialogTitle>{initial ? '회차 수정' : '회차 등록'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {serverError && <Alert severity="error">{serverError}</Alert>}
            <TextField
              label="시작일"
              type="date"
              required
              value={form.startDate}
              onChange={(e) => setForm({ ...form, startDate: e.target.value })}
              error={!!errors.startDate}
              helperText={errors.startDate}
              slotProps={{ inputLabel: { shrink: true } }}
            />
            <TextField
              label="장소"
              required
              value={form.place}
              onChange={(e) => setForm({ ...form, place: e.target.value })}
              error={!!errors.place}
              helperText={errors.place}
              slotProps={{ htmlInput: { maxLength: 200 } }}
            />
            <TextField
              label="정원"
              type="number"
              required
              value={form.capacity}
              onChange={(e) => setForm({ ...form, capacity: e.target.value })}
              error={!!errors.capacity}
              helperText={errors.capacity}
              slotProps={{ htmlInput: { min: 1, step: 1 } }}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>취소</Button>
          <Button type="submit" variant="contained" disabled={saving}>
            저장
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
