#!/usr/bin/env bash
# 安装 UQM systemd 用户服务：开机自启 + 崩溃自动重启 + 定时健康检查
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
USER_UNIT_DIR="${HOME}/.config/systemd/user"
SRC="${ROOT}/scripts/systemd"

mkdir -p "${USER_UNIT_DIR}" "${ROOT}/logs"
chmod +x "${ROOT}/scripts/uqm-backend-start.sh" \
         "${ROOT}/scripts/uqm-frontend-start.sh" \
         "${ROOT}/scripts/uqm-watchdog.sh" \
         "${ROOT}/scripts/uqm-status.sh"

echo "==> 安装 systemd 单元到 ${USER_UNIT_DIR}"
cp "${SRC}/uqm.target" \
   "${SRC}/uqm-backend.service" \
   "${SRC}/uqm-frontend.service" \
   "${SRC}/frpc-uqm.service" \
   "${SRC}/uqm-watchdog.service" \
   "${SRC}/uqm-watchdog.timer" \
   "${USER_UNIT_DIR}/"

# 停止手工启动的旧进程，避免端口冲突
echo "==> 停止旧的手工进程..."
pkill -f 'com.uqm.UqmApplication' 2>/dev/null || true
pkill -f 'spring-boot:run.*university-quality-monitor' 2>/dev/null || true
pkill -f 'vite.*5173' 2>/dev/null || true
pkill -f 'node.*vite' 2>/dev/null || true
sleep 2

systemctl --user daemon-reload

echo "==> 启用开机自启（需 loginctl enable-linger）"
loginctl enable-linger "${USER}" 2>/dev/null || true

systemctl --user enable uqm.target \
  uqm-backend.service \
  uqm-frontend.service \
  frpc-uqm.service \
  uqm-watchdog.timer

echo "==> 启动服务栈..."
systemctl --user start uqm.target
systemctl --user start uqm-watchdog.timer

echo ""
echo "安装完成。常用命令："
echo "  bash ${ROOT}/scripts/uqm-status.sh          # 查看状态"
echo "  systemctl --user status uqm-backend           # 后端"
echo "  systemctl --user restart uqm-frontend         # 重启前端"
echo "  tail -f ${ROOT}/logs/watchdog.log             # 看门狗日志"
echo ""
bash "${ROOT}/scripts/uqm-status.sh"
