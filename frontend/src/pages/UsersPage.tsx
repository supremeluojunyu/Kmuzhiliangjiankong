import { PlusOutlined } from '@ant-design/icons';
import {
  Button,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useState } from 'react';
import { fetchColleges } from '@/api/auth';
import { fetchAllGroups } from '@/api/group';
import { createUser, fetchUsers, updateUser, type UserManageItem } from '@/api/userManage';
import { useAuth } from '@/contexts/AuthContext';
import type { College } from '@/types';

export default function UsersPage() {
  const { isCollegeScoped } = useAuth();
  const [list, setList] = useState<UserManageItem[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<UserManageItem | null>(null);
  const [colleges, setColleges] = useState<College[]>([]);
  const [groups, setGroups] = useState<{ groupId: number; groupName: string }[]>([]);
  const [form] = Form.useForm();

  const load = async (p = page, kw = keyword) => {
    setLoading(true);
    try {
      const res = await fetchUsers(p, 20, kw || undefined);
      setList(res.list);
      setTotal(res.total);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load(page, keyword);
  }, [page]);

  useEffect(() => {
    fetchColleges().then(setColleges).catch(() => {});
    fetchAllGroups().then(setGroups).catch(() => {});
  }, []);

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({
      status: true,
      groupIds: [5],
      collegeId: colleges[0]?.collegeId ?? 1,
    });
    setOpen(true);
  };

  const openEdit = (record: UserManageItem) => {
    setEditing(record);
    form.setFieldsValue({
      name: record.name,
      account: record.account,
      collegeId: record.collegeId,
      status: record.status === 1,
      groupIds: record.groups.map((g) => g.groupId),
      defaultGroupId: record.defaultGroupId,
      email: record.email,
      wechatUserId: record.wechatUserId,
    });
    setOpen(true);
  };

  const handleSave = async () => {
    const values = await form.validateFields();
    try {
      if (editing) {
        await updateUser(editing.userId, {
          name: values.name,
          collegeId: values.collegeId,
          status: values.status ? 1 : 0,
          groupIds: values.groupIds,
          defaultGroupId: values.defaultGroupId,
          password: values.password || undefined,
          email: values.email || undefined,
          wechatUserId: values.wechatUserId || undefined,
        });
        message.success('更新成功');
      } else {
        await createUser({
          name: values.name,
          account: values.account,
          password: values.password,
          collegeId: values.collegeId,
          groupIds: values.groupIds,
          defaultGroupId: values.defaultGroupId,
          email: values.email || undefined,
          wechatUserId: values.wechatUserId || undefined,
        });
        message.success('创建成功');
      }
      setOpen(false);
      load(page);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '保存失败');
    }
  };

  const columns: ColumnsType<UserManageItem> = [
    { title: '姓名', dataIndex: 'name' },
    { title: '账号', dataIndex: 'account' },
    { title: '邮箱', dataIndex: 'email', ellipsis: true, render: (v?: string) => v || '—' },
    { title: '企微 ID', dataIndex: 'wechatUserId', ellipsis: true, render: (v?: string) => v || '—' },
    { title: '学院', dataIndex: 'collegeName' },
    {
      title: '所属组',
      render: (_, r) => r.groups.map((g) => <Tag key={g.groupId}>{g.groupName}</Tag>),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 80,
      render: (s: number) => (s === 1 ? <Tag color="green">启用</Tag> : <Tag>禁用</Tag>),
    },
    {
      title: '操作',
      width: 80,
      render: (_, r) => <Button type="link" onClick={() => openEdit(r)}>编辑</Button>,
    },
  ];

  return (
    <div>
      <Space style={{ marginBottom: 16, width: '100%', justifyContent: 'space-between' }}>
        <Typography.Title level={4} style={{ margin: 0 }}>用户管理</Typography.Title>
        <Space>
          <Input.Search
            placeholder="搜索姓名/账号"
            onSearch={(v) => { setKeyword(v); setPage(1); load(1, v); }}
            style={{ width: 200 }}
          />
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新增用户</Button>
        </Space>
      </Space>
      <Table
        rowKey="userId"
        loading={loading}
        columns={columns}
        dataSource={list}
        pagination={{ current: page, total, pageSize: 20, onChange: setPage }}
      />
      <Modal
        title={editing ? '编辑用户' : '新增用户'}
        open={open}
        onOk={handleSave}
        onCancel={() => setOpen(false)}
        destroyOnClose
        width={520}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="姓名" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="account" label="账号" rules={[{ required: true }]}>
            <Input disabled={!!editing} />
          </Form.Item>
          <Form.Item name="password" label={editing ? '新密码（留空不改）' : '密码'}>
            <Input.Password placeholder={editing ? '留空则不修改' : '默认 admin123'} />
          </Form.Item>
          <Form.Item name="email" label="邮箱（通知用）">
            <Input type="email" placeholder="user@school.edu" />
          </Form.Item>
          <Form.Item name="wechatUserId" label="企业微信 UserId">
            <Input placeholder="企微成员账号" />
          </Form.Item>
          <Form.Item name="collegeId" label="学院" rules={[{ required: true }]}>
            <Select
              disabled={isCollegeScoped()}
              options={colleges.map((c) => ({ value: c.collegeId, label: c.collegeName }))}
            />
          </Form.Item>
          <Form.Item name="groupIds" label="所属组" rules={[{ required: true }]}>
            <Select mode="multiple" options={groups.map((g) => ({ value: g.groupId, label: g.groupName }))} />
          </Form.Item>
          <Form.Item name="defaultGroupId" label="默认组">
            <Select allowClear options={groups.map((g) => ({ value: g.groupId, label: g.groupName }))} />
          </Form.Item>
          {editing && (
            <Form.Item name="status" label="启用" valuePropName="checked">
              <Switch />
            </Form.Item>
          )}
        </Form>
      </Modal>
    </div>
  );
}
