import { PlusOutlined } from '@ant-design/icons';
import { Button, Modal, Space, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AllocateModal from '@/components/AllocateModal';
import { fetchTasks, pauseTask, publishTask, resumeTask, stopTask } from '@/api/task';
import { useAuth } from '@/contexts/AuthContext';
import type { TaskItem } from '@/types';

const statusColor: Record<string, string> = {
  draft: 'default',
  published: 'blue',
  in_progress: 'processing',
  paused: 'warning',
  closed: 'success',
};

const statusLabel: Record<string, string> = {
  draft: '草稿',
  published: '已发布',
  in_progress: '进行中',
  paused: '已暂停',
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

  const handlePause = async (taskId: number) => {
    try {
      await pauseTask(taskId);
      message.success('任务已暂停，可点击「编辑」修改流程');
      load(page);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '暂停失败');
    }
  };

  const handleResume = async (taskId: number) => {
    try {
      await resumeTask(taskId);
      message.success('任务已恢复运行');
      load(page);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '恢复失败');
    }
  };

  const handleStop = (taskId: number) => {
    Modal.confirm({
      title: '确认停止任务？',
      content: '停止后所有进行中的实例将关闭，且不可再提交。',
      okType: 'danger',
      onOk: async () => {
        await stopTask(taskId);
        message.success('任务已停止');
        load(page);
      },
    });
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
      width: 320,
      render: (_, r) => (
        <Space wrap>
          {r.status === 'paused' ? (
            <Button type="link" onClick={() => navigate(`/tasks/${r.taskId}`)}>
              编辑
            </Button>
          ) : (
            <Button type="link" onClick={() => navigate(`/tasks/${r.taskId}`)}>
              {r.status === 'draft' ? '编辑' : '查看'}
            </Button>
          )}
          {r.status === 'draft' && hasPermission('task:create') && (
            <Button type="link" onClick={() => handlePublish(r.taskId)}>
              发布
            </Button>
          )}
          {['published', 'in_progress'].includes(r.status) && hasPermission('task:config') && (
            <>
              <Button type="link" onClick={() => handlePause(r.taskId)}>
                暂停
              </Button>
              <Button type="link" danger onClick={() => handleStop(r.taskId)}>
                停止
              </Button>
            </>
          )}
          {r.status === 'paused' && hasPermission('task:config') && (
            <>
              <Button type="link" onClick={() => handleResume(r.taskId)}>
                恢复
              </Button>
              <Button type="link" danger onClick={() => handleStop(r.taskId)}>
                停止
              </Button>
            </>
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
