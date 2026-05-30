@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"

echo secapp ECS MySQL client (ECS Exec)
echo.
echo Opens mysql CLI on secapp. Requires Session Manager Plugin and running ECS task.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0aws\scripts\mysql-shell.ps1"
set EXITCODE=%ERRORLEVEL%

echo.
if %EXITCODE% neq 0 (
  echo [FAILED] exit code %EXITCODE%
) else (
  echo [DONE]
)

pause
endlocal & exit /b %EXITCODE%
