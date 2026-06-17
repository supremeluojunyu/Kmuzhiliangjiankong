import { ExclamationCircleOutlined } from '@ant-design/icons';
import { Input, Modal, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { DELETE_CONFIRM_PHRASE } from '@/constants/deleteConfirm';

interface ConfirmDeleteModalProps {
  open: boolean;
  title?: string;
  description?: string;
  count: number;
  loading?: boolean;
  onCancel: () => void;
  onConfirm: () => void | Promise<void>;
}

export default function ConfirmDeleteModal({
  open,
  title = '确认删除',
  description,
  count,
  loading = false,
  onCancel,
  onConfirm,
}: ConfirmDeleteModalProps) {
  const [phrase, setPhrase] = useState('');

  useEffect(() => {
    if (!open) {
      setPhrase('');
    }
  }, [open]);

  const matched = phrase === DELETE_CONFIRM_PHRASE;

  return (
    <Modal
      title={
        <span>
          <ExclamationCircleOutlined style={{ color: '#ff4d4f', marginRight: 8 }} />
          {title}
        </span>
      }
      open={open}
      okText="确认删除"
      okType="danger"
      okButtonProps={{ disabled: !matched, loading }}
      cancelText="取消"
      onCancel={onCancel}
      onOk={onConfirm}
      destroyOnClose
    >
      <Typography.Paragraph type="danger">
        即将删除 {count} 项，此操作不可恢复。
      </Typography.Paragraph>
      {description && (
        <Typography.Paragraph type="secondary">{description}</Typography.Paragraph>
      )}
      <Typography.Paragraph>
        请输入
        <Typography.Text code strong>
          {DELETE_CONFIRM_PHRASE}
        </Typography.Text>
        以确认：
      </Typography.Paragraph>
      <Input
        value={phrase}
        onChange={(e) => setPhrase(e.target.value)}
        placeholder={DELETE_CONFIRM_PHRASE}
        onPressEnter={() => matched && !loading && onConfirm()}
        autoFocus
      />
    </Modal>
  );
}

export { DELETE_CONFIRM_PHRASE };
