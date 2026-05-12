#!/usr/bin/env bash
# ============================================================
# init-config.sh - 新人 Onboarding 初始化脚本
#
# 用法：
#   chmod +x scripts/init-config.sh
#   ./scripts/init-config.sh [dev|test|staging|prod]
#
# 功能：
#   1. 检查 Docker 环境
#   2. 检查 .env.local 是否存在，不存在则创建
#   3. 将 Nacos 配置模板推送到 Nacos 配置中心
#   4. 等待 Nacos 就绪
# ============================================================

set -euo pipefail

NAMESPACE="${1:-dev}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
NACOS_SERVER="${NACOS_SERVER:-localhost:8848}"
NACOS_USERNAME="${NACOS_USERNAME:-nacos}"
NACOS_PASSWORD="${NACOS_PASSWORD:-nacos}"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info()    { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()    { echo -e "${YELLOW}[WARN]${NC} $1"; }
error()   { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# ================================================
# 1. 检查 Docker 环境
# ================================================
check_docker() {
    if ! command -v docker &>/dev/null; then
        error "Docker 未安装，请先安装 Docker Desktop"
    fi
    if ! docker info &>/dev/null; then
        error "Docker 未运行，请启动 Docker Desktop"
    fi
    info "Docker 环境检查通过"
}

# ================================================
# 2. 检查 .env.local
# ================================================
check_env_local() {
    local env_file="${PROJECT_ROOT}/.env.local"
    if [[ ! -f "$env_file" ]]; then
        warn ".env.local 不存在，正在创建..."
        cat > "$env_file" <<'EOF'
# ================================================
# 本地环境变量（勿提交到 Git）
# ================================================

# ----- 服务端口 -----
SERVER_PORT=8080
APP_NAME=app-service

# ----- 数据库（默认 MySQL，生产使用 192.168.13.202:23306） -----
DB_TYPE=mysql
DB_HOST=192.168.13.202
DB_PORT=23306
DB_NAME=manpou
DB_USER=root
DB_PASSWORD=manpou23306

# ----- Nacos 注册中心 -----
NACOS_SERVER=localhost:8848
NACOS_NAMESPACE=dev

# ----- Redis -----
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redis123
REDIS_DB=0

# ----- Kafka -----
KAFKA_BOOTSTRAP_SERVERS=localhost:29092

# ----- MinIO -----
MINIO_ENDPOINT=localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin123
MINIO_BUCKET=app-files

# ----- Java 服务特定 -----
JAVA_DSN=jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
EOF
        info ".env.local 创建完成，请根据需要修改"
    else
        info ".env.local 已存在"
    fi
}

# ================================================
# 3. 等待 Nacos 就绪
# ================================================
wait_for_nacos() {
    local max_attempts=30
    local attempt=1
    info "等待 Nacos 就绪（${NACOS_SERVER}）..."

    while [[ $attempt -le $max_attempts ]]; do
        if curl -sf "http://${NACOS_SERVER}/nacos/v1/console/health/readiness" &>/dev/null; then
            info "Nacos 已就绪"
            return 0
        fi
        echo -n "."
        sleep 2
        ((attempt++))
    done
    error "Nacos 启动超时（${max_attempts} 次 × 2s）"
}

# ================================================
# 4. 推送 Nacos 配置
# ================================================
push_nacos_config() {
    info "推送 Nacos 配置到 ${NAMESPACE} 命名空间..."

    local config_dir="${PROJECT_ROOT}/config/nacos/${NAMESPACE}"
    if [[ ! -d "$config_dir" ]]; then
        warn "Nacos 配置目录不存在：${config_dir}，跳过"
        return 0
    fi

    # 确保命名空间存在
    ensure_namespace "${NAMESPACE}"

    # 推送共享配置
    local shared_file="${PROJECT_ROOT}/config/nacos/shared-common.yml"
    if [[ -f "$shared_file" ]]; then
        push_config "shared-common.yml" "$(cat "$shared_file")"
    fi

    # 推送各模块配置
    for config_file in "${config_dir}"/*.yml; do
        [[ -f "$config_file" ]] || continue
        local filename="$(basename "$config_file")"
        push_config "$filename" "$(cat "$config_file")"
    done

    info "Nacos 配置推送完成"
}

# 确保命名空间存在
ensure_namespace() {
    local ns="$1"
    local response
    response=$(curl -s -X POST "http://${NACOS_SERVER}/nacos/v1/console/namespaces" \
        -d "custom=true&namespaceName=${ns}&namespace=${ns}" \
        -u "${NACOS_USERNAME}:${NACOS_PASSWORD}" || true)
    info "命名空间 ${ns} 检查/创建完成"
}

# 推送单个配置到 Nacos
push_config() {
    local filename="$1"
    local content="$2"
    local response

    # 对 content 进行 URL 编码
    local encoded_content
    encoded_content=$(python3 -c "import urllib.parse; print(urllib.parse.quote('''$content'''))" 2>/dev/null || \
                     python -c "import urllib; print(urllib.quote('''$content'''))" 2>/dev/null || \
                     echo "$content")

    response=$(curl -s -X POST "http://${NACOS_SERVER}/nacos/v1/cs/configs" \
        -d "dataId=${filename}&group=DEFAULT_GROUP&content=${encoded_content}&type=yaml" \
        -u "${NACOS_USERNAME}:${NACOS_PASSWORD}")

    if echo "$response" | grep -q '"message":"success"\|true'; then
        info "  ✓ ${filename}"
    else
        warn "  ✗ ${filename}（可能已存在或配置重复）"
    fi
}

# ================================================
# 5. 启动 Docker Compose
# ================================================
start_docker_compose() {
    local compose_file="${PROJECT_ROOT}/docker/compose.yaml"
    if [[ ! -f "$compose_file" ]]; then
        warn "Docker Compose 文件不存在：${compose_file}"
        return 0
    fi

    # 检查是否已有容器运行
    if docker ps --format '{{.Names}}' | grep -q "^nacos$"; then
        info "Docker 服务已在运行，跳过启动"
    else
        info "启动 Docker Compose 服务..."
        docker compose -f "$compose_file" up -d
        info "Docker Compose 服务启动完成"
    fi
}

# ================================================
# 主流程
# ================================================
main() {
    echo ""
    echo "========================================"
    echo "  企业级开发环境初始化"
    echo "  目标命名空间: ${NAMESPACE}"
    echo "========================================"
    echo ""

    check_docker
    check_env_local
    start_docker_compose
    wait_for_nacos
    push_nacos_config "${NAMESPACE}"

    echo ""
    echo "========================================"
    echo -e "  ${GREEN}初始化完成！${NC}"
    echo ""
    echo "  下一步："
    echo "  1. 打开 http://localhost:8848/nacos"
    echo "     用户名/密码: nacos / nacos"
    echo "  2. 启动 Java 服务（IDE 中运行 TemplateApplication）"
    echo "  3. 访问 Swagger: http://localhost:8080/swagger-ui.html"
    echo "  4. MinIO Console: http://localhost:9001"
    echo "  5. Grafana: http://localhost:3000（admin/admin123）"
    echo "========================================"
}

main "$@"
