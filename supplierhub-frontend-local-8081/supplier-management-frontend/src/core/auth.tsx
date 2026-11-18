import { createContext, useContext, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { api } from './api';
import type { Role, SessionInfo } from './types';
interface AuthState {
  status: 'loading' | 'guest' | 'signedIn';
  current: SessionInfo | null;
  login: (login: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  can: (...roles: Role[]) => boolean;
}
const AuthContext = createContext<AuthState | null>(null);
export function AuthProvider({children}:{children:ReactNode}) {
  const [status,setStatus]=useState<AuthState['status']>('loading');
  const [current,setCurrent]=useState<SessionInfo | null>(null);
  useEffect(() => {
    let alive = true;
    api.onExpire(() => {if (alive) { setCurrent(null); setStatus('guest'); }});
    (async () => {
      try {
        if (!api.hasRefresh() || !(await api.refresh())) { if (alive) setStatus('guest'); return; }
        const me = await api.get<SessionInfo>('/auth/me');
        if (alive) {setCurrent(me);setStatus('signedIn');}
      } catch {api.clear();if (alive) setStatus('guest');}
    })();
    return () => {alive=false;api.onExpire(() => undefined);};
  },[]);
  const login = async (name: string, password: string) => {
    await api.login(name, password);
    try {
      const me = await api.get<SessionInfo>('/auth/me');setCurrent(me);setStatus('signedIn');
    } catch (err) {api.clear();throw err;}
  };
  const logout = async () => {try {await api.logout();} finally {setCurrent(null);setStatus('guest');}};
  const can = (...roles:Role[]) => Boolean(current && roles.some(role => current.roles.includes(role)));
  return <AuthContext.Provider value={{status,current,login,logout,can}}>{children}</AuthContext.Provider>;
}
export function useAuth() { const context = useContext(AuthContext); if (!context) throw new Error('AuthProvider missing'); return context; }
export const BUSINESS_ROLES: Role[] = ['PROCUREMENT_SPECIALIST','PROCUREMENT_MANAGER'];
