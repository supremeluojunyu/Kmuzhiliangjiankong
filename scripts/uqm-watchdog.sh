#!/usr/bin/env bash
# 健康检查：依赖服务 down 或 HTTP 不可达时自动重启
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=uqm.env
source "${SCRIPT_DIR}/uqm.env"

LOG="${UQM_LOG_DIR}/watchdog.log"
mkdir -p "${UQM_LOG_DIR}"

log() {
  echo "$(date '+%Y-%m-%d %H:%M:%S') $*" >> "${LOG}"
}

http_ok() {
  local url="$1"
  local code
  code=$(curl -s -o /dev/null -w '%{http_code}' --connect-timeout 5 --max-time 10 "${url}" 2>/dev/null || echo "000")
  [[ "${code}" =~ ^[23] ]]
}

restart_user() {
  local unit="$1"
  log "RESTART user ${unit}"
  systemctl --user restart "${unit}" || log "FAILED restart ${unit}"
}

restart_system() {
  local unit="$1"
  log "RESTART system ${unit}"
  sudo systemctl restart "${unit}" || log "FAILED restart ${unit}"
}

service_uptime_sec() {
  local unit="$1"
  local ts
  ts=$(systemctl --user show -p ActiveEnterTimestamp --value "${unit}" 2>/dev/null || true)
  [[ -z "${ts}" || "${ts}" == "n/a" ]] && echo 0 && return
  local start_epoch now_epoch
  start_epoch=$(date -d "${ts}" +%s 2>/dev/null || echo 0)
  now_epoch=$(date +%s)
  echo $((now_epoch - start_epoch))
}

ensure_system_active() {
  local unit="$1"
  if ! systemctl is-active --quiet "${unit}"; then
    log "WARN ${unit} inactive"
    restart_system "${unit}"
  fi
}

ensure_user_active() {
  local unit="$1"
  local state
  state=$(systemctl --user show -p ActiveState --value "${unit}" 2>/dev/null || echo unknown)
  if [[ "${state}" == "activating" ]]; then
    return
  fi
  if ! systemctl --user is-active --quiet "${unit}"; then
    log "WARN ${unit} inactive (state=${state})"
    restart_user "${unit}"
  fi
}

check_http_service() {
  local unit="$1" url="$2" min_uptime="${3:-45}"
  ensure_user_active "${unit}"
  if ! systemctl --user is-active --quiet "${unit}"; then
    return
  fi
  local sub uptime
  sub=$(systemctl --user show -p SubState --value "${unit}" 2>/dev/null || echo unknown)
  [[ "${sub}" != "running" ]] && return
  uptime=$(service_uptime_sec "${unit}")
  if (( uptime < min_uptime )); then
    return
  fi
  if ! http_ok "${url}"; then
    log "WARN ${unit} HTTP failed (${url}, uptime=${uptime}s)"
    restart_user "${unit}"
  fi
}

# 系统级基础依赖
ensure_system_active mysql.service
ensure_system_active redis-server.service
ensure_system_active nginx.service

# 应用栈
check_http_service uqm-backend.service "http://127.0.0.1:8080/api/health" 60
check_http_service uqm-frontend.service "http://127.0.0.1:${UQM_FRONTEND_PORT}/" 30
ensure_user_active frpc-uqm.service

# 公网探测（服务稳定运行后再检查，避免启动期误重启）
backend_up=$(service_uptime_sec uqm-backend.service)
frontend_up=$(service_uptime_sec uqm-frontend.service)
if (( backend_up >= 60 && frontend_up >= 30 )) && ! http_ok "${UQM_PUBLIC_URL}/"; then
  log "WARN public URL check failed: ${UQM_PUBLIC_URL}"
  restart_user frpc-uqm.service
  sleep 5
  if ! http_ok "${UQM_PUBLIC_URL}/"; then
    restart_user uqm-frontend.service
  fi
fi

log "OK watchdog pass"
