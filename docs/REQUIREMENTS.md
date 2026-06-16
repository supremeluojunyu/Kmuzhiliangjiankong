# 高校质量监控管理信息系统 – 优化版需求说明书 v2.1

> 基于 v2.0 补全缺失项、统一术语、明确边界。开发以本文档为准。

---

## 变更摘要（相对 v2.0）

| 类别 | 优化内容 |
|------|----------|
| 数据模型 | 补充学院表、权限表、附件表、操作日志表、任务附件字段 |
| 认证 | 明确 JWT + `X-Current-Group-Id` 请求头方案 |
| 流程引擎 | 补充节点激活/流转规则、审核节点配置 Schema |
| 消息 | 支持多组广播（关联表） |
| API | 统一响应格式、分页规范、错误码 |
| 院级隔离 | 明确院级管理组数据范围规则 |
| 部署 | 提供 Docker Compose 与本地开发说明 |

---

## 1. 项目概述

为高校教学质量监控、专业评估、课程检查等业务，开发支持**多角色协同、任务可配置、流程可定义**的监控管理信息系统。

**技术栈（确定）**

| 层级 | 选型 |
|------|------|
| 前端 | React 18 + TypeScript + Ant Design 5 + ECharts |
| 后端 | Java 17 + Spring Boot 3 + MyBatis-Plus |
| 数据库 | MySQL 8 + Redis |
| 文件 | 本地存储（`/data/uploads`，可扩展 OSS） |
| 认证 | JWT（Access Token 2h） |

---

## 2. 用户与权限模型

### 2.1 用户基本属性

| 字段 | 说明 |
|------|------|
| user_id | 主键 |
| name | 姓名 |
| college_id | 主学院 FK → college |
| account | 登录账号（唯一） |
| password | BCrypt 加密 |
| status | 1=启用 / 0=禁用 |
| created_at | 创建时间 |

### 2.2 学院表（新增）

```sql
CREATE TABLE `college` (
  `college_id` INT PRIMARY KEY AUTO_INCREMENT,
  `college_name` VARCHAR(100) NOT NULL,
  `college_code` VARCHAR(20) UNIQUE,
  `status` TINYINT DEFAULT 1
);
```

### 2.3 用户组

**内置组**（初始化数据）：

| group_id | group_name | 说明 |
|----------|------------|------|
| 1 | 系统管理员组 | 全部权限 |
| 2 | 校级管理组 | 全校范围 |
| 3 | 院级管理组 | 本院范围 |
| 4 | 专家/评审组 | 评分、查看 |
| 5 | 材料提交组 | 提交材料 |
| 6 | 查看组 | 只读 |
| 7 | 评分组 | 评分 |

支持动态新增组；`parent_group_id` 用于组层级（可选）。

### 2.4 权限模型（新增）

```sql
CREATE TABLE `permission` (
  `permission_id` INT PRIMARY KEY AUTO_INCREMENT,
  `permission_code` VARCHAR(50) UNIQUE NOT NULL,
  `permission_name` VARCHAR(100) NOT NULL
);

CREATE TABLE `group_permission` (
  `group_id` INT,
  `permission_id` INT,
  PRIMARY KEY (`group_id`, `permission_id`)
);
```

**权限码枚举**：

- `task:create` — 创建任务
- `task:config` — 配置流程
- `task:allocate` — 分配任务
- `stat:view_all` — 查看全校统计
- `stat:view_college` — 查看本院统计
- `message:send` — 发送组消息
- `data:export` — 导出数据
- `user:manage` — 用户管理
- `group:manage` — 组管理

**院级数据隔离**：院级管理组用户操作时，`college_id` 自动过滤为本院（由 `user.college_id` 决定）。

### 2.5 多组关系与身份切换

- 用户可属于多个组；`user_group.is_default=1` 标记默认组。
- 登录后恢复 `user_session_pref.current_group_id`（若无则用默认组）。
- 请求头携带 `X-Current-Group-Id`；后端校验用户是否属于该组。
- 切换身份 < 500ms：仅更新 Redis 缓存 + DB 偏好，无需重新签发 JWT。

### 2.6 待办数计算

每个组列表返回 `pending_count`：当前用户在该组下、状态为 `pending/in_progress` 的任务实例数。

---

## 3. 消息交互

### 3.1 消息类型

| type | 说明 |
|------|------|
| system | 系统通知（任务发布、截止提醒） |
| broadcast | 组内广播 |
| comment | 任务评论/讨论 |

### 3.2 多组广播（修正）

