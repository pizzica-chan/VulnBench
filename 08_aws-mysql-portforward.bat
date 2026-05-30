@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"

echo secapp ECS MySQL port forward (localhost:13306)
echo.
echo Requires: Session Manager Plugin, running ECS task (04_aws-ecs-start.bat)
echo Keep this window open. Connect from another terminal with mysql client.
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0aws\scripts\mysql-port-forward.ps1"
set EXITCODE=%ERRORLEVEL%

echo.
if %EXITCODE% neq 0 (
  echo [FAILED] exit code %EXITCODE%
) else (
  echo [DONE]
)

pause
endlocal & exit /b %EXITCODE%
