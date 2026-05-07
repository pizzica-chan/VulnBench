@echo off
setlocal
REM Windows から「WSL 未起動 + Docker 停止」前提で、http://localhost:8080 まで一気に起動する
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0one-click-wsl-up.ps1" %*
