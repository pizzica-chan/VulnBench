@echo off
setlocal
REM WSL 上で起動済みの Compose コンテナを docker compose restart する
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0one-click-wsl-restart.ps1" %*
