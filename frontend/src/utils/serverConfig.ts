import axios from 'axios';
import { Capacitor } from '@capacitor/core';
import type { ApiResponse } from '@/types';
import { nativeHttpRequest } from '@/api/capacitorHttpAdapter';
import { isMobileApp } from '@/utils/app';

/** 默认公网服务器（APK 首次启动写入，不界面明文展示） */
export const DEFAULT_SERVER_URL = (
  String(import.meta.env.VITE_API_BASE_URL || '') || 'http://124.220.4.69:5555'
).replace(/\/$/, '');

const LEGACY_KEY = 'uqm_api_base';
const STORAGE_KEY = 'uqm_srv_cfg';
const INIT_KEY = 'uqm_srv_inited';

function obfuscate(value: string): string {
  const bytes = Array.from(value).map((ch, i) => ch.charCodeAt(0) ^ (0x5a + (i % 23)));
  return btoa(String.fromCharCode(...bytes));
}

function deobfuscate(encoded: string): string {
  try {
    const bytes = Array.from(atob(encoded)).map((ch, i) => ch.charCodeAt(0) ^ (0x5a + (i % 23)));
    return String.fromCharCode(...bytes).replace(/\/$/, '');
  } catch {
    return '';
  }
}

function isValidServerUrl(url: string): boolean {
  return /^https?:\/\/.+/i.test(url);
}

function readCustomServer(): string {
  const stored = localStorage.getItem(STORAGE_KEY);
  if (stored) {
    const decoded = deobfuscate(stored);
    if (decoded && isValidServerUrl(decoded)) return decoded;
    localStorage.removeItem(STORAGE_KEY);
  }
  const legacy = localStorage.getItem(LEGACY_KEY);
  if (legacy && isValidServerUrl(legacy)) {
    setCustomServer(legacy);
    localStorage.removeItem(LEGACY_KEY);
    return legacy.replace(/\/$/, '');
  }
  return '';
}

export function hasCustomServer(): boolean {
  return !!localStorage.getItem(STORAGE_KEY);
}

/** APP 启动时写入默认服务器（混淆存储），避免首次登录连不上 */
export function ensureDefaultServerConfig(): void {
  const mobile =
    import.meta.env.VITE_APP_MODE === 'mobile' || Capacitor.isNativePlatform();
  if (!mobile || !DEFAULT_SERVER_URL) return;

  const existing = readCustomServer();
  if (existing) return;

  setCustomServer(DEFAULT_SERVER_URL);
  localStorage.setItem(INIT_KEY, '1');
}

export function getApiBaseUrl(): string {
  const custom = readCustomServer();
  if (custom) return custom;
  return DEFAULT_SERVER_URL;
}

export function setCustomServer(url: string) {
  let normalized = url.trim().replace(/\/$/, '');
  normalized = normalized.replace(/\/api\/?$/i, '');
  if (!isValidServerUrl(normalized)) {
    throw new Error('无效的服务器地址');
  }
  localStorage.setItem(STORAGE_KEY, obfuscate(normalized));
}

export function clearCustomServer() {
  localStorage.removeItem(STORAGE_KEY);
  localStorage.removeItem(INIT_KEY);
  if (DEFAULT_SERVER_URL) {
    setCustomServer(DEFAULT_SERVER_URL);
  }
}

export function getApiPrefix(): string {
  const base = getApiBaseUrl();
  return base ? `${base}/api` : '/api';
}

export function resolveAssetUrl(path?: string | null): string | undefined {
  if (!path) return undefined;
  if (path.startsWith('http://') || path.startsWith('https://') || path.startsWith('data:')) {
    return path;
  }
  const base = getApiBaseUrl();
  if (path.startsWith('/')) return base ? `${base}${path}` : path;
  return path;
}

export async function testServerConnection(serverUrl: string): Promise<void> {
  let base = serverUrl.trim().replace(/\/$/, '');
  base = base.replace(/\/api\/?$/i, '');
  if (!isValidServerUrl(base)) {
    throw new Error('请输入有效的 http 或 https 地址');
  }
  const url = `${base}/api/health`;
  if (isMobileApp()) {
    const data = await nativeHttpRequest<ApiResponse<unknown>>({ url, method: 'GET', timeout: 12000 });
    if (data?.code !== 0) {
      throw new Error(data?.message || '服务器响应异常');
    }
    return;
  }
  const { data } = await axios.get<ApiResponse<unknown>>(url, { timeout: 12000 });
  if (data?.code !== 0) {
    throw new Error(data?.message || '服务器响应异常');
  }
}

export function isNetworkError(err: unknown): boolean {
  if (axios.isAxiosError(err)) {
    return !err.response && !!err.request;
  }
  const msg = err instanceof Error ? err.message : String(err);
  return (
    msg === 'Network Error' ||
    msg.includes('无法连接') ||
    msg.includes('timeout') ||
    msg.includes('Timeout') ||
    msg.includes('Failed to connect') ||
    msg.includes('Unable to resolve host')
  );
}

export function formatApiError(err: unknown): string {
  const msg = err instanceof Error ? err.message : String(err);
  if (msg.includes('is not a function')) {
    return '客户端请求异常，请更新到最新版 APP';
  }
  if (isNetworkError(err)) {
    return '无法连接服务器，请检查网络后重试';
  }
  if (axios.isAxiosError(err)) {
    const body = err.response?.data as { message?: string } | undefined;
    if (body?.message) return body.message;
  }
  if (err instanceof Error && err.message) return err.message;
  return '请求失败';
}
