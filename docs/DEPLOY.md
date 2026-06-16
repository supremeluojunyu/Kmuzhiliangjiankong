# 部署指南

## 方式一：Docker Compose（推荐）

### 前置条件

- Docker 20.10+
- Docker Compose v2

安装示例（Ubuntu/Debian）：

```bash
sudo apt update
sudo apt install -y docker.io docker-compose-v2
sudo usermod -aG docker $USER
newgrp docker
```

### 启动

```bash
cd university-quality-monitor
cp .env.example .env   # 可选，修改密码与端口
./scripts/docker-up.sh
```

或手动：

```bash
docker compose up -d --build
```

### 访问

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost |
| 后端 API | http://localhost:8080/api |
| 健康检查 | http://localhost:8080/api/health |

默认账号：`admin` / `admin123`

### 常用命令

```bash
docker compose ps          # 查看状态
docker compose logs -f     # 查看日志
docker compose down        # 停止并移除容器
docker compose down -v     # 同时删除数据卷（清空数据库）
```

### 服务说明

| 容器 | 说明 |
|------|------|
| uqm-mysql | MySQL 8，首次启动自动执行 schema/data/seed SQL |
| uqm-redis | Redis 7，会话与分布式锁 |
| uqm-backend | Spring Boot，profile=`docker` |
| uqm-frontend | Nginx 静态资源 + `/api` 反向代理 |

上传文件持久化在 Docker volume `upload_data`（挂载至后端 `/data/uploads`）。

---

## 方式二：本地开发

### 前置条件

- JDK 17、Maven 3.9+
- Node.js 20+、npm
- MySQL 8、Redis 7

### 数据库

```bash
sudo mysql -e "CREATE DATABASE IF NOT EXISTS uqm CHARACTER SET utf8mb4;"
sudo mysql -e "CREATE USER IF NOT EXISTS 'uqm'@'localhost' IDENTIFIED BY 'uqm_dev_2026';"
sudo mysql -e "GRANT ALL ON uqm.* TO 'uqm'@'localhost';"

sudo mysql uqm < backend/src/main/resources/db/schema.sql
sudo mysql uqm < backend/src/main/resources/db/data.sql
sudo mysql uqm < backend/src/main/resources/db/seed-test-users.sql
```

### 启动

```bash
# 终端 1
cd backend && mvn spring-boot:run

# 终端 2
cd frontend && npm install && npm run dev
```

或使用脚本：

```bash
chmod +x scripts/dev.sh
./scripts/dev.sh
```

---

## 文件存储

### 本地存储（默认）

```yaml
uqm.upload.type: local
uqm.upload.path: /data/uploads
```

### S3 兼容存储（MinIO / 阿里云 OSS）

`application.yml` 或环境变量：

```bash
UPLOAD_TYPE=s3
S3_ENDPOINT=https://oss-cn-hangzhou.aliyuncs.com   # 或 MinIO 地址
S3_BUCKET=uqm-uploads
S3_ACCESS_KEY=your-key
S3_SECRET_KEY=your-secret
S3_PATH_STYLE=true   # MinIO 一般为 true；阿里云 OSS 一般为 false
```

Docker 启用 MinIO（开发/测试）：

```bash
# .env 中设置 UPLOAD_TYPE=s3 及 S3_* 变量
docker compose --profile oss up -d
```

MinIO 控制台：http://localhost:9001（默认 minioadmin / minioadmin）

文件仍通过 `/api/files/{key}` 下载，前端无需改动。

---

## 生产环境建议

1. **修改密钥**：在 `.env` 中设置强随机 `JWT_SECRET`、`DB_PASSWORD`
2. **HTTPS**：在 Nginx 前增加 TLS 终止（如 Caddy / 云负载均衡）
3. **备份**：定期备份 MySQL volume 与 `upload_data`
4. **日志**：关闭 MyBatis SQL 日志（docker profile 已默认关闭）
5. **监控**：可对接 `/api/health` 做存活探测

---

## 测试账号

| 账号 | 密码 | 说明 |
|------|------|------|
| admin | admin123 | 系统管理员 |
| dean_info | admin123 | 信息学院院级管理员 |
| teacher_info / teacher_econ / teacher_art / teacher_sci | admin123 | 材料提交组 |
| expert1 | admin123 | 专家/评审组 |
