import api from './client';
import type { ApiResponse } from '@/types';

export interface CollegeManageItem {
  collegeId: number;
  collegeName: string;
  collegeCode?: string;
  status: number;
  createdAt?: string;
  userCount: number;
  deletable?: boolean;
}

export async function fetchCollegesManage() {
  const { data } = await api.get<ApiResponse<CollegeManageItem[]>>('/colleges/manage');
  return data.data;
}

export async function createCollege(payload: {
  collegeName: string;
  collegeCode?: string;
  status?: number;
}) {
  const { data } = await api.post<ApiResponse<CollegeManageItem>>('/colleges/manage', payload);
  return data.data;
}

export async function updateCollege(
  collegeId: number,
  payload: Partial<{ collegeName: string; collegeCode: string; status: number }>
) {
  const { data } = await api.put<ApiResponse<CollegeManageItem>>(
    `/colleges/manage/${collegeId}`,
    payload
  );
  return data.data;
}

export async function batchDeleteColleges(ids: number[], confirmPhrase: string) {
  const { data } = await api.post<ApiResponse<{ deletedCount: number; errors?: string[] }>>(
    '/colleges/manage/batch-delete',
    { ids, confirmPhrase }
  );
  return data.data;
}
