import api from './client';
import type { ApiResponse, MessageItem, PageResult } from '@/types';

export async function fetchMessages(page = 1, pageSize = 20) {
  const { data } = await api.get<ApiResponse<PageResult<MessageItem>>>('/messages', {
    params: { page, pageSize },
  });
  return data.data;
}

export async function fetchUnreadCount() {
  const { data } = await api.get<ApiResponse<{ count: number }>>('/messages/unread-count');
  return data.data.count;
}

export async function sendMessage(payload: {
  title: string;
  content?: string;
  messageType?: string;
  taskId?: number;
  targetGroupIds: number[];
}) {
  const { data } = await api.post<ApiResponse<MessageItem>>('/messages', payload);
  return data.data;
}

export async function markMessageRead(messageId: number) {
  await api.post(`/messages/${messageId}/read`);
}

export async function markAllMessagesRead() {
  await api.post('/messages/read-all');
}
