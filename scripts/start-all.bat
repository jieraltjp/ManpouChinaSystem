@echo off
chcp 65001 > nul 2>&1
setlocal EnableDelayedExpansion

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_DIR=%%~fI"

set "ALLINONE_PORT=18090"
set "USER_PORT=18081"
set "WEB_PORT=13000"
set "JAVA_OPTS=-Xms512m -Xmx1024m"

set "INFO=[INFO] "
set "WARN=[WARN] "
set "STEP=[STEP] "
set "OK=[OK]   "

REM Parse argument
set "TARGET=%~1"
if "!TARGET!"=="" set "TARGET=all"

REM Ensure logs dir exists
if not exist "%PROJECT_DIR%\logs" mkdir "%PROJECT_DIR%\logs"

echo.
echo =========================================
echo   ManpouChinaSystem - Phase 0 Startup
echo =========================================
echo.

REM Dispatch
if "!TARGET!"=="manpou" goto :do_manpou
if "!TARGET!"=="user"   goto :do_user
if "!TARGET!"=="web"    goto :do_web
if "!TARGET!"=="status" goto :do_status
if "!TARGET!"=="all"    goto :do_all
echo Usage: %~nx0 [all^|manpou^|user^|web^|status]
goto :end

REM ============================================================
REM [ALL] Start all three services
REM ============================================================
:do_all
    call :do_manpou
    echo.
    call :do_user
    echo.
    call :do_web
    echo.
    call :do_status
    goto :end

REM ============================================================
REM [SERVICE] manpou-allinone
REM ============================================================
:do_manpou
    echo !STEP!Starting manpou-allinone ^(!ALLINONE_PORT!^)...

    REM Kill existing process on port
    for /f "delims=" %%i in ('powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort !ALLINONE_PORT! -ErrorAction SilentlyContinue ^| Select-Object -First 1 -ExpandProperty OwningProcess" 2^>nul') do (
        echo !WARN!Port !ALLINONE_PORT! occupied ^(PID=%%i^), killing...
        powershell -NoProfile -Command "Stop-Process -Id %%i -Force -ErrorAction SilentlyContinue" > nul 2>&1
    )
    ping -n 2 127.0.0.1 > nul 2>&1

    REM Find jar
    set "jar="
    for /f "delims=" %%f in ('dir /b /o-n "%PROJECT_DIR%\apps\manpou-allinone\target\manpou-allinone-*.jar" 2^>nul') do (
        set "jar=%PROJECT_DIR%\apps\manpou-allinone\target\%%f"
    )

    REM Compile if jar not found
    if "!jar!"=="" (
        echo !WARN!Jar not found, compiling ^(please wait^)...
        cd /d "%PROJECT_DIR%\apps\manpou-allinone"
        call mvn package -DskipTests -q -Drevision=1.0.0
        for /f "delims=" %%f in ('dir /b /o-n "%PROJECT_DIR%\apps\manpou-allinone\target\manpou-allinone-*.jar" 2^>nul') do (
            set "jar=%PROJECT_DIR%\apps\manpou-allinone\target\%%f"
        )
    )

    if "!jar!"=="" (
        echo !WARN!manpou-allinone jar still not found, skip
        goto :eof
    )

    REM Launch via PowerShell to avoid cmd quoting issues
    powershell -NoProfile -Command "Start-Process -FilePath java -ArgumentList '!JAVA_OPTS!','-jar','!jar!','--server.port=!ALLINONE_PORT!','--spring.profiles.active=local' -RedirectStandardOutput '%PROJECT_DIR%\logs\manpou-allinone.log' -RedirectStandardError '%PROJECT_DIR%\logs\manpou-allinone.err' -WindowStyle Minimized -PassThru | ForEach-Object { \$_.Id }" > "%PROJECT_DIR%\logs\manpou-allinone.pid" 2>nul
    echo !INFO!manpou-allinone launched

    REM Wait for port
    call :do_wait_port !ALLINONE_PORT! manpou-allinone
    goto :eof

REM ============================================================
REM [SERVICE] user-service
REM ============================================================
:do_user
    echo !STEP!Starting user-service ^(!USER_PORT!^)...

    for /f "delims=" %%i in ('powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort !USER_PORT! -ErrorAction SilentlyContinue ^| Select-Object -First 1 -ExpandProperty OwningProcess" 2^>nul') do (
        echo !WARN!Port !USER_PORT! occupied ^(PID=%%i^), killing...
        powershell -NoProfile -Command "Stop-Process -Id %%i -Force -ErrorAction SilentlyContinue" > nul 2>&1
    )
    ping -n 2 127.0.0.1 > nul 2>&1

    set "jar="
    for /f "delims=" %%f in ('dir /b /o-n "%PROJECT_DIR%\apps\user-service\target\user-service-*.jar" 2^>nul') do (
        set "jar=%PROJECT_DIR%\apps\user-service\target\%%f"
    )

    if "!jar!"=="" (
        echo !WARN!Jar not found, compiling...
        cd /d "%PROJECT_DIR%\apps\user-service"
        if exist "mvnw.cmd" (
            call mvnw.cmd package -DskipTests -q -Drevision=1.0.0
        ) else if exist "pom.xml" (
            call mvn package -DskipTests -q -Drevision=1.0.0
        ) else (
            echo !WARN!user-service pom.xml not found, skip
            goto :eof
        )
        for /f "delims=" %%f in ('dir /b /o-n "%PROJECT_DIR%\apps\user-service\target\user-service-*.jar" 2^>nul') do (
            set "jar=%PROJECT_DIR%\apps\user-service\target\%%f"
        )
    )

    if "!jar!"=="" (
        echo !WARN!user-service jar still not found, skip
        goto :eof
    )

    powershell -NoProfile -Command "Start-Process -FilePath java -ArgumentList '!JAVA_OPTS!','-jar','!jar!','--server.port=!USER_PORT!','--spring.profiles.active=local' -RedirectStandardOutput '%PROJECT_DIR%\logs\user-service.log' -RedirectStandardError '%PROJECT_DIR%\logs\user-service.err' -WindowStyle Minimized -PassThru | ForEach-Object { \$_.Id }" > "%PROJECT_DIR%\logs\user-service.pid" 2>nul
    echo !INFO!user-service launched

    call :do_wait_port !USER_PORT! user-service
    goto :eof

