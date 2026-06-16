import { CapacitorHttp } from '@capacitor/core';
import { FileOpener } from '@capacitor-community/file-opener';
import { Directory, Filesystem } from '@capacitor/filesystem';
import { fetchPublicAppRelease, type PublicAppRelease } from '@/api/public';
import { getAppVersion, isNewerByCode, isNewerVersion } from '@/utils/appVersion';
import { isMobileApp } from '@/utils/app';

const DISMISS_KEY = 'uqm_update_dismiss';

export interface AppUpdateInfo {
  currentVersion: string;
  release: PublicAppRelease;
}

export function getDismissedVersion(): string | null {
  return localStorage.getItem(DISMISS_KEY);
}

export function dismissUpdate(version: string) {
  localStorage.setItem(DISMISS_KEY, version);
}

export function clearDismissedUpdate() {
  localStorage.removeItem(DISMISS_KEY);
}

export async function checkAppUpdate(): Promise<AppUpdateInfo | null> {
  if (!isMobileApp()) return null;

  const currentVersion = getAppVersion();
  const currentCode = parseInt(String(import.meta.env.VITE_APP_VERSION_CODE || '0'), 10) || undefined;

  let release: PublicAppRelease;
  try {
    release = await fetchPublicAppRelease();
  } catch {
    return null;
  }

  if (!release.enabled || !release.apkUrl || !release.version) {
    return null;
  }

  const dismissed = getDismissedVersion();
  if (dismissed === release.version) {
    return null;
  }

  const newerByName = isNewerVersion(release.version, currentVersion);
  const newerByCode = isNewerByCode(release.versionCode, currentCode);

  if (!newerByName && !newerByCode) {
    return null;
  }

  return { currentVersion, release };
}

export async function downloadAndInstallApk(
  apkUrl: string,
  onProgress?: (percent: number) => void
): Promise<void> {
  onProgress?.(5);

  const response = await CapacitorHttp.get({
    url: apkUrl,
    responseType: 'blob',
    connectTimeout: 120000,
    readTimeout: 300000,
  });

  if (response.status < 200 || response.status >= 300) {
    throw new Error(`下载失败 (HTTP ${response.status})`);
  }

  onProgress?.(60);

  const base64 =
    typeof response.data === 'string'
      ? response.data
      : typeof response.data === 'object'
        ? JSON.stringify(response.data)
        : String(response.data);

  const fileName = `uqmonitor-update-${Date.now()}.apk`;

  await Filesystem.writeFile({
    path: fileName,
    data: base64,
    directory: Directory.Cache,
  });

  onProgress?.(85);

  const { uri } = await Filesystem.getUri({
    path: fileName,
    directory: Directory.Cache,
  });

  onProgress?.(100);

  await FileOpener.open({
    filePath: uri,
    contentType: 'application/vnd.android.package-archive',
  });
}
