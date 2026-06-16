import axios from 'axios';
import type { ApiResponse } from '@/types';
import { capacitorHttpAdapter } from '@/api/capacitorHttpAdapter';
import { isMobileApp } from '@/utils/app';
import { formatApiError, getApiPrefix } from '@/utils/serverConfig';

const TOKEN_KEY = 'uqm_token';
const GROUP_KEY = 'uqm_current_group_id';

export const getToken = () => localStorage.getItem(TOKEN_KEY);
export const setToken = (token: string) => localStorage.setItem(TOKEN_KEY, token);
export const clearToken = () => localStorage.removeItem(TOKEN_KEY);

export const getStoredGroupId = () => {
  const v = localStorage.getItem(GROUP_KEY);
  return v ? Number(v) : null;
};
export const setStoredGroupId = (groupId: number) =>
  localStorage.setItem(GROUP_KEY, String(groupId));

const api = axios.create({
  timeout: 30000,
  ...(isMobileApp() ? { adapter: capacitorHttpAdapter } : {}),
});

api.interceptors.request.use((config) => {
  config.baseURL = getApiPrefix();
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  const groupId = getStoredGroupId();
  if (groupId) {
    config.headers['X-Current-Group-Id'] = String(groupId);
  }
  return config;
});

api.interceptors.response.use(
  (res) => {
    const body = res.data as ApiResponse<unknown>;
    if (body.code !== 0) {
      return Promise.reject(new Error(body.message || '请求失败'));
    }
    return res;
  },
  (err) => {
    if (err.response?.status === 401) {
      clearToken();
      window.location.href = '/login';
      return Promise.reject(err);
    }
    const body = err.response?.data as ApiResponse<unknown> | undefined;
    const msg = body?.message || formatApiError(err);
    return Promise.reject(new Error(msg));
  }
);

export default api;
