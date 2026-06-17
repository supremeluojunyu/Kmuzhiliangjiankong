import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import { Button, Modal, Space, Table, Tabs, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { Key } from 'react';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { batchDeleteTasks, fetchTasks, pauseTask, publishTask, resumeTask, stopTask } from '@/api/task';
import AllocateModal from '@/components/AllocateModal';
import ConfirmDeleteModal from '@/components/ConfirmDeleteModal';
import { DELETE_CONFIRM_PHRASE } from '@/constants/deleteConfirm';
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

function showDeleteResult(result: { deletedCount: number; errors?: string[] }) {
  if (result.deletedCount > 0) {
    message.success(`已删除 ${result.deletedCount} 个任务`);
  }
  if (result.errors?.length) {
    Modal.warning({
      title: '部分任务删除失败',
      content: (
        <div style={{ whiteSpace: 'pre-wrap' }}>{result.errors.join('\n')}</div>
      ),
    });
  }
}

export default function TasksPage() {
  const { hasPermission, canViewStats, user } = useAuth();
  const navigate = useNavigate();
  const [list, setList] = useState<TaskItem[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [loading, setLoading] = useState(false);
  const [allocateTaskId, setAllocateTaskId] = useState<number | null>(null);
  const [selectedRowKeys, setSelectedRowKeys] = useState<Key[]>([]);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const canAccess = hasPermission('task:create') || hasPermission('task:config');
  const canDelete = hasPermission('task:config');

  const load = async (p = page, status = statusFilter) => {
    setLoading(true);
    try {
      const res = await fetchTasks(p, 20, status === 'all' ? undefined : status);
      setList(res.list);
      setTotal(res.total);
      setSelectedRowKeys([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (canAccess) {
      load(page, statusFilter);
    }
  }, [page, statusFilter, canAccess]);

  const handlePublish = async (taskId: number) => {
    try {
      await publishTask(taskId);
      message.success('发布成功');
      load(page, statusFilter);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '发布失败');
    }
  };

  const handlePause = async (taskId: number) => {
    try {
      await pauseTask(taskId);
      message.success('任务已暂停，可点击「编辑」修改流程');
      load(page, statusFilter);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '暂停失败');
    }
  };

  const handleResume = async (taskId: number) => {
    try {
      await resumeTask(taskId);
      message.success('任务已恢复运行');
      load(page, statusFilter);
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
        load(page, statusFilter);
      },
    });
  };

  const handleBatchDelete = async () => {
    setDeleting(true);
    try {
      const result = await batchDeleteTasks(
        selectedRowKeys.map(Number),
        DELETE_CONFIRM_PHRASE
      );
      setDeleteOpen(false);
      showDeleteResult(result);
      load(page, statusFilter);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '删除失败');
    } finally {
      setDeleting(false);
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
      width: 360,
      render: (_, r) => (
        <Space wrap>
          <Button type="link" onClick={() => navigate(`/tasks/${r.taskId}`)}>
            {r.editable ? '编辑' : '查看'}
          </Button>
          {r.status === 'draft' && r.creatorId === user?.userId && hasPermission('task:create') && (
            <Button type="link" onClick={() => handlePublish(r.taskId)}>
              发布
            </Button>
          )}
          {['published', 'in_progress'].includes(r.status) && r.canManage && (
            <>
              <Button type="link" onClick={() => handlePause(r.taskId)}>
                暂停
              </Button>
              <Button type="link" danger onClick={() => handleStop(r.taskId)}>
                停止
              </Button>
            </>
          )}
          {r.status === 'paused' && r.canManage && (
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

  if (!canAccess) {
    return <Typography.Text type="danger">无任务管理权限</Typography.Text>;
  }

  return (
    <div>
      <Space style={{ marginBottom: 16, width: '100%', justifyContent: 'space-between' }} wrap>
        <Typography.Title level={4} style={{ margin: 0 }}>
          任务管理
        </Typography.Title>
        <Space wrap>
          {canDelete && (
            <Button
              danger
              icon={<DeleteOutlined />}
              disabled={selectedRowKeys.length === 0}
              onClick={() => setDeleteOpen(true)}
            >
              删除{selectedRowKeys.length > 0 ? ` (${selectedRowKeys.length})` : ''}
            </Button>
          )}
          {hasPermission('task:create') && (
            <>
              <Button onClick={() => navigate('/tasks/templates')}>任务模板</Button>
              <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/tasks/new')}>
                创建任务
              </Button>
            </>
          )}
        </Space>
      </Space>
      <Tabs
        activeKey={statusFilter}
        onChange={(k) => { setStatusFilter(k); setPage(1); }}
        style={{ marginBottom: 16 }}
        items={[
          { key: 'all', label: '全部' },
          { key: 'draft', label: '草稿' },
          { key: 'published', label: '已发布' },
          { key: 'in_progress', label: '进行中' },
          { key: 'paused', label: '已暂停' },
          { key: 'closed', label: '已结束' },
        ]}
      />
      <Table
        rowKey="taskId"
        loading={loading}
        columns={columns}
        dataSource={list}
        rowSelection={canDelete ? {
          selectedRowKeys,
          onChange: setSelectedRowKeys,
          getCheckboxProps: (record) => ({
            disabled: record.deletable === false,
          }),
        } : undefined}
        pagination={{ current: page, total, pageSize: 20, onChange: setPage }}
      />
      <ConfirmDeleteModal
        open={deleteOpen}
        title="删除任务"
        description="将同时删除该任务下的所有实例、节点记录、分配记录及关联讨论。进行中的任务需先暂停或停止。"
        count={selectedRowKeys.length}
        loading={deleting}
        onCancel={() => setDeleteOpen(false)}
        onConfirm={handleBatchDelete}
      />
      {allocateTaskId && (
        <AllocateModal
          open={!!allocateTaskId}
          taskId={allocateTaskId}
          onClose={() => setAllocateTaskId(null)}
          onSuccess={() => load(page, statusFilter)}
        />
      )}
    </div>
  );
}
