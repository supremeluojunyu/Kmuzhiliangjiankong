import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import { Button, Form, Input, Modal, Select, Space, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { Key } from 'react';
import { useEffect, useState } from 'react';
import {
  batchDeleteColleges,
  createCollege,
  fetchCollegesManage,
  updateCollege,
  type CollegeManageItem,
} from '@/api/collegeManage';
import ConfirmDeleteModal from '@/components/ConfirmDeleteModal';
import { DELETE_CONFIRM_PHRASE } from '@/constants/deleteConfirm';

function showDeleteResult(result: { deletedCount: number; errors?: string[] }) {
  if (result.deletedCount > 0) {
    message.success(`已删除 ${result.deletedCount} 个学院`);
  }
  if (result.errors?.length) {
    Modal.warning({
      title: '部分学院删除失败',
      content: (
        <div style={{ whiteSpace: 'pre-wrap' }}>{result.errors.join('\n')}</div>
      ),
    });
  }
}

export default function CollegesPage() {
  const [list, setList] = useState<CollegeManageItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<CollegeManageItem | null>(null);
  const [form] = Form.useForm();
  const [selectedRowKeys, setSelectedRowKeys] = useState<Key[]>([]);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const colleges = await fetchCollegesManage();
      setList(colleges);
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
    form.setFieldsValue({ status: 1 });
    setOpen(true);
  };

  const openEdit = (record: CollegeManageItem) => {
    setEditing(record);
    form.setFieldsValue({
      collegeName: record.collegeName,
      collegeCode: record.collegeCode,
      status: record.status,
    });
    setOpen(true);
  };

  const handleSave = async () => {
    const values = await form.validateFields();
    try {
      if (editing) {
        await updateCollege(editing.collegeId, values);
        message.success('更新成功');
      } else {
        await createCollege(values);
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
      const result = await batchDeleteColleges(
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

  const columns: ColumnsType<CollegeManageItem> = [
    { title: '学院名称', dataIndex: 'collegeName' },
    { title: '学院代码', dataIndex: 'collegeCode', width: 120, render: (v) => v || '—' },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: (s: number) => (
        <Tag color={s === 1 ? 'success' : 'default'}>{s === 1 ? '启用' : '停用'}</Tag>
      ),
    },
    { title: '用户数', dataIndex: 'userCount', width: 80 },
    {
      title: '操作',
      width: 100,
      render: (_, record) => (
        <Button type="link" size="small" onClick={() => openEdit(record)}>
          编辑
        </Button>
      ),
    },
  ];

  const deletableSelected = selectedRowKeys.filter((key) => {
    const item = list.find((c) => c.collegeId === key);
    return item?.deletable !== false;
  });

  return (
    <div>
      <Space style={{ marginBottom: 16, width: '100%', justifyContent: 'space-between' }} wrap>
        <Typography.Title level={4} style={{ margin: 0 }}>
          学院管理
        </Typography.Title>
        <Space wrap>
          {selectedRowKeys.length > 0 && (
            <Button
              danger
              icon={<DeleteOutlined />}
              disabled={deletableSelected.length === 0}
              onClick={() => setDeleteOpen(true)}
            >
              删除选中 ({deletableSelected.length})
            </Button>
          )}
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新建学院
          </Button>
        </Space>
      </Space>

      <Typography.Paragraph type="secondary">
        维护学院名称与代码。用户管理、任务分配等页面的学院下拉选项均来自此处；停用后不会出现在下拉列表中。
      </Typography.Paragraph>

      <Table
        rowKey="collegeId"
        loading={loading}
        dataSource={list}
        columns={columns}
        rowSelection={{
          selectedRowKeys,
          onChange: setSelectedRowKeys,
          getCheckboxProps: (record) => ({
            disabled: record.deletable === false,
          }),
        }}
        pagination={false}
      />

      <Modal
        title={editing ? '编辑学院' : '新建学院'}
        open={open}
        onOk={handleSave}
        onCancel={() => setOpen(false)}
        destroyOnHidden
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="collegeName"
            label="学院名称"
            rules={[{ required: true, message: '请输入学院名称' }]}
          >
            <Input placeholder="如：信息学院" maxLength={100} />
          </Form.Item>
          <Form.Item name="collegeCode" label="学院代码" extra="可选，用于导入导出等场景，建议大写英文缩写">
            <Input placeholder="如：INFO" maxLength={20} />
          </Form.Item>
          <Form.Item name="status" label="状态" rules={[{ required: true }]}>
            <Select
              options={[
                { value: 1, label: '启用' },
                { value: 0, label: '停用' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>

      <ConfirmDeleteModal
        open={deleteOpen}
        loading={deleting}
        count={deletableSelected.length}
        onConfirm={handleBatchDelete}
        onCancel={() => setDeleteOpen(false)}
      />
    </div>
  );
}
