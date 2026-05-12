@echo off
chcp 65001 > nul 2>&1
setlocal enabledelayedexpansion

:: ============================================================
:: init-config.bat - 新人 Onboarding 初始化脚本
:: ============================================================

set "NAMESPACE=%~1"
if "%NAMESPACE%"=="" set "NAMESPACE=dev"

set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR:~0,-1%"

:: 环境变量默认值
if not defined NACOS_SERVER set "NACOS_SERVER=localhost:8848"
if not defined NACOS_USERNAME set "NACOS_USERNAME=nacos"
if not defined NACOS_PASSWORD set "NACOS_PASSWORD=nacos"

set "INFO=[INFO] "
set "WARN=[WARN] "
set "ERROR=[ERROR]"

echo.
echo =========================================
echo   企业级开发环境初始化
echo   目标命名空间: %NAMESPACE%
echo =========================================
echo.

:: ================================================
:: 1. 检查 Docker 环境
:: ================================================
echo !INFO!检查 Docker 环境...
docker info > nul 2>&1
if errorlevel 1 (
    echo !ERROR!Docker 未运行，请启动 Docker Desktop
    endlocal
    exit /b 1
)
echo !INFO!Docker 环境检查通过

:: ================================================
:: 2. 检查 .env.local
:: ================================================
set "env_file=%PROJECT_DIR%\.env.local"
if not exist "!env_file!" (
    echo !WARN!.env.local 不存在，正在创建...
    (
        echo # ================================================
        echo # 本地环境变量（勿提交到 Git）
        echo # ================================================
        echo SERVER_PORT=8080
        echo APP_NAME=app-service
        echo DB_TYPE=mysql
        echo DB_HOST=192.168.13.202
        echo DB_PORT=23306
        echo DB_NAME=manpou
        echo DB_USER=root
        echo DB_PASSWORD=manpou23306
        echo NACOS_SERVER=localhost:8848
        echo NACOS_NAMESPACE=dev
        echo REDIS_HOST=localhost
        echo REDIS_PORT=6379
        echo REDIS_PASSWORD=redis123
        echo REDIS_DB=0
        echo KAFKA_BOOTSTRAP_SERVERS=localhost:29092
        echo MINIO_ENDPOINT=localhost:9000
        echo MINIO_ACCESS_KEY=minioadmin
        echo MINIO_SECRET_KEY=minioadmin123
        echo MINIO_BUCKET=app-files
        echo JAVA_DSN=jdbc:mysql://%%DB_HOST%%:%%DB_PORT%%/%%DB_NAME%%?useSSL=false^&allowPublicKeyRetrieval=true^&serverTimezone=Asia/Shanghai
    ) > "!env_file!"
    echo !INFO!.env.local 创建完成，请根据需要修改
) else (
    echo !INFO!.env.local 已存在
)

:: ================================================
:: 3. 启动 Docker Compose
:: ================================================
set "compose_file=%PROJECT_DIR%\docker\compose.yaml"
if not exist "!compose_file!" (
    echo !WARN!Docker Compose 文件不存在：!compose_file!
) else (
    docker ps --format "{{.Names}}" | findstr /c:"nacos" > nul 2>&1
    if not errorlevel 1 (
        echo !INFO!Docker 服务已在运行，跳过启动
    ) else (
        echo !INFO!启动 Docker Compose 服务...
        docker compose -f "!compose_file!" up -d
        echo !INFO!Docker Compose 服务启动完成
    )
)

:: ================================================
:: 4. 等待 Nacos 就绪
:: ================================================
echo !INFO!等待 Nacos 就绪（%NACOS_SERVER%）...
set "max_attempts=30"
set "attempt=0"
:wait_nacos_loop
    set /a attempt+=1
    curl -sf "http://%NACOS_SERVER%/nacos/v1/console/health/readiness" > nul 2>&1
    if not errorlevel 1 (
        echo !INFO!Nacos 已就绪
        goto :nacos_ready
    )
    if !attempt! GEQ %max_attempts% (
        echo !ERROR!Nacos 启动超时（%max_attempts% 次 x 2s）
        endlocal
        exit /b 1
    )
    ping -n 3 127.0.0.1 > nul 2>&1
    goto :wait_nacos_loop
:nacos_ready

:: ================================================
:: 5. 提示 Nacos 配置
:: ================================================
set "config_dir=%PROJECT_DIR%\config\nacos\%NAMESPACE%"
if not exist "!config_dir!" (
    echo !WARN!Nacos 配置目录不存在：!config_dir!，跳过
) else (
    echo !INFO!请手动访问 http://localhost:8848/nacos 推送配置
)

echo.
echo =========================================
echo   初始化完成！
echo.
echo   下一步：
echo   1. 打开 http://localhost:8848/nacos
echo      用户名/密码: nacos / nacos
echo   2. 启动 Java 服务（IDE 中运行 TemplateApplication）
echo   3. 访问 Swagger: http://localhost:8080/swagger-ui.html
echo   4. MinIO Console: http://localhost:9001
echo   5. Grafana: http://localhost:3000（admin/admin123）
echo =========================================

endlocal
