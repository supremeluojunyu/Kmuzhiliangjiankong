export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export interface GroupInfo {
  groupId: number;
  groupName: string;
  isDefault: number;
  pendingCount: number;
}

export interface UserProfile {
  userId: number;
  name: string;
  account: string;
  collegeId?: number;
  collegeName?: string;
  currentGroupId: number;
  currentGroupName: string;
  permissions: string[];
  groups: GroupInfo[];
}

export interface LoginResponse extends UserProfile {
  token: string;
}

export interface College {
  collegeId: number;
  collegeName: string;
  collegeCode: string;
}

export interface PageResult<T> {
  list: T[];
  total: number;
  page: number;
  pageSize: number;
}

export interface MessageItem {
  messageId: number;
  senderId: number;
  senderName: string;
  title: string;
  content: string;
  messageType: string;
  taskId?: number;
  instanceId?: number;
  sendTime: string;
  isRead: boolean;
  targetGroupNames?: string[];
  targetUserNames?: string[];
}

export interface FlowNode {
  nodeId: string;
  nodeType: 'submit' | 'view' | 'score' | 'approve';
  nodeName?: string;
  executeGroupId: number;
  dependsOn: string[];
  executionMode?: 'sequential' | 'parallel' | 'any';
  timeLimitHours?: number;
  config?: Record<string, unknown>;
}

export interface TaskFlowConfig {
  taskId?: number;
  nodes: FlowNode[];
  globalTimeStart?: string;
  globalTimeEnd?: string;
}

export interface TaskItem {
  taskId: number;
  taskName: string;
  description?: string;
  flowConfig?: TaskFlowConfig;
  status: string;
  creatorId?: number;
  creatorName?: string;
  createdAt: string;
}

export interface UserGroup {
  groupId: number;
  groupName: string;
}
