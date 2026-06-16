import { Typography } from 'antd';

export default function PlaceholderPage({ title }: { title: string }) {
  return (
    <div>
      <Typography.Title level={4}>{title}</Typography.Title>
      <Typography.Paragraph type="secondary">功能开发中，将在后续阶段实现。</Typography.Paragraph>
    </div>
  );
}
