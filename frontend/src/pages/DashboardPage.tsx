import { Card, Col, Row, Statistic, Typography, Tag, List } from 'antd';
import { useEffect, useState } from 'react';
import { fetchColleges } from '@/api/auth';
import { useAuth } from '@/contexts/AuthContext';
import type { College } from '@/types';

export default function DashboardPage() {
  const { user, canViewStats, isCollegeScoped } = useAuth();
  const [colleges, setColleges] = useState<College[]>([]);

  useEffect(() => {
    fetchColleges().then(setColleges).catch(() => {});
  }, []);

  return (
    <div>
      <Typography.Title level={4}>工作台</Typography.Title>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic title="当前身份" value={user?.currentGroupName || '-'} />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic title="所属学院" value={user?.collegeName || '-'} />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic title="权限数量" value={user?.permissions.length || 0} suffix="项" />
          </Card>
        </Col>
      </Row>

      <Card title="当前权限" style={{ marginTop: 16 }}>
        {user?.permissions.map((p) => (
          <Tag key={p} color="blue" style={{ marginBottom: 8 }}>
            {p}
          </Tag>
        ))}
      </Card>

      {canViewStats() && (
        <Card
          title={isCollegeScoped() ? `学院信息（本院：${user?.collegeName || '-'}）` : '学院列表（全校视图）'}
          style={{ marginTop: 16 }}
        >
          <List
            dataSource={colleges}
            renderItem={(item) => (
              <List.Item>
                {item.collegeName}
                <Tag>{item.collegeCode}</Tag>
              </List.Item>
            )}
          />
        </Card>
      )}

      <Card title="开发进度" style={{ marginTop: 16 }}>
        <Typography.Paragraph>
          第一阶段（用户/学院/组/身份切换）已完成。后续阶段：消息 → 任务流程 → 分配 → 执行 → 统计。
        </Typography.Paragraph>
      </Card>
    </div>
  );
}
