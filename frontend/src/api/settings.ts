import api from './client';
import type { ApiResponse } from '@/types';

export interface AuthSettings {
  enabled: boolean;
  provider: 'local' | 'cas' | 'oauth2';
  localLoginEnabled: boolean;
  casServerUrl?: string;
  casLoginPath?: string;
  serviceUrl?: string;
  oauthIssuer?: string;
  oauthClientId?: string;
  oauthClientSecret?: string;
  oauthRedirectUri?: string;
  oauthScope?: string;
  autoProvision?: boolean;
  defaultGroupId?: number;
  frontendBaseUrl?: string;
}

export interface NotificationSettings {
  emailEnabled: boolean;
  smtpHost?: string;
  smtpPort?: number;
  smtpUsername?: string;
  smtpPassword?: string;
  smtpFrom?: string;
  smtpSsl?: boolean;
  wechatEnabled: boolean;
  wechatCorpId?: string;
  wechatAgentId?: string;
  wechatSecret?: string;
  notifyOnTaskPublish?: boolean;
  notifyOnMessageBroadcast?: boolean;
  notifyOnDeadline?: boolean;
  deadlineRemindDays?: number;
}

export interface RetentionSettings {
  enabled: boolean;
  taskDataYears?: number;
  messageDataYears?: number;
  logDataYears?: number;
  runHour?: number;
  lastRunAt?: string;
  lastRunSummary?: string;
}

export interface StorageSettings {
  type: 'local' | 's3';
  localPath?: string;
  s3Endpoint?: string;
  s3Region?: string;
  s3Bucket?: string;
  s3AccessKey?: string;
  s3SecretKey?: string;
  s3PathStyleAccess?: boolean;
}

export interface SystemSettings {
  auth: AuthSettings;
  notification: NotificationSettings;
  retention: RetentionSettings;
  storage: StorageSettings;
}

export interface PublicAuthConfig {
  externalAuthEnabled: boolean;
  provider?: string;
  localLoginEnabled: boolean;
  casLoginUrl?: string;
  oauthLoginUrl?: string;
}

export async function fetchPublicAuthConfig() {
  const { data } = await api.get<ApiResponse<PublicAuthConfig>>('/auth/config');
  return data.data;
}

export async function fetchSystemSettings() {
  const { data } = await api.get<ApiResponse<SystemSettings>>('/admin/settings');
  return data.data;
}

export async function updateSystemSettings(payload: SystemSettings) {
  const { data } = await api.put<ApiResponse<SystemSettings>>('/admin/settings', payload);
  return data.data;
}

export async function testNotification(channel: 'email' | 'wechat', target: string) {
  await api.post('/admin/settings/test-notification', { channel, target });
}

export async function runRetentionCleanup() {
  const { data } = await api.post<ApiResponse<string>>('/admin/settings/retention/run');
  return data.data;
}

export async function runDeadlineRemind() {
  const { data } = await api.post<ApiResponse<string>>('/admin/settings/deadline/remind');
  return data.data;
}

export async function runOverdueCheck() {
  const { data } = await api.post<ApiResponse<string>>('/admin/settings/deadline/overdue-check');
  return data.data;
}

export async function testStorageConnection() {
  const { data } = await api.post<ApiResponse<string>>('/admin/settings/storage/test');
  return data.data;
}
