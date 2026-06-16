/** 当前 APK 版本（构建时注入） */
export function getAppVersion(): string {
  return String(import.meta.env.VITE_APP_VERSION || '1.0.0');
}

export function parseVersion(version: string): number[] {
  return version
    .replace(/^v/i, '')
    .split('.')
    .map((part) => parseInt(part.replace(/[^\d].*$/, ''), 10) || 0);
}

export function isNewerVersion(remote: string, current: string): boolean {
  const r = parseVersion(remote);
  const c = parseVersion(current);
  const len = Math.max(r.length, c.length);
  for (let i = 0; i < len; i += 1) {
    const rv = r[i] ?? 0;
    const cv = c[i] ?? 0;
    if (rv > cv) return true;
    if (rv < cv) return false;
  }
  return false;
}

export function isNewerByCode(remoteCode: number | undefined, currentCode: number | undefined): boolean {
  if (remoteCode == null || currentCode == null) return false;
  return remoteCode > currentCode;
}
