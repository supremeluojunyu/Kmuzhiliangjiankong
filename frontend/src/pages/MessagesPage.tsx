import { MailOutlined, PlusOutlined } from '@ant-design/icons';
import {
  Button,
  Form,
  Input,
  Modal,
  Popconfirm,
  Space,
  Table,
  Tabs,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useState } from 'react';
import {
  describeSendTargets,
  canDeleteMessage,
  deleteMessage,
  fetchMessages,
  fetchSendTargets,
  markAllMessagesRead,
  markMessageRead,
  sendMessage,
  type MessageDirection,
  type MessageSendTargetGroup,
} from '@/api/message';
import MessageTargetPicker, {
  hasMessageTargetSelection,
  type MessageTargetSelection,
} from '@/components/MessageTargetPicker';
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
  const { hasPermission, user } = useAuth();
  const [list, setList] = useState<MessageItem[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [direction, setDirection] = useState<MessageDirection>('all');
  const [loading, setLoading] = useState(false);
  const [sendOpen, setSendOpen] = useState(false);
  const [sendTargets, setSendTargets] = useState<MessageSendTargetGroup[]>([]);
  const [targetsLoading, setTargetsLoading] = useState(false);
  const [form] = Form.useForm();

  const load = async (p = page, dir = direction) => {
    setLoading(true);
    try {
      const res = await fetchMessages(p, 20, dir);
      setList(res.list);
      setTotal(res.total);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load(page, direction);
  }, [page, direction]);

  const openSendModal = async () => {
    form.resetFields();
    setSendOpen(true);
    setTargetsLoading(true);
    try {
      const targets = await fetchSendTargets();
      setSendTargets(targets);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '加载接收对象失败');
    } finally {
      setTargetsLoading(false);
    }
  };

  const columns: ColumnsType<MessageItem> = [
    {
      title: '方向',
      width: 80,
      render: (_, r) =>
        r.sentByMe ? (
          <Tag color="cyan">发出</Tag>
        ) : (
          <Tag color="geekblue">收到</Tag>
        ),
    },
    {
      title: '状态',
      width: 80,
      render: (_, r) => {
        if (r.sentByMe) return <Tag color="green">已发出</Tag>;
        return r.isRead ? <Tag>已读</Tag> : <Tag color="red">未读</Tag>;
      },
    },
    {
      title: '标题',
      dataIndex: 'title',
      render: (text, r) => (
        <Typography.Link
          onClick={async () => {
            if (!r.sentByMe && !r.isRead) {
              await markMessageRead(r.messageId);
              load(page, direction);
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
      title: '接收对象',
      key: 'targets',
      render: (_, r) => (
        <>
          {r.targetGroupNames?.map((n) => (
            <Tag key={`g-${n}`} color="blue">
              {n}（全员）
            </Tag>
          ))}
          {r.targetUserNames?.map((n) => (
            <Tag key={`u-${n}`} color="purple">
              {n}
            </Tag>
          ))}
        </>
      ),
    },
    { title: '时间', dataIndex: 'sendTime', width: 180 },
    {
      title: '操作',
      width: 80,
      render: (_, r) =>
        canDeleteMessage(hasPermission, user?.userId, r) ? (
          <Popconfirm
            title="确定删除该消息？"
            description="删除后所有接收者将无法再查看此消息"
            onConfirm={async () => {
              try {
                await deleteMessage(r.messageId);
                message.success('消息已删除');
                load(page, direction);
              } catch (e) {
                message.error(e instanceof Error ? e.message : '删除失败');
              }
            }}
          >
            <Button type="link" danger size="small">
              删除
            </Button>
          </Popconfirm>
        ) : null,
    },
  ];

  const handleSend = async () => {
    const values = await form.validateFields();
    const picked = values.targets as MessageTargetSelection | undefined;
    if (!hasMessageTargetSelection(picked)) {
      message.warning('请选择接收组或个人');
      return;
    }

    const targetGroupIds = picked!.targetGroupIds;
    const targetUserIds = picked!.targetUserIds;

    try {
      const result = await sendMessage({
        title: values.title,
        content: values.content,
        targetGroupIds,
        targetUserIds,
      });
      const summary = describeSendTargets(sendTargets, targetGroupIds, targetUserIds);
      setSendOpen(false);
      form.resetFields();
      setDirection('sent');
      setPage(1);
      load(1, 'sent');

      Modal.success({
        title: '消息发送成功',
        content: (
          <div>
            <p style={{ marginBottom: 8 }}>
              已向 <strong>{summary}</strong> 发送消息：
            </p>
            <p style={{ margin: 0 }}>「{result.title || values.title}」</p>
            <p style={{ marginTop: 8, marginBottom: 0, color: '#666' }}>
              可在「我发出的」中查看已发送记录
            </p>
          </div>
        ),
        okText: '知道了',
        centered: true,
      });
    } catch (e) {
      message.error(e instanceof Error ? e.message : '发送失败');
    }
  };

  return (
    <div className="mobile-page">
      <Space style={{ marginBottom: 16, width: '100%', justifyContent: 'space-between' }} wrap>
        <Typography.Title level={4} style={{ margin: 0 }}>
          <MailOutlined /> 消息中心
        </Typography.Title>
        <Space wrap>
          <Button onClick={async () => { await markAllMessagesRead(); load(page, direction); }}>
            全部已读
          </Button>
          {hasPermission('message:send') && (
            <Button type="primary" icon={<PlusOutlined />} onClick={openSendModal}>
              发送消息
            </Button>
          )}
        </Space>
      </Space>

      <Tabs
        activeKey={direction}
        onChange={(key) => {
          setDirection(key as MessageDirection);
          setPage(1);
        }}
        style={{ marginBottom: 12 }}
        items={[
          { key: 'all', label: '全部' },
          { key: 'received', label: '我收到的' },
          { key: 'sent', label: '我发出的' },
        ]}
      />

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
        title="发送消息"
        open={sendOpen}
        onOk={handleSend}
        onCancel={() => setSendOpen(false)}
        destroyOnClose
        width={720}
        okText="发送"
        styles={{ body: { maxHeight: '70vh', overflowY: 'auto' } }}
      >
        <Form form={form} layout="vertical" initialValues={{ targets: { targetGroupIds: [], targetUserIds: [] } }}>
          <Form.Item name="title" label="标题" rules={[{ required: true, message: '请输入标题' }]}>
            <Input placeholder="消息标题" />
          </Form.Item>
          <Form.Item name="content" label="内容" getValueFromEvent={(v) => v}>
            <RichTextEditor placeholder="支持加粗、列表、链接等格式" />
          </Form.Item>
          <Form.Item
            name="targets"
            label="接收对象"
            rules={[
              {
                validator: (_, value: MessageTargetSelection) =>
                  hasMessageTargetSelection(value)
                    ? Promise.resolve()
                    : Promise.reject(new Error('请至少选择一个组（全员）或组内成员')),
              },
            ]}
          >
            <MessageTargetPicker groups={sendTargets} loading={targetsLoading} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
