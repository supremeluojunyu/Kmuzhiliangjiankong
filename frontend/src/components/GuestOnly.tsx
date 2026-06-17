import { Spin } from 'antd';
import { Navigate, useSearchParams } from 'react-router-dom';
import { getToken } from '@/api/client';
import { useAuth } from '@/contexts/AuthContext';
import { getDefaultHomePath } from '@/utils/app';
import { getSafeRedirectPath } from '@/utils/authRedirect';

/** 仅未登录可访问（登录页、回调页） */
export default function GuestOnly({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth();
  const [searchParams] = useSearchParams();

  if (loading) {
    return (
      <div className="center-page">
        <Spin size="large" />
      </div>
    );
  }

  if (user && getToken()) {
    const target = getSafeRedirectPath(searchParams.get('redirect')) || getDefaultHomePath();
    return <Navigate to={target} replace />;
  }

  return <>{children}</>;
}
