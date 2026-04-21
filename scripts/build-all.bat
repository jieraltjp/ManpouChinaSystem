@echo off
chcp 65001 > nul 2>&1
setlocal EnableDelayedExpansion

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_DIR=%%~fI"

cd /d "%PROJECT_DIR%"

echo.
echo ======================================
echo   ManpouChinaSystem - Build All Services
echo ======================================

set "SERVICES=manpou-allinone user-service product-service procurement-service warehouse-service customs-service logistics-service finance-service notification-service"

for %%svc in (%SERVICES%) do (
    echo.
    echo ^>^>^> Building %%svc ...

    set "svc_dir=%PROJECT_DIR%\apps\%%svc"

    if not exist "!svc_dir!" (
        echo     SKIP: %%svc not found
        goto :build_next
    )

    cd /d "!svc_dir!"

    if exist "mvnw.cmd" (
        call mvnw.cmd clean package -DskipTests -Drevision=1.0.0
        if errorlevel 1 (
            echo     [FAIL] %%svc build failed
            goto :build_next
        )
    ) else if exist "pom.xml" (
        call mvn clean package -DskipTests -Drevision=1.0.0
        if errorlevel 1 (
            echo     [FAIL] %%svc build failed
            goto :build_next
        )
    ) else (
        echo     SKIP: pom.xml not found in %%svc
        goto :build_next
    )

    echo     [OK] %%svc build complete

    :build_next
    cd /d "%PROJECT_DIR%" > nul 2>&1
)

echo.
echo ======================================
echo   All services built
echo ======================================

endlocal
