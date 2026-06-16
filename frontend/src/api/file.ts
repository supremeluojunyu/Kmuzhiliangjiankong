import api from './client';
import type { ApiResponse } from '@/types';

export interface UploadedFile {
  fileName: string;
  filePath: string;
  fileSize: number;
  url: string;
}

export async function uploadFile(file: File) {
  const form = new FormData();
  form.append('file', file);
  const { data } = await api.post<ApiResponse<UploadedFile>>('/files/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return data.data;
}

export function fileDownloadUrl(filePath: string) {
  return `/api/files/${filePath}`;
}
