#!/usr/bin/env bash
# 本地开发一键启动（需已安装 MySQL、Redis，并完成数据库初始化）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo "==> 检查 MySQL / Redis..."
command -v mysql >/dev/null || { echo "请先安装 MySQL"; exit 1; }
command -v redis-cli >/dev/null || { echo "请先安装 Redis"; exit 1; }

echo "==> 初始化数据库（如已存在可跳过报错）..."
mysql -u uqm -puqm_dev_2026 uqm < "$ROOT/backend/src/main/resources/db/schema.sql" 2>/dev/null || true
mysql -u uqm -puqm_dev_2026 uqm < "$ROOT/backend/src/main/resources/db/data.sql" 2>/dev/null || true
mysql -u uqm -puqm_dev_2026 uqm < "$ROOT/backend/src/main/resources/db/seed-test-users.sql" 2>/dev/null || true

mkdir -p "$ROOT/data/uploads"

echo "==> 启动后端 (8080)..."
cd "$ROOT/backend"
TZ=Asia/Shanghai mvn -q spring-boot:run -Dspring-boot.run.jvmArguments="-Duser.timezone=Asia/Shanghai" &
BACKEND_PID=$!

echo "==> 启动前端 (5173)..."
cd "$ROOT/frontend"
[ -d node_modules ] || npm install
npm run dev &
FRONTEND_PID=$!

cleanup() {
  echo "==> 停止服务..."
  kill "$BACKEND_PID" "$FRONTEND_PID" 2>/dev/null || true
}
trap cleanup EXIT INT TERM

echo ""
echo "后端: http://localhost:8080"
echo "前端: http://localhost:5173"
echo "默认账号: admin / admin123"
echo "按 Ctrl+C 停止"
wait
