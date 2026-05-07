@echo off
setlocal
REM one-click-wsl-up で起動したコンテナを止める
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0one-click-wsl-down.ps1" %*
