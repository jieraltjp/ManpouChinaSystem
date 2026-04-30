#!/bin/bash
# ============================================================
# start-all.sh - Phase 0 三服务一键启动
#
# 用法：
#   ./start-all.sh          # 启动全部
#   ./start-all.sh manpou   # 仅启动 manpou-allinone
#   ./start-all.sh user     # 仅启动 user-service
#   ./start-all.sh web      # 仅启动前端
#
# 行为：
#   1. 检测端口占用，有则先 kill
#   2. 后台启动所有服务
#   3. 等待就绪后输出结果
# ============================================================

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# ---- 配置 ----
ALLINONE_PORT=18090
USER_PORT=18081
WEB_PORT=13000

JAVA_OPTS="-Xms512m -Xmx1024m"

# ---- Profile 选择 ----
# Windows (git-bash): local（H2 内存数据库）
# Linux (Ubuntu Server): development（远程 MySQL 192.168.13.202:23306）
if is_windows; then
    SPRING_PROFILE="local"
else
    SPRING_PROFILE="${SPRING_PROFILES_ACTIVE:-development}"
fi

# ---- 颜色 ----
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

info()   { echo -e "${GREEN}[INFO]${NC}  $1"; }
warn()   { echo -e "${YELLOW}[WARN]${NC} $1"; }
error()  { echo -e "${RED}[ERROR]${NC} $1"; }
step()   { echo -e "${CYAN}[STEP]${NC}  $1"; }
ok()     { echo -e "${GREEN}[OK]${NC}   $1"; }

# ============================================================
# 工具函数
# ============================================================

# ---- Windows (git-bash / MSYS) 检测 ----
is_windows() {
    case "$(uname -s)" in
        MINGW*|MSYS*|CYGWIN*) return 0 ;;
        *) return 1 ;;
    esac
}

# 获取占用端口的进程 PID（Windows 用 PowerShell，Linux 用 ss/lsof）
get_port_pid() {
    local port="$1"
    if is_windows; then
        # Windows: PowerShell Get-NetTCPConnection
        powershell -Command "
            \$c = Get-NetTCPConnection -LocalPort ${port} -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty OwningProcess
            if (\$c) { Write-Output \$c }
        " 2>/dev/null | tr -d '\r\n'
    else
        # Linux: ss → pid=
        if command -v ss &>/dev/null; then
            ss -tlnp 2>/dev/null | grep ":${port} " | \
                grep -oP 'pid=\K[0-9]+' | head -1
        elif command -v lsof &>/dev/null; then
            lsof -ti ":${port}" 2>/dev/null | head -1
        fi
    fi
}

# 检测端口是否被占用
is_port_used() {
    local port="$1"
    if is_windows; then
        powershell -Command "Get-NetTCPConnection -LocalPort ${port} -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty LocalPort" 2>/dev/null | grep -q "${port}"
    elif command -v ss &>/dev/null; then
        ss -tlnp 2>/dev/null | grep -q ":${port} "
    elif command -v netstat &>/dev/null; then
        netstat -tlnp 2>/dev/null | grep -q ":${port} "
    else
        lsof -i ":${port}" &>/dev/null
    fi
}

# 强制关闭端口
kill_port() {
    local port="$1"
    local svc="$2"
    local pid
    pid=$(get_port_pid "$port")
    if [[ -n "$pid" ]] && [[ "$pid" != "0" ]]; then
        warn "端口 ${port} 被占用（PID=${pid}），正在关闭 ${svc}..."
        if is_windows; then
            powershell -Command "Stop-Process -Id ${pid} -Force -ErrorAction SilentlyContinue" 2>/dev/null || true
        else
            kill "$pid" 2>/dev/null || true
        fi
        sleep 2
        # 如果还在，强制杀死
        if is_port_used "$port"; then
            local new_pid
            new_pid=$(get_port_pid "$port")
            if [[ -n "$new_pid" ]] && [[ "$new_pid" != "0" ]]; then
                if is_windows; then
                    powershell -Command "Stop-Process -Id ${new_pid} -Force -ErrorAction SilentlyContinue" 2>/dev/null || true
                else
                    kill -9 "$new_pid" 2>/dev/null || true
                fi
            fi
            sleep 1
        fi
    fi
}

# 等待端口就绪
wait_port() {
    local port="$1"
    local name="$2"
    local max_wait=30
    local count=0
    while ! is_port_used "$port"; do
        sleep 1
        ((count++)) || true
        if [[ $count -ge $max_wait ]]; then
            error "${name} 启动超时（${max_wait}s）"
            return 1
        fi
    done
    ok "${name} 就绪 (端口 ${port})"
    return 0
}

# 检测命令是否存在
has_cmd() {
    command -v "$1" &>/dev/null
}

# ============================================================
# 服务启动函数
# ============================================================

