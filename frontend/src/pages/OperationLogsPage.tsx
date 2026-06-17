import { FileTextOutlined } from '@ant-design/icons';
import { DatePicker, Input, Select, Space, Table, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { Dayjs } from 'dayjs';
import { useEffect, useState } from 'react';
import {
  actionLabel,
  canViewOperationLogs,
  fetchLogActionTypes,
  fetchOperationLogs,
  type OperationLogItem,
} from '@/api/log';
import { useAuth } from '@/contexts/AuthContext';

const { RangePicker } = DatePicker;

export default function OperationLogsPage() {
  const { hasPermission } = useAuth();
  const [list, setList] = useState<OperationLogItem[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [action, setAction] = useState<string | undefined>();
  const [userName, setUserName] = useState('');
  const [dateRange, setDateRange] = useState<[Dayjs | null, Dayjs | null] | null>(null);
  const [actionOptions, setActionOptions] = useState<{ value: string; label: string }[]>([]);
  const [loading, setLoading] = useState(false);

  const allowed = canViewOperationLogs(hasPermission);

  const load = async (p = page) => {
    setLoading(true);
    try {
      const res = await fetchOperationLogs({
        page: p,
        pageSize: 20,
        action,
        userName: userName.trim() || undefined,
        dateFrom: dateRange?.[0]?.startOf('day').format('YYYY-MM-DDTHH:mm:ss'),
        dateTo: dateRange?.[1]?.endOf('day').format('YYYY-MM-DDTHH:mm:ss'),
      });
      setList(res.list);
      setTotal(res.total);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!allowed) return;
    fetchLogActionTypes()
      .then((actions) => setActionOptions(actions.map((a) => ({ value: a, label: actionLabel(a) }))))
      .catch(() => {});
  }, [allowed]);

  useEffect(() => {
    if (allowed) {
      load(page);
    }
  }, [page, allowed]);

  const handleSearch = () => {
    setPage(1);
    load(1);
  };

  const columns: ColumnsType<OperationLogItem> = [
    { title: '时间', dataIndex: 'createdAt', width: 170 },
    { title: '操作人', dataIndex: 'userName', width: 100 },
    { title: '身份组', dataIndex: 'groupName', width: 120, render: (v) => v || '—' },
    {
      title: '操作',
      dataIndex: 'action',
      width: 130,
      render: (a: string) => actionLabel(a),
    },
    { title: '目标类型', dataIndex: 'targetType', width: 90 },
    { title: '目标ID', dataIndex: 'targetId', width: 80, render: (v) => v ?? '—' },
    {
      title: 'IP',
      dataIndex: 'ip',
      width: 140,
      render: (ip: string) => ip || '—',
    },
    { title: '详情', dataIndex: 'detailJson', ellipsis: true },
  ];

  if (!allowed) {
    return <Typography.Text type="danger">无权限查看操作日志</Typography.Text>;
  }

  return (
    <div>
      <Typography.Title level={4}>
        <FileTextOutlined /> 操作日志
      </Typography.Title>
      <Space wrap style={{ marginBottom: 16 }}>
        <Select
          allowClear
          placeholder="操作类型"
          style={{ width: 180 }}
          value={action}
          options={actionOptions}
          onChange={setAction}
        />
        <Input
          placeholder="操作人姓名"
          style={{ width: 140 }}
          value={userName}
          onChange={(e) => setUserName(e.target.value)}
          onPressEnter={handleSearch}
          allowClear
        />
        <RangePicker
          value={dateRange}
          onChange={(v) => setDateRange(v)}
          placeholder={['开始日期', '结束日期']}
        />
        <Input.Search
          placeholder="搜索"
          style={{ width: 100 }}
          onSearch={handleSearch}
          enterButton
        />
      </Space>
      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={list}
        pagination={{ current: page, total, pageSize: 20, onChange: setPage }}
        scroll={{ x: 960 }}
      />
    </div>
  );
}
