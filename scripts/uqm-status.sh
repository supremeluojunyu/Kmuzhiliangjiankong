#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=uqm.env
source "${SCRIPT_DIR}/uqm.env"

check() {
  local name="$1" url="$2"
  local code
  code=$(curl -s -o /dev/null -w '%{http_code}' --connect-timeout 5 "${url}" 2>/dev/null || echo "000")
  if [[ "${code}" =~ ^[23] ]]; then
    printf '  %-18s OK (%s)\n' "${name}" "${code}"
  else
    printf '  %-18s FAIL (%s)\n' "${name}" "${code}"
  fi
}

echo "==> 系统服务"
for u in mysql redis-server nginx; do
  st=$(systemctl is-active "${u}.service" 2>/dev/null || echo unknown)
  printf '  %-18s %s\n' "${u}" "${st}"
done

echo ""
echo "==> UQM 用户服务"
for u in uqm-backend uqm-frontend frpc-uqm uqm-watchdog.timer; do
  st=$(systemctl --user is-active "${u}" 2>/dev/null || echo unknown)
  printf '  %-18s %s\n' "${u}" "${st}"
done

echo ""
echo "==> HTTP 探活"
check "backend" "http://127.0.0.1:8080/api/health"
check "frontend" "http://127.0.0.1:${UQM_FRONTEND_PORT}/"
check "public" "${UQM_PUBLIC_URL}/"

echo ""
echo "日志: ${UQM_LOG_DIR}/"
