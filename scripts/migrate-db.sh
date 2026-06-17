#!/usr/bin/env bash
# 增量数据库迁移：按 migrate-p*.sql 文件名顺序执行，已执行版本记录在 schema_migration 表
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MIG_DIR="$ROOT/backend/src/main/resources/db"

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-uqm}"
DB_USER="${DB_USER:-uqm}"
DB_PASSWORD="${DB_PASSWORD:-uqm_dev_2026}"

MYSQL=(mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" --default-character-set=utf8mb4)

usage() {
  cat <<EOF
用法: $0 [--baseline] [--baseline-all]

  默认：仅执行尚未记录在 schema_migration 中的迁移脚本
  --baseline-all：将现有全部 migrate-p*.sql 标记为已执行（不运行 SQL），适用于已手工升级过的库
  --baseline：同 --baseline-all
EOF
}

ensure_migration_table() {
  "${MYSQL[@]}" -e "
CREATE TABLE IF NOT EXISTS \`schema_migration\` (
  \`version\` VARCHAR(64) PRIMARY KEY,
  \`applied_at\` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
"
}

is_applied() {
  local version="$1"
  local count
  count=$("${MYSQL[@]}" -N -e "SELECT COUNT(*) FROM schema_migration WHERE version='${version}'")
  [[ "$count" -gt 0 ]]
}

mark_applied() {
  local version="$1"
  "${MYSQL[@]}" -e "INSERT IGNORE INTO schema_migration (version) VALUES ('${version}')"
}

baseline_all() {
  ensure_migration_table
  local f version
  for f in "$MIG_DIR"/migrate-p*.sql; do
    [[ -f "$f" ]] || continue
    version=$(basename "$f" .sql)
    mark_applied "$version"
    echo "  baseline: $version"
  done
  echo "==> 已标记全部迁移为已执行"
}

apply_pending() {
  ensure_migration_table

  local perm_count
  perm_count=$("${MYSQL[@]}" -N -e "SELECT COUNT(*) FROM permission" 2>/dev/null || echo 0)
  local tracked
  tracked=$("${MYSQL[@]}" -N -e "SELECT COUNT(*) FROM schema_migration" 2>/dev/null || echo 0)

  if [[ "$tracked" -eq 0 && "$perm_count" -ge 14 ]]; then
    echo "==> 检测到已初始化数据库（permission=$perm_count），自动 baseline 历史迁移..."
    baseline_all
    return
  fi

  local f version
  for f in $(ls "$MIG_DIR"/migrate-p*.sql 2>/dev/null | sort); do
    version=$(basename "$f" .sql)
    if is_applied "$version"; then
      echo "  skip: $version"
      continue
    fi
    echo "==> apply: $version"
    "${MYSQL[@]}" < "$f"
    mark_applied "$version"
  done
  echo "==> 迁移完成"
}

main() {
  local mode="apply"
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --baseline|--baseline-all) mode="baseline" ;;
      -h|--help) usage; exit 0 ;;
      *) echo "未知参数: $1"; usage; exit 1 ;;
    esac
    shift
  done

  if [[ "$mode" == "baseline" ]]; then
    baseline_all
  else
    apply_pending
  fi
}

main "$@"
