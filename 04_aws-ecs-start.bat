@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"

echo secapp ECS start (desired-count=1)
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0aws\scripts\ecs-scale.ps1" -DesiredCount 1
set EXITCODE=%ERRORLEVEL%

echo.
if %EXITCODE% neq 0 (
  echo [FAILED] exit code %EXITCODE%
) else (
  echo [DONE] Tasks will start in 2-3 minutes. Run 09_aws-app-url.bat for the ALB URL.
)

pause
endlocal & exit /b %EXITCODE%
