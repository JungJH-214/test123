import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Link,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import type { CourseRequest } from '@eduhub/api-client';
import { FormEvent, useState } from 'react';
import { useAdminCourseActions } from '../../hooks/useAdminCourseActions';
import { useCourses } from '../../hooks/useCourses';
import { navigate } from '../../hooks/useHashRoute';
import { CourseFormDialog } from './CourseFormDialog';
import { STATUS_LABELS } from './statusLabels';

/** 관리자 과정 목록 (FR-03). 등록하면 새 과정의 관리 화면으로 간다. */
export function AdminCourseListPage() {
  const [keywordInput, setKeywordInput] = useState('');
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [creating, setCreating] = useState(false);
  const { data, loading, error } = useCourses({ keyword, page, size });
  const { createCourse } = useAdminCourseActions();

  const handleSearch = (event: FormEvent) => {
    event.preventDefault();
    setKeyword(keywordInput.trim());
    setPage(0);
  };

  const handleCreate = async (request: CourseRequest) => {
    const created = await createCourse(request);
    setCreating(false);
    navigate(`/admin/courses/${created.id}`);
  };

  const courses = data?.content ?? [];

  return (
    <Box sx={{ p: 3 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 2 }}>
        <Typography variant="h5" component="h1">
          과정 관리
        </Typography>
        <Button variant="contained" onClick={() => setCreating(true)}>
          과정 등록
        </Button>
      </Stack>

      <Stack component="form" direction="row" spacing={2} sx={{ mb: 2 }} onSubmit={handleSearch}>
        <TextField
          label="과정명 검색"
          size="small"
          value={keywordInput}
          onChange={(e) => setKeywordInput(e.target.value)}
        />
        <Button type="submit" variant="outlined">
          검색
        </Button>
      </Stack>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          과정 목록을 불러오지 못했습니다. ({error.message})
        </Alert>
      )}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress />
        </Box>
      ) : (
        <Paper>
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>ID</TableCell>
                  <TableCell>과정명</TableCell>
                  <TableCell>분류</TableCell>
                  <TableCell>상태</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {courses.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={4} align="center">
                      과정이 없습니다.
                    </TableCell>
                  </TableRow>
                ) : (
                  courses.map((course) => (
                    <TableRow key={course.id} hover>
                      <TableCell>{course.id}</TableCell>
                      <TableCell>
                        <Link href={`#/admin/courses/${course.id}`}>{course.title}</Link>
                      </TableCell>
                      <TableCell>{course.category}</TableCell>
                      <TableCell>{STATUS_LABELS[course.status ?? ''] ?? course.status}</TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
          <TablePagination
            component="div"
            count={data?.totalElements ?? 0}
            page={page}
            rowsPerPage={size}
            rowsPerPageOptions={[10, 20, 50]}
            onPageChange={(_, nextPage) => setPage(nextPage)}
            onRowsPerPageChange={(e) => {
              setSize(Number(e.target.value));
              setPage(0);
            }}
            labelRowsPerPage="페이지당 행 수"
          />
        </Paper>
      )}

      <CourseFormDialog
        open={creating}
        onClose={() => setCreating(false)}
        onSubmit={handleCreate}
      />
    </Box>
  );
}
