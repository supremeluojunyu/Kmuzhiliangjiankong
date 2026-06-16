import axios from 'axios';
import api, { getStoredGroupId, getToken } from './client';
import type { ApiResponse } from '@/types';
import { capacitorHttpAdapter } from '@/api/capacitorHttpAdapter';
import { isMobileApp } from '@/utils/app';
import { getApiPrefix } from '@/utils/serverConfig';

const rawClient = axios.create({
  ...(isMobileApp() ? { adapter: capacitorHttpAdapter } : {}),
});

export interface UploadedFile {
  fileName: string;
  filePath: string;
  fileSize: number;
  url: string;
}

export interface MaterialFileItem {
  name: string;
  path: string;
}

export async function uploadFile(file: File) {
  const form = new FormData();
  form.append('file', file);
  const { data } = await api.post<ApiResponse<UploadedFile>>('/files/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return data.data;
}

function authHeaders(): Record<string, string> {
  const headers: Record<string, string> = {};
  const token = getToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  const groupId = getStoredGroupId();
  if (groupId) {
    headers['X-Current-Group-Id'] = String(groupId);
  }
  return headers;
}

export async function fetchFileBlob(filePath: string): Promise<Blob> {
  const res = await rawClient.get(`${getApiPrefix()}/files/${encodeURIComponent(filePath)}`, {
    responseType: 'blob',
    headers: authHeaders(),
  });
  return res.data;
}

export async function downloadFile(filePath: string, fileName: string) {
  const blob = await fetchFileBlob(filePath);
  triggerBlobDownload(blob, fileName);
}

export async function batchDownloadFiles(files: MaterialFileItem[], zipName: string) {
  const res = await rawClient.post(
    `${getApiPrefix()}/files/batch-download`,
    { files, zipName },
    { responseType: 'blob', headers: authHeaders() }
  );
  triggerBlobDownload(res.data, zipName.endsWith('.zip') ? zipName : `${zipName}.zip`);
}

function triggerBlobDownload(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = fileName;
  a.click();
  URL.revokeObjectURL(url);
}

export function isPreviewableImage(fileName: string) {
  return /\.(jpe?g|png|gif|webp)$/i.test(fileName);
}

export function isPreviewablePdf(fileName: string) {
  return /\.pdf$/i.test(fileName);
}

export type PreviewKind = 'image' | 'pdf' | 'office' | 'none';

export function getPreviewKind(fileName: string): PreviewKind {
  if (isPreviewableImage(fileName)) return 'image';
  if (isPreviewablePdf(fileName)) return 'pdf';
  if (/\.(doc|docx|xls|xlsx|ppt|pptx)$/i.test(fileName)) return 'office';
  return 'none';
}

export function isPreviewable(fileName: string) {
  return getPreviewKind(fileName) !== 'none';
}

export async function fetchFilePreviewHtml(filePath: string): Promise<string> {
  const res = await rawClient.get(`${getApiPrefix()}/files/preview/${encodeURIComponent(filePath)}`, {
    responseType: 'text',
    headers: authHeaders(),
  });
  return res.data;
}
