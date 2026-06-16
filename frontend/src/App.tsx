import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider, useAuth } from '@/contexts/AuthContext';
import { BrandingProvider } from '@/contexts/BrandingContext';
import AppLayout from '@/layouts/AppLayout';
import MobileLayout from '@/layouts/MobileLayout';
import DashboardPage from '@/pages/DashboardPage';
import LoginPage from '@/pages/LoginPage';
import UsersPage from '@/pages/UsersPage';
import GroupsPage from '@/pages/GroupsPage';
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
import { Spin } from 'antd';
import { getDefaultHomePath, isMobileApp } from '@/utils/app';

function PrivateRoute({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth();
  if (loading) {
    return (
      <div className="center-page">
        <Spin size="large" />
      </div>
    );
  }
  if (!user) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

function AppShell() {
  return isMobileApp() ? <MobileLayout /> : <AppLayout />;
}

function AppRoutes() {
  const homePath = getDefaultHomePath();

  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/login/callback" element={<AuthCallbackPage />} />
      <Route path="/download" element={<DownloadPage />} />
      <Route
        path="/"
        element={
          <PrivateRoute>
            <AppShell />
          </PrivateRoute>
        }
      >
        <Route index element={isMobileApp() ? <Navigate to={homePath} replace /> : <DashboardPage />} />
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
        <Route path="logs" element={<OperationLogsPage />} />
        <Route path="settings" element={<SystemSettingsPage />} />
      </Route>
      <Route path="*" element={<Navigate to={homePath} replace />} />
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
