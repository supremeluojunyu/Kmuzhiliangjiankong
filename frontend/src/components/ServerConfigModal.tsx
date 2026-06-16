import { Button, Form, Input, Modal, Space, Typography, message } from 'antd';
import { useState } from 'react';
import { clearCustomServer, setCustomServer, testServerConnection } from '@/utils/serverConfig';

interface ServerConfigModalProps {
  open: boolean;
  onClose: () => void;
  onSaved?: () => void;
}

export default function ServerConfigModal({ open, onClose, onSaved }: ServerConfigModalProps) {
  const [form] = Form.useForm<{ serverUrl: string }>();
  const [testing, setTesting] = useState(false);
  const [saving, setSaving] = useState(false);

  const handleClose = () => {
    form.resetFields();
    onClose();
  };

  const handleTest = async () => {
    const url = form.getFieldValue('serverUrl')?.trim();
    if (!url) {
      message.warning('请输入服务器地址');
      return;
    }
    setTesting(true);
    try {
      await testServerConnection(url);
      message.success('连接成功');
    } catch (e) {
      message.error(e instanceof Error ? e.message : '连接失败');
    } finally {
      setTesting(false);
    }
  };

  const handleSave = async () => {
    const values = await form.validateFields();
    const url = values.serverUrl.trim();
    setSaving(true);
    try {
      await testServerConnection(url);
      setCustomServer(url);
      message.success('服务器配置已保存');
      form.resetFields();
      onSaved?.();
      onClose();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '保存失败');
    } finally {
      setSaving(false);
    }
  };

  const handleClear = () => {
    Modal.confirm({
      title: '清除自定义配置',
      content: '清除后将使用应用内置默认服务器，确定继续？',
      onOk: () => {
        clearCustomServer();
        form.resetFields();
        message.success('已恢复默认配置');
        onSaved?.();
      },
    });
  };

  return (
    <Modal
      title="配置服务器"
      open={open}
      onCancel={handleClose}
      destroyOnClose
      footer={null}
    >
      <Typography.Paragraph type="secondary" style={{ marginBottom: 16 }}>
        请输入新的服务器地址。已保存的配置不会在界面中显示。
      </Typography.Paragraph>
      <Form form={form} layout="vertical">
        <Form.Item
          name="serverUrl"
          label="服务器地址"
          rules={[
            { required: true, message: '请输入服务器地址' },
            { pattern: /^https?:\/\/.+/i, message: '需以 http:// 或 https:// 开头' },
          ]}
        >
          <Input placeholder="例如：http://124.220.4.69:5555（不要加 /api）" autoComplete="off" />
        </Form.Item>
        <Space wrap>
          <Button onClick={handleTest} loading={testing}>
            测试连接
          </Button>
          <Button type="primary" onClick={handleSave} loading={saving}>
            保存
          </Button>
          <Button type="link" danger onClick={handleClear}>
            清除自定义配置
          </Button>
        </Space>
      </Form>
    </Modal>
  );
}