start_manpou() {
    step "启动 manpou-allinone (${ALLINONE_PORT})..."

    # 检测并关闭已有进程
    if is_port_used "$ALLINONE_PORT"; then
        kill_port "$ALLINONE_PORT" "manpou-allinone"
    fi

    local jar_file="${PROJECT_DIR}/apps/manpou-allinone/target"
    # 查找 jar
    local jar
    jar=$(ls "${jar_file}"/manpou-allinone-*.jar 2>/dev/null | head -1)

    if [[ -z "$jar" ]]; then
        warn "未找到 manpou-allinone jar，正在编译..."
        cd "${PROJECT_DIR}/apps/manpou-allinone"
        if ! mvn package -DskipTests -q; then
            error "manpou-allinone 编译失败"
            return 1
        fi
        jar=$(ls "${jar_file}"/manpou-allinone-*.jar 2>/dev/null | head -1)
    fi

    if [[ -z "$jar" ]]; then
        error "仍无法找到 manpou-allinone jar"
        return 1
    fi

    nohup java $JAVA_OPTS -jar "$jar" \
        --server.port=${ALLINONE_PORT} \
        --spring.profiles.active=${SPRING_PROFILE} \
        > "${PROJECT_DIR}/logs/manpou-allinone.log" 2>&1 &
    local pid=$!
    echo "$pid" > "${PROJECT_DIR}/logs/manpou-allinone.pid"
    info "manpou-allinone 已启动 (PID=${pid})"

    wait_port "$ALLINONE_PORT" "manpou-allinone"
}

start_user() {
    step "启动 user-service (${USER_PORT})..."

    if is_port_used "$USER_PORT"; then
        kill_port "$USER_PORT" "user-service"
    fi

    local svc_dir="${PROJECT_DIR}/apps/user-service"
    local jar_dir="${svc_dir}/target"
    local jar
    jar=$(ls "${jar_dir}"/user-service-*.jar 2>/dev/null | head -1)

    if [[ -z "$jar" ]]; then
        warn "未找到 user-service jar，正在编译..."
        cd "$svc_dir"
        if ! ./mvnw package -DskipTests -q; then
            error "user-service 编译失败，跳过（Phase 0 可选）"
            return 0
        fi
        jar=$(ls "${jar_dir}"/user-service-*.jar 2>/dev/null | head -1)
    fi

    if [[ -z "$jar" ]]; then
        warn "user-service jar 不存在，跳过"
        return 0
    fi

    nohup java $JAVA_OPTS -jar "$jar" \
        --server.port=${USER_PORT} \
        --spring.profiles.active=${SPRING_PROFILE} \
        > "${PROJECT_DIR}/logs/user-service.log" 2>&1 &
    local pid=$!
    echo "$pid" > "${PROJECT_DIR}/logs/user-service.pid"
    info "user-service 已启动 (PID=${pid})"

    wait_port "$USER_PORT" "user-service"
}

start_web() {
    step "启动前端 web (${WEB_PORT})..."

    if is_port_used "$WEB_PORT"; then
        kill_port "$WEB_PORT" "web"
    fi

    local web_dir="${PROJECT_DIR}/apps/web"

    # 检查 node_modules
    if [[ ! -d "${web_dir}/node_modules" ]]; then
        warn "未安装 node_modules，正在 npm install..."
        cd "$web_dir"
        npm install
    fi

    # 后台启动 vite dev server
    cd "$web_dir"
    nohup npm run dev \
        > "${PROJECT_DIR}/logs/web.log" 2>&1 &
    local pid=$!
    echo "$pid" > "${PROJECT_DIR}/logs/web.pid"
    info "web 已启动 (PID=${pid})"

    wait_port "$WEB_PORT" "web"
}

# ============================================================
# 状态检测
# ============================================================
status_all() {
    echo ""
    echo "========================================"
    echo "  Phase 0 服务状态"
    echo "========================================"

    local all_ok=true

    check_service() {
        local name="$1"
        local port="$2"
        if is_port_used "$port"; then
            local pid
            pid=$(get_port_pid "$port")
            echo -e "  ${GREEN}●${NC}  ${name}    :${port}  (PID=${pid})"
        else
            echo -e "  ${RED}○${NC}  ${name}    :${port}  (未运行)"
            all_ok=false
        fi
    }

    check_service "manpou-allinone"  "$ALLINONE_PORT"
    check_service "user-service"      "$USER_PORT"
    check_service "web"              "$WEB_PORT"

    echo ""
    if $all_ok; then
        echo -e "  ${GREEN}全部服务已就绪${NC}"
        echo ""
        echo "  访问地址："
        echo "    前端：  http://localhost:${WEB_PORT}"
        echo "    allinone: http://localhost:${ALLINONE_PORT}/swagger-ui/index.html"
    else
        echo -e "  ${YELLOW}部分服务未运行${NC}"
    fi
    echo "========================================"
}

# ============================================================
# 主流程
# ============================================================
main() {
    local target="${1:-all}"

    # 确保日志目录存在
    mkdir -p "${PROJECT_DIR}/logs"

    echo ""
    echo "========================================"
    echo "  ManpouChinaSystem - Phase 0 启动"
    echo "========================================"
    echo ""

    case "$target" in
        manpou)
            start_manpou
            ;;
        user)
            start_user
            ;;
        web)
            start_web
            ;;
        status)
            status_all
            ;;
        all)
            start_manpou
            echo ""
            start_user
            echo ""
            start_web
            echo ""
            status_all
            ;;
        *)
            echo "用法: $0 [all|manpou|user|web|status]"
            echo "  all     - 启动全部三个服务（默认）"
            echo "  manpou  - 仅启动 manpou-allinone"
            echo "  user    - 仅启动 user-service"
            echo "  web     - 仅启动前端"
            echo "  status  - 查看服务状态"
            exit 1
            ;;
    esac
}

main "$@"
