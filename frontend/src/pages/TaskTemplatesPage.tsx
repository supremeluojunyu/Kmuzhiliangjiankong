import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import { Button, Popconfirm, Space, Table, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  createTaskFromTemplate,
  deleteTemplate,
  fetchTemplates,
  type TaskTemplateItem,
} from '@/api/taskTemplate';
import { useAuth } from '@/contexts/AuthContext';

export default function TaskTemplatesPage() {
  const { hasPermission } = useAuth();
  const navigate = useNavigate();
  const [list, setList] = useState<TaskTemplateItem[]>([]);
  const [loading, setLoading] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      setList(await fetchTemplates());
    } catch (e) {
      message.error(e instanceof Error ? e.message : '加载失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const handleUse = async (template: TaskTemplateItem) => {
    try {
      const task = await createTaskFromTemplate(template.templateId, {
        taskName: `${template.templateName}（副本）`,
        description: template.description,
      });
      message.success('已基于模板创建草稿任务');
      navigate(`/tasks/${task.taskId}`);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '创建失败');
    }
  };

  const columns: ColumnsType<TaskTemplateItem> = [
    { title: '模板名称', dataIndex: 'templateName' },
    { title: '说明', dataIndex: 'description', ellipsis: true },
    { title: '节点数', dataIndex: 'nodeCount', width: 80 },
    { title: '创建人', dataIndex: 'creatorName', width: 100 },
    { title: '更新时间', dataIndex: 'updatedAt', width: 180 },
    {
      title: '操作',
      width: 200,
      render: (_, r) => (
        <Space>
          {hasPermission('task:create') && (
            <Button type="link" onClick={() => handleUse(r)}>
              使用模板
            </Button>
          )}
          {hasPermission('task:config') && (
            <Popconfirm title="确定删除该模板？" onConfirm={async () => {
              try {
                await deleteTemplate(r.templateId);
                message.success('已删除');
                load();
              } catch (e) {
                message.error(e instanceof Error ? e.message : '删除失败');
              }
            }}>
              <Button type="link" danger icon={<DeleteOutlined />}>
                删除
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  if (!hasPermission('task:create') && !hasPermission('task:config')) {
    return <Typography.Text type="danger">无任务模板访问权限</Typography.Text>;
  }

  return (
    <div>
      <Space style={{ marginBottom: 16, width: '100%', justifyContent: 'space-between' }}>
        <Typography.Title level={4} style={{ margin: 0 }}>
          任务模板
        </Typography.Title>
        <Space>
          <Button onClick={() => navigate('/tasks')}>返回任务列表</Button>
          {hasPermission('task:create') && (
            <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/tasks/new')}>
              空白创建
            </Button>
          )}
        </Space>
      </Space>
      <Table
        rowKey="templateId"
        loading={loading}
        columns={columns}
        dataSource={list}
        pagination={false}
      />
    </div>
  );
}
