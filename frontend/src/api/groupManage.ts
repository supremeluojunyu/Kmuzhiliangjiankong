import api from './client';
import type { ApiResponse } from '@/types';

export interface GroupManageItem {
  groupId: number;
  groupName: string;
  description?: string;
  parentGroupId?: number;
  createdAt: string;
  permissionIds: number[];
  permissionCodes: string[];
  memberCount: number;
}

export interface PermissionItem {
  permissionId: number;
  permissionCode: string;
  permissionName: string;
}

export async function fetchGroupsManage() {
  const { data } = await api.get<ApiResponse<GroupManageItem[]>>('/groups/manage');
  return data.data;
}

export async function fetchPermissions() {
  const { data } = await api.get<ApiResponse<PermissionItem[]>>('/groups/manage/permissions');
  return data.data;
}

export async function createGroup(payload: {
  groupName: string;
  description?: string;
  parentGroupId?: number;
  permissionIds?: number[];
}) {
  const { data } = await api.post<ApiResponse<GroupManageItem>>('/groups/manage', payload);
  return data.data;
}

export async function updateGroup(
  groupId: number,
  payload: Partial<{
    groupName: string;
    description: string;
    parentGroupId: number;
    permissionIds: number[];
  }>
) {
  const { data } = await api.put<ApiResponse<GroupManageItem>>(`/groups/manage/${groupId}`, payload);
  return data.data;
}

export async function deleteGroup(groupId: number) {
  await api.delete(`/groups/manage/${groupId}`);
}
