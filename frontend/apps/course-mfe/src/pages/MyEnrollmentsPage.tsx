import { Alert, Box, Typography } from '@mui/material';

/** 내 신청 내역 (FR-07). 화면은 FR-07에서 만든다. 지금은 로그인 보호 경로만 잡아 둔다. */
export function MyEnrollmentsPage() {
  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h5" component="h1" gutterBottom>
        내 신청 내역
      </Typography>
      <Alert severity="info">준비 중입니다.</Alert>
    </Box>
  );
}
