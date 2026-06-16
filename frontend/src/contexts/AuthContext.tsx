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
import { clearToken, getToken, setStoredGroupId, setToken } from '@/api/client';
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

  const fetchGroups = useCallback(async () => {
    const list = await authApi.fetchGroups();
    setGroups(list);
  }, []);

  useEffect(() => {
    const init = async () => {
      if (!getToken()) {
        setLoading(false);
        return;
      }
      try {
        await refreshProfile();
      } catch {
        clearToken();
      } finally {
        setLoading(false);
      }
    };
    init();
  }, [refreshProfile]);

  const login = useCallback(async (account: string, password: string) => {
    const res = await authApi.login(account, password);
    setToken(res.token);
    setStoredGroupId(res.currentGroupId);
    setUser(res);
    setGroups(res.groups);
  }, []);

  const logout = useCallback(() => {
    clearToken();
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
    }),
    [user, loading, login, logout, groups, setCurrentGroupId, fetchGroups, hasPermission, canViewStats, isCollegeScoped, refreshProfile]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
