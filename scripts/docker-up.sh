#!/usr/bin/env bash
# Docker Compose 一键部署
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if ! command -v docker >/dev/null 2>&1; then
  echo "未检测到 Docker，请先安装："
  echo "  sudo apt update && sudo apt install -y docker.io docker-compose-v2"
  echo "  sudo usermod -aG docker \$USER && newgrp docker"
  exit 1
fi

if [ ! -f .env ]; then
  cp .env.example .env
  echo "已生成 .env，可按需修改后重新运行"
fi

echo "==> 构建并启动容器..."
docker compose up -d --build

echo ""
echo "等待服务就绪..."
for i in $(seq 1 60); do
  if curl -sf http://localhost:${FRONTEND_PORT:-80}/ >/dev/null 2>&1 \
     && curl -sf http://localhost:${BACKEND_PORT:-8080}/api/health >/dev/null 2>&1; then
    echo "服务已启动"
    break
  fi
  sleep 2
done

echo ""
echo "前端: http://localhost:${FRONTEND_PORT:-80}"
echo "后端: http://localhost:${BACKEND_PORT:-8080}"
echo "健康检查: http://localhost:${BACKEND_PORT:-8080}/api/health"
echo "默认账号: admin / admin123"
echo ""
echo "查看日志: docker compose logs -f"
echo "停止服务: docker compose down"
