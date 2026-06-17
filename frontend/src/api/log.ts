import api from './client';
import type { ApiResponse, PageResult } from '@/types';

export interface OperationLogItem {
  id: number;
  userId: number;
  userName: string;
  groupId: number;
  groupName?: string;
  action: string;
  targetType: string;
  targetId: number;
  detailJson: string;
  ip: string;
  createdAt: string;
}

export interface OperationLogQuery {
  page?: number;
  pageSize?: number;
  action?: string;
  userName?: string;
  dateFrom?: string;
  dateTo?: string;
}

export const ACTION_LABELS: Record<string, string> = {
  'auth:login': '用户登录',
  'auth:external_login': '外部认证登录',
  'task:create': '创建任务',
  'task:update': '更新任务',
  'task:publish': '发布任务',
  'task:pause': '暂停任务',
  'task:resume': '恢复任务',
  'task:stop': '停止任务',
  'task:delete': '删除任务',
  'template:create': '创建模板',
  'template:from_task': '从任务保存模板',
  'template:use': '使用模板',
  'template:delete': '删除模板',
  'user:create': '创建用户',
  'user:update': '更新用户',
  'user:import': '导入用户',
  'user:delete': '删除用户',
  'group:create': '创建组',
  'group:update': '更新组',
  'group:delete': '删除组',
  'group:import': '导入组',
  'message:send': '发送消息',
  'message:delete': '删除消息',
};

export function actionLabel(action: string): string {
  return ACTION_LABELS[action] || action;
}

export async function fetchOperationLogs(params: OperationLogQuery = {}) {
  const { page = 1, pageSize = 20, action, userName, dateFrom, dateTo } = params;
  const { data } = await api.get<ApiResponse<PageResult<OperationLogItem>>>('/logs', {
    params: { page, pageSize, action, userName, dateFrom, dateTo },
  });
  return data.data;
}

export async function fetchLogActionTypes() {
  const { data } = await api.get<ApiResponse<string[]>>('/logs/actions');
  return data.data;
}

export function canViewOperationLogs(hasPermission: (code: string) => boolean): boolean {
  return hasPermission('user:manage')
    || hasPermission('group:manage')
    || hasPermission('system:config');
}
