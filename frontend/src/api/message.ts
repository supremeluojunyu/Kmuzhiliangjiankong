import api from './client';
import type { ApiResponse, MessageItem, PageResult } from '@/types';

export interface MessageSendTargetUser {
  userId: number;
  name: string;
  account: string;
}

export interface MessageSendTargetGroup {
  groupId: number;
  groupName: string;
  users: MessageSendTargetUser[];
}

export type MessageDirection = 'all' | 'received' | 'sent';

export async function fetchMessages(page = 1, pageSize = 20, direction: MessageDirection = 'all') {
  const { data } = await api.get<ApiResponse<PageResult<MessageItem>>>('/messages', {
    params: { page, pageSize, direction },
  });
  return data.data;
}

export async function fetchUnreadCount() {
  const { data } = await api.get<ApiResponse<{ count: number }>>('/messages/unread-count');
  return data.data.count;
}

export async function fetchSendTargets() {
  const { data } = await api.get<ApiResponse<MessageSendTargetGroup[]>>('/messages/send-targets');
  return data.data;
}

export async function sendMessage(payload: {
  title: string;
  content?: string;
  messageType?: string;
  taskId?: number;
  targetGroupIds?: number[];
  targetUserIds?: number[];
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

export async function deleteMessage(messageId: number) {
  await api.delete(`/messages/${messageId}`);
}

export function canDeleteMessage(
  hasPermission: (code: string) => boolean,
  currentUserId: number | undefined,
  message: { senderId?: number; sentByMe?: boolean },
): boolean {
  if (hasPermission('user:manage') || hasPermission('group:manage') || hasPermission('system:config')) {
    return true;
  }
  return hasPermission('message:send')
    && !!currentUserId
    && (message.sentByMe || message.senderId === currentUserId);
}

/** 树形选择值：g-{groupId} 整组，u-{userId} 个人 */
export function parseSendTargets(values: string[]): {
  targetGroupIds: number[];
  targetUserIds: number[];
} {
  const targetGroupIds: number[] = [];
  const targetUserIds: number[] = [];
  for (const v of values) {
    if (v.startsWith('g-')) {
      targetGroupIds.push(Number(v.slice(2)));
    } else if (v.startsWith('u-')) {
      targetUserIds.push(Number(v.slice(2)));
    }
  }
  return { targetGroupIds, targetUserIds };
}

export function buildSendTargetTree(groups: MessageSendTargetGroup[]) {
  return groups.map((g) => ({
    title: g.groupName,
    value: `g-${g.groupId}`,
    key: `g-${g.groupId}`,
    children: g.users.map((u) => ({
      title: `${u.name}（${u.account}）`,
      value: `u-${u.userId}`,
      key: `u-${g.groupId}-${u.userId}`,
    })),
  }));
}

export function describeSendTargets(
  groups: MessageSendTargetGroup[],
  targetGroupIds: number[],
  targetUserIds: number[],
): string {
  const parts: string[] = [];
  for (const gid of targetGroupIds) {
    const g = groups.find((x) => x.groupId === gid);
    parts.push(g ? `组「${g.groupName}」全员` : `组 #${gid}`);
  }
  for (const uid of targetUserIds) {
    for (const g of groups) {
      const u = g.users.find((x) => x.userId === uid);
      if (u) {
        parts.push(u.name);
        break;
      }
    }
  }
  return parts.join('、') || '所选对象';
}
