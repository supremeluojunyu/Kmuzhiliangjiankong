import { BarChartOutlined, DownloadOutlined } from '@ant-design/icons';
import { Button, Card, Col, Input, Row, Space, Statistic, Table, Typography, message } from 'antd';
import ReactECharts from 'echarts-for-react';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { fetchTask } from '@/api/task';
import {
  exportReviews,
  fetchScoreSummary,
  fetchTaskProgress,
  type ScoreSummary,
  type TaskProgress,
} from '@/api/stat';
import { useAuth } from '@/contexts/AuthContext';

export default function TaskStatPage() {
  const { taskId } = useParams();
  const navigate = useNavigate();
  const { canViewStats, isCollegeScoped, hasPermission } = useAuth();
  const id = Number(taskId);

  const [taskName, setTaskName] = useState('');
  const [progress, setProgress] = useState<TaskProgress | null>(null);
  const [scores, setScores] = useState<ScoreSummary | null>(null);
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(true);

  const load = async (kw = keyword) => {
    setLoading(true);
    try {
      const [p, s, t] = await Promise.all([
        fetchTaskProgress(id),
        fetchScoreSummary(id, kw || undefined),
        fetchTask(id),
      ]);
      setProgress(p);
      setScores(s);
      setTaskName(t.taskName);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '加载失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (id) load();
  }, [id]);

  const pieOption = useMemo(() => {
    if (!progress) return {};
    return {
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        data: [
          { value: progress.completedInstances, name: '已完成', itemStyle: { color: '#91cc75' } },
          {
            value: progress.totalInstances - progress.completedInstances,
            name: '未完成',
            itemStyle: { color: '#ee6666' },
          },
        ],
        label: { show: true, formatter: '{b}: {d}%' },
      }],
    };
  }, [progress]);

  const collegeBarOption = useMemo(() => {
    if (!progress?.collegeProgress?.length) return {};
    return {
      title: { text: '各学院完成率', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: progress.collegeProgress.map((c) => c.collegeName),
      },
      yAxis: { type: 'value', name: '完成率 (%)', max: 100 },
      series: [{
        type: 'bar',
        data: progress.collegeProgress.map((c) => c.rate),
        itemStyle: { color: '#5470c6' },
        label: { show: true, position: 'top', formatter: '{c}%' },
      }],
    };
  }, [progress]);

  const nodeBarOption = useMemo(() => {
    if (!progress?.nodeProgress?.length) return {};
    return {
      title: { text: '各节点完成率', left: 'center', textStyle: { fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: progress.nodeProgress.map((n) => n.nodeName || n.nodeId),
      },
      yAxis: { type: 'value', name: '完成率 (%)', max: 100 },
      series: [{
        type: 'bar',
        data: progress.nodeProgress.map((n) => n.rate),
        itemStyle: { color: '#73c0de' },
        label: { show: true, position: 'top', formatter: '{c}%' },
      }],
    };
  }, [progress]);

  if (!canViewStats()) {
    return <Typography.Text type="danger">无查看统计权限</Typography.Text>;
  }

  const collegeScope = progress?.scope === 'college' || isCollegeScoped();

  return (
    <div>
      <Space style={{ marginBottom: 16, width: '100%', justifyContent: 'space-between' }}>
        <Space>
          <Button onClick={() => navigate('/tasks')}>返回</Button>
          <Typography.Title level={4} style={{ margin: 0 }}>
            <BarChartOutlined /> {taskName || '任务统计'}
            {collegeScope && progress?.scopeCollegeName && (
              <Typography.Text type="secondary" style={{ fontSize: 14, marginLeft: 8 }}>
                （本院：{progress.scopeCollegeName}）
              </Typography.Text>
            )}
          </Typography.Title>
        </Space>
        {hasPermission('data:export') && (
          <Button
            icon={<DownloadOutlined />}
            onClick={async () => {
              try {
                await exportReviews(id, keyword || undefined);
                message.success('导出成功');
              } catch {
                message.error('导出失败');
              }
            }}
          >
            导出评语 Excel
          </Button>
        )}
      </Space>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={8}>
          <Card loading={loading}>
            <Statistic title="任务实例总数" value={progress?.totalInstances ?? 0} />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card loading={loading}>
            <Statistic title="已完成" value={progress?.completedInstances ?? 0} suffix="个" />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card loading={loading}>
            <Statistic
              title="总体完成率"
              value={progress?.completionRate ?? 0}
              suffix="%"
              precision={1}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} md={collegeScope ? 24 : 8}>
          <Card title={collegeScope ? '本院完成情况' : '总体完成情况'} loading={loading}>
            <ReactECharts option={pieOption} style={{ height: 280 }} />
          </Card>
        </Col>
        {!collegeScope && (
          <Col xs={24} md={16}>
            <Card title="各学院完成率" loading={loading}>
              <ReactECharts option={collegeBarOption} style={{ height: 280 }} />
            </Card>
          </Col>
        )}
      </Row>

      <Card title="各节点完成进度" style={{ marginTop: 16 }} loading={loading}>
        <ReactECharts option={nodeBarOption} style={{ height: 300 }} />
      </Card>

      <Card
        title="评分汇总"
        style={{ marginTop: 16 }}
        extra={
          <Space>
            <Input.Search
              placeholder="评语关键词"
              allowClear
              onSearch={(v) => { setKeyword(v); load(v); }}
              style={{ width: 200 }}
            />
          </Space>
        }
        loading={loading}
      >
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={6}><Statistic title="平均分" value={scores?.average ?? '-'} /></Col>
          <Col span={6}><Statistic title="最高分" value={scores?.max ?? '-'} /></Col>
          <Col span={6}><Statistic title="最低分" value={scores?.min ?? '-'} /></Col>
          <Col span={6}><Statistic title="评分数量" value={scores?.totalScores ?? 0} /></Col>
        </Row>
        <Table
          rowKey={(r) => `${r.instanceId}-${r.nodeId}`}
          size="small"
          dataSource={scores?.reviews ?? []}
          columns={[
            { title: '学院', dataIndex: 'collegeName', width: 120 },
            { title: '执行人', dataIndex: 'userName', width: 100 },
            { title: '节点', dataIndex: 'nodeName', width: 120 },
            {
              title: '评分/等级',
              width: 100,
              render: (_, r) => r.grade ?? r.score ?? '-',
            },
            { title: '评语', dataIndex: 'comment' },
          ]}
          pagination={{ pageSize: 10 }}
        />
      </Card>
    </div>
  );
}
