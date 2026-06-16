import api from './client';
import type { ApiResponse, College, GroupInfo, LoginResponse, UserProfile } from '@/types';

export async function login(account: string, password: string) {
  const { data } = await api.post<ApiResponse<LoginResponse>>('/auth/login', {
    account,
    password,
  });
  return data.data;
}

export async function fetchGroups() {
  const { data } = await api.get<ApiResponse<GroupInfo[]>>('/user/groups');
  return data.data;
}

export async function switchGroup(groupId: number) {
  const { data } = await api.post<ApiResponse<UserProfile>>('/user/switch-group', {
    groupId,
  });
  return data.data;
}

export async function fetchProfile() {
  const { data } = await api.get<ApiResponse<UserProfile>>('/user/profile');
  return data.data;
}

export async function fetchColleges() {
  const { data } = await api.get<ApiResponse<College[]>>('/colleges');
  return data.data;
}
