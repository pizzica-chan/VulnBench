@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"

echo secapp CodePipeline manual run
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0aws\scripts\run-pipeline.ps1"
set EXITCODE=%ERRORLEVEL%

echo.
if %EXITCODE% neq 0 (
  echo [FAILED] exit code %EXITCODE%
) else (
  echo [DONE] Pipeline started. Check AWS Console for Build/Test/Deploy progress.
)

pause
endlocal & exit /b %EXITCODE%
