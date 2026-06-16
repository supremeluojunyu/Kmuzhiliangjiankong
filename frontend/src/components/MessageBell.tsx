import { BellOutlined } from '@ant-design/icons';
import { Badge, Button } from 'antd';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchUnreadCount } from '@/api/message';

export default function MessageBell() {
  const [count, setCount] = useState(0);
  const navigate = useNavigate();

  const load = () => {
    fetchUnreadCount().then(setCount).catch(() => {});
  };

  useEffect(() => {
    load();
    const onChange = () => load();
    window.addEventListener('identity-changed', onChange);
    return () => window.removeEventListener('identity-changed', onChange);
  }, []);

  return (
    <Badge count={count} size="small">
      <Button
        type="text"
        icon={<BellOutlined />}
        onClick={() => navigate('/messages')}
      />
    </Badge>
  );
}
