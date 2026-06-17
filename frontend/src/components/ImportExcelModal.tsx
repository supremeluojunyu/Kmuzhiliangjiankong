import { DownloadOutlined, UploadOutlined } from '@ant-design/icons';
import { Alert, Button, Modal, Space, Typography, Upload, message } from 'antd';
import type { UploadFile } from 'antd/es/upload/interface';
import { useState } from 'react';
import type { ImportResult } from '@/api/importUtil';

interface ImportExcelModalProps {
  open: boolean;
  title: string;
  description?: string;
  onClose: () => void;
  onDownloadTemplate: () => Promise<void>;
  onImport: (file: File) => Promise<ImportResult>;
  onSuccess?: () => void;
}

export default function ImportExcelModal({
  open,
  title,
  description,
  onClose,
  onDownloadTemplate,
  onImport,
  onSuccess,
}: ImportExcelModalProps) {
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const [uploading, setUploading] = useState(false);
  const [result, setResult] = useState<ImportResult | null>(null);

  const handleClose = () => {
    setFileList([]);
    setResult(null);
    onClose();
  };

  const handleImport = async () => {
    const file = fileList[0]?.originFileObj;
    if (!file) {
      message.warning('请先选择 Excel 文件');
      return;
    }
    setUploading(true);
    try {
      const res = await onImport(file);
      setResult(res);
      if (res.successCount > 0) {
        onSuccess?.();
      }
      Modal.success({
        title: '导入完成',
        content: (
          <div>
            <p>成功 {res.successCount} 条，跳过 {res.skipCount} 条，失败 {res.failCount} 条</p>
            {res.messages && res.messages.length > 0 && (
              <ul style={{ maxHeight: 200, overflow: 'auto', paddingLeft: 18 }}>
                {res.messages.map((m) => (
                  <li key={m}>{m}</li>
                ))}
              </ul>
            )}
          </div>
        ),
        okText: '知道了',
      });
    } catch (e) {
      message.error(e instanceof Error ? e.message : '导入失败');
    } finally {
      setUploading(false);
    }
  };

  return (
    <Modal
      title={title}
      open={open}
      onCancel={handleClose}
      footer={[
        <Button key="cancel" onClick={handleClose}>
          关闭
        </Button>,
        <Button key="import" type="primary" loading={uploading} onClick={handleImport}>
          开始导入
        </Button>,
      ]}
      destroyOnClose
      width={560}
    >
      {description && (
        <Typography.Paragraph type="secondary">{description}</Typography.Paragraph>
      )}
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        <Button icon={<DownloadOutlined />} onClick={() => onDownloadTemplate().catch(() => message.error('下载模板失败'))}>
          下载导入模板
        </Button>
        <Upload
          accept=".xlsx,.xls"
          maxCount={1}
          beforeUpload={(file) => {
            setFileList([{ uid: file.uid, name: file.name, originFileObj: file }]);
            setResult(null);
            return false;
          }}
          onRemove={() => {
            setFileList([]);
            setResult(null);
          }}
          fileList={fileList}
        >
          <Button icon={<UploadOutlined />}>选择 Excel 文件</Button>
        </Upload>
        {result && (
          <Alert
            type={result.failCount > 0 ? 'warning' : 'success'}
            showIcon
            message={`成功 ${result.successCount}，跳过 ${result.skipCount}，失败 ${result.failCount}`}
          />
        )}
      </Space>
    </Modal>
  );
}
