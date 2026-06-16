import {
  Alert,
  Button,
  Card,
  Col,
  Divider,
  Form,
  Input,
  InputNumber,
  Row,
  Select,
  Space,
  Switch,
  Tabs,
  Typography,
  message,
} from 'antd';
import { useEffect, useState } from 'react';
import { fetchAllGroups } from '@/api/group';
import {
  fetchSystemSettings,
  runDeadlineRemind,
  runOverdueCheck,
  runRetentionCleanup,
  testNotification,
  testStorageConnection,
  updateSystemSettings,
  type SystemSettings,
} from '@/api/settings';
import { useAuth } from '@/contexts/AuthContext';
import { Navigate } from 'react-router-dom';

export default function SystemSettingsPage() {
  const { hasPermission } = useAuth();
  const [form] = Form.useForm<SystemSettings>();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [groups, setGroups] = useState<{ groupId: number; groupName: string }[]>([]);

  const load = async () => {
    setLoading(true);
    try {
      const settings = await fetchSystemSettings();
      form.setFieldsValue(settings);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '加载失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    fetchAllGroups().then(setGroups).catch(() => {});
  }, []);

  const handleSave = async () => {
    const values = await form.validateFields();
    setSaving(true);
    try {
      await updateSystemSettings(values);
      message.success('配置已保存');
      load();
    } catch (e) {
      message.error(e instanceof Error ? e.message : '保存失败');
    } finally {
      setSaving(false);
    }
  };

  const provider = Form.useWatch(['auth', 'provider'], form);
  const emailEnabled = Form.useWatch(['notification', 'emailEnabled'], form);
  const wechatEnabled = Form.useWatch(['notification', 'wechatEnabled'], form);
  const storageType = Form.useWatch(['storage', 'type'], form);

  if (!hasPermission('system:config')) {
    return <Navigate to="/" replace />;
  }

  return (
    <div>
      <Space style={{ marginBottom: 16, width: '100%', justifyContent: 'space-between' }}>
        <Typography.Title level={4} style={{ margin: 0 }}>
          系统配置
        </Typography.Title>
        <Button type="primary" loading={saving} onClick={handleSave}>
          保存全部配置
        </Button>
      </Space>

      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message="仅系统管理员可修改。密钥类字段留空或显示 ****** 时将保留原值。"
      />

      <Form form={form} layout="vertical" disabled={loading}>
        <Tabs
          items={[
            {
              key: 'auth',
              label: '统一认证',
              children: (
                <Card>
                  <Row gutter={16}>
                    <Col span={8}>
                      <Form.Item name={['auth', 'enabled']} label="启用外部认证" valuePropName="checked">
                        <Switch />
                      </Form.Item>
                    </Col>
                    <Col span={8}>
                      <Form.Item name={['auth', 'localLoginEnabled']} label="保留本地账号登录" valuePropName="checked">
                        <Switch />
                      </Form.Item>
                    </Col>
                    <Col span={8}>
                      <Form.Item name={['auth', 'autoProvision']} label="外部用户自动建档" valuePropName="checked">
                        <Switch />
                      </Form.Item>
                    </Col>
                  </Row>
                  <Row gutter={16}>
                    <Col span={8}>
                      <Form.Item name={['auth', 'provider']} label="认证方式">
                        <Select options={[
                          { value: 'local', label: '仅本地' },
                          { value: 'cas', label: 'CAS 单点登录' },
                          { value: 'oauth2', label: 'OAuth2 / OIDC' },
                        ]} />
                      </Form.Item>
                    </Col>
                    <Col span={8}>
                      <Form.Item name={['auth', 'defaultGroupId']} label="自动建档默认组">
                        <Select options={groups.map((g) => ({ value: g.groupId, label: g.groupName }))} />
                      </Form.Item>
                    </Col>
                    <Col span={8}>
                      <Form.Item name={['auth', 'frontendBaseUrl']} label="前端地址（回调跳转）">
                        <Input placeholder="http://localhost:5173" />
                      </Form.Item>
                    </Col>
                  </Row>

                  {provider === 'cas' && (
                    <>
                      <Divider>CAS 配置</Divider>
                      <Row gutter={16}>
                        <Col span={12}>
                          <Form.Item name={['auth', 'casServerUrl']} label="CAS 服务器地址">
                            <Input placeholder="https://cas.example.edu/cas" />
                          </Form.Item>
                        </Col>
                        <Col span={6}>
                          <Form.Item name={['auth', 'casLoginPath']} label="登录路径">
                            <Input placeholder="/login" />
                          </Form.Item>
                        </Col>
                        <Col span={6}>
                          <Form.Item name={['auth', 'serviceUrl']} label="Service URL（可选）">
                            <Input placeholder="后端回调地址，留空自动生成" />
                          </Form.Item>
                        </Col>
                      </Row>
                    </>
                  )}

                  {provider === 'oauth2' && (
                    <>
                      <Divider>OAuth2 / OIDC 配置</Divider>
                      <Row gutter={16}>
                        <Col span={12}>
                          <Form.Item name={['auth', 'oauthIssuer']} label="Issuer 地址">
                            <Input placeholder="https://idp.example.edu" />
                          </Form.Item>
                        </Col>
                        <Col span={12}>
                          <Form.Item name={['auth', 'oauthRedirectUri']} label="Redirect URI">
                            <Input placeholder="http://localhost:8080/api/auth/oauth2/callback" />
                          </Form.Item>
                        </Col>
                        <Col span={8}>
                          <Form.Item name={['auth', 'oauthClientId']} label="Client ID">
                            <Input />
                          </Form.Item>
                        </Col>
                        <Col span={8}>
                          <Form.Item name={['auth', 'oauthClientSecret']} label="Client Secret">
                            <Input.Password placeholder="留空保留原密钥" />
                          </Form.Item>
                        </Col>
                        <Col span={8}>
                          <Form.Item name={['auth', 'oauthScope']} label="Scope">
                            <Input placeholder="openid profile email" />
                          </Form.Item>
                        </Col>
                      </Row>
                    </>
                  )}
                </Card>
              ),
            },
            {
              key: 'notification',
              label: '消息通知',
              children: (
                <Card>
                  <Typography.Title level={5}>邮件通知</Typography.Title>
                  <Row gutter={16}>
                    <Col span={6}>
                      <Form.Item name={['notification', 'emailEnabled']} label="启用邮件" valuePropName="checked">
                        <Switch />
                      </Form.Item>
                    </Col>
                  </Row>
                  {emailEnabled && (
                    <Row gutter={16}>
                      <Col span={8}><Form.Item name={['notification', 'smtpHost']} label="SMTP 主机"><Input /></Form.Item></Col>
                      <Col span={4}><Form.Item name={['notification', 'smtpPort']} label="端口"><InputNumber style={{ width: '100%' }} /></Form.Item></Col>
                      <Col span={6}><Form.Item name={['notification', 'smtpUsername']} label="用户名"><Input /></Form.Item></Col>
                      <Col span={6}><Form.Item name={['notification', 'smtpPassword']} label="密码"><Input.Password placeholder="留空保留" /></Form.Item></Col>
                      <Col span={8}><Form.Item name={['notification', 'smtpFrom']} label="发件人"><Input placeholder="noreply@school.edu" /></Form.Item></Col>
                      <Col span={4}><Form.Item name={['notification', 'smtpSsl']} label="TLS" valuePropName="checked"><Switch /></Form.Item></Col>
                    </Row>
                  )}

                  <Divider />
                  <Typography.Title level={5}>企业微信通知</Typography.Title>
                  <Row gutter={16}>
                    <Col span={6}>
                      <Form.Item name={['notification', 'wechatEnabled']} label="启用企微" valuePropName="checked">
                        <Switch />
                      </Form.Item>
                    </Col>
                  </Row>
                  {wechatEnabled && (
                    <Row gutter={16}>
                      <Col span={8}><Form.Item name={['notification', 'wechatCorpId']} label="Corp ID"><Input /></Form.Item></Col>
                      <Col span={8}><Form.Item name={['notification', 'wechatAgentId']} label="Agent ID"><Input /></Form.Item></Col>
                      <Col span={8}><Form.Item name={['notification', 'wechatSecret']} label="Secret"><Input.Password placeholder="留空保留" /></Form.Item></Col>
                    </Row>
                  )}

                  <Divider />
                  <Typography.Title level={5}>通知场景</Typography.Title>
                  <Row gutter={16}>
                    <Col span={8}>
                      <Form.Item name={['notification', 'notifyOnTaskPublish']} label="任务发布时通知" valuePropName="checked">
                        <Switch />
                      </Form.Item>
                    </Col>
                    <Col span={8}>
                      <Form.Item name={['notification', 'notifyOnMessageBroadcast']} label="组广播时通知" valuePropName="checked">
                        <Switch />
                      </Form.Item>
                    </Col>
                    <Col span={8}>
                      <Form.Item name={['notification', 'notifyOnDeadline']} label="截止提醒" valuePropName="checked">
                        <Switch />
                      </Form.Item>
                    </Col>
                    <Col span={8}>
                      <Form.Item name={['notification', 'deadlineRemindDays']} label="提前提醒天数">
                        <InputNumber min={1} max={30} style={{ width: '100%' }} />
                      </Form.Item>
                    </Col>
                  </Row>

                  <Space wrap>
                    <Button onClick={async () => {
                      const email = prompt('测试邮箱地址');
                      if (!email) return;
                      try {
                        await testNotification('email', email);
                        message.success('测试邮件已发送');
                      } catch (e) {
                        message.error(e instanceof Error ? e.message : '发送失败');
                      }
                    }}>测试邮件</Button>
                    <Button onClick={async () => {
                      const uid = prompt('企业微信 UserId');
                      if (!uid) return;
                      try {
                        await testNotification('wechat', uid);
                        message.success('测试企微消息已发送');
                      } catch (e) {
                        message.error(e instanceof Error ? e.message : '发送失败');
                      }
                    }}>测试企微</Button>
                    <Button onClick={async () => {
                      try {
                        const summary = await runDeadlineRemind();
                        message.success(summary);
                      } catch (e) {
                        message.error(e instanceof Error ? e.message : '执行失败');
                      }
                    }}>立即发送截止提醒</Button>
                    <Button onClick={async () => {
                      try {
                        const summary = await runOverdueCheck();
                        message.success(summary);
                      } catch (e) {
                        message.error(e instanceof Error ? e.message : '执行失败');
                      }
                    }}>立即检测逾期</Button>
                  </Space>
                </Card>
              ),
            },
            {
              key: 'storage',
              label: '文件存储',
              children: (
                <Card>
                  <Alert
                    type="warning"
                    showIcon
                    style={{ marginBottom: 16 }}
                    message="修改存储配置后，新上传文件将写入新存储。已有文件路径不会自动迁移。"
                  />
                  <Row gutter={16}>
                    <Col span={8}>
                      <Form.Item name={['storage', 'type']} label="存储类型">
                        <Select options={[
                          { value: 'local', label: '本地磁盘' },
                          { value: 's3', label: 'S3 兼容（MinIO / OSS）' },
                        ]} />
                      </Form.Item>
                    </Col>
                  </Row>
                  {storageType === 'local' && (
                    <Form.Item name={['storage', 'localPath']} label="本地目录">
                      <Input placeholder="./data/uploads 或绝对路径" />
                    </Form.Item>
                  )}
                  {storageType === 's3' && (
                    <Row gutter={16}>
                      <Col span={12}>
                        <Form.Item name={['storage', 's3Endpoint']} label="Endpoint">
                          <Input placeholder="http://minio:9000" />
                        </Form.Item>
                      </Col>
                      <Col span={6}>
                        <Form.Item name={['storage', 's3Region']} label="Region">
                          <Input placeholder="us-east-1" />
                        </Form.Item>
                      </Col>
                      <Col span={6}>
                        <Form.Item name={['storage', 's3Bucket']} label="Bucket">
                          <Input />
                        </Form.Item>
                      </Col>
                      <Col span={8}>
                        <Form.Item name={['storage', 's3AccessKey']} label="Access Key">
                          <Input />
                        </Form.Item>
                      </Col>
                      <Col span={8}>
                        <Form.Item name={['storage', 's3SecretKey']} label="Secret Key">
                          <Input.Password placeholder="留空保留" />
                        </Form.Item>
                      </Col>
                      <Col span={8}>
                        <Form.Item name={['storage', 's3PathStyleAccess']} label="路径风格访问" valuePropName="checked">
                          <Switch />
                        </Form.Item>
                      </Col>
                    </Row>
                  )}
                  <Button onClick={async () => {
                    try {
                      await handleSave();
                      const msg = await testStorageConnection();
                      message.success(msg);
                    } catch (e) {
                      message.error(e instanceof Error ? e.message : '连接失败');
                    }
                  }}>保存并测试连接</Button>
                </Card>
              ),
            },
            {
              key: 'retention',
              label: '数据保留',
              children: (
                <Card>
                  <Row gutter={16}>
                    <Col span={6}>
                      <Form.Item name={['retention', 'enabled']} label="启用自动清理" valuePropName="checked">
                        <Switch />
                      </Form.Item>
                    </Col>
                    <Col span={6}>
                      <Form.Item name={['retention', 'runHour']} label="每日执行小时 (0-23)">
                        <InputNumber min={0} max={23} style={{ width: '100%' }} />
                      </Form.Item>
                    </Col>
                  </Row>
                  <Row gutter={16}>
                    <Col span={8}>
                      <Form.Item name={['retention', 'taskDataYears']} label="已完成任务实例保留（年）">
                        <InputNumber min={1} max={20} style={{ width: '100%' }} />
                      </Form.Item>
                    </Col>
                    <Col span={8}>
                      <Form.Item name={['retention', 'messageDataYears']} label="消息记录保留（年）">
                        <InputNumber min={1} max={20} style={{ width: '100%' }} />
                      </Form.Item>
                    </Col>
                    <Col span={8}>
                      <Form.Item name={['retention', 'logDataYears']} label="操作日志保留（年）">
                        <InputNumber min={1} max={20} style={{ width: '100%' }} />
                      </Form.Item>
                    </Col>
                  </Row>
                  <Form.Item name={['retention', 'lastRunSummary']} label="上次执行结果">
                    <Input.TextArea rows={2} readOnly />
                  </Form.Item>
                  <Button onClick={async () => {
                    try {
                      const summary = await runRetentionCleanup();
                      message.success(summary);
                      load();
                    } catch (e) {
                      message.error(e instanceof Error ? e.message : '执行失败');
                    }
                  }}>立即执行清理</Button>
                </Card>
              ),
            },
          ]}
        />
      </Form>
    </div>
  );
}
