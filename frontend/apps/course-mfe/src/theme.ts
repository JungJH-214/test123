import { createTheme } from '@mui/material/styles';

/** 앱 공통 MUI 테마. */
export const theme = createTheme({
  palette: {
    primary: { main: '#1565c0' },
  },
  typography: {
    fontFamily: ['"Pretendard"', '"Noto Sans KR"', '"Malgun Gothic"', 'sans-serif'].join(','),
  },
});
