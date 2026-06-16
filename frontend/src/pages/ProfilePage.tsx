import {
  CloudDownloadOutlined,
  LogoutOutlined,
  QuestionCircleOutlined,
  SettingOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { Avatar, Button, Card, List, Space, Typography } from 'antd';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { APP_UPDATE_CHECK_EVENT } from '@/components/AppUpdateChecker';
import IdentitySwitcher from '@/components/IdentitySwitcher';
import ServerConfigModal from '@/components/ServerConfigModal';
import { useAuth } from '@/contexts/AuthContext';
import { useBranding } from '@/contexts/BrandingContext';
import { getAppVersion } from '@/utils/appVersion';
import { isMobileApp } from '@/utils/app';

export default function ProfilePage() {
  const { user, logout, groups } = useAuth();
  const { branding } = useBranding();
  const navigate = useNavigate();
  const [configOpen, setConfigOpen] = useState(false);

  const menuItems = [
    {
      key: 'help',
      icon: <QuestionCircleOutlined />,
      label: '使用帮助',
      onClick: () => navigate('/help'),
    },
    {
      key: 'update',
      icon: <CloudDownloadOutlined />,
      label: '检查应用更新',
      onClick: () => window.dispatchEvent(new Event(APP_UPDATE_CHECK_EVENT)),
    },
    ...(isMobileApp()
      ? [
          {
            key: 'server',
            icon: <SettingOutlined />,
            label: '服务器配置',
            onClick: () => setConfigOpen(true),
          },
        ]
      : []),
  ];

  return (
    <div className="mobile-page profile-page">
      <Card bordered={false} className="profile-card">
        <Space align="center" size="middle">
          <Avatar size={64} icon={<UserOutlined />} style={{ background: branding.primaryColor }} />
          <div>
            <Typography.Title level={4} style={{ margin: 0 }}>
              {user?.name}
            </Typography.Title>
            <Typography.Text type="secondary">{user?.account}</Typography.Text>
            <Typography.Text type="secondary" style={{ display: 'block', fontSize: 13 }}>
              {user?.collegeName || '—'} · {user?.currentGroupName}
            </Typography.Text>
          </div>
        </Space>
      </Card>

      <Card title="当前身份" size="small" className="profile-section">
        <IdentitySwitcher />
      </Card>

      {groups.length > 1 && (
        <Card title="我的身份组" size="small" className="profile-section">
          {groups.map((g) => (
            <div key={g.groupId} className="profile-group-row">
              <Typography.Text>{g.groupName}</Typography.Text>
              {g.pendingCount > 0 && (
                <Typography.Text type="danger">待办 {g.pendingCount}</Typography.Text>
              )}
            </div>
          ))}
        </Card>
      )}

      <List
        className="profile-menu"
        dataSource={menuItems}
        renderItem={(item) => (
          <List.Item onClick={item.onClick} className="profile-menu-item">
            <Space>
              {item.icon}
              {item.label}
            </Space>
          </List.Item>
        )}
      />

      <div className="profile-footer">
        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
          {branding.siteName} · v{getAppVersion()}
        </Typography.Text>
        <Button
          type="default"
          danger
          block
          icon={<LogoutOutlined />}
          style={{ marginTop: 12 }}
          onClick={() => {
            logout();
            navigate('/login');
          }}
        >
          退出登录
        </Button>
      </div>

      <ServerConfigModal open={configOpen} onClose={() => setConfigOpen(false)} />
    </div>
  );
}
