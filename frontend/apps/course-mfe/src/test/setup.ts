import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

// vitest globals를 끄고 있어 Testing Library의 자동 cleanup이 등록되지 않는다. 테스트마다 렌더 결과를 지운다.
afterEach(() => {
  cleanup();
});
