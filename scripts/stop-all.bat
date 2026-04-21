@echo off
chcp 65001 > nul 2>&1
setlocal EnableDelayedExpansion

set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR:~0,-1%"

set "ALLINONE_PORT=18090"
set "USER_PORT=18081"
set "WEB_PORT=13000"

set "INFO=[INFO] "
set "WARN=[WARN] "
set "OK=[OK]   "

set "TARGET=%~1"
if "!TARGET!"=="" set "TARGET=all"

echo.
echo =========================================
echo   Phase 0 - Stop Services
echo =========================================
echo.

REM === Dispatch ===
if "!TARGET!"=="manpou" goto :do_manpou
if "!TARGET!"=="user"   goto :do_user
if "!TARGET!"=="web"    goto :do_web
if "!TARGET!"=="all"    goto :do_all
echo Usage: %~nx0 [all^|manpou^|user^|web]
goto :end

REM ============================================================
REM [ALL] Stop all
REM ============================================================
:do_all
    call :do_stop web              !WEB_PORT!
    call :do_stop user-service     !USER_PORT!
    call :do_stop manpou-allinone  !ALLINONE_PORT!
    echo.
    echo =========================================
    echo   All Phase 0 services stopped
    echo =========================================
    goto :end

REM ============================================================
REM [SERVICE] manpou
REM ============================================================
:do_manpou
    call :do_stop manpou-allinone !ALLINONE_PORT!
    goto :end

REM ============================================================
REM [SERVICE] user
REM ============================================================
:do_user
    call :do_stop user-service !USER_PORT!
    goto :end

REM ============================================================
REM [SERVICE] web
REM ============================================================
:do_web
    call :do_stop web !WEB_PORT!
    goto :end

REM ============================================================
REM [COMMON] Stop one service: read PID file then port fallback
REM ============================================================
:do_stop
    set "_name=%~1"
    set "_port=%~2"
    set "_pidfile=%PROJECT_DIR%\logs\!_name!.pid"

    REM Try PID file first
    if exist "!_pidfile!" (
        set /p _saved_pid=<"!_pidfile!"
        if defined _saved_pid if not "!_saved_pid!"=="" (
            if not "!_saved_pid!"=="0" (
                powershell -NoProfile -Command "Stop-Process -Id !_saved_pid! -Force -ErrorAction SilentlyContinue" > nul 2>&1
                echo !OK!!_name! ^(PID=!_saved_pid!^) stopped
            )
        )
        del /f /q "!_pidfile!" > nul 2>&1
    )

    REM Port fallback
    for /f "delims=" %%p in ('powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort !_port! -ErrorAction SilentlyContinue ^| Select-Object -First 1 -ExpandProperty OwningProcess" 2^>nul') do (
        if not "%%p"=="" (
            powershell -NoProfile -Command "Stop-Process -Id %%p -Force -ErrorAction SilentlyContinue" > nul 2>&1
            echo !OK!!_name! ^(port !_port!, PID=%%p^) stopped
            goto :eof
        )
    )
    echo !INFO!!_name! ^(port !_port!^) not running
    goto :eof

REM ============================================================
REM [EXIT]
REM ============================================================
:end
    endlocal
