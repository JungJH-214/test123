import type { UserResponse } from '@eduhub/api-client';
import { createContext, ReactNode, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { Location, useLocation, useNavigate } from 'react-router-dom';
import { authApi, fetchMe } from '../api/clients';
import { tokenStore, UNAUTHORIZED_EVENT } from '../api/http';

export const LOGIN_PATH = '/login';

/** 로그인 화면으로 보낼 때 location.state에 담는 값. */
export interface LoginLocationState {
  from?: Location;
}

export interface AuthContextValue {
  /** 로그인한 사용자. 로그인하지 않았으면 null. */
  user: UserResponse | null;
  /** 새로고침 뒤 저장된 토큰을 /api/me로 확인하는 중이면 true. */
  initializing: boolean;
  /** 로그인 상태에서 401을 받아(토큰 만료 등) 로그아웃되었으면 true. 다시 로그인하면 false. */
  expired: boolean;
  login: (employeeNo: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

/** 로그인 상태 (NFR-03). BrowserRouter 안에 둔다. */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(() => (tokenStore.getToken() ? tokenStore.getUser() : null));
  const [initializing, setInitializing] = useState(() => tokenStore.getToken() !== null);
  const [expired, setExpired] = useState(false);

  const navigate = useNavigate();
  const location = useLocation();

  // 이벤트 핸들러에서 최신 값을 읽는다 (핸들러를 다시 등록하지 않으려고).
  const locationRef = useRef(location);
  locationRef.current = location;
  useEffect(() => {
    // http 인터셉터는 지금 쓰는 토큰으로 보낸 요청이 401일 때만 이 이벤트를 보낸다. 곧 로그인 상태가 만료된 것이다.
    const handleUnauthorized = () => {
      setExpired(true);
      setUser(null);
      // 이미 로그인 화면이면 그대로 둔다. 다시 보내면 원래 가려던 곳(state.from)이 로그인 화면으로 덮인다.
      const current = locationRef.current;
      if (current.pathname !== LOGIN_PATH) {
        const state: LoginLocationState = { from: current };
        navigate(LOGIN_PATH, { replace: true, state });
      }
    };
    window.addEventListener(UNAUTHORIZED_EVENT, handleUnauthorized);
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, handleUnauthorized);
  }, [navigate]);

  // 만료 안내는 로그인 화면에서만 의미가 있다. 로그인하지 않고 다른 화면으로 가면 지운다.
  useEffect(() => {
    if (location.pathname !== LOGIN_PATH) {
      setExpired(false);
    }
  }, [location.pathname]);

  // 새로고침 시 저장된 토큰이 아직 유효한지 서버에서 확인한다. 401이면 위 핸들러가 정리한다.
  useEffect(() => {
    const sentToken = tokenStore.getToken();
    if (!sentToken) {
      return;
    }
    let cancelled = false;
    fetchMe()
      .then((response) => {
        // 확인하는 사이 로그아웃하거나 다른 계정으로 로그인했으면 이 응답을 쓰지 않는다.
        if (!cancelled && tokenStore.getToken() === sentToken) {
          tokenStore.saveUser(response.data);
          setUser(response.data);
        }
      })
      .catch(() => {
        // 401은 인터셉터와 이벤트가 처리한다. 그 밖의 오류(네트워크 등)는 저장된 사용자로 계속한다.
      })
      .finally(() => {
        if (!cancelled) {
          setInitializing(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const login = useCallback(async (employeeNo: string, password: string) => {
    const response = await authApi.login({ loginRequest: { employeeNo, password } });
    const { accessToken, user: loggedIn } = response.data;
    // 생성 모델은 모든 필드가 선택이다. 백엔드는 항상 채워 보내므로 빠지면 계약 위반으로 본다.
    if (!accessToken || !loggedIn) {
      throw new Error('로그인 응답에 토큰이나 사용자 정보가 없습니다.');
    }
    tokenStore.save(accessToken, loggedIn);
    setUser(loggedIn);
    setExpired(false);
  }, []);

  const logout = useCallback(() => {
    tokenStore.clear();
    setUser(null);
    setExpired(false);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({ user, initializing, expired, login, logout }),
    [user, initializing, expired, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext);
  if (!value) {
    throw new Error('useAuth는 AuthProvider 안에서만 쓸 수 있습니다');
  }
  return value;
}
