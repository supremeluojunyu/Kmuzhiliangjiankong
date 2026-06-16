import api from './client';
import type { ApiResponse } from '@/types';

export interface GroupEntity {
  groupId: number;
  groupName: string;
}

export async function fetchAllGroups() {
  const { data } = await api.get<ApiResponse<GroupEntity[]>>('/groups');
  return data.data;
}
