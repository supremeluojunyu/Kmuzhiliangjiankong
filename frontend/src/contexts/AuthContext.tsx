import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import * as authApi from '@/api/auth';
import { clearAuthStorage, getToken, setStoredGroupId, setToken } from '@/api/client';
import type { GroupInfo, UserProfile } from '@/types';

interface AuthContextValue {
  user: UserProfile | null;
  loading: boolean;
  login: (account: string, password: string) => Promise<void>;
  logout: () => void;
  groups: GroupInfo[];
  currentGroupId: number | null;
  setCurrentGroupId: (groupId: number) => Promise<boolean>;
  fetchGroups: () => Promise<void>;
  hasPermission: (code: string) => boolean;
  canViewStats: () => boolean;
  isCollegeScoped: () => boolean;
  refreshProfile: () => Promise<void>;
  /** 向后端校验 token，无效则清除本地登录态 */
  validateSession: () => Promise<boolean>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [groups, setGroups] = useState<GroupInfo[]>([]);
  const [loading, setLoading] = useState(true);

  const refreshProfile = useCallback(async () => {
    const profile = await authApi.fetchProfile();
    setUser(profile);
    setGroups(profile.groups);
    setStoredGroupId(profile.currentGroupId);
  }, []);

  const validateSession = useCallback(async (): Promise<boolean> => {
    if (!getToken()) {
      setUser(null);
      setGroups([]);
      return false;
    }
    try {
      await refreshProfile();
      return true;
    } catch {
      clearAuthStorage();
      setUser(null);
      setGroups([]);
      return false;
    }
  }, [refreshProfile]);

  const fetchGroups = useCallback(async () => {
    const list = await authApi.fetchGroups();
    setGroups(list);
  }, []);

  useEffect(() => {
    validateSession().finally(() => setLoading(false));
  }, [validateSession]);

  const login = useCallback(async (account: string, password: string) => {
    const res = await authApi.login(account, password);
    setToken(res.token);
    setStoredGroupId(res.currentGroupId);
    setUser(res);
    setGroups(res.groups);
  }, []);

  const logout = useCallback(() => {
    clearAuthStorage();
    setUser(null);
    setGroups([]);
  }, []);

  const setCurrentGroupId = useCallback(async (groupId: number) => {
    const profile = await authApi.switchGroup(groupId);
    setStoredGroupId(profile.currentGroupId);
    setUser(profile);
    setGroups(profile.groups);
    window.dispatchEvent(new Event('identity-changed'));
    return true;
  }, []);

  const hasPermission = useCallback(
    (code: string) => user?.permissions.includes(code) ?? false,
    [user]
  );

  const canViewStats = useCallback(
    () => hasPermission('stat:view_all') || hasPermission('stat:view_college'),
    [hasPermission]
  );

  const isCollegeScoped = useCallback(
    () => hasPermission('stat:view_college') && !hasPermission('stat:view_all'),
    [hasPermission]
  );

  const value = useMemo(
    () => ({
      user,
      loading,
      login,
      logout,
      groups,
      currentGroupId: user?.currentGroupId ?? null,
      setCurrentGroupId,
      fetchGroups,
      hasPermission,
      canViewStats,
      isCollegeScoped,
      refreshProfile,
      validateSession,
    }),
    [
      user,
      loading,
      login,
      logout,
      groups,
      setCurrentGroupId,
      fetchGroups,
      hasPermission,
      canViewStats,
      isCollegeScoped,
      refreshProfile,
      validateSession,
    ]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
