import { CloudDownloadOutlined } from '@ant-design/icons';
import { Button, Modal, Progress, Space, Typography, message } from 'antd';
import { useCallback, useEffect, useState } from 'react';
import {
  checkAppUpdate,
  clearDismissedUpdate,
  dismissUpdate,
  downloadAndInstallApk,
  type AppUpdateInfo,
} from '@/utils/appUpdate';
import { isMobileApp } from '@/utils/app';

export const APP_UPDATE_CHECK_EVENT = 'uqm-check-update';

export default function AppUpdateChecker() {
  const [updateInfo, setUpdateInfo] = useState<AppUpdateInfo | null>(null);
  const [open, setOpen] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState<string | null>(null);

  const runCheck = useCallback(async (force = false) => {
    if (!isMobileApp()) return;
    if (force) clearDismissedUpdate();
    try {
      const info = await checkAppUpdate();
      if (info) {
        setUpdateInfo(info);
        setOpen(true);
      } else if (force) {
        message.info('已是最新版本');
      }
    } catch {
      if (force) message.error('检查更新失败，请检查网络');
    }
  }, []);

  useEffect(() => {
    runCheck(false);
  }, [runCheck]);

  useEffect(() => {
    const onManualCheck = () => runCheck(true);
    window.addEventListener(APP_UPDATE_CHECK_EVENT, onManualCheck);
    return () => window.removeEventListener(APP_UPDATE_CHECK_EVENT, onManualCheck);
  }, [runCheck]);

  const handleUpdate = async () => {
    if (!updateInfo?.release.apkUrl) return;
    setDownloading(true);
    setError(null);
    setProgress(0);
    try {
      await downloadAndInstallApk(updateInfo.release.apkUrl, setProgress);
      clearDismissedUpdate();
      setOpen(false);
    } catch (e) {
      setError(e instanceof Error ? e.message : '更新失败');
    } finally {
      setDownloading(false);
    }
  };

  const handleLater = () => {
    if (updateInfo?.release.version) {
      dismissUpdate(updateInfo.release.version);
    }
    setOpen(false);
  };

  if (!open) return null;

  if (!updateInfo) return null;
  return (
    <Modal
      title="发现新版本"
      open={open}
      onCancel={handleLater}
      footer={null}
      closable={!downloading}
      maskClosable={!downloading}
    >
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
          <Typography.Text>
            当前版本：<Typography.Text type="secondary">{updateInfo.currentVersion}</Typography.Text>
            {' → '}
            最新版本：<Typography.Text strong>v{updateInfo.release.version}</Typography.Text>
          </Typography.Text>

          {updateInfo.release.releaseNotes && (
            <div className="download-notes">
              <Typography.Title level={5} style={{ marginTop: 0 }}>
                更新说明
              </Typography.Title>
              <Typography.Paragraph style={{ whiteSpace: 'pre-wrap', marginBottom: 0 }}>
                {updateInfo.release.releaseNotes}
              </Typography.Paragraph>
            </div>
          )}

          {downloading && <Progress percent={progress} status="active" />}

          {error && <Typography.Text type="danger">{error}</Typography.Text>}

          <Space wrap>
            <Button
              type="primary"
              icon={<CloudDownloadOutlined />}
              loading={downloading}
              onClick={handleUpdate}
            >
              立即更新
            </Button>
            <Button disabled={downloading} onClick={handleLater}>
              稍后
            </Button>
          </Space>
        </Space>
    </Modal>
  );
}
