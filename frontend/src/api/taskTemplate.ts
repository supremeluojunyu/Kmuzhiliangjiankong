import api from './client';
import type { ApiResponse, TaskFlowConfig, TaskItem } from '@/types';

export interface TaskTemplateItem {
  templateId: number;
  templateName: string;
  description?: string;
  flowConfig?: TaskFlowConfig;
  creatorId?: number;
  creatorName?: string;
  nodeCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export async function fetchTemplates() {
  const { data } = await api.get<ApiResponse<TaskTemplateItem[]>>('/task/templates');
  return data.data;
}

export async function fetchTemplate(templateId: number) {
  const { data } = await api.get<ApiResponse<TaskTemplateItem>>(`/task/templates/${templateId}`);
  return data.data;
}

export async function saveTemplate(payload: {
  templateName: string;
  description?: string;
  flowConfig: TaskFlowConfig;
}) {
  const { data } = await api.post<ApiResponse<TaskTemplateItem>>('/task/templates', payload);
  return data.data;
}

export async function saveTemplateFromTask(
  taskId: number,
  payload: { templateName: string; description?: string }
) {
  const { data } = await api.post<ApiResponse<TaskTemplateItem>>(
    `/task/templates/from-task/${taskId}`,
    payload
  );
  return data.data;
}

export async function createTaskFromTemplate(
  templateId: number,
  payload?: { taskName?: string; description?: string }
) {
  const { data } = await api.post<ApiResponse<TaskItem>>(
    `/task/templates/${templateId}/create-task`,
    payload ?? {}
  );
  return data.data;
}

export async function deleteTemplate(templateId: number) {
  await api.delete(`/task/templates/${templateId}`);
}
