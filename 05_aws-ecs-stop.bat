@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"

echo secapp ECS stop (desired-count=0)
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0aws\scripts\ecs-scale.ps1" -DesiredCount 0
set EXITCODE=%ERRORLEVEL%

echo.
if %EXITCODE% neq 0 (
  echo [FAILED] exit code %EXITCODE%
) else (
  echo [DONE] Fargate tasks stopped. Pipeline and other stack resources remain (small cost).
)

pause
endlocal & exit /b %EXITCODE%
