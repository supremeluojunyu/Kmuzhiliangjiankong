import { PlusOutlined } from '@ant-design/icons';
import { Button, Space, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AllocateModal from '@/components/AllocateModal';
import { fetchTasks, publishTask } from '@/api/task';
import { useAuth } from '@/contexts/AuthContext';
import type { TaskItem } from '@/types';

const statusColor: Record<string, string> = {
  draft: 'default',
  published: 'blue',
  in_progress: 'processing',
  closed: 'success',
};

const statusLabel: Record<string, string> = {
  draft: '草稿',
  published: '已发布',
  in_progress: '进行中',
  closed: '已结束',
};

export default function TasksPage() {
  const { hasPermission, canViewStats } = useAuth();
  const navigate = useNavigate();
  const [list, setList] = useState<TaskItem[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [allocateTaskId, setAllocateTaskId] = useState<number | null>(null);

  const load = async (p = page) => {
    setLoading(true);
    try {
      const res = await fetchTasks(p);
      setList(res.list);
      setTotal(res.total);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load(page);
  }, [page]);

  const handlePublish = async (taskId: number) => {
    try {
      await publishTask(taskId);
      message.success('发布成功');
      load(page);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '发布失败');
    }
  };

  const columns: ColumnsType<TaskItem> = [
    { title: '任务名称', dataIndex: 'taskName' },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (s: string) => <Tag color={statusColor[s]}>{statusLabel[s] || s}</Tag>,
    },
    { title: '节点数', width: 80, render: (_, r) => r.flowConfig?.nodes?.length ?? 0 },
    { title: '创建人', dataIndex: 'creatorName', width: 100 },
    { title: '创建时间', dataIndex: 'createdAt', width: 180 },
    {
      title: '操作',
      width: 260,
      render: (_, r) => (
        <Space wrap>
          <Button type="link" onClick={() => navigate(`/tasks/${r.taskId}`)}>
            {r.status === 'draft' ? '编辑' : '查看'}
          </Button>
          {r.status === 'draft' && hasPermission('task:create') && (
            <Button type="link" onClick={() => handlePublish(r.taskId)}>
              发布
            </Button>
          )}
          {['published', 'in_progress'].includes(r.status) && hasPermission('task:allocate') && (
            <Button type="link" onClick={() => setAllocateTaskId(r.taskId)}>
              分配
            </Button>
          )}
          {['published', 'in_progress', 'closed'].includes(r.status) && canViewStats() && (
            <Button type="link" onClick={() => navigate(`/tasks/${r.taskId}/stats`)}>
              统计
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Space style={{ marginBottom: 16, width: '100%', justifyContent: 'space-between' }}>
        <Typography.Title level={4} style={{ margin: 0 }}>
          任务管理
        </Typography.Title>
        {hasPermission('task:create') && (
          <Space>
            <Button onClick={() => navigate('/tasks/templates')}>任务模板</Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/tasks/new')}>
              创建任务
            </Button>
          </Space>
        )}
      </Space>
      <Table
        rowKey="taskId"
        loading={loading}
        columns={columns}
        dataSource={list}
        pagination={{ current: page, total, pageSize: 20, onChange: setPage }}
      />
      {allocateTaskId && (
        <AllocateModal
          open={!!allocateTaskId}
          taskId={allocateTaskId}
          onClose={() => setAllocateTaskId(null)}
          onSuccess={() => load(page)}
        />
      )}
    </div>
  );
}
