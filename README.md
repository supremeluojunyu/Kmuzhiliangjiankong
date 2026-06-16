# 高校质量监控管理信息系统

支持多角色协同、任务可配置、流程可定义的高校教学质量监控管理平台。

## 技术栈

- **后端**：Java 17 + Spring Boot 3 + MyBatis-Plus + MySQL + Redis + JWT
- **前端**：React 18 + TypeScript + Ant Design 5 + Vite

## 快速启动

### 1. 数据库初始化

```bash
sudo mysql uqm < backend/src/main/resources/db/schema.sql
sudo mysql uqm < backend/src/main/resources/db/data.sql
```

### 2. 启动后端（端口 8080）

```bash
cd backend
mvn spring-boot:run
# 开发调试（输出 SQL）：mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 3. 启动前端（端口 5173）

```bash
cd frontend
npm install
npm run dev
```

### 4. 默认账号

| 账号 | 密码 | 说明 |
|------|------|------|
| admin | admin123 | 系统管理员，可切换「系统管理员组」「校级管理组」 |
| dean_info | admin123 | 信息学院院级管理员（本院数据隔离） |

## 项目结构

```
university-quality-monitor/
├── backend/          # Spring Boot 后端
├── frontend/         # React 前端
├── docs/
│   ├── REQUIREMENTS.md   # 优化版需求说明书 v2.1
│   └── DEPLOY.md         # 部署指南（Docker / 本地）
├── scripts/
│   ├── dev.sh            # 本地开发启动
│   └── docker-up.sh      # Docker 一键部署
├── docker-compose.yml
├── .env.example
└── data/uploads/     # 文件上传目录
```

## 已完成功能

### P1 用户与身份
- [x] 用户登录（JWT）
- [x] 学院 / 用户组基础数据
- [x] 多组关系与权限模型
- [x] 身份切换（`X-Current-Group-Id` + Redis 缓存）
- [x] 前端身份切换组件（含待办 Badge）

### P2 消息交互
- [x] 多组广播发送（`message:send` 权限）
- [x] 消息列表 / 未读计数 / 标记已读
- [x] 顶部消息铃铛（未读 Badge）

### P3 任务流程配置
- [x] 创建 / 编辑草稿任务
- [x] JSON 流程配置（节点类型、依赖、执行模式）
- [x] 流程引擎校验（节点 ID 唯一、依赖存在、无环）
- [x] 发布任务
- [x] 前端可视化节点配置表单

### P4 任务分配
- [x] 手动 / 按学院 / 随机 / 按总量四种分配
- [x] Redis 分布式锁

### P5 任务执行
- [x] 我的任务列表 + 节点提交（材料/评分/审核/查看）
- [x] 流程自动流转

### P6 进度统计
- [x] 任务进度看板（实例数、完成率、节点/学院维度）
- [x] ECharts 环形图 + 柱状图
- [x] 评分汇总 + 评语关键词搜索
- [x] 评语 Excel 导出（Apache POI）

### P7 系统完善
- [x] 用户管理 CRUD（含多组分配）
- [x] 组管理 CRUD（含权限配置）
- [x] 操作日志（登录、用户/组变更等）
- [x] 文件上传（任务材料提交）

### P14 系统配置
- [x] CAS / OAuth2 统一认证（可保留本地登录）
- [x] SMTP 邮件 + 企业微信通知
- [x] 数据保留策略（定时 / 手动清理）
- [x] 管理员系统配置页（`/settings`）

### P15 任务时效
- [x] 截止前 N 天邮件/企微/站内提醒
- [x] 超时实例自动标记 `overdue`

### P16 存储与讨论
- [x] 文件存储管理员配置（本地 / S3 兼容）
- [x] 存储连接在线测试
- [x] 任务实例页讨论评论（流程相关组可见）

## API 示例

```bash
# 登录
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"account":"admin","password":"admin123"}'

# 切换身份
curl -X POST http://localhost:8080/api/user/switch-group \
  -H 'Authorization: Bearer <token>' \
  -H 'X-Current-Group-Id: 1' \
  -H 'Content-Type: application/json' \
  -d '{"groupId": 2}'
```

## 开发阶段

详见 [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md)

| 阶段 | 状态 |
|------|------|
| P1 用户/组/身份切换 | ✅ 已完成 |
| P2 消息 | ✅ 已完成 |
| P3 任务流程 | ✅ 已完成 |
| P4 任务分配 | ✅ 已完成 |
| P5 任务执行 | ✅ 已完成 |
| P6 统计导出 | ✅ 已完成 |
| P7 权限细化/日志/上传 | ✅ 已完成 |
| P8 院级数据隔离 | ✅ 已完成 |
| P10 任务模板 | ✅ 已完成 |
| P11 富文本编辑器（Quill） | ✅ 已完成 |
| P12 OSS/S3 文件存储 | ✅ 已完成 |
| P13 等级制评分 | ✅ 已完成 |
| P14 系统配置（认证/通知/保留） | ✅ 已完成 |
| P15 截止提醒 / 逾期检测 | ✅ 已完成 |
| P16 存储配置 / 任务讨论 | ✅ 已完成 |
| Docker 一键部署 | ✅ 已完成 |

## Docker 部署

详见 [docs/DEPLOY.md](docs/DEPLOY.md)

```bash
cp .env.example .env
./scripts/docker-up.sh
# 或: docker compose up -d --build
```

访问 http://localhost ，默认账号 `admin / admin123`。
