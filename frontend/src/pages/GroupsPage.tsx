import { DeleteOutlined, PlusOutlined, UploadOutlined } from '@ant-design/icons';
import { Button, Form, Input, Modal, Select, Space, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { Key } from 'react';
import { useEffect, useState } from 'react';
import {
  batchDeleteGroups,
  createGroup,
  fetchGroupsManage,
  fetchPermissions,
  updateGroup,
  downloadGroupImportTemplate,
  importGroupsFromExcel,
  type GroupManageItem,
  type PermissionItem,
} from '@/api/groupManage';
import ConfirmDeleteModal from '@/components/ConfirmDeleteModal';
import ImportExcelModal from '@/components/ImportExcelModal';
import { DELETE_CONFIRM_PHRASE } from '@/constants/deleteConfirm';

function showDeleteResult(result: { deletedCount: number; errors?: string[] }) {
  if (result.deletedCount > 0) {
    message.success(`已删除 ${result.deletedCount} 个组`);
  }
  if (result.errors?.length) {
    Modal.warning({
      title: '部分组删除失败',
      content: (
        <div style={{ whiteSpace: 'pre-wrap' }}>{result.errors.join('\n')}</div>
      ),
    });
  }
}

export default function GroupsPage() {
  const [list, setList] = useState<GroupManageItem[]>([]);
  const [permissions, setPermissions] = useState<PermissionItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [importOpen, setImportOpen] = useState(false);
  const [editing, setEditing] = useState<GroupManageItem | null>(null);
  const [form] = Form.useForm();
  const [selectedRowKeys, setSelectedRowKeys] = useState<Key[]>([]);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const [groups, perms] = await Promise.all([fetchGroupsManage(), fetchPermissions()]);
      setList(groups);
      setPermissions(perms);
      setSelectedRowKeys([]);
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

  const handleBatchDelete = async () => {
    setDeleting(true);
    try {
      const result = await batchDeleteGroups(
        selectedRowKeys.map(Number),
        DELETE_CONFIRM_PHRASE
      );
      setDeleteOpen(false);
      showDeleteResult(result);
      load();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '删除失败');
    } finally {
      setDeleting(false);
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
      width: 80,
      render: (_, r) => (
        <Button type="link" onClick={() => openEdit(r)}>编辑</Button>
      ),
    },
  ];

  return (
    <div>
      <Space style={{ marginBottom: 16, width: '100%', justifyContent: 'space-between' }}>
        <Typography.Title level={4} style={{ margin: 0 }}>组管理</Typography.Title>
        <Space>
          <Button
            danger
            icon={<DeleteOutlined />}
            disabled={selectedRowKeys.length === 0}
            onClick={() => setDeleteOpen(true)}
          >
            删除{selectedRowKeys.length > 0 ? ` (${selectedRowKeys.length})` : ''}
          </Button>
          <Button icon={<UploadOutlined />} onClick={() => setImportOpen(true)}>
            批量导入
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新增组</Button>
        </Space>
      </Space>
      <Table
        rowKey="groupId"
        loading={loading}
        columns={columns}
        dataSource={list}
        rowSelection={{
          selectedRowKeys,
          onChange: setSelectedRowKeys,
          getCheckboxProps: (record) => ({
            disabled: !record.deletable,
          }),
        }}
        pagination={false}
      />
      <ConfirmDeleteModal
        open={deleteOpen}
        title="删除用户组"
        description="将级联删除该组关联的任务（已暂停或已停止）、组内成员关系及消息投递记录。内置组不可删除；仍有进行中关联任务的组不可删除。"
        count={selectedRowKeys.length}
        loading={deleting}
        onCancel={() => setDeleteOpen(false)}
        onConfirm={handleBatchDelete}
      />
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
      <ImportExcelModal
        open={importOpen}
        title="批量导入组"
        description="请先下载模板。权限代码可填 user:manage、message:send 等，多个用逗号分隔；留空则创建无权限组。组名已存在时将自动跳过。"
        onClose={() => setImportOpen(false)}
        onDownloadTemplate={downloadGroupImportTemplate}
        onImport={importGroupsFromExcel}
        onSuccess={load}
      />
    </div>
  );
}
