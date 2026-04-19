#!/bin/bash
# 构建所有微服务

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "======================================"
echo "  ManpouChinaSystem - 构建所有服务"
echo "======================================"

# 准备构建参数
JAVA_VERSION="${JAVA_VERSION:-17}"
IMAGE_REGISTRY="${IMAGE_REGISTRY:-registry.manpou.com}"
BUILD_TIME="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
GIT_COMMIT="$(git rev-parse --short HEAD 2>/dev/null || echo 'unknown')"

# 服务列表
SERVICES=(
    "user-service"
    "product-service"
    "procurement-service"
    "warehouse-service"
    "customs-service"
    "logistics-service"
    "finance-service"
    "notification-service"
)

# 构建每个服务
for svc in "${SERVICES[@]}"; do
    echo ""
    echo ">>> 构建 $svc ..."

    svc_dir="$PROJECT_DIR/apps/$svc"

    if [ ! -d "$svc_dir" ]; then
        echo "    跳过: $svc 不存在"
        continue
    fi

    # Maven 构建
    cd "$svc_dir"
    ./mvnw clean package -DskipTests \
        -Drevision=1.0.0 \
        -Dgit.commit="$GIT_COMMIT" \
        -Dbuild.time="$BUILD_TIME"

    echo "    ✓ $svc 构建完成"
done

echo ""
echo "======================================"
echo "  所有服务构建完成！"
echo "======================================"
