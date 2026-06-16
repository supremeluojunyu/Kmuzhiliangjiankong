import {
  AppstoreOutlined,
  MailOutlined,
  MoreOutlined,
  ProfileOutlined,
  ScheduleOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { Badge, Drawer, Layout, List, Space, Spin, Typography } from 'antd';
import { useMemo, useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import MessageBell from '@/components/MessageBell';
import { useAuth } from '@/contexts/AuthContext';
import { useBranding } from '@/contexts/BrandingContext';

const { Header, Content } = Layout;

type TabKey = '/my-tasks' | '/messages' | '/profile' | 'more';

const PAGE_TITLES: Record<string, string> = {
  '/my-tasks': '我的任务',
  '/messages': '消息中心',
  '/profile': '我的',
  '/help': '使用帮助',
  '/tasks': '任务管理',
  '/users': '用户管理',
  '/groups': '组管理',
  '/settings': '系统配置',
};

export default function MobileLayout() {
  const { loading, hasPermission } = useAuth();
  const { branding, logoSrc } = useBranding();
  const navigate = useNavigate();
  const location = useLocation();
  const [moreOpen, setMoreOpen] = useState(false);

  const activeTab: TabKey = useMemo(() => {
    if (location.pathname.startsWith('/my-tasks')) return '/my-tasks';
    if (location.pathname.startsWith('/messages')) return '/messages';
    if (location.pathname.startsWith('/profile') || location.pathname.startsWith('/help')) {
      return '/profile';
    }
    if (
      location.pathname.startsWith('/tasks') ||
      location.pathname.startsWith('/users') ||
      location.pathname.startsWith('/groups') ||
      location.pathname.startsWith('/settings')
    ) {
      return 'more';
    }
    return '/my-tasks';
  }, [location.pathname]);

  const pageTitle = useMemo(() => {
    const key = Object.keys(PAGE_TITLES).find((k) => location.pathname.startsWith(k));
    return key ? PAGE_TITLES[key] : branding.siteShortName;
  }, [location.pathname, branding.siteShortName]);

  const moreItems = [
    ...(hasPermission('task:create') || hasPermission('task:config')
      ? [{ key: '/tasks', icon: <ProfileOutlined />, label: '任务管理' }]
      : []),
    ...(hasPermission('user:manage')
      ? [{ key: '/users', icon: <UserOutlined />, label: '用户管理' }]
      : []),
    ...(hasPermission('group:manage')
      ? [{ key: '/groups', icon: <TeamOutlined />, label: '组管理' }]
      : []),
  ];

  const bottomTabs: { key: TabKey; icon: React.ReactNode; label: string }[] = [
    { key: '/my-tasks', icon: <ScheduleOutlined />, label: '任务' },
    { key: '/messages', icon: <MailOutlined />, label: '消息' },
    { key: '/profile', icon: <UserOutlined />, label: '我的' },
    { key: 'more', icon: <MoreOutlined />, label: '更多' },
  ];

  if (loading) {
    return (
      <div className="center-page">
        <Spin size="large" />
      </div>
    );
  }

  return (
    <Layout className="mobile-layout">
      <Header className="mobile-header">
        <Space size={8}>
          {logoSrc ? (
            <img src={logoSrc} alt="" className="mobile-logo-img" />
          ) : (
            <AppstoreOutlined style={{ fontSize: 22, color: branding.primaryColor }} />
          )}
          <Typography.Text strong className="mobile-header-title">
            {pageTitle}
          </Typography.Text>
        </Space>
        <MessageBell />
      </Header>

      <Content className="mobile-content">
        <Outlet />
      </Content>

      <nav className="mobile-tabbar" aria-label="主导航">
        {bottomTabs.map((tab) => (
          <button
            key={tab.key}
            type="button"
            className={`mobile-tabbar-item${activeTab === tab.key ? ' active' : ''}`}
            onClick={() => {
              if (tab.key === 'more') {
                setMoreOpen(true);
              } else {
                navigate(tab.key);
              }
            }}
          >
            {tab.key === 'more' && moreItems.length > 0 ? (
              <Badge dot={activeTab === 'more'}>{tab.icon}</Badge>
            ) : (
              tab.icon
            )}
            <span>{tab.label}</span>
          </button>
        ))}
      </nav>

      <Drawer
        title="更多功能"
        placement="bottom"
        height="auto"
        className="mobile-more-drawer"
        open={moreOpen}
        onClose={() => setMoreOpen(false)}
      >
        {moreItems.length === 0 ? (
          <Typography.Text type="secondary">暂无可用管理功能</Typography.Text>
        ) : (
          <List
            dataSource={moreItems}
            renderItem={(item) => (
              <List.Item
                onClick={() => {
                  navigate(item.key);
                  setMoreOpen(false);
                }}
                style={{ cursor: 'pointer' }}
              >
                <Space>
                  {item.icon}
                  {item.label}
                </Space>
              </List.Item>
            )}
          />
        )}
      </Drawer>
    </Layout>
  );
}
