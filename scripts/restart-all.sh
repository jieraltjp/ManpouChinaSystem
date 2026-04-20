#!/bin/bash
# ============================================================
# restart-all.sh - Phase 0 三服务一键重启
#
# 用法：
#   ./restart-all.sh          # 重启全部
#   ./restart-all.sh manpou   # 仅重启 manpou-allinone
#   ./restart-all.sh user     # 仅重启 user-service
#   ./restart-all.sh web      # 仅重启前端
# ============================================================

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
target="${1:-all}"

"$SCRIPT_DIR/stop-all.sh" "$target"
echo ""
echo "========================================"
echo "  等待 2 秒..."
echo "========================================"
sleep 2
echo ""
"$SCRIPT_DIR/start-all.sh" "$target"
