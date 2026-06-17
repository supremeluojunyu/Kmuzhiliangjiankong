import {
  DashboardOutlined,
  FileTextOutlined,
  LogoutOutlined,
  MailOutlined,
  ProfileOutlined,
  ScheduleOutlined,
  QuestionCircleOutlined,
  SettingOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { Layout, Menu, Typography, Button, Space, Spin } from 'antd';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import IdentitySwitcher from '@/components/IdentitySwitcher';
import MessageBell from '@/components/MessageBell';
import { useAuth } from '@/contexts/AuthContext';
import { useBranding } from '@/contexts/BrandingContext';
import { isMobileApp } from '@/utils/app';

const { Header, Sider, Content } = Layout;

export default function AppLayout() {
  const { user, logout, loading, hasPermission } = useAuth();
  const { branding, logoSrc } = useBranding();
  const navigate = useNavigate();
  const location = useLocation();

  if (loading) {
    return (
      <div className="center-page">
        <Spin size="large" />
      </div>
    );
  }

  const mobileMode = isMobileApp();
  const canViewLogs = hasPermission('user:manage')
    || hasPermission('group:manage')
    || hasPermission('system:config');

  const menuItems = [
    ...(!mobileMode ? [{ key: '/', icon: <DashboardOutlined />, label: '工作台' }] : []),
    { key: '/my-tasks', icon: <ScheduleOutlined />, label: '我的任务' },
    { key: '/messages', icon: <MailOutlined />, label: '消息中心' },
    { key: '/help', icon: <QuestionCircleOutlined />, label: '使用帮助' },
    ...(hasPermission('task:create') || hasPermission('task:config')
      ? [{ key: '/tasks', icon: <ProfileOutlined />, label: '任务管理' }]
      : []),
    ...(hasPermission('user:manage')
      ? [{ key: '/users', icon: <UserOutlined />, label: '用户管理' }]
      : []),
    ...(hasPermission('group:manage')
      ? [{ key: '/groups', icon: <TeamOutlined />, label: '组管理' }]
      : []),
    ...(canViewLogs && !mobileMode
      ? [{ key: '/logs', icon: <FileTextOutlined />, label: '操作日志' }]
      : []),
    ...(hasPermission('system:config')
      ? [{ key: '/settings', icon: <SettingOutlined />, label: '系统配置' }]
      : []),
  ];

  return (
    <Layout className="app-layout">
      <Sider breakpoint="lg" collapsedWidth={64} theme="dark">
        <div className="logo">
          {logoSrc ? (
            <img src={logoSrc} alt="" className="sidebar-logo-img" />
          ) : (
            branding.siteShortName
          )}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[
            location.pathname.startsWith('/tasks') ? '/tasks'
              : location.pathname.startsWith('/my-tasks') ? '/my-tasks'
              : location.pathname.startsWith('/logs') ? '/logs'
              : location.pathname.startsWith('/settings') ? '/settings'
              : location.pathname.startsWith('/help') ? '/help'
              : location.pathname,
          ]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <Layout>
        <Header className="app-header">
          <Typography.Text strong>
            {user?.currentGroupName} · {user?.name}
          </Typography.Text>
          <Space>
            <MessageBell />
            <IdentitySwitcher />
            <Button type="text" icon={<LogoutOutlined />} onClick={() => { logout(); navigate('/login'); }}>
              退出
            </Button>
          </Space>
        </Header>
        <Content className="app-content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
