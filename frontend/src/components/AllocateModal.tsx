import { Form, InputNumber, Modal, Radio, Select, message } from 'antd';
import { useEffect, useState } from 'react';
import { fetchColleges } from '@/api/auth';
import { fetchAllGroups } from '@/api/group';
import { allocateTask, type AllocateRequest } from '@/api/task';
import { useAuth } from '@/contexts/AuthContext';
import type { College } from '@/types';

interface Props {
  open: boolean;
  taskId: number;
  defaultTargetGroupId?: number;
  onClose: () => void;
  onSuccess: () => void;
}

export default function AllocateModal({ open, taskId, defaultTargetGroupId, onClose, onSuccess }: Props) {
  const { isCollegeScoped } = useAuth();
  const [form] = Form.useForm();
  const [colleges, setColleges] = useState<College[]>([]);
  const [groups, setGroups] = useState<{ groupId: number; groupName: string }[]>([]);
  const [loading, setLoading] = useState(false);
  const allocationType = Form.useWatch('allocationType', form);

  useEffect(() => {
    if (open) {
      fetchColleges().then(setColleges).catch(() => {});
      fetchAllGroups().then(setGroups).catch(() => {});
      form.resetFields();
      form.setFieldsValue({
        allocationType: 'manual',
        targetGroupId: defaultTargetGroupId ?? 5,
        collegeIds: colleges.map((c) => c.collegeId),
      });
    }
  }, [open, form, defaultTargetGroupId]);

  useEffect(() => {
    if (open && colleges.length > 0) {
      form.setFieldValue('collegeIds', colleges.map((c) => c.collegeId));
    }
  }, [open, colleges, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    setLoading(true);
    try {
      const payload: AllocateRequest = {
        taskId,
        allocationType: values.allocationType,
        targetGroupId: values.targetGroupId,
        collegeIds: values.collegeIds,
        totalInstances: values.totalInstances,
      };
      const result = await allocateTask(payload);
      message.success(`分配成功，共生成 ${result.createdCount} 个任务实例`);
      onSuccess();
      onClose();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '分配失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title="分配任务"
      open={open}
      onOk={handleOk}
      onCancel={onClose}
      confirmLoading={loading}
      destroyOnClose
      width={520}
    >
      <Form form={form} layout="vertical">
        <Form.Item name="allocationType" label="分配方式" rules={[{ required: true }]}>
          <Radio.Group>
            <Radio value="manual">手动分配（每用户1实例）</Radio>
            <Radio value="by_college">按学院全量</Radio>
            <Radio value="random">随机分配</Radio>
            <Radio value="by_total">按总量分配</Radio>
          </Radio.Group>
        </Form.Item>
        <Form.Item name="targetGroupId" label="目标执行组" rules={[{ required: true }]}>
          <Select options={groups.map((g) => ({ value: g.groupId, label: g.groupName }))} />
        </Form.Item>
        <Form.Item name="collegeIds" label="学院范围" rules={[{ required: true, message: '请选择学院' }]}>
          <Select
            mode="multiple"
            disabled={isCollegeScoped()}
            options={colleges.map((c) => ({ value: c.collegeId, label: c.collegeName }))}
          />
        </Form.Item>
        {(allocationType === 'random' || allocationType === 'by_total') && (
          <Form.Item
            name="totalInstances"
            label="任务实例总数"
            rules={[{ required: allocationType === 'by_total', message: '请输入总量' }]}
          >
            <InputNumber min={1} style={{ width: '100%' }} placeholder="随机/按总量分配时的实例数" />
          </Form.Item>
        )}
      </Form>
    </Modal>
  );
}
