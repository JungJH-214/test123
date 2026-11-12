import { useEffect, useState } from 'react';

function currentRoute(): string {
  return window.location.hash.replace(/^#/, '') || '/';
}

/** URL 해시(#/admin/...)를 경로로 돌려준다. 라우터를 들이기 전까지 쓰는 간단한 방식이다. */
export function useHashRoute(): string {
  const [route, setRoute] = useState(currentRoute);

  useEffect(() => {
    const onChange = () => setRoute(currentRoute());
    window.addEventListener('hashchange', onChange);
    return () => window.removeEventListener('hashchange', onChange);
  }, []);

  return route;
}

export function navigate(route: string): void {
  window.location.hash = route;
}
