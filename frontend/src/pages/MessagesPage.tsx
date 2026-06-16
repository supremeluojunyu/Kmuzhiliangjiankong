import { MailOutlined, PlusOutlined } from '@ant-design/icons';
import {
  Button,
  Form,
  Input,
  Modal,
  Space,
  Table,
  Tag,
  TreeSelect,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useMemo, useState } from 'react';
import {
  buildSendTargetTree,
  describeSendTargets,
  fetchMessages,
  fetchSendTargets,
  markAllMessagesRead,
  markMessageRead,
  parseSendTargets,
  sendMessage,
  type MessageSendTargetGroup,
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
  const [sendTargets, setSendTargets] = useState<MessageSendTargetGroup[]>([]);
  const [targetsLoading, setTargetsLoading] = useState(false);
  const [form] = Form.useForm();

  const targetTree = useMemo(() => buildSendTargetTree(sendTargets), [sendTargets]);

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

  const openSendModal = async () => {
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
  ];

  const handleSend = async () => {
    const values = await form.validateFields();
    const raw = values.targets || [];
    const selected: string[] = raw.map((item: string | { value: string }) =>
      typeof item === 'string' ? item : item.value,
    );
    const { targetGroupIds, targetUserIds } = parseSendTargets(selected);
    if (targetGroupIds.length === 0 && targetUserIds.length === 0) {
      message.warning('请选择接收组或个人');
      return;
    }

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
      load(1);
      setPage(1);

      Modal.success({
        title: '消息发送成功',
        content: (
          <div>
            <p style={{ marginBottom: 8 }}>
              已向 <strong>{summary}</strong> 发送消息：
            </p>
            <p style={{ margin: 0 }}>「{result.title || values.title}」</p>
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
      <Space style={{ marginBottom: 16, width: '100%', justifyContent: 'space-between' }}>
        <Typography.Title level={4} style={{ margin: 0 }}>
          <MailOutlined /> 消息中心
        </Typography.Title>
        <Space>
          <Button onClick={async () => { await markAllMessagesRead(); load(page); }}>
            全部已读
          </Button>
          {hasPermission('message:send') && (
            <Button type="primary" icon={<PlusOutlined />} onClick={openSendModal}>
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
        title="发送消息"
        open={sendOpen}
        onOk={handleSend}
        onCancel={() => setSendOpen(false)}
        destroyOnClose
        width={640}
        okText="发送"
      >
        <Form form={form} layout="vertical">
          <Form.Item name="title" label="标题" rules={[{ required: true, message: '请输入标题' }]}>
            <Input placeholder="消息标题" />
          </Form.Item>
          <Form.Item name="content" label="内容" getValueFromEvent={(v) => v}>
            <RichTextEditor placeholder="支持加粗、列表、链接等格式" />
          </Form.Item>
          <Form.Item
            name="targets"
            label="接收对象"
            extra="选择组：组内全员接收；选择组下个人：仅该人接收"
            rules={[{ required: true, message: '请选择接收组或个人' }]}
          >
            <TreeSelect
              treeData={targetTree}
              treeCheckable
              showCheckedStrategy={TreeSelect.SHOW_CHILD}
              treeCheckStrictly
              placeholder={targetsLoading ? '加载中…' : '请选择组或个人'}
              loading={targetsLoading}
              disabled={targetsLoading}
              style={{ width: '100%' }}
              maxTagCount="responsive"
              listHeight={280}
              treeDefaultExpandAll
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
