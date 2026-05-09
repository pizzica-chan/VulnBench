@echo off
setlocal
REM WSL 上で app イメージを再ビルドし app コンテナを載せ替える（ソース反映）
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0one-click-wsl-restart.ps1" %*
