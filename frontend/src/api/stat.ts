import axios from 'axios';
import api, { getStoredGroupId, getToken } from './client';
import type { ApiResponse } from '@/types';

export interface NodeProgress {
  nodeId: string;
  nodeName: string;
  nodeType: string;
  completed: number;
  total: number;
  rate: number;
}

export interface CollegeProgress {
  collegeId: number;
  collegeName: string;
  total: number;
  completed: number;
  rate: number;
}

export interface TaskProgress {
  taskId: number;
  taskName: string;
  totalInstances: number;
  completedInstances: number;
  completionRate: number;
  nodeProgress: NodeProgress[];
  collegeProgress: CollegeProgress[];
  scope?: 'all' | 'college';
  scopeCollegeName?: string;
}

export interface ReviewItem {
  instanceId: number;
  userName: string;
  collegeName: string;
  score?: number | null;
  grade?: string | null;
  comment: string;
  nodeId: string;
  nodeName: string;
}

export interface ScoreSummary {
  average: number | null;
  max: number | null;
  min: number | null;
  totalScores: number;
  byCollege: { collegeName: string; average: number; max: number; min: number; count: number }[];
  reviews: ReviewItem[];
}

export async function fetchTaskProgress(taskId: number) {
  const { data } = await api.get<ApiResponse<TaskProgress>>(`/stat/task-progress/${taskId}`);
  return data.data;
}

export async function fetchScoreSummary(taskId: number, keyword?: string) {
  const { data } = await api.get<ApiResponse<ScoreSummary>>(`/stat/score-summary/${taskId}`, {
    params: { keyword },
  });
  return data.data;
}

export async function exportReviews(taskId: number, keyword?: string) {
  const res = await axios.get(`/api/stat/export-reviews/${taskId}`, {
    params: { keyword },
    responseType: 'blob',
    headers: {
      Authorization: `Bearer ${getToken()}`,
      'X-Current-Group-Id': String(getStoredGroupId() ?? ''),
    },
  });
  const url = URL.createObjectURL(res.data);
  const a = document.createElement('a');
  a.href = url;
  a.download = `评语汇总_任务${taskId}.xlsx`;
  a.click();
  URL.revokeObjectURL(url);
}
