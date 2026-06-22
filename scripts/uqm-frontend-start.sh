#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=uqm.env
source "${SCRIPT_DIR}/uqm.env"

mkdir -p "${UQM_LOG_DIR}"
cd "${UQM_ROOT}/frontend"

if [[ ! -d node_modules ]]; then
  echo "==> 安装前端依赖..."
  npm ci
fi

if [[ ! -f dist/index.html ]] || [[ "${UQM_FORCE_BUILD:-0}" == "1" ]]; then
  echo "==> 构建前端静态资源..."
  npm run build
fi

exec npx vite preview --host 0.0.0.0 --port "${UQM_FRONTEND_PORT}"
