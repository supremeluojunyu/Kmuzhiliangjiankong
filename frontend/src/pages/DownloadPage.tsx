import { AndroidOutlined, DownloadOutlined } from '@ant-design/icons';
import { Button, Card, Space, Spin, Tag, Typography } from 'antd';
import { QRCodeSVG } from 'qrcode.react';
import { useEffect, useState } from 'react';
import { fetchPublicAppRelease, type PublicAppRelease } from '@/api/public';
import { DOWNLOAD_PAGE_TITLE } from '@/config/branding';
import { useBranding } from '@/contexts/BrandingContext';

export default function DownloadPage() {
  const { branding, logoSrc, loading: brandingLoading } = useBranding();
  const [release, setRelease] = useState<PublicAppRelease | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchPublicAppRelease()
      .then(setRelease)
      .catch((e) => {
        setRelease(null);
        setError(e instanceof Error ? e.message : '加载版本信息失败');
      })
      .finally(() => setLoading(false));
  }, []);

  const apkUrl = release?.apkUrl || '';
  const pageLoading = brandingLoading || loading;

  return (
    <div
      className="download-page"
      style={{ background: branding.loginBackground || undefined }}
    >
      <Card className="download-card" bordered={false}>
        {pageLoading ? (
          <div className="center-page" style={{ minHeight: 240 }}>
            <Spin size="large" />
          </div>
        ) : (
          <>
            <Space direction="vertical" align="center" style={{ width: '100%' }} size="large">
              {logoSrc ? (
                <img src={logoSrc} alt="" className="download-logo" />
              ) : (
                <AndroidOutlined style={{ fontSize: 48, color: branding.primaryColor }} />
              )}
              <div style={{ textAlign: 'center' }}>
                <Typography.Title level={3} style={{ marginBottom: 8 }}>
                  {branding.downloadPageTitle || DOWNLOAD_PAGE_TITLE}
                </Typography.Title>
                <Typography.Paragraph type="secondary">
                  {branding.downloadPageDescription}
                </Typography.Paragraph>
              </div>

              {error ? (
                <Typography.Text type="danger">{error}（请确认后端已重启并包含 /api/public 接口）</Typography.Text>
              ) : release?.enabled === false || !apkUrl ? (
                <Typography.Text type="secondary">暂无可下载版本，请在系统配置 → 品牌与 APP 中填写 APK 下载地址。</Typography.Text>
              ) : (
                <>
                  <Space wrap>
                    {release?.version && <Tag color="blue">v{release.version}</Tag>}
                    {release?.minAndroidVersion && (
                      <Tag>Android {release.minAndroidVersion}+</Tag>
                    )}
                    {release?.publishedAt && (
                      <Tag>{release.publishedAt.slice(0, 10)}</Tag>
                    )}
                  </Space>

                  <div className="download-qr">
                    <QRCodeSVG value={apkUrl} size={200} level="M" includeMargin />
                    <Typography.Text type="secondary" style={{ marginTop: 12, display: 'block' }}>
                      使用手机浏览器扫码下载
                    </Typography.Text>
                  </div>

                  <Button
                    type="primary"
                    size="large"
                    icon={<DownloadOutlined />}
                    href={apkUrl}
                    target="_blank"
                    rel="noreferrer"
                    style={{ background: branding.primaryColor }}
                  >
                    下载 Android 客户端
                  </Button>

                  {release?.releaseNotes && (
                    <div className="download-notes">
                      <Typography.Title level={5}>更新说明</Typography.Title>
                      <Typography.Paragraph style={{ whiteSpace: 'pre-wrap' }}>
                        {release.releaseNotes}
                      </Typography.Paragraph>
                    </div>
                  )}
                </>
              )}
            </Space>
          </>
        )}
      </Card>
    </div>
  );
}
