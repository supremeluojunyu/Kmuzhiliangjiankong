import { Spin, Typography, message } from 'antd';
import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { setStoredGroupId, setToken } from '@/api/client';
import { fetchProfile } from '@/api/auth';

export default function AuthCallbackPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();

  useEffect(() => {
    const token = params.get('token');
    if (!token) {
      message.error('登录回调缺少 token');
      navigate('/login', { replace: true });
      return;
    }
    setToken(token);
    fetchProfile()
      .then((profile) => {
        setStoredGroupId(profile.currentGroupId);
        message.success('统一认证登录成功');
        navigate('/', { replace: true });
        window.location.reload();
      })
      .catch(() => {
        message.error('登录状态同步失败');
        navigate('/login', { replace: true });
      });
  }, [params, navigate]);

  return (
    <div className="center-page">
      <Spin size="large" />
      <Typography.Paragraph style={{ marginTop: 16 }}>正在完成登录...</Typography.Paragraph>
    </div>
  );
}
