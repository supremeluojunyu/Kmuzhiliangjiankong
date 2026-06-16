import api from './client';
import type { ApiResponse, PageResult } from '@/types';

export interface OperationLogItem {
  id: number;
  userId: number;
  userName: string;
  groupId: number;
  action: string;
  targetType: string;
  targetId: number;
  detailJson: string;
  ip: string;
  createdAt: string;
}

export async function fetchOperationLogs(page = 1, pageSize = 20, action?: string) {
  const { data } = await api.get<ApiResponse<PageResult<OperationLogItem>>>('/logs', {
    params: { page, pageSize, action },
  });
  return data.data;
}
