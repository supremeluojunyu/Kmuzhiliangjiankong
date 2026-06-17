import { getDefaultHomePath } from '@/utils/app';

/** 登录后回跳地址，仅允许站内相对路径，防止开放重定向 */
export function getSafeRedirectPath(raw: string | null | undefined): string | null {
  if (!raw) return null;
  let path = raw.trim();
  if (!path.startsWith('/') || path.startsWith('//')) {
    return null;
  }
  if (path.startsWith('/login')) {
    return null;
  }
  if (path === '/') {
    return getDefaultHomePath();
  }
  return path;
}

export function buildLoginUrl(fromPath?: string): string {
  if (!fromPath || fromPath.startsWith('/login')) {
    return '/login';
  }
  const safe = getSafeRedirectPath(fromPath);
  if (!safe) return '/login';
  return `/login?redirect=${encodeURIComponent(safe)}`;
}
