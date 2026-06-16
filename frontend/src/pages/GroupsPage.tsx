import { PlusOutlined } from '@ant-design/icons';
import { Button, Form, Input, Modal, Select, Space, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useState } from 'react';
import {
  createGroup,
  deleteGroup,
  fetchGroupsManage,
  fetchPermissions,
  updateGroup,
  type GroupManageItem,
  type PermissionItem,
} from '@/api/groupManage';

export default function GroupsPage() {
  const [list, setList] = useState<GroupManageItem[]>([]);
  const [permissions, setPermissions] = useState<PermissionItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<GroupManageItem | null>(null);
  const [form] = Form.useForm();

  const load = async () => {
    setLoading(true);
    try {
      const [groups, perms] = await Promise.all([fetchGroupsManage(), fetchPermissions()]);
      setList(groups);
      setPermissions(perms);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    setOpen(true);
  };

  const openEdit = (record: GroupManageItem) => {
    setEditing(record);
    form.setFieldsValue({
      groupName: record.groupName,
      description: record.description,
      permissionIds: record.permissionIds,
    });
    setOpen(true);
  };

  const handleSave = async () => {
    const values = await form.validateFields();
    try {
      if (editing) {
        await updateGroup(editing.groupId, values);
        message.success('更新成功');
      } else {
        await createGroup(values);
        message.success('创建成功');
      }
      setOpen(false);
      load();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '保存失败');
    }
  };

  const handleDelete = async (groupId: number) => {
    try {
      await deleteGroup(groupId);
      message.success('删除成功');
      load();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '删除失败');
    }
  };

  const columns: ColumnsType<GroupManageItem> = [
    { title: '组名称', dataIndex: 'groupName' },
    { title: '描述', dataIndex: 'description' },
    { title: '成员数', dataIndex: 'memberCount', width: 80 },
    {
      title: '权限',
      render: (_, r) => r.permissionCodes?.map((c) => <Tag key={c}>{c}</Tag>),
    },
    {
      title: '操作',
      width: 140,
      render: (_, r) => (
        <Space>
          <Button type="link" onClick={() => openEdit(r)}>编辑</Button>
          {r.groupId > 7 && (
            <Button type="link" danger onClick={() => handleDelete(r.groupId)}>删除</Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Space style={{ marginBottom: 16, width: '100%', justifyContent: 'space-between' }}>
        <Typography.Title level={4} style={{ margin: 0 }}>组管理</Typography.Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新增组</Button>
      </Space>
      <Table rowKey="groupId" loading={loading} columns={columns} dataSource={list} pagination={false} />
      <Modal
        title={editing ? '编辑组' : '新增组'}
        open={open}
        onOk={handleSave}
        onCancel={() => setOpen(false)}
        destroyOnClose
        width={520}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="groupName" label="组名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="permissionIds" label="权限">
            <Select
              mode="multiple"
              options={permissions.map((p) => ({
                value: p.permissionId,
                label: `${p.permissionName} (${p.permissionCode})`,
              }))}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
