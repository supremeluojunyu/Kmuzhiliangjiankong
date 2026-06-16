import { Badge, Select } from 'antd';
import { useEffect } from 'react';
import { useAuth } from '@/contexts/AuthContext';

export default function IdentitySwitcher() {
  const { currentGroupId, setCurrentGroupId, groups, fetchGroups } = useAuth();

  useEffect(() => {
    fetchGroups();
    const handler = () => fetchGroups();
    window.addEventListener('identity-changed', handler);
    return () => window.removeEventListener('identity-changed', handler);
  }, [fetchGroups]);

  const handleChange = async (groupId: number) => {
    await setCurrentGroupId(groupId);
  };

  return (
    <Select
      value={currentGroupId ?? undefined}
      onChange={handleChange}
      style={{ minWidth: 220 }}
      placeholder="选择身份"
      options={groups.map((g) => ({
        value: g.groupId,
        label: (
          <span>
            {g.groupName}
            {g.pendingCount > 0 && (
              <Badge count={g.pendingCount} offset={[12, 0]} size="small" />
            )}
          </span>
        ),
      }))}
    />
  );
}
