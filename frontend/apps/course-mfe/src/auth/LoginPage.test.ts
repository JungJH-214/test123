import type { Location } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { redirectTarget } from './LoginPage';

function loc(pathname: string, search = '', hash = ''): Location {
  return { pathname, search, hash, state: null, key: 'k' };
}

describe('redirectTarget', () => {
  it('원래 가려던 앱 안 경로로 돌려보낸다', () => {
    expect(redirectTarget(loc('/admin/courses/3', '?tab=1', '#s'))).toBe('/admin/courses/3?tab=1#s');
  });

  it('from이 없으면 과정 목록으로 보낸다', () => {
    expect(redirectTarget(undefined)).toBe('/');
  });

  it('다른 출처로 읽힐 수 있는 경로(//)나 로그인 화면 자신은 받지 않는다', () => {
    expect(redirectTarget(loc('//evil.example/x'))).toBe('/');
    expect(redirectTarget(loc('/login'))).toBe('/');
    expect(redirectTarget(loc('https://evil.example'))).toBe('/');
  });
});
