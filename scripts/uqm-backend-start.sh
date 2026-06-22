#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=uqm.env
source "${SCRIPT_DIR}/uqm.env"

mkdir -p "${UQM_LOG_DIR}"

if [[ ! -f "${UQM_JAR}" ]]; then
  echo "==> JAR 不存在，正在构建后端..."
  cd "${UQM_ROOT}/backend"
  mvn -q package -DskipTests
fi

JAVA_ARGS=(-Duser.timezone=Asia/Shanghai)
if [[ -n "${UQM_SPRING_PROFILE:-}" ]]; then
  JAVA_ARGS+=(-Dspring.profiles.active="${UQM_SPRING_PROFILE}")
fi

exec "${UQM_JAVA}" "${JAVA_ARGS[@]}" -jar "${UQM_JAR}"
