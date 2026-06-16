import api from './client';
import { nativeHttpRequest } from './capacitorHttpAdapter';
import type { ApiResponse, College, GroupInfo, LoginResponse, UserProfile } from '@/types';
import { isMobileApp } from '@/utils/app';
import { getApiPrefix } from '@/utils/serverConfig';

export async function login(account: string, password: string) {
  if (isMobileApp()) {
    const body = await nativeHttpRequest<ApiResponse<LoginResponse>>({
      url: `${getApiPrefix()}/auth/login`,
      method: 'POST',
      data: { account, password },
      timeout: 30000,
    });
    if (body.code !== 0) {
      throw new Error(body.message || '登录失败');
    }
    return body.data;
  }
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
