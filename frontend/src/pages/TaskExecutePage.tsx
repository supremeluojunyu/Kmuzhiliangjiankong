import { ArrowLeftOutlined, CheckOutlined } from '@ant-design/icons';
import {
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  List,
  Radio,
  Space,
  Steps,
  Tag,
  Typography,
  Upload,
  message,
} from 'antd';
import type { UploadFile } from 'antd/es/upload/interface';
import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { uploadFile } from '@/api/file';
import {
  fetchInstance,
  fetchInstanceComments,
  postInstanceComment,
  submitNode,
  type MyTaskItem,
  type TaskCommentItem,
} from '@/api/task';
import RichTextEditor from '@/components/RichTextEditor';
import RichTextView from '@/components/RichTextView';
import { useAuth } from '@/contexts/AuthContext';

const nodeTypeLabel: Record<string, string> = {
  submit: '提交材料',
  view: '查看材料',
  score: '评分',
  approve: '审核',
};

export default function TaskExecutePage() {
  const { instanceId } = useParams();
  const navigate = useNavigate();
  const { currentGroupId } = useAuth();
  const [task, setTask] = useState<MyTaskItem | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const [comments, setComments] = useState<TaskCommentItem[]>([]);
  const [commentHtml, setCommentHtml] = useState('');
  const [commentSubmitting, setCommentSubmitting] = useState(false);
  const [form] = Form.useForm();

  const loadComments = async () => {
    try {
      const list = await fetchInstanceComments(Number(instanceId));
      setComments(list);
    } catch {
      setComments([]);
    }
  };

  const load = async () => {
    setLoading(true);
    try {
      const data = await fetchInstance(Number(instanceId));
      setTask(data);
      const active = data.nodeRecords?.find(
        (n) =>
          (n.status === 'in_progress' || n.status === 'draft')
          && n.executeGroupId === currentGroupId
      );
      if (active?.submitData) {
        form.setFieldsValue(active.submitData);
        const files = (active.submitData.files as { name: string; path: string }[]) || [];
        setFileList(files.map((f, i) => ({
          uid: String(i),
          name: f.name,
          status: 'done',
          response: { filePath: f.path, fileName: f.name },
        })));
      } else {
        setFileList([]);
      }
    } catch {
      message.error('加载失败');
    } finally {
      setLoading(false);
    }
    loadComments();
  };

  useEffect(() => {
    load();
  }, [instanceId]);

  const activeNode = task?.nodeRecords?.find(
    (n) =>
      (n.status === 'in_progress' || n.status === 'draft')
      && n.executeGroupId === currentGroupId
  );

  const waitingOnOtherGroup = !activeNode && task?.nodeRecords?.some(
    (n) => (n.status === 'in_progress' || n.status === 'draft')
      && n.executeGroupId !== currentGroupId
  );

  const waitingNode = task?.nodeRecords?.find(
    (n) => (n.status === 'in_progress' || n.status === 'draft')
      && n.executeGroupId !== currentGroupId
  );

  const handleSubmit = async (draft = false) => {
    if (!activeNode) return;
    let values: Record<string, unknown> = {};
    if (activeNode.nodeType === 'view') {
      values = { viewed: true };
    } else if (draft) {
      values = { ...form.getFieldsValue(), files: collectFiles() };
    } else {
      values = await form.validateFields();
      values.files = collectFiles();
    }
    setSubmitting(true);
    try {
      await submitNode(Number(instanceId), activeNode.nodeId, values, draft);
      message.success(draft ? '草稿已保存' : '提交成功');
      load();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '提交失败');
    } finally {
      setSubmitting(false);
    }
  };

  const collectFiles = () =>
    fileList
      .filter((f) => f.status === 'done')
      .map((f) => ({
        name: (f.response as { fileName?: string })?.fileName || f.name,
        path: (f.response as { filePath?: string })?.filePath || f.name,
      }));

  const handlePostComment = async () => {
    const text = commentHtml.replace(/<[^>]+>/g, '').trim();
    if (!text) {
      message.warning('请输入评论内容');
      return;
    }
    setCommentSubmitting(true);
    try {
      await postInstanceComment(Number(instanceId), commentHtml);
      setCommentHtml('');
      message.success('评论已发布');
      loadComments();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '发布失败');
    } finally {
      setCommentSubmitting(false);
    }
  };

  if (loading || !task) {
    return <Typography.Text>加载中...</Typography.Text>;
  }

  const stepItems = (task.nodeRecords || []).map((n) => {
    const isMine = n.executeGroupId === currentGroupId;
    const typeLabel = nodeTypeLabel[n.nodeType] || n.nodeType;
    const groupHint = n.executeGroupName ? ` · ${n.executeGroupName}` : '';
    return {
      title: n.nodeName || n.nodeId,
      description: `${typeLabel}${groupHint}${isMine ? '（本组负责）' : ''}`,
      status: (n.status === 'completed'
        ? 'finish'
        : n.status === 'in_progress' || n.status === 'draft'
          ? isMine ? 'process' : 'wait'
          : 'wait') as 'finish' | 'process' | 'wait',
    };
  });

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/my-tasks')}>
          返回
        </Button>
        <Typography.Title level={4} style={{ margin: 0 }}>
          {task.taskName}
        </Typography.Title>
        <Tag>{task.status}</Tag>
      </Space>

      <Card title="流程进度" style={{ marginBottom: 16 }}>
        <Steps direction="vertical" size="small" items={stepItems} />
      </Card>

      {activeNode && task.status !== 'completed' ? (
        <Card title={`执行：${activeNode.nodeName}（${nodeTypeLabel[activeNode.nodeType]}）`}>
          <Form form={form} layout="vertical">
            {activeNode.nodeType === 'submit' && (
              <>
                <Form.Item name="remark" label="说明">
                  <Input.TextArea rows={3} placeholder="材料说明" />
                </Form.Item>
                <Form.Item label="上传材料">
                  <Upload
                    fileList={fileList}
                    customRequest={async ({ file, onSuccess, onError }) => {
                      try {
                        const result = await uploadFile(file as File);
                        onSuccess?.(result);
                      } catch (e) {
                        onError?.(e as Error);
                      }
                    }}
                    onChange={({ fileList: fl }) => setFileList(fl)}
                    multiple
                  >
                    <Button>选择文件</Button>
                  </Upload>
                </Form.Item>
              </>
            )}
            {activeNode.nodeType === 'score' && (() => {
              const scoreMode = activeNode.config?.scoreMode === 'grade' ? 'grade' : 'numeric';
              const gradeOptions = (activeNode.config?.gradeOptions as string[] | undefined)
                ?? ['优', '良', '中', '差'];
              return scoreMode === 'grade' ? (
                <>
                  <Form.Item name="grade" label="等级" rules={[{ required: true, message: '请选择等级' }]}>
                    <Radio.Group>
                      {gradeOptions.map((g) => (
                        <Radio key={g} value={g}>{g}</Radio>
                      ))}
                    </Radio.Group>
                  </Form.Item>
                  <Form.Item name="comment" label="评语">
                    <Input.TextArea rows={3} />
                  </Form.Item>
                </>
              ) : (
                <>
                  <Form.Item name="score" label="评分（0–100）" rules={[{ required: true }]}>
                    <InputNumber min={0} max={100} style={{ width: 200 }} />
                  </Form.Item>
                  <Form.Item name="comment" label="评语">
                    <Input.TextArea rows={3} />
                  </Form.Item>
                </>
              );
            })()}
            {activeNode.nodeType === 'approve' && (
              <>
                <Form.Item name="result" label="审核结果" rules={[{ required: true }]}>
                  <Radio.Group>
                    <Radio value="approved">通过</Radio>
                    <Radio value="rejected">驳回</Radio>
                  </Radio.Group>
                </Form.Item>
                <Form.Item name="comment" label="意见">
                  <Input.TextArea rows={3} />
                </Form.Item>
              </>
            )}
            {activeNode.nodeType === 'view' && (
              <Typography.Paragraph type="secondary">
                查看节点：确认已阅后可提交完成。
              </Typography.Paragraph>
            )}
          </Form>
          <Space>
            {(activeNode.nodeType === 'submit') && (
              <Button onClick={() => handleSubmit(true)} loading={submitting}>
                保存草稿
              </Button>
            )}
            <Button type="primary" icon={<CheckOutlined />} onClick={() => handleSubmit(false)} loading={submitting}>
              提交
            </Button>
          </Space>
        </Card>
      ) : waitingOnOtherGroup ? (
        <Card>
          <Typography.Text type="secondary">
            当前流程节点「{waitingNode?.nodeName || waitingNode?.nodeId}」（
            {nodeTypeLabel[waitingNode?.nodeType || ''] || waitingNode?.nodeType}
            {waitingNode?.executeGroupName ? ` · ${waitingNode.executeGroupName}` : ''}
            ）正在处理中，请等待该组完成后再查看后续进度。
          </Typography.Text>
        </Card>
      ) : task.status === 'completed' ? (
        <Card>
          <Typography.Text type="success">任务已全部完成</Typography.Text>
        </Card>
      ) : (
        <Card>
          <Typography.Text type="secondary">当前无待处理节点，请等待流程推进</Typography.Text>
        </Card>
      )}

      <Card title="任务讨论" style={{ marginTop: 16 }}>
        <List
          locale={{ emptyText: '暂无讨论，可在下方发表评论' }}
          dataSource={comments}
          renderItem={(item) => (
            <List.Item>
              <List.Item.Meta
                title={
                  <Space>
                    <Typography.Text strong>{item.senderName || '用户'}</Typography.Text>
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                      {item.sendTime?.replace('T', ' ').slice(0, 16)}
                    </Typography.Text>
                  </Space>
                }
                description={<RichTextView html={item.content} />}
              />
            </List.Item>
          )}
        />
        <div style={{ marginTop: 16 }}>
          <RichTextEditor
            value={commentHtml}
            onChange={setCommentHtml}
            placeholder="输入讨论内容…"
            minHeight={120}
          />
          <Button
            type="primary"
            style={{ marginTop: 12 }}
            loading={commentSubmitting}
            onClick={handlePostComment}
          >
            发表评论
          </Button>
        </div>
      </Card>
    </div>
  );
}