```sql
CREATE TABLE `message` (
  `message_id` INT PRIMARY KEY AUTO_INCREMENT,
  `sender_id` INT,
  `title` VARCHAR(200),
  `content` TEXT,
  `message_type` VARCHAR(20) DEFAULT 'broadcast',
  `task_id` INT NULL COMMENT '关联任务跳转',
  `instance_id` INT NULL,
  `send_time` DATETIME
);

CREATE TABLE `message_target_group` (
  `message_id` INT,
  `group_id` INT,
  PRIMARY KEY (`message_id`, `group_id`)
);
```

用户可见消息 = 其所属任一 `group_id` 在 `message_target_group` 中的消息。

---

## 4. 任务流程配置

### 4.1 任务定义

| 字段 | 说明 |
|------|------|
| task_id | 主键 |
| task_name | 名称 |
| description | 富文本 |
| config_json | 节点配置 JSON |
| status | draft / published / in_progress / closed |
| creator_id | 创建人 |
| created_at | 创建时间 |

**附件**（新增）：

```sql
CREATE TABLE `task_attachment` (
  `id` INT PRIMARY KEY AUTO_INCREMENT,
  `task_id` INT,
  `file_name` VARCHAR(255),
  `file_path` VARCHAR(500),
  `file_size` BIGINT,
  `uploaded_at` DATETIME
);
```

### 4.2 节点类型

| node_type | 说明 |
|-----------|------|
| submit | 提交材料 |
| view | 查看材料（只读） |
| score | 评分 + 评语 |
| approve | 审核（通过/驳回） |

### 4.3 节点配置 Schema（补全 approve/view）

```json
{
  "config": {
    "submit_config": {
      "max_files": 3,
      "allowed_extensions": ["pdf", "docx"],
      "require_remark": false
    },
    "score_config": {
      "min_score": 0,
      "max_score": 100,
      "score_type": "numeric",
      "require_comment": true
    },
    "approve_config": {
      "allow_reject": true,
      "require_comment_on_reject": true
    },
    "view_config": {
      "visible_node_ids": ["node_submit"]
    }
  }
}
```

### 4.4 流程引擎规则（新增）

1. **节点激活**：所有 `depends_on` 节点均为 `completed` 时激活。
2. **execution_mode**：
   - `sequential`：同组内按分配顺序依次执行（会签）
   - `parallel`：同组多人可同时执行
   - `any`：同组任一人完成即节点完成（或签）
3. **逾期**：节点激活后超过 `time_limit_hours` 标记 `overdue`；是否阻止提交由任务级 `allow_late_submit` 控制（默认 true）。
4. **流转**：节点完成后自动激活下游节点；所有末端节点完成则实例 `completed`。

---

## 5. 任务实例与执行

### 5.1 实例状态

`pending` → `in_progress` → `completed` / `overdue` / `closed`

### 5.2 节点执行记录 submit_data 结构

```json
// submit
{ "files": [{"name":"a.pdf","path":"/uploads/..."}], "remark": "说明" }
// score
{ "score": 85, "comment": "良好" }
// approve
{ "result": "approved|rejected", "comment": "意见" }
```

### 5.3 草稿

提交材料支持 `status=draft` 的 node_record，正式提交后变为 `completed`。

---

## 6. 任务分配

| 类型 | allocation_type | 说明 |
|------|-----------------|------|
| 手动 | manual | 指定学院 → 该学院目标组用户 |
| 按学院 | by_college | 预设学院列表全量分配 |
| 随机 | random | 伪代码见 v2.0 §6.2 |
| 按总量 | by_total | 按比例分配，不足则 redistribute |

**分配记录**（新增）：

```sql
CREATE TABLE `task_allocation` (
  `id` INT PRIMARY KEY AUTO_INCREMENT,
  `task_id` INT,
  `allocation_type` VARCHAR(20),
  `params_json` JSON,
  `created_by` INT,
  `created_at` DATETIME
);
```

分配过程使用 Redis 分布式锁 `lock:allocate:{task_id}` 防并发重复。

---

## 7. 进度统计

- 任务看板：总实例、完成数、完成率、各节点完成比例、各学院完成率
- 评分汇总：平均/最高/最低、分学院、评语关键词搜索
- 导出：Excel（Apache POI）
- ECharts 配置见 v2.0 §7.2

---

## 8. 操作日志（新增）

```sql
CREATE TABLE `operation_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` INT,
  `group_id` INT,
  `action` VARCHAR(100),
  `target_type` VARCHAR(50),
  `target_id` INT,
  `detail` JSON,
  `ip` VARCHAR(45),
  `created_at` DATETIME
);
```

