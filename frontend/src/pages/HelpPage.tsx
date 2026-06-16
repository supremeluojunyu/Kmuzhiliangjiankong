import { Card, Collapse, Typography } from 'antd';
import { useAuth } from '@/contexts/AuthContext';
import { buildHelpSections } from '@/config/helpContent';

export default function HelpPage() {
  const { user } = useAuth();
  const sections = buildHelpSections(
    user?.currentGroupId ?? null,
    user?.currentGroupName,
    user?.permissions ?? []
  );

  return (
    <div>
      <Typography.Title level={4}>使用帮助</Typography.Title>
      <Typography.Paragraph type="secondary">
        以下内容根据您当前登录身份（{user?.account} · {user?.currentGroupName}）自动生成，说明可用功能与操作步骤。
      </Typography.Paragraph>

      <Collapse
        defaultActiveKey={sections.map((_, i) => String(i))}
        items={sections.map((section, index) => ({
          key: String(index),
          label: section.title,
          children: (
            <ul style={{ margin: 0, paddingLeft: 20 }}>
              {section.items.map((item) => (
                <li key={item} style={{ marginBottom: 8 }}>
                  <Typography.Text>{item}</Typography.Text>
                </li>
              ))}
            </ul>
          ),
        }))}
      />

      <Card size="small" style={{ marginTop: 16 }} title="账号信息">
        <Typography.Text>
          姓名：{user?.name} · 学院：{user?.collegeName || '—'} · 账号：{user?.account}
        </Typography.Text>
      </Card>
    </div>
  );
}
