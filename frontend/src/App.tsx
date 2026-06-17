import { BrowserRouter, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { AuthProvider, useAuth } from '@/contexts/AuthContext';
import { BrandingProvider } from '@/contexts/BrandingContext';
import AppLayout from '@/layouts/AppLayout';
import MobileLayout from '@/layouts/MobileLayout';
import DashboardPage from '@/pages/DashboardPage';
import LoginPage from '@/pages/LoginPage';
import UsersPage from '@/pages/UsersPage';
import GroupsPage from '@/pages/GroupsPage';
import CollegesPage from '@/pages/CollegesPage';
import OperationLogsPage from '@/pages/OperationLogsPage';
import MessagesPage from '@/pages/MessagesPage';
import MyTasksPage from '@/pages/MyTasksPage';
import TaskExecutePage from '@/pages/TaskExecutePage';
import TaskStatPage from '@/pages/TaskStatPage';
import TaskTemplatesPage from '@/pages/TaskTemplatesPage';
import TaskEditPage from '@/pages/TaskEditPage';
import HelpPage from '@/pages/HelpPage';
import ProfilePage from '@/pages/ProfilePage';
import SystemSettingsPage from '@/pages/SystemSettingsPage';
import AuthCallbackPage from '@/pages/AuthCallbackPage';
import TasksPage from '@/pages/TasksPage';
import DownloadPage from '@/pages/DownloadPage';
import AppUpdateChecker from '@/components/AppUpdateChecker';
import GuestOnly from '@/components/GuestOnly';
import RequireAuth from '@/components/RequireAuth';
import { Spin } from 'antd';
import { getDefaultHomePath, isMobileApp } from '@/utils/app';
import { useViewport } from '@/hooks/useViewport';
import { buildLoginUrl } from '@/utils/authRedirect';
import { getToken } from '@/api/client';

function AppShell() {
  const { isNarrow } = useViewport();
  if (isMobileApp() || isNarrow) {
    return <MobileLayout />;
  }
  return <AppLayout />;
}

function IndexPage() {
  const { isNarrow } = useViewport();
  if (isMobileApp() || isNarrow) {
    return <Navigate to="/my-tasks" replace />;
  }
  return <DashboardPage />;
}

function RootRedirect() {
  const { isNarrow } = useViewport();
  const { user, loading } = useAuth();
  if (loading) {
    return (
      <div className="center-page">
        <Spin size="large" />
      </div>
    );
  }
  if (!user || !getToken()) {
    return <Navigate to="/login" replace />;
  }
  const home = isMobileApp() || isNarrow ? '/my-tasks' : getDefaultHomePath();
  return <Navigate to={home} replace />;
}

function UnknownRouteGuard() {
  const location = useLocation();
  const from = location.pathname + location.search;
  if (!getToken()) {
    return <Navigate to={buildLoginUrl(from)} replace />;
  }
  return (
    <RequireAuth>
      <RootRedirect />
    </RequireAuth>
  );
}

function AppRoutes() {
  return (
    <Routes>
      <Route
        path="/login"
        element={
          <GuestOnly>
            <LoginPage />
          </GuestOnly>
        }
      />
      <Route path="/login/callback" element={<AuthCallbackPage />} />
      <Route path="/download" element={<DownloadPage />} />
      <Route
        path="/"
        element={
          <RequireAuth>
            <AppShell />
          </RequireAuth>
        }
      >
        <Route index element={<IndexPage />} />
        <Route path="messages" element={<MessagesPage />} />
        <Route path="profile" element={<ProfilePage />} />
        <Route path="help" element={<HelpPage />} />
        <Route path="my-tasks" element={<MyTasksPage />} />
        <Route path="my-tasks/:instanceId" element={<TaskExecutePage />} />
        <Route path="tasks" element={<TasksPage />} />
        <Route path="tasks/templates" element={<TaskTemplatesPage />} />
        <Route path="tasks/:taskId/stats" element={<TaskStatPage />} />
        <Route path="tasks/:taskId" element={<TaskEditPage />} />
        <Route path="users" element={<UsersPage />} />
        <Route path="groups" element={<GroupsPage />} />
        <Route path="colleges" element={<CollegesPage />} />
        <Route path="logs" element={<OperationLogsPage />} />
        <Route path="settings" element={<SystemSettingsPage />} />
      </Route>
      <Route path="*" element={<UnknownRouteGuard />} />
    </Routes>
  );
}

export default function App() {
  return (
    <BrandingProvider>
      <AuthProvider>
        {isMobileApp() && <AppUpdateChecker />}
        <BrowserRouter>
          <AppRoutes />
        </BrowserRouter>
      </AuthProvider>
    </BrandingProvider>
  );
}
