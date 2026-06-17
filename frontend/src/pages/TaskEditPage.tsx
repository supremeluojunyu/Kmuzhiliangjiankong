import { MinusCircleOutlined, PlusOutlined, SaveOutlined } from '@ant-design/icons';
import {
  Button,
  Card,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Typography,
  Alert,
  message,
} from 'antd';
import dayjs from 'dayjs';
import { useEffect, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { fetchAllGroups } from '@/api/group';
import { createTask, fetchTask, pauseTask, publishTask, resumeTask, stopTask, updateTask } from '@/api/task';
import { fetchTemplate, fetchTemplates, saveTemplate, saveTemplateFromTask } from '@/api/taskTemplate';
import RichTextEditor from '@/components/RichTextEditor';
import RichTextView from '@/components/RichTextView';
import { useAuth } from '@/contexts/AuthContext';
import type { FlowNode } from '@/types';
import { DATETIME_FMT, formatDateTime, parseDateTime } from '@/utils/datetime';

const NODE_TYPES = [
  { value: 'submit', label: '提交材料' },
  { value: 'view', label: '查看材料' },
  { value: 'score', label: '评分' },
  { value: 'approve', label: '审核' },
];

const EXEC_MODES = [
  { value: 'sequential', label: '顺序（会签）' },
  { value: 'parallel', label: '并行' },
  { value: 'any', label: '或签（任一完成）' },
];

export default function TaskEditPage() {
  const { taskId } = useParams();
  const [searchParams] = useSearchParams();
  const isNew = taskId === 'new';
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const [form] = Form.useForm();
  const [groups, setGroups] = useState<{ groupId: number; groupName: string }[]>([]);
  const [templates, setTemplates] = useState<{ templateId: number; templateName: string }[]>([]);
  const [status, setStatus] = useState('draft');
  const [editable, setEditable] = useState(true);
  const [canManage, setCanManage] = useState(false);
  const [loading, setLoading] = useState(false);
  const [saveTplOpen, setSaveTplOpen] = useState(false);
  const [tplForm] = Form.useForm();
  const description = Form.useWatch('description', form);

  const readOnly = !isNew && !editable;
  const isPaused = status === 'paused';

  useEffect(() => {
    fetchAllGroups().then(setGroups).catch(() => {});
    fetchTemplates()
      .then((list) => setTemplates(list.map((t) => ({
        templateId: t.templateId,
        templateName: t.templateName,
      }))))
      .catch(() => {});
  }, []);

  const applyTemplate = async (templateId: number) => {
    try {
      const tpl = await fetchTemplate(templateId);
      form.setFieldsValue({
        taskName: tpl.templateName,
        description: tpl.description,
        globalTimeStart: parseDateTime(tpl.flowConfig?.globalTimeStart),
        globalTimeEnd: parseDateTime(tpl.flowConfig?.globalTimeEnd),
        nodes: tpl.flowConfig?.nodes?.length ? tpl.flowConfig.nodes : [defaultNode()],
      });
      message.success('已载入模板流程');
    } catch (e) {
      message.error(e instanceof Error ? e.message : '载入模板失败');
    }
  };

  useEffect(() => {
    const tplId = searchParams.get('templateId');
    if (isNew && tplId) {
      applyTemplate(Number(tplId));
    }
  }, [isNew, searchParams]);

  useEffect(() => {
    if (!isNew && taskId) {
      setLoading(true);
      fetchTask(Number(taskId))
        .then((task) => {
          setStatus(task.status);
          setEditable(task.editable ?? false);
          setCanManage(task.canManage ?? false);
          form.setFieldsValue({
            taskName: task.taskName,
            description: task.description,
            globalTimeStart: parseDateTime(task.flowConfig?.globalTimeStart),
            globalTimeEnd: parseDateTime(task.flowConfig?.globalTimeEnd),
            nodes: task.flowConfig?.nodes?.length
              ? task.flowConfig.nodes
              : [defaultNode()],
          });
        })
        .catch(() => message.error('加载任务失败'))
        .finally(() => setLoading(false));
    } else {
      form.setFieldsValue({ nodes: [defaultNode()] });
    }
  }, [taskId, isNew, form]);

  const defaultNode = (): Partial<FlowNode> => ({
    nodeId: `node_${Date.now()}`,
    nodeType: 'submit',
    nodeName: '',
    executeGroupId: groups[0]?.groupId ?? 5,
    dependsOn: [],
    executionMode: 'sequential',
    timeLimitHours: 72,
  });

  const buildPayload = (values: Record<string, unknown>) => ({
    taskName: values.taskName as string,
    description: values.description as string,
    flowConfig: {
      nodes: (values.nodes as FlowNode[]).map((n) => ({
        ...n,
        dependsOn: n.dependsOn || [],
      })),
      globalTimeStart: formatDateTime(values.globalTimeStart as dayjs.Dayjs | string | undefined),
      globalTimeEnd: formatDateTime(values.globalTimeEnd as dayjs.Dayjs | string | undefined),
    },
  });

  const handleSave = async () => {
    const values = await form.validateFields();
    const payload = buildPayload(values);
    try {
      if (isNew) {
        const task = await createTask(payload);
        message.success('创建成功');
        navigate(`/tasks/${task.taskId}`, { replace: true });
      } else {
        await updateTask(Number(taskId), payload);
        message.success('保存成功');
      }
    } catch (e) {
      message.error(e instanceof Error ? e.message : '保存失败');
    }
  };

  const handlePublish = async () => {
    await handleSave();
    try {
      await publishTask(Number(taskId));
      message.success('发布成功');
      navigate('/tasks');
    } catch (e) {
      message.error(e instanceof Error ? e.message : '发布失败');
    }
  };

  const handlePause = async () => {
    try {
      const task = await pauseTask(Number(taskId));
      setStatus(task.status);
      setEditable(task.editable ?? true);
      message.success('任务已暂停，可修改流程配置');
    } catch (e) {
      message.error(e instanceof Error ? e.message : '暂停失败');
    }
  };

  const handleResume = async () => {
    try {
      if (!readOnly) {
        await handleSave();
      }
      await resumeTask(Number(taskId));
      message.success('任务已恢复运行');
      navigate('/tasks');
    } catch (e) {
      message.error(e instanceof Error ? e.message : '恢复失败');
    }
  };

  const handleStop = async () => {
    Modal.confirm({
      title: '确认停止任务？',
      content: '停止后所有进行中的实例将关闭，且不可再提交。如需重新运行请创建新任务。',
      okType: 'danger',
      onOk: async () => {
        await stopTask(Number(taskId));
        message.success('任务已停止');
        navigate('/tasks');
      },
    });
  };

  const handleSaveAsTemplate = async () => {
    const values = await tplForm.validateFields();
    const taskValues = await form.validateFields();
    const payload = buildPayload(taskValues);
    try {
      if (isNew) {
        await saveTemplate({
          templateName: values.templateName,
          description: values.description,
          flowConfig: payload.flowConfig!,
        });
      } else {
        await saveTemplateFromTask(Number(taskId), {
          templateName: values.templateName,
          description: values.description,
        });
      }
      message.success('模板保存成功');
      setSaveTplOpen(false);
      tplForm.resetFields();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '保存模板失败');
    }
  };

  if (!hasPermission('task:create') && isNew) {
    return <Typography.Text type="danger">无创建任务权限</Typography.Text>;
  }

  if (!isNew && !hasPermission('task:create') && !hasPermission('task:config')) {
    return <Typography.Text type="danger">无任务管理权限</Typography.Text>;
  }

  return (
    <div>
      <Typography.Title level={4}>
        {isNew ? '创建任务' : isPaused ? '编辑任务（已暂停）' : readOnly ? '查看任务' : '编辑任务'}
      </Typography.Title>

      {isPaused && (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          message="任务已暂停"
          description="可修改流程节点与时间配置。保存后点击「恢复运行」继续；进行中实例将按新流程同步未完成的节点。"
        />
      )}

      {readOnly && !isNew && status !== 'closed' && (
        <Alert
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
          message="当前为只读查看"
          description={
            canManage
              ? '任务已发布或进行中，如需修改请先点击「暂停后可编辑」。如需彻底移除，请在任务列表中选中后删除。'
              : '任务已发布或进行中，如需修改请联系管理员暂停任务。如需彻底移除，请联系管理员在任务列表中删除。'
          }
        />
      )}

      {isNew && templates.length > 0 && (
        <Card size="small" style={{ marginBottom: 16 }}>
          <Space>
            <Typography.Text>从模板载入：</Typography.Text>
            <Select
              placeholder="选择任务模板"
              style={{ width: 260 }}
              allowClear
              options={templates.map((t) => ({ value: t.templateId, label: t.templateName }))}
              onChange={(v) => v && applyTemplate(v)}
            />
            <Button type="link" onClick={() => navigate('/tasks/templates')}>
              管理模板
            </Button>
          </Space>
        </Card>
      )}

      <Form form={form} layout="vertical" disabled={readOnly || loading}>
        <Card title="基本信息" style={{ marginBottom: 16 }}>
          <Form.Item name="taskName" label="任务名称" rules={[{ required: true }]}>
            <Input placeholder="如：2026秋季课程大纲检查" />
          </Form.Item>
          {readOnly ? (
            <Form.Item label="任务说明">
              <RichTextView html={description} />
            </Form.Item>
          ) : (
            <Form.Item name="description" label="任务说明" getValueFromEvent={(v) => v}>
              <RichTextEditor placeholder="填写任务背景、要求与注意事项" minHeight={180} />
            </Form.Item>
          )}
          <Space wrap>
            {readOnly ? (
              <>
                <Form.Item label="整体开始时间">
                  <Typography.Text>{formatDateTime(form.getFieldValue('globalTimeStart')) || '—'}</Typography.Text>
                </Form.Item>
                <Form.Item label="整体结束时间">
                  <Typography.Text>{formatDateTime(form.getFieldValue('globalTimeEnd')) || '—'}</Typography.Text>
                </Form.Item>
              </>
            ) : (
              <>
                <Form.Item name="globalTimeStart" label="整体开始时间">
                  <DatePicker
                    showTime={{ format: 'HH:mm' }}
                    format={DATETIME_FMT}
                    placeholder="选择开始时间"
                    style={{ width: 220 }}
                  />
                </Form.Item>
                <Form.Item name="globalTimeEnd" label="整体结束时间">
                  <DatePicker
                    showTime={{ format: 'HH:mm' }}
                    format={DATETIME_FMT}
                    placeholder="选择结束时间"
                    style={{ width: 220 }}
                  />
                </Form.Item>
              </>
            )}
          </Space>
        </Card>

        <Card title="流程节点配置">
          <Form.List name="nodes">
            {(fields, { add, remove }) => (
              <>
                {fields.map(({ key, name, ...rest }) => (
                  <Card
                    key={key}
                    size="small"
                    style={{ marginBottom: 12, background: '#fafafa' }}
                    extra={
                      !readOnly && fields.length > 1 ? (
                        <MinusCircleOutlined onClick={() => remove(name)} />
                      ) : null
                    }
                  >
                    <Space wrap style={{ width: '100%' }}>
                      <Form.Item
                        {...rest}
                        name={[name, 'nodeId']}
                        label="节点ID"
                        rules={[{ required: true }]}
                      >
                        <Input style={{ width: 140 }} />
                      </Form.Item>
                      <Form.Item
                        {...rest}
                        name={[name, 'nodeName']}
                        label="节点名称"
                      >
                        <Input style={{ width: 160 }} />
                      </Form.Item>
                      <Form.Item
                        {...rest}
                        name={[name, 'nodeType']}
                        label="节点类型"
                        rules={[{ required: true }]}
                      >
                        <Select options={NODE_TYPES} style={{ width: 120 }} />
                      </Form.Item>
                      <Form.Item
                        {...rest}
                        name={[name, 'executeGroupId']}
                        label="执行组"
                        rules={[{ required: true }]}
                      >
                        <Select
                          style={{ width: 160 }}
                          options={groups.map((g) => ({
                            value: g.groupId,
                            label: g.groupName,
                          }))}
                        />
                      </Form.Item>
                      <Form.Item
                        {...rest}
                        name={[name, 'executionMode']}
                        label="执行模式"
                      >
                        <Select options={EXEC_MODES} style={{ width: 140 }} />
                      </Form.Item>
                      <Form.Item
                        {...rest}
                        name={[name, 'timeLimitHours']}
                        label="时限(小时)"
                      >
                        <InputNumber min={0} style={{ width: 100 }} />
                      </Form.Item>
                    </Space>
                    <Form.Item
                      {...rest}
                      name={[name, 'dependsOn']}
                      label="依赖节点ID（多选，留空表示无依赖）"
                    >
                      <Select mode="tags" placeholder="输入前置 node_id" style={{ maxWidth: 480 }} />
                    </Form.Item>
                    <Form.Item noStyle shouldUpdate>
                      {() =>
                        form.getFieldValue(['nodes', name, 'nodeType']) === 'score' ? (
                          <Space wrap style={{ width: '100%' }}>
                            <Form.Item
                              {...rest}
                              name={[name, 'config', 'scoreMode']}
                              label="评分方式"
                              initialValue="numeric"
                            >
                              <Select
                                style={{ width: 140 }}
                                options={[
                                  { value: 'numeric', label: '百分制 (0–100)' },
                                  { value: 'grade', label: '等级制' },
                                ]}
                              />
                            </Form.Item>
                            {form.getFieldValue(['nodes', name, 'config', 'scoreMode']) === 'grade' && (
                              <Form.Item
                                {...rest}
                                name={[name, 'config', 'gradeOptions']}
                                label="等级选项"
                                initialValue={['优', '良', '中', '差']}
                              >
                                <Select mode="tags" style={{ minWidth: 280 }} placeholder="如：优、良、中、差" />
                              </Form.Item>
                            )}
                          </Space>
                        ) : null
                      }
                    </Form.Item>
                  </Card>
                ))}
                {!readOnly && (
                  <Button
                    type="dashed"
                    onClick={() => add(defaultNode())}
                    block
                    icon={<PlusOutlined />}
                  >
                    添加节点
                  </Button>
                )}
              </>
            )}
          </Form.List>
        </Card>

        {!readOnly && (
          <Space style={{ marginTop: 16 }}>
            <Button type="primary" onClick={handleSave}>
              {isPaused ? '保存配置' : '保存草稿'}
            </Button>
            {!isNew && !isPaused && hasPermission('task:create') && (
              <Button onClick={handlePublish}>保存并发布</Button>
            )}
            {isPaused && canManage && (
              <>
                <Button type="primary" onClick={handleResume}>恢复运行</Button>
                <Button danger onClick={handleStop}>停止任务</Button>
              </>
            )}
            <Button onClick={() => navigate('/tasks')}>返回</Button>
          </Space>
        )}
        {readOnly && !isNew && (
          <Space style={{ marginTop: 16 }} wrap>
            {['published', 'in_progress'].includes(status) && canManage && (
              <>
                <Button onClick={handlePause}>暂停后可编辑</Button>
                <Button danger onClick={handleStop}>停止</Button>
              </>
            )}
            <Button onClick={() => navigate('/tasks')}>返回</Button>
          </Space>
        )}
        {hasPermission('task:config') && (
          <div style={{ marginTop: readOnly ? 0 : 8 }}>
            <Button icon={<SaveOutlined />} onClick={() => setSaveTplOpen(true)}>
              保存为模板
            </Button>
          </div>
        )}
      </Form>

      <Modal
        title="保存为任务模板"
        open={saveTplOpen}
        onOk={handleSaveAsTemplate}
        onCancel={() => setSaveTplOpen(false)}
        destroyOnClose
      >
        <Form form={tplForm} layout="vertical">
          <Form.Item name="templateName" label="模板名称" rules={[{ required: true }]}>
            <Input placeholder="如：课程大纲检查标准流程" />
          </Form.Item>
          <Form.Item name="description" label="模板说明">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
