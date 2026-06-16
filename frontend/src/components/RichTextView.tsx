import { Typography } from 'antd';

interface RichTextViewProps {
  html?: string | null;
  className?: string;
}

function isEmptyHtml(html: string) {
  const t = html.trim();
  return !t || t === '<p><br></p>' || t === '<p></p>';
}

export default function RichTextView({ html, className }: RichTextViewProps) {
  if (!html || isEmptyHtml(html)) {
    return <Typography.Text type="secondary">无内容</Typography.Text>;
  }

  if (!html.includes('<')) {
    return (
      <Typography.Paragraph className={className} style={{ whiteSpace: 'pre-wrap', marginBottom: 0 }}>
        {html}
      </Typography.Paragraph>
    );
  }

  return (
    <div
      className={`rich-text-view${className ? ` ${className}` : ''}`}
      dangerouslySetInnerHTML={{ __html: html }}
    />
  );
}
