import { Spin } from 'antd';
import { useEffect, useState } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { getToken } from '@/api/client';
import { useAuth } from '@/contexts/AuthContext';

/** 受保护路由：每次进入都向后端校验登录态，未登录强制跳转登录页 */
export default function RequireAuth({ children }: { children: React.ReactNode }) {
  const { user, loading, validateSession } = useAuth();
  const location = useLocation();
  const [verifying, setVerifying] = useState(true);

  useEffect(() => {
    let active = true;
    setVerifying(true);
    validateSession()
      .catch(() => {})
      .finally(() => {
        if (active) setVerifying(false);
      });
    return () => {
      active = false;
    };
  }, [location.pathname, validateSession]);

  if (loading || verifying) {
    return (
      <div className="center-page">
        <Spin size="large" tip="验证登录状态…" />
      </div>
    );
  }

  if (!user || !getToken()) {
    const redirect = encodeURIComponent(location.pathname + location.search);
    return <Navigate to={`/login?redirect=${redirect}`} replace />;
  }

  return <>{children}</>;
}
