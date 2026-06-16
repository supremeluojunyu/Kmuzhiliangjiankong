import { DownloadOutlined, FileOutlined } from '@ant-design/icons';
import { Button, Card, Image, Space, Spin, Typography, message } from 'antd';
import { useEffect, useState } from 'react';
import {
  batchDownloadFiles,
  downloadFile,
  fetchFileBlob,
  fetchFilePreviewHtml,
  getPreviewKind,
  type MaterialFileItem,
} from '@/api/file';

interface MaterialViewerProps {
  title?: string;
  remark?: string;
  files: MaterialFileItem[];
  zipName?: string;
}

export default function MaterialViewer({ title, remark, files, zipName = '材料打包' }: MaterialViewerProps) {
  const [previewUrls, setPreviewUrls] = useState<Record<string, string>>({});
  const [previewHtml, setPreviewHtml] = useState<Record<string, string>>({});
  const [loadingPaths, setLoadingPaths] = useState<Set<string>>(new Set());
  const [batchLoading, setBatchLoading] = useState(false);

  useEffect(() => {
    let cancelled = false;
    const createdUrls: string[] = [];

    const loadPreviews = async () => {
      for (const file of files) {
        const kind = getPreviewKind(file.name);
        if (kind === 'none') {
          continue;
        }
        setLoadingPaths((prev) => new Set(prev).add(file.path));
        try {
          if (kind === 'office') {
            const html = await fetchFilePreviewHtml(file.path);
            if (!cancelled) {
              setPreviewHtml((prev) => ({ ...prev, [file.path]: html }));
            }
          } else {
            const blob = await fetchFileBlob(file.path);
            if (cancelled) {
              return;
            }
            const url = URL.createObjectURL(blob);
            createdUrls.push(url);
            setPreviewUrls((prev) => ({ ...prev, [file.path]: url }));
          }
        } catch {
          if (!cancelled) {
            message.error(`加载预览失败：${file.name}`);
          }
        } finally {
          if (!cancelled) {
            setLoadingPaths((prev) => {
              const next = new Set(prev);
              next.delete(file.path);
              return next;
            });
          }
        }
      }
    };

    setPreviewUrls({});
    setPreviewHtml({});
    loadPreviews();

    return () => {
      cancelled = true;
      createdUrls.forEach((url) => URL.revokeObjectURL(url));
    };
  }, [files]);

  const handleBatchDownload = async () => {
    if (files.length === 0) {
      return;
    }
    setBatchLoading(true);
    try {
      await batchDownloadFiles(files, zipName);
      message.success('已开始下载');
    } catch (e) {
      message.error(e instanceof Error ? e.message : '批量下载失败');
    } finally {
      setBatchLoading(false);
    }
  };

  if (files.length === 0 && !remark) {
    return <Typography.Text type="secondary">（未上传附件）</Typography.Text>;
  }

  return (
    <div>
      {title && (
        <Typography.Text strong style={{ display: 'block', marginBottom: 8 }}>
          {title}
        </Typography.Text>
      )}
      {remark && (
        <Typography.Paragraph type="secondary" style={{ marginBottom: 12 }}>
          说明：{remark}
        </Typography.Paragraph>
      )}

      {files.length > 0 && (
        <Space style={{ marginBottom: 12 }}>
          <Button
            icon={<DownloadOutlined />}
            loading={batchLoading}
            onClick={handleBatchDownload}
          >
            批量下载（{files.length} 个文件）
          </Button>
        </Space>
      )}

      {files.map((file) => {
        const loading = loadingPaths.has(file.path);
        const previewUrl = previewUrls[file.path];
        const html = previewHtml[file.path];
        const kind = getPreviewKind(file.name);

        return (
          <Card
            key={file.path}
            size="small"
            style={{ marginBottom: 12 }}
            title={
              <Space>
                <FileOutlined />
                <span>{file.name}</span>
              </Space>
            }
            extra={
              <Button
                type="link"
                size="small"
                onClick={() => downloadFile(file.path, file.name).catch(() => message.error('下载失败'))}
              >
                下载
              </Button>
            }
          >
            {kind !== 'none' && loading && (
              <div style={{ textAlign: 'center', padding: 24 }}>
                <Spin tip="加载预览中…" />
              </div>
            )}
            {kind === 'image' && !loading && previewUrl && (
              <Image
                src={previewUrl}
                alt={file.name}
                style={{ maxWidth: '100%', maxHeight: 480 }}
              />
            )}
            {kind === 'pdf' && !loading && previewUrl && (
              <iframe
                title={file.name}
                src={previewUrl}
                style={{ width: '100%', height: 520, border: '1px solid #f0f0f0', borderRadius: 4 }}
              />
            )}
            {kind === 'office' && !loading && html && (
              <iframe
                title={file.name}
                srcDoc={html}
                sandbox=""
                style={{ width: '100%', height: 560, border: '1px solid #f0f0f0', borderRadius: 4 }}
              />
            )}
            {kind === 'none' && (
              <Typography.Text type="secondary">
                压缩包请下载后查看。
              </Typography.Text>
            )}
          </Card>
        );
      })}
    </div>
  );
}
