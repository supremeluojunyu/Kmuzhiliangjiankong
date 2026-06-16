import { LockOutlined, SettingOutlined, UserOutlined } from '@ant-design/icons';
import { Button, Card, Divider, Form, Input, Modal, Space, Typography, message } from 'antd';
import { useEffect, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import ServerConfigModal from '@/components/ServerConfigModal';
import { fetchPublicAuthConfig, type PublicAuthConfig } from '@/api/settings';
import { useAuth } from '@/contexts/AuthContext';
import { useBranding } from '@/contexts/BrandingContext';
import { getDefaultHomePath, isMobileApp } from '@/utils/app';
import { formatApiError, getApiBaseUrl, isNetworkError } from '@/utils/serverConfig';
import {
  enterManualServerMode,
  exitManualServerMode,
  isManualServerMode,
} from '@/utils/serverMode';

const NETWORK_FAIL_THRESHOLD = 2;

export default function LoginPage() {
  const { login, user } = useAuth();
  const { branding, logoSrc } = useBranding();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [authConfig, setAuthConfig] = useState<PublicAuthConfig | null>(null);
  const [networkFailCount, setNetworkFailCount] = useState(0);
  const [configOpen, setConfigOpen] = useState(false);
  const [manualMode, setManualMode] = useState(isManualServerMode());

  useEffect(() => {
    if (!getApiBaseUrl()) return;
    fetchPublicAuthConfig().then(setAuthConfig).catch(() => {});
  }, []);

  useEffect(() => {
    setManualMode(isManualServerMode());
  }, [configOpen]);

  if (user) {
    return <Navigate to={getDefaultHomePath()} replace />;
  }

  const openServerConfig = () => {
    enterManualServerMode();
    setManualMode(true);
    setConfigOpen(true);
  };

  const promptServerConfig = () => {
    Modal.confirm({
      title: '无法连接服务器',
      content: '多次连接失败，是否手动配置服务器地址？',
      okText: '去配置',
      cancelText: '稍后再说',
      onOk: () => openServerConfig(),
    });
  };

  const onFinish = async (values: { account: string; password: string }) => {
    if (isMobileApp() && !getApiBaseUrl()) {
      promptServerConfig();
      return;
    }
    setLoading(true);
    try {
      await login(values.account, values.password);
      setNetworkFailCount(0);
      exitManualServerMode();
      setManualMode(false);
      message.success('登录成功');
      navigate(getDefaultHomePath());
    } catch (e) {
      if (isNetworkError(e)) {
        const next = networkFailCount + 1;
        setNetworkFailCount(next);
        enterManualServerMode();
        setManualMode(true);
        if (next >= NETWORK_FAIL_THRESHOLD) {
          promptServerConfig();
        } else {
          message.error(formatApiError(e));
        }
      } else {
        message.error(formatApiError(e));
      }
    } finally {
      setLoading(false);
    }
  };

  const showLocal = !authConfig || authConfig.localLoginEnabled;
  const showExternal = authConfig?.externalAuthEnabled && !isMobileApp();

  return (
    <div className="login-page" style={{ background: branding.loginBackground || undefined }}>
      <Card className="login-card" bordered={false}>
        <Space direction="vertical" align="center" style={{ width: '100%', marginBottom: 24 }}>
          {logoSrc ? (
            <img src={logoSrc} alt="" className="login-logo" />
          ) : null}
          <Typography.Title level={3} style={{ textAlign: 'center', margin: 0 }}>
            {branding.siteName}
          </Typography.Title>
          {branding.siteSubtitle && (
            <Typography.Text type="secondary">{branding.siteSubtitle}</Typography.Text>
          )}
        </Space>

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
            <Button
              type="primary"
              htmlType="submit"
              block
              loading={loading}
              style={{ background: branding.primaryColor }}
            >
              登录
            </Button>

            {isMobileApp() && manualMode && (
              <Button
                type="link"
                block
                icon={<SettingOutlined />}
                onClick={openServerConfig}
                style={{ marginTop: 8 }}
              >
                重新配置服务器
              </Button>
            )}
          </Form>
        )}

        {showLocal && !isMobileApp() && (
          <Typography.Paragraph type="secondary" style={{ marginTop: 16, textAlign: 'center' }}>
            默认账号：admin / admin123
          </Typography.Paragraph>
        )}
      </Card>

      <ServerConfigModal
        open={configOpen}
        onClose={() => setConfigOpen(false)}
        onSaved={() => {
          enterManualServerMode();
          setManualMode(true);
          setNetworkFailCount(0);
        }}
      />
    </div>
  );
}