REM ============================================================
REM [SERVICE] web frontend
REM ============================================================
:do_web
    echo !STEP!Starting web ^(!WEB_PORT!^)...

    for /f "delims=" %%i in ('powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort !WEB_PORT! -ErrorAction SilentlyContinue ^| Select-Object -First 1 -ExpandProperty OwningProcess" 2^>nul') do (
        echo !WARN!Port !WEB_PORT! occupied ^(PID=%%i^), killing...
        powershell -NoProfile -Command "Stop-Process -Id %%i -Force -ErrorAction SilentlyContinue" > nul 2>&1
    )
    ping -n 2 127.0.0.1 > nul 2>&1

    set "web_dir=%PROJECT_DIR%\apps\web"

    if not exist "!web_dir!" (
        echo !WARN!web dir not found, skip
        goto :eof
    )

    if not exist "!web_dir!\node_modules" (
        echo !WARN!node_modules not found, running npm install...
        cd /d "!web_dir!"
        call npm install
    )

    cd /d "!web_dir!"
    powershell -NoProfile -Command "Start-Process -FilePath npm -ArgumentList 'run','dev' -WorkingDirectory '!web_dir!' -RedirectStandardOutput '%PROJECT_DIR%\logs\web.log' -RedirectStandardError '%PROJECT_DIR%\logs\web.err' -WindowStyle Minimized -PassThru | ForEach-Object { \$_.Id }" > "%PROJECT_DIR%\logs\web.pid" 2>nul
    echo !INFO!web launched

    call :do_wait_port !WEB_PORT! web
    goto :eof

REM ============================================================
REM [COMMON] Wait for port readiness
REM ============================================================
:do_wait_port
    set "_port=%~1"
    set "_name=%~2"
    set "_cnt=0"
    :wait_loop
        set "_found="
        for /f "delims=" %%p in ('powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort !_port! -ErrorAction SilentlyContinue ^| Select-Object -First 1 -ExpandProperty LocalPort" 2^>nul') do (
            if not "%%p"=="" set "_found=1"
        )
        if defined _found (
            echo !OK!!_name! ready ^(port !_port!^)
            goto :eof
        )
        set /a _cnt+=1
        if !_cnt! GEQ 30 (
            echo !WARN!!_name! startup timeout ^(30s^), skip wait
            goto :eof
        )
        ping -n 2 127.0.0.1 > nul 2>&1
        goto :wait_loop

REM ============================================================
REM [COMMON] Status check
REM ============================================================
:do_status
    echo.
    echo =========================================
    echo   Phase 0 Service Status
    echo =========================================

    set "_ok=1"

    for /f "delims=" %%p in ('powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort !ALLINONE_PORT! -ErrorAction SilentlyContinue ^| Select-Object -First 1 -ExpandProperty OwningProcess" 2^>nul') do (
        echo   [ON]  manpou-allinone :!ALLINONE_PORT!  ^(PID=%%p^)
        goto :st_user
    )
    echo   [OFF] manpou-allinone :!ALLINONE_PORT!  ^(not running^)
    set "_ok=0"

    :st_user
    for /f "delims=" %%p in ('powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort !USER_PORT! -ErrorAction SilentlyContinue ^| Select-Object -First 1 -ExpandProperty OwningProcess" 2^>nul') do (
        echo   [ON]  user-service     :!USER_PORT!  ^(PID=%%p^)
        goto :st_web
    )
    echo   [OFF] user-service     :!USER_PORT!  ^(not running^)
    set "_ok=0"

    :st_web
    for /f "delims=" %%p in ('powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort !WEB_PORT! -ErrorAction SilentlyContinue ^| Select-Object -First 1 -ExpandProperty OwningProcess" 2^>nul') do (
        echo   [ON]  web             :!WEB_PORT!  ^(PID=%%p^)
        goto :st_done
    )
    echo   [OFF] web             :!WEB_PORT!  ^(not running^)
    set "_ok=0"

    :st_done
    echo.
    if !_ok!==1 (
        echo   All services ready
        echo.
        echo   Access:
        echo     Frontend:  http://localhost:!WEB_PORT!
        echo     allinone:  http://localhost:!ALLINONE_PORT!/swagger-ui/index.html
    ) else (
        echo   Some services not running
    )
    echo =========================================
    goto :eof

REM ============================================================
REM [EXIT]
REM ============================================================
:end
    endlocal
