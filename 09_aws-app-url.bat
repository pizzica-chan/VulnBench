@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"

echo secapp ALB application URL
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0aws\scripts\show-app-url.ps1"
set EXITCODE=%ERRORLEVEL%

echo.
if %EXITCODE% neq 0 (
  echo [FAILED] exit code %EXITCODE%
) else (
  echo [DONE] Open Application URL in your browser.
)

pause
endlocal & exit /b %EXITCODE%
