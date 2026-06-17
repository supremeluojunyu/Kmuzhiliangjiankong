import api from './client';
import type { ApiResponse, PageResult, TaskFlowConfig, TaskItem } from '@/types';

export interface AllocateRequest {
  taskId: number;
  allocationType: 'manual' | 'by_college' | 'random' | 'by_total';
  targetGroupId: number;
  collegeIds: number[];
  totalInstances?: number;
}

export interface AllocateResult {
  allocationId: number;
  taskId: number;
  allocationType: string;
  createdCount: number;
  instanceIds: number[];
}

export interface NodeRecordItem {
  id: number;
  nodeId: string;
  nodeName: string;
  nodeType: string;
  executeGroupId?: number;
  executeGroupName?: string;
  config?: Record<string, unknown>;
  status: string;
  submitData?: Record<string, unknown>;
  startTime?: string;
  endTime?: string;
  canOperate?: boolean;
}

export interface MyTaskItem {
  instanceId: number;
  taskId: number;
  taskName: string;
  status: string;
  currentNodeId?: string;
  currentNodeName?: string;
  currentNodeType?: string;
  createdAt: string;
  completedAt?: string;
  nodeRecords?: NodeRecordItem[];
  referenceMaterials?: NodeRecordItem[];
  fullView?: boolean;
}

export async function fetchTasks(page = 1, pageSize = 20, status?: string) {
  const { data } = await api.get<ApiResponse<PageResult<TaskItem>>>('/task/list', {
    params: { page, pageSize, status },
  });
  return data.data;
}

export async function fetchTask(taskId: number) {
  const { data } = await api.get<ApiResponse<TaskItem>>(`/task/${taskId}`);
  return data.data;
}

export async function createTask(payload: {
  taskName: string;
  description?: string;
  flowConfig?: TaskFlowConfig;
}) {
  const { data } = await api.post<ApiResponse<TaskItem>>('/task/create', payload);
  return data.data;
}

export async function updateTask(
  taskId: number,
  payload: { taskName: string; description?: string; flowConfig?: TaskFlowConfig }
) {
  const { data } = await api.put<ApiResponse<TaskItem>>(`/task/${taskId}`, payload);
  return data.data;
}

export async function publishTask(taskId: number) {
  const { data } = await api.post<ApiResponse<TaskItem>>(`/task/publish/${taskId}`);
  return data.data;
}

export async function pauseTask(taskId: number) {
  const { data } = await api.post<ApiResponse<TaskItem>>(`/task/${taskId}/pause`);
  return data.data;
}

export async function resumeTask(taskId: number) {
  const { data } = await api.post<ApiResponse<TaskItem>>(`/task/${taskId}/resume`);
  return data.data;
}

export async function stopTask(taskId: number) {
  const { data } = await api.post<ApiResponse<TaskItem>>(`/task/${taskId}/stop`);
  return data.data;
}

export async function batchDeleteTasks(ids: number[], confirmPhrase: string) {
  const { data } = await api.post<ApiResponse<{ deletedCount: number; errors?: string[] }>>(
    '/task/batch-delete',
    { ids, confirmPhrase }
  );
  return data.data;
}

export async function allocateTask(payload: AllocateRequest) {
  const { data } = await api.post<ApiResponse<AllocateResult>>('/task/allocate', payload);
  return data.data;
}

export async function fetchMyTasks(page = 1, pageSize = 20, filter?: string) {
  const { data } = await api.get<ApiResponse<PageResult<MyTaskItem>>>('/task/my-list', {
    params: { page, pageSize, filter },
  });
  return data.data;
}

export async function fetchInstance(instanceId: number) {
  const { data } = await api.get<ApiResponse<MyTaskItem>>(`/task/instance/${instanceId}`);
  return data.data;
}

export async function submitNode(
  instanceId: number,
  nodeId: string,
  submitData: Record<string, unknown>,
  draft = false
) {
  const { data } = await api.post<ApiResponse<MyTaskItem>>(
    `/task/submit/${instanceId}/${nodeId}`,
    { submitData, draft }
  );
  return data.data;
}

export interface TaskCommentItem {
  messageId: number;
  senderId?: number;
  senderName?: string;
  title: string;
  content: string;
  messageType: string;
  sendTime: string;
}

export async function fetchInstanceComments(instanceId: number) {
  const { data } = await api.get<ApiResponse<TaskCommentItem[]>>(`/task/instance/${instanceId}/comments`);
  return data.data;
}

export async function postInstanceComment(instanceId: number, content: string) {
  const { data } = await api.post<ApiResponse<TaskCommentItem>>(`/task/instance/${instanceId}/comments`, { content });
  return data.data;
}
