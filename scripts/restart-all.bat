@echo off
chcp 65001 > nul 2>&1
setlocal EnableDelayedExpansion

set "SCRIPT_DIR=%~dp0"
set "TARGET=%~1"
if "!TARGET!"=="" set "TARGET=all"

echo.
echo =========================================
echo   Stopping services...
echo =========================================
call "%SCRIPT_DIR%stop-all.bat" %TARGET%

echo.
echo =========================================
echo   Waiting 2 seconds...
echo =========================================
ping -n 3 127.0.0.1 > nul 2>&1

call "%SCRIPT_DIR%start-all.bat" %TARGET%

endlocal
