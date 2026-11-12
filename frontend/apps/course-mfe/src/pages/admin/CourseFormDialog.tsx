import type { CourseRequest } from '@eduhub/api-client';
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Stack,
  TextField,
} from '@mui/material';
import { FormEvent, useEffect, useState } from 'react';
import { CATEGORIES } from '../../constants/categories';

export interface CourseFormDialogProps {
  open: boolean;
  /** 수정이면 기존 값, 등록이면 생략한다. */
  initial?: CourseRequest;
  onClose: () => void;
  /** 실패하면 throw한다. 다이얼로그가 메시지를 보여 준다. */
  onSubmit: (request: CourseRequest) => Promise<void>;
}

const EMPTY: CourseRequest = { title: '', category: '', description: '' };

/** 서버 검증(CourseRequest)과 같은 규칙으로 먼저 확인한다. */
export function validateCourse(form: CourseRequest): Partial<Record<keyof CourseRequest, string>> {
  const errors: Partial<Record<keyof CourseRequest, string>> = {};
  if (!form.title.trim()) errors.title = '필수 입력입니다';
  else if (form.title.length > 200) errors.title = '200자 이하로 입력하세요';
  if (!form.category.trim()) errors.category = '필수 입력입니다';
  else if (form.category.length > 50) errors.category = '50자 이하로 입력하세요';
  if ((form.description ?? '').length > 2000) errors.description = '2000자 이하로 입력하세요';
  return errors;
}

/** 과정 등록·수정 다이얼로그 (FR-03). */
export function CourseFormDialog({ open, initial, onClose, onSubmit }: CourseFormDialogProps) {
  const [form, setForm] = useState<CourseRequest>(initial ?? EMPTY);
  const [submitted, setSubmitted] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (open) {
      setForm(initial ?? EMPTY);
      setSubmitted(false);
      setServerError(null);
    }
  }, [open, initial]);

  const errors = submitted ? validateCourse(form) : {};
  const categories: string[] = [...CATEGORIES];
  if (form.category && !categories.includes(form.category)) {
    categories.push(form.category);
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setSubmitted(true);
    if (Object.keys(validateCourse(form)).length > 0) {
      return;
    }
    setSaving(true);
    setServerError(null);
    try {
      await onSubmit({
        title: form.title.trim(),
        category: form.category,
        description: form.description?.trim() || undefined,
      });
    } catch (e) {
      setServerError(e instanceof Error ? e.message : String(e));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <form onSubmit={handleSubmit} noValidate>
        <DialogTitle>{initial ? '과정 수정' : '과정 등록'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {serverError && <Alert severity="error">{serverError}</Alert>}
            <TextField
              label="과정명"
              required
              value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
              error={!!errors.title}
              helperText={errors.title}
              slotProps={{ htmlInput: { maxLength: 200 } }}
            />
            <TextField
              select
              label="분류"
              required
              value={form.category}
              onChange={(e) => setForm({ ...form, category: e.target.value })}
              error={!!errors.category}
              helperText={errors.category}
            >
              {categories.map((c) => (
                <MenuItem key={c} value={c}>
                  {c}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              label="설명"
              multiline
              minRows={3}
              value={form.description ?? ''}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              error={!!errors.description}
              helperText={errors.description}
              slotProps={{ htmlInput: { maxLength: 2000 } }}
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
