import api from './client';
import type { ApiResponse } from '@/types';

export interface ImportResult {
  successCount: number;
  skipCount: number;
  failCount: number;
  messages?: string[];
}

function triggerBlobDownload(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = fileName;
  a.click();
  URL.revokeObjectURL(url);
}

export async function downloadImportTemplate(path: string, fileName: string) {
  const { data } = await api.get<Blob>(path, { responseType: 'blob' });
  triggerBlobDownload(data, fileName);
}

export async function uploadImportFile(path: string, file: File): Promise<ImportResult> {
  const form = new FormData();
  form.append('file', file);
  const { data } = await api.post<ApiResponse<ImportResult>>(path, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return data.data;
}
