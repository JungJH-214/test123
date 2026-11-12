import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

// vitest globals를 켜지 않아 RTL의 자동 cleanup이 돌지 않는다. 테스트마다 렌더 결과를 비운다.
afterEach(() => {
  cleanup();
});
