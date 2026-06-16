import { ClockCircleOutlined } from '@ant-design/icons';
import { Button, Table, Tabs, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchMyTasks, type MyTaskItem } from '@/api/task';

const statusMap: Record<string, { color: string; text: string }> = {
  pending: { color: 'default', text: '待处理' },
  in_progress: { color: 'processing', text: '进行中' },
  completed: { color: 'success', text: '已完成' },
  overdue: { color: 'error', text: '已逾期' },
};

const nodeTypeLabel: Record<string, string> = {
  submit: '提交材料',
  view: '查看',
  score: '评分',
  approve: '审核',
};

export default function MyTasksPage() {
  const navigate = useNavigate();
  const [filter, setFilter] = useState<string>('pending');
  const [list, setList] = useState<MyTaskItem[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);

  const load = async (p = page, f = filter) => {
    setLoading(true);
    try {
      const res = await fetchMyTasks(p, 20, f === 'all' ? undefined : f);
      setList(res.list);
      setTotal(res.total);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load(page, filter);
  }, [page, filter]);

  const columns: ColumnsType<MyTaskItem> = [
    { title: '任务名称', dataIndex: 'taskName' },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (s: string) => {
        const m = statusMap[s] || { color: 'default', text: s };
        return <Tag color={m.color}>{m.text}</Tag>;
      },
    },
    {
      title: '当前节点',
      width: 160,
      render: (_, r) =>
        r.currentNodeName ? (
          <span>
            {r.currentNodeName}{' '}
            <Tag>{nodeTypeLabel[r.currentNodeType || ''] || r.currentNodeType}</Tag>
          </span>
        ) : (
          '-'
        ),
    },
    { title: '创建时间', dataIndex: 'createdAt', width: 180 },
    {
      title: '操作',
      width: 100,
      render: (_, r) => (
        <Button type="link" onClick={() => navigate(`/my-tasks/${r.instanceId}`)}>
          {r.status === 'completed' ? '查看' : '处理'}
        </Button>
      ),
    },
  ];

  return (
    <div>
      <Typography.Title level={4}>
        <ClockCircleOutlined /> 我的任务
      </Typography.Title>
      <Tabs
        activeKey={filter}
        onChange={(k) => { setFilter(k); setPage(1); }}
        items={[
          { key: 'pending', label: '待处理' },
          { key: 'completed', label: '已完成' },
          { key: 'overdue', label: '已逾期' },
          { key: 'all', label: '全部' },
        ]}
      />
      <Table
        rowKey="instanceId"
        loading={loading}
        columns={columns}
        dataSource={list}
        pagination={{ current: page, total, pageSize: 20, onChange: setPage }}
      />
    </div>
  );
}
