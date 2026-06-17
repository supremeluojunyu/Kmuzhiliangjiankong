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
import { useViewport } from '@/hooks/useViewport';
import { isMobileApp } from '@/utils/app';

const { Header, Sider, Content } = Layout;

export default function AppLayout() {
  const { user, logout, loading, hasPermission } = useAuth();
  const { branding, logoSrc } = useBranding();
  const navigate = useNavigate();
  const location = useLocation();
  const { isCompact, isShort } = useViewport();

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

  const mainMenuItems = [
    ...(!mobileMode ? [{ key: '/', icon: <DashboardOutlined />, label: '工作台' }] : []),
    { key: '/my-tasks', icon: <ScheduleOutlined />, label: '我的任务' },
    { key: '/messages', icon: <MailOutlined />, label: '消息中心' },
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
  ];

  const footerMenuItems = [
    { key: '/help', icon: <QuestionCircleOutlined />, label: '使用帮助' },
    ...(hasPermission('system:config')
      ? [{ key: '/settings', icon: <SettingOutlined />, label: '系统配置' }]
      : []),
  ];

  const selectedKey = location.pathname.startsWith('/tasks') ? '/tasks'
    : location.pathname.startsWith('/my-tasks') ? '/my-tasks'
    : location.pathname.startsWith('/logs') ? '/logs'
    : location.pathname.startsWith('/settings') ? '/settings'
    : location.pathname.startsWith('/help') ? '/help'
    : location.pathname;

  return (
    <Layout
      className={`app-layout${isCompact ? ' app-layout--compact' : ''}${isShort ? ' app-layout--short' : ''}`}
    >
      <Sider
        breakpoint="lg"
        collapsedWidth={56}
        width={220}
        theme="dark"
        className="app-sider"
      >
        <div className="logo">
          {logoSrc ? (
            <img src={logoSrc} alt="" className="sidebar-logo-img" />
          ) : (
            branding.siteShortName
          )}
        </div>
        <div className="sidebar-body">
          <Menu
            theme="dark"
            mode="inline"
            selectedKeys={[selectedKey]}
            items={mainMenuItems}
            onClick={({ key }) => navigate(key)}
            className="sidebar-menu sidebar-menu-main"
          />
        </div>
        <div className="sidebar-footer">
          <Menu
            theme="dark"
            mode="inline"
            selectedKeys={[selectedKey]}
            items={footerMenuItems}
            onClick={({ key }) => navigate(key)}
            className="sidebar-menu sidebar-menu-footer"
          />
        </div>
      </Sider>
      <Layout className="app-main">
        <Header className="app-header">
          <Typography.Text strong className="app-header-user" title={`${user?.currentGroupName} · ${user?.name}`}>
            {user?.currentGroupName} · {user?.name}
          </Typography.Text>
          <Space wrap className="app-header-actions">
            <MessageBell />
            <IdentitySwitcher />
            <Button type="text" icon={<LogoutOutlined />} onClick={() => { logout(); navigate('/login'); }}>
              <span className="app-header-logout-text">退出</span>
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
