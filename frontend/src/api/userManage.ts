import api from './client';
import type { ApiResponse, PageResult } from '@/types';

export interface UserManageItem {
  userId: number;
  name: string;
  account: string;
  collegeId: number;
  collegeName?: string;
  status: number;
  createdAt: string;
  groups: { groupId: number; groupName: string }[];
  defaultGroupId?: number;
  email?: string;
  wechatUserId?: string;
}

export async function fetchUsers(page = 1, pageSize = 20, keyword?: string) {
  const { data } = await api.get<ApiResponse<PageResult<UserManageItem>>>('/users', {
    params: { page, pageSize, keyword },
  });
  return data.data;
}

export async function createUser(payload: {
  name: string;
  account: string;
  password?: string;
  collegeId: number;
  groupIds: number[];
  defaultGroupId?: number;
  email?: string;
  wechatUserId?: string;
}) {
  const { data } = await api.post<ApiResponse<UserManageItem>>('/users', payload);
  return data.data;
}

export async function updateUser(
  userId: number,
  payload: Partial<{
    name: string;
    collegeId: number;
    status: number;
    groupIds: number[];
    defaultGroupId: number;
    password: string;
    email?: string;
    wechatUserId?: string;
  }>
) {
  const { data } = await api.put<ApiResponse<UserManageItem>>(`/users/${userId}`, payload);
  return data.data;
}
