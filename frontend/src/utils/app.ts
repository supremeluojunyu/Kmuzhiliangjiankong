import { Capacitor } from '@capacitor/core';
import {
  getApiBaseUrl,
  getApiPrefix,
  resolveAssetUrl,
} from '@/utils/serverConfig';

export const isMobileApp = () =>
  import.meta.env.VITE_APP_MODE === 'mobile' || Capacitor.isNativePlatform();

export { getApiBaseUrl, getApiPrefix, resolveAssetUrl };

export function getDefaultHomePath(): string {
  return isMobileApp() ? '/my-tasks' : '/';
}

// 兼容旧引用
export { setCustomServer as setApiBaseUrl } from '@/utils/serverConfig';
