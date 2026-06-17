import { Checkbox, Collapse, Empty, Spin, Typography } from 'antd';
import { useMemo } from 'react';
import type { MessageSendTargetGroup } from '@/api/message';

export interface MessageTargetSelection {
  targetGroupIds: number[];
  targetUserIds: number[];
}

interface MessageTargetPickerProps {
  groups: MessageSendTargetGroup[];
  loading?: boolean;
  value?: MessageTargetSelection;
  onChange?: (value: MessageTargetSelection) => void;
}

function emptySelection(): MessageTargetSelection {
  return { targetGroupIds: [], targetUserIds: [] };
}

export default function MessageTargetPicker({
  groups,
  loading,
  value,
  onChange,
}: MessageTargetPickerProps) {
  const selection = value ?? emptySelection();
  const broadcastSet = useMemo(() => new Set(selection.targetGroupIds), [selection.targetGroupIds]);
  const userSet = useMemo(() => new Set(selection.targetUserIds), [selection.targetUserIds]);

  const emit = (next: MessageTargetSelection) => {
    onChange?.({
      targetGroupIds: [...new Set(next.targetGroupIds)],
      targetUserIds: [...new Set(next.targetUserIds)],
    });
  };

  const toggleGroupBroadcast = (groupId: number, checked: boolean) => {
    const nextGroups = new Set(selection.targetGroupIds);
    const nextUsers = new Set(selection.targetUserIds);
    const group = groups.find((g) => g.groupId === groupId);
    if (checked) {
      nextGroups.add(groupId);
      group?.users.forEach((u) => nextUsers.delete(u.userId));
    } else {
      nextGroups.delete(groupId);
    }
    emit({ targetGroupIds: [...nextGroups], targetUserIds: [...nextUsers] });
  };

  const toggleUser = (groupId: number, userId: number, checked: boolean) => {
    if (broadcastSet.has(groupId)) return;
    const nextUsers = new Set(selection.targetUserIds);
    if (checked) {
      nextUsers.add(userId);
    } else {
      nextUsers.delete(userId);
    }
    emit({ targetGroupIds: [...selection.targetGroupIds], targetUserIds: [...nextUsers] });
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: 24 }}>
        <Spin tip="加载接收对象…" />
      </div>
    );
  }

  if (groups.length === 0) {
    return <Empty description="暂无可选组或成员" />;
  }

  const defaultOpenKeys = groups.map((g) => String(g.groupId));

  return (
    <div className="message-target-picker">
      <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 8 }}>
        勾选「组内全员」则整组接收；展开组后勾选个人则仅该人接收（二者不可同时选）
      </Typography.Text>
      <Collapse
        defaultActiveKey={defaultOpenKeys}
        items={groups.map((group) => {
          const isBroadcast = broadcastSet.has(group.groupId);
          const selectedInGroup = group.users.filter((u) => userSet.has(u.userId));
          const summary =
            isBroadcast
              ? '组内全员'
              : selectedInGroup.length > 0
                ? `已选 ${selectedInGroup.length} 人`
                : '未选择';

          return {
            key: String(group.groupId),
            label: (
              <span>
                {group.groupName}
                <Typography.Text type="secondary" style={{ marginLeft: 8, fontSize: 12 }}>
                  {summary}
                </Typography.Text>
              </span>
            ),
            children: (
              <div className="message-target-group-panel">
                <Checkbox
                  checked={isBroadcast}
                  onChange={(e) => toggleGroupBroadcast(group.groupId, e.target.checked)}
                >
                  组内全员（{group.users.length} 人）
                </Checkbox>
                <div className="message-target-user-list">
                  {group.users.map((user) => (
                    <Checkbox
                      key={user.userId}
                      checked={isBroadcast || userSet.has(user.userId)}
                      disabled={isBroadcast}
                      onChange={(e) => toggleUser(group.groupId, user.userId, e.target.checked)}
                    >
                      {user.name}
                      <Typography.Text type="secondary" style={{ marginLeft: 6 }}>
                        {user.account}
                      </Typography.Text>
                    </Checkbox>
                  ))}
                </div>
              </div>
            ),
          };
        })}
      />
    </div>
  );
}

export function hasMessageTargetSelection(value?: MessageTargetSelection): boolean {
  if (!value) return false;
  return value.targetGroupIds.length > 0 || value.targetUserIds.length > 0;
}
