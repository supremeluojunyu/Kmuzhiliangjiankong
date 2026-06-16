import axios from 'axios';
import type { ApiResponse } from '@/types';
import { capacitorHttpAdapter } from '@/api/capacitorHttpAdapter';
import { isMobileApp } from '@/utils/app';
import { getApiPrefix } from '@/utils/serverConfig';

export interface PublicBranding {
  siteName: string;
  siteShortName: string;
  siteSubtitle?: string;
  logoUrl?: string;
  faviconUrl?: string;
  primaryColor?: string;
  loginBackground?: string;
  downloadPageTitle?: string;
  downloadPageDescription?: string;
  defaultServerUrl?: string;
}

export interface PublicAppRelease {
  version?: string;
  versionCode?: number;
  apkUrl?: string;
  releaseNotes?: string;
  publishedAt?: string;
  minAndroidVersion?: string;
  enabled?: boolean;
}

const publicClient = axios.create({
  timeout: 15000,
  ...(isMobileApp() ? { adapter: capacitorHttpAdapter } : {}),
});

async function publicGet<T>(path: string): Promise<T> {
  const { data } = await publicClient.get<ApiResponse<T>>(`${getApiPrefix()}${path}`);
  if (data.code !== 0) throw new Error(data.message || '请求失败');
  return data.data;
}

export async function fetchPublicBranding() {
  return publicGet<PublicBranding>('/public/branding');
}

export async function fetchPublicAppRelease() {
  return publicGet<PublicAppRelease>('/public/app-release');
}
