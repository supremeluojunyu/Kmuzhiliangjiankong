import { Input, Table, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useState } from 'react';
import { fetchOperationLogs, type OperationLogItem } from '@/api/log';

export default function OperationLogsPage() {
  const [list, setList] = useState<OperationLogItem[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [action, setAction] = useState('');
  const [loading, setLoading] = useState(false);

  const load = async (p = page, act = action) => {
    setLoading(true);
    try {
      const res = await fetchOperationLogs(p, 20, act || undefined);
      setList(res.list);
      setTotal(res.total);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load(page, action);
  }, [page]);

  const columns: ColumnsType<OperationLogItem> = [
    { title: '时间', dataIndex: 'createdAt', width: 180 },
    { title: '操作人', dataIndex: 'userName', width: 100 },
    { title: '操作', dataIndex: 'action', width: 140 },
    { title: '目标类型', dataIndex: 'targetType', width: 80 },
    { title: '目标ID', dataIndex: 'targetId', width: 80 },
    { title: 'IP', dataIndex: 'ip', width: 120 },
    { title: '详情', dataIndex: 'detailJson', ellipsis: true },
  ];

  return (
    <div>
      <Typography.Title level={4}>操作日志</Typography.Title>
      <Input.Search
        placeholder="按操作类型筛选"
        style={{ width: 240, marginBottom: 16 }}
        onSearch={(v) => { setAction(v); setPage(1); load(1, v); }}
        allowClear
      />
      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={list}
        pagination={{ current: page, total, pageSize: 20, onChange: setPage }}
      />
    </div>
  );
}
