import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { Button, Card, Divider, Form, Input, Space, Typography, message } from 'antd';
import { useEffect, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { fetchPublicAuthConfig, type PublicAuthConfig } from '@/api/settings';
import { useAuth } from '@/contexts/AuthContext';

export default function LoginPage() {
  const { login, user } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [authConfig, setAuthConfig] = useState<PublicAuthConfig | null>(null);

  useEffect(() => {
    fetchPublicAuthConfig().then(setAuthConfig).catch(() => {});
  }, []);

  if (user) {
    return <Navigate to="/" replace />;
  }

  const onFinish = async (values: { account: string; password: string }) => {
    setLoading(true);
    try {
      await login(values.account, values.password);
      message.success('登录成功');
      navigate('/');
    } catch (e) {
      message.error(e instanceof Error ? e.message : '登录失败');
    } finally {
      setLoading(false);
    }
  };

  const showLocal = !authConfig || authConfig.localLoginEnabled;
  const showExternal = authConfig?.externalAuthEnabled;

  return (
    <div className="login-page">
      <Card className="login-card" bordered={false}>
        <Typography.Title level={3} style={{ textAlign: 'center', marginBottom: 32 }}>
          高校质量监控管理信息系统
        </Typography.Title>

        {showExternal && (
          <>
            <Space direction="vertical" style={{ width: '100%', marginBottom: 16 }}>
              {authConfig?.casLoginUrl && (
                <Button block size="large" href={authConfig.casLoginUrl}>
                  CAS 统一认证登录
                </Button>
              )}
              {authConfig?.oauthLoginUrl && (
                <Button block size="large" href={authConfig.oauthLoginUrl}>
                  OAuth2 统一认证登录
                </Button>
              )}
            </Space>
            {showLocal && <Divider plain>或使用本地账号</Divider>}
          </>
        )}

        {showLocal && (
          <Form layout="vertical" onFinish={onFinish} size="large">
            <Form.Item name="account" rules={[{ required: true, message: '请输入账号' }]}>
              <Input prefix={<UserOutlined />} placeholder="账号" />
            </Form.Item>
            <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
              <Input.Password prefix={<LockOutlined />} placeholder="密码" />
            </Form.Item>
            <Button type="primary" htmlType="submit" block loading={loading}>
              登录
            </Button>
          </Form>
        )}

        {showLocal && (
          <Typography.Paragraph type="secondary" style={{ marginTop: 16, textAlign: 'center' }}>
            默认账号：admin / admin123
          </Typography.Paragraph>
        )}
      </Card>
    </div>
  );
}
