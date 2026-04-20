#!/bin/bash
# ============================================================
# stop-all.sh - Phase 0 三服务一键停止
#
# 用法：
#   ./stop-all.sh          # 停止全部
#   ./stop-all.sh manpou   # 仅停止 manpou-allinone
#   ./stop-all.sh user     # 仅停止 user-service
#   ./stop-all.sh web      # 仅停止前端
# ============================================================

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# ---- 配置 ----
ALLINONE_PORT=18090
USER_PORT=18081
WEB_PORT=13000

# ---- 颜色 ----
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

info()   { echo -e "${GREEN}[INFO]${NC}  $1"; }
warn()   { echo -e "${YELLOW}[WARN]${NC} $1"; }
error()  { echo -e "${RED}[ERROR]${NC} $1"; }
ok()     { echo -e "${GREEN}[OK]${NC}   $1"; }

# ============================================================
# 工具函数
# ============================================================

# 获取占用端口的进程 PID（Windows git-bash 兼容）
get_port_pid() {
    local port="$1"
    # Windows git-bash: 使用 netstat 找到 PID
    if command -v netstat &>/dev/null; then
        netstat -tlnp 2>/dev/null | grep ":${port} " | \
            grep -oP '\s+[0-9]+$' | tr -d ' ' | head -1
    elif command -v ss &>/dev/null; then
        ss -tlnp 2>/dev/null | grep ":${port} " | \
            grep -oP 'pid=\K[0-9]+' | head -1
    elif command -v lsof &>/dev/null; then
        lsof -ti ":${port}" 2>/dev/null | head -1
    fi
}

# 读取 PID 文件
read_pid_file() {
    local pid_file="$1"
    if [[ -f "$pid_file" ]]; then
        cat "$pid_file"
    fi
}

# 强制关闭进程（支持 PID 文件 + 端口检测）
kill_by_pid() {
    local pid="$1"
    local name="$2"
    if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
        kill "$pid" 2>/dev/null || true
        sleep 1
        if kill -0 "$pid" 2>/dev/null; then
            kill -9 "$pid" 2>/dev/null || true
        fi
        ok "${name} (PID=${pid}) 已停止"
    fi
}

# 停止指定服务
stop_service() {
    local name="$1"
    local port="$2"

    local pid_file="${PROJECT_DIR}/logs/${name}.pid"
    local pid

    # 优先用 PID 文件
    if [[ -f "$pid_file" ]]; then
        pid=$(read_pid_file "$pid_file")
        if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
            kill "$pid" 2>/dev/null || true
            sleep 1
            if kill -0 "$pid" 2>/dev/null; then
                kill -9 "$pid" 2>/dev/null || true
            fi
            ok "${name} (PID=${pid}) 已停止"
            rm -f "$pid_file"
            return 0
        else
            rm -f "$pid_file"
        fi
    fi

    # 兜底：按端口查找
    pid=$(get_port_pid "$port")
    if [[ -n "$pid" ]]; then
        kill "$pid" 2>/dev/null || true
        sleep 1
        if kill -0 "$pid" 2>/dev/null; then
            kill -9 "$pid" 2>/dev/null || true
        fi
        ok "${name} (端口 ${port}, PID=${pid}) 已停止"
    else
        info "${name} (端口 ${port}) 未运行"
    fi
}

# ============================================================
# 主流程
# ============================================================
main() {
    local target="${1:-all}"

    echo ""
    echo "========================================"
    echo "  Phase 0 服务停止"
    echo "========================================"
    echo ""

    case "$target" in
        manpou)
            stop_service "manpou-allinone" "$ALLINONE_PORT"
            ;;
        user)
            stop_service "user-service" "$USER_PORT"
            ;;
        web)
            stop_service "web" "$WEB_PORT"
            ;;
        all)
            stop_service "web"              "$WEB_PORT"
            stop_service "user-service"      "$USER_PORT"
            stop_service "manpou-allinone"   "$ALLINONE_PORT"
            echo ""
            echo "========================================"
            echo "  全部 Phase 0 服务已停止"
            echo "========================================"
            ;;
        *)
            echo "用法: $0 [all|manpou|user|web]"
            echo "  all     - 停止全部三个服务（默认）"
            echo "  manpou  - 仅停止 manpou-allinone"
            echo "  user    - 仅停止 user-service"
            echo "  web     - 仅停止前端"
            exit 1
            ;;
    esac
}

main "$@"