---

## 9. API 规范

### 9.1 统一响应

```json
{
  "code": 0,
  "message": "success",
  "data": { }
}
```

错误码：`0` 成功，`401` 未认证，`403` 无权限，`400` 参数错误，`500` 服务器错误。

### 9.2 分页

请求：`page=1&pageSize=20`  
响应：`{ list: [], total: 100, page: 1, pageSize: 20 }`

### 9.3 关键接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/login | 登录，返回 JWT + 默认组 |
| GET | /api/user/groups | 可切换组 + pending_count |
| POST | /api/user/switch-group | body: `{ groupId }` |
| GET | /api/colleges | 学院列表 |
| CRUD | /api/users, /api/groups | 用户/组管理 |
| POST | /api/task/create | 创建任务 |
| POST | /api/task/publish/{id} | 发布 |
| POST | /api/task/allocate | 分配 |
| GET | /api/task/my-list | 我的任务（按当前组） |
| POST | /api/task/submit/{instanceId}/{nodeId} | 提交 |
| GET | /api/stat/task-progress/{taskId} | 进度 |
| GET | /api/messages | 消息列表 |
| POST | /api/messages | 发送消息 |

---

## 10. 非功能性需求

| 项目 | 要求 |
|------|------|
| 并发 | 500 人评分、1000 人提交（连接池 + Redis 缓存） |
| 安全 | RBAC、BCrypt、参数校验、SQL 预编译 |
| 身份切换 | < 500ms |
| 文件大小 | 单文件 ≤ 50MB |

---

## 11. 开发阶段

| 阶段 | 内容 | 状态 |
|------|------|------|
| P1 | 用户/学院/组/权限/登录/身份切换 | 进行中 |
| P2 | 消息 |
| P3 | 任务定义与流程引擎 |
| P4 | 任务分配 |
| P5 | 任务执行 |
| P6 | 统计与导出 |
| P7 | 权限细化、日志、性能 | ✅ 已完成 |
| P8 | 院级数据隔离 | ✅ 已完成 |
| P10 | 任务模板 | ✅ 已完成 |
| P11 | 富文本编辑器 | ✅ 已完成 |
| P12 | OSS/S3 文件存储 | ✅ 已完成 |
| P13 | 等级制评分 | ✅ 已完成 |
| P14 | 统一认证 / 邮件企微通知 / 数据保留 / 系统配置页 | ✅ 已完成 |
| P15 | 截止提醒 / 逾期自动检测 | ✅ 已完成 |
| P16 | 存储配置 UI / 任务讨论评论 | ✅ 已完成 |
| Docker 部署 | Compose 一键启动 | ✅ 已完成 |

---

## 12. 待确认事项（需产品方答复）

1. **登录方式**：是否仅账号密码？是否需要对接学校统一身份认证（CAS/OAuth）？
2. **富文本编辑器**：Quill / TinyMCE / 其他？ → **已采用 Quill**（P11）
3. **文件存储**：本地存储是否满足生产？OSS 厂商偏好？ → **已支持 S3 兼容存储**（P12，含 MinIO / 阿里云 OSS）
4. **等级制评分**：等级枚举（优/良/中/差）是否固定？ → **已支持节点级配置**（P13，默认优/良/中/差）
5. **任务模板**：是否需要「保存为模板」复用流程配置？ → **已实现**（P10）
6. **通知渠道**：除站内消息外，是否需要邮件/企业微信？ → **已实现**（P14，系统配置页可开关）
7. **数据保留**：历史任务数据保留年限？ → **已实现**（P14，可配置自动清理）

---

## 13. P14 / P15 补充说明

| 功能 | 说明 |
|------|------|
| 统一认证 | CAS / OAuth2 OIDC，可保留本地登录，管理员在 `/settings` 配置 |
| 邮件 / 企微 | SMTP + 企业微信应用消息，支持任务发布、组广播、截止提醒 |
| 数据保留 | 按年清理已完成任务实例、消息、操作日志 |
| 截止提醒 | 每日 8:00 扫描 `globalTimeEnd` 与节点 `timeLimitHours`，提前 N 天通知 |
| 逾期检测 | 每小时自动将超时实例标记为 `overdue` |
| 文件存储 | 管理员可切换本地 / S3，支持在线测试连接 |
| 任务讨论 | 任务实例页评论，流程相关组成员可见 |

权限码：`system:config`（系统管理员组）

---

*版本：2.2 | 更新：2026-06-16*
