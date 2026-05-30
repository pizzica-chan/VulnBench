@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"

echo secapp AWS stack deletion
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0aws\scripts\delete-stack.ps1"
set EXITCODE=%ERRORLEVEL%

echo.
if %EXITCODE% neq 0 (
  echo [FAILED] exit code %EXITCODE%
) else (
  echo [DONE]
)

pause
endlocal & exit /b %EXITCODE%
