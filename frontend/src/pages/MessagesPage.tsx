import { MailOutlined, PlusOutlined } from '@ant-design/icons';
import {
  Button,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useState } from 'react';
import { fetchAllGroups } from '@/api/group';
import {
  fetchMessages,
  markAllMessagesRead,
  markMessageRead,
  sendMessage,
} from '@/api/message';
import RichTextEditor from '@/components/RichTextEditor';
import RichTextView from '@/components/RichTextView';
import { useAuth } from '@/contexts/AuthContext';
import type { MessageItem } from '@/types';

const statusMap: Record<string, { color: string; text: string }> = {
  broadcast: { color: 'blue', text: '组内广播' },
  system: { color: 'orange', text: '系统通知' },
  comment: { color: 'green', text: '任务评论' },
};

export default function MessagesPage() {
  const { hasPermission } = useAuth();
  const [list, setList] = useState<MessageItem[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [sendOpen, setSendOpen] = useState(false);
  const [groups, setGroups] = useState<{ groupId: number; groupName: string }[]>([]);
  const [form] = Form.useForm();

  const load = async (p = page) => {
    setLoading(true);
    try {
      const res = await fetchMessages(p);
      setList(res.list);
      setTotal(res.total);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load(page);
  }, [page]);

  useEffect(() => {
    fetchAllGroups().then(setGroups).catch(() => {});
  }, []);

  const columns: ColumnsType<MessageItem> = [
    {
      title: '状态',
      width: 80,
      render: (_, r) =>
        r.isRead ? <Tag>已读</Tag> : <Tag color="red">未读</Tag>,
    },
    {
      title: '标题',
      dataIndex: 'title',
      render: (text, r) => (
        <Typography.Link
          onClick={async () => {
            if (!r.isRead) {
              await markMessageRead(r.messageId);
              load(page);
            }
          }}
        >
          {text}
        </Typography.Link>
      ),
    },
    {
      title: '类型',
      dataIndex: 'messageType',
      width: 100,
      render: (t: string) => {
        const s = statusMap[t] || { color: 'default', text: t };
        return <Tag color={s.color}>{s.text}</Tag>;
      },
    },
    { title: '发送人', dataIndex: 'senderName', width: 100 },
    {
      title: '目标组',
      dataIndex: 'targetGroupNames',
      render: (names: string[]) => names?.map((n) => <Tag key={n}>{n}</Tag>),
    },
    { title: '时间', dataIndex: 'sendTime', width: 180 },
  ];

  const handleSend = async () => {
    const values = await form.validateFields();
    try {
      await sendMessage(values);
      message.success('发送成功');
      setSendOpen(false);
      form.resetFields();
      load(1);
      setPage(1);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '发送失败');
    }
  };

  return (
    <div>
      <Space style={{ marginBottom: 16, width: '100%', justifyContent: 'space-between' }}>
        <Typography.Title level={4} style={{ margin: 0 }}>
          <MailOutlined /> 消息中心
        </Typography.Title>
        <Space>
          <Button onClick={async () => { await markAllMessagesRead(); load(page); }}>
            全部已读
          </Button>
          {hasPermission('message:send') && (
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setSendOpen(true)}>
              发送消息
            </Button>
          )}
        </Space>
      </Space>

      <Table
        rowKey="messageId"
        loading={loading}
        columns={columns}
        dataSource={list}
        pagination={{
          current: page,
          total,
          pageSize: 20,
          onChange: setPage,
        }}
        expandable={{
          expandedRowRender: (r) => <RichTextView html={r.content} />,
        }}
      />

      <Modal
        title="发送组内消息"
        open={sendOpen}
        onOk={handleSend}
        onCancel={() => setSendOpen(false)}
        destroyOnClose
        width={640}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="title" label="标题" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="content" label="内容" getValueFromEvent={(v) => v}>
            <RichTextEditor placeholder="支持加粗、列表、链接等格式" />
          </Form.Item>
          <Form.Item
            name="targetGroupIds"
            label="目标组（可多选）"
            rules={[{ required: true, message: '请选择目标组' }]}
          >
            <Select
              mode="multiple"
              options={groups.map((g) => ({ value: g.groupId, label: g.groupName }))}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
