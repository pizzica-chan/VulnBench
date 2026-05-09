#requires -Version 5.1
<#
.SYNOPSIS
  WSL が止まっていてもよい状態から、Docker Desktop 起動 → WSL 起動 → compose up までを 1 本化する（Windows 側から実行）。

.DESCRIPTION
  - エクスプローラーからダブルクリックする想定で scripts\one-click-wsl-up.cmd から呼ぶ。
  - Docker Desktop が止まっていれば起動し、デーモン応答まで待つ（最大約 3 分）。
  - 続いて wsl.exe を起動し（初回は WSL VM の立ち上げに数十秒かかることがある）、./scripts/wsl-up.sh を実行する。

.PARAMETER SkipDockerDesktopStart
  Docker Desktop の自動起動を試みない。

.PARAMETER FollowLogs
  起動後に app コンテナのログを追従する（Ctrl+C で終了。コンテナは止まらない）。

.PARAMETER NoPause
  成功時に Enter 待ちをしない（他スクリプトから呼ぶとき用）。
#>
param(
    [switch] $SkipDockerDesktopStart,
    [switch] $FollowLogs,
    [switch] $NoPause
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command wsl.exe -ErrorAction SilentlyContinue)) {
    throw "wsl.exe が見つかりません。このスクリプトは Windows 上で実行してください。WSL の導入: https://learn.microsoft.com/ja-jp/windows/wsl/install"
}

. "$PSScriptRoot\Ensure-DockerDesktop.ps1"

function Convert-WindowsPathToWslPath {
    param([Parameter(Mandatory)][string]$WindowsPath)
    $resolved = (Resolve-Path -LiteralPath $WindowsPath).Path
    if ($resolved -notmatch '^([A-Za-z]):\\(.*)$') {
        throw "WSL パス変換: ドライブレター付きのローカルパスが必要です（例 D:\workspace\secure）。現在: $resolved"
    }
    $drive = $matches[1].ToLowerInvariant()
    $tail = ($matches[2] -replace '\\', '/').TrimEnd('/')
    "/mnt/$drive/$tail"
}

function Test-WslSupportsCd {
    try {
        $help = (& wsl.exe --help 2>&1 | Out-String)
        return $help -match '--cd'
    } catch {
        return $false
    }
}

Write-Host "==> WSL がコマンドを実行できるか確認しています..."
$null = wsl.exe -e sh -c "exit 0" 2>&1
if ($LASTEXITCODE -ne 0) {
    throw "WSL でコマンドを実行できませんでした。`nwsl --install を実行するか、Microsoft Store から Linux ディストリを入れてください。`n詳細: https://learn.microsoft.com/ja-jp/windows/wsl/install"
}

if (-not $SkipDockerDesktopStart) {
    Start-DockerDesktopIfNeeded
} elseif (-not (Test-DockerDaemon)) {
    throw "Docker デーモンに接続できません。-SkipDockerDesktopStart を外すか、Docker Desktop を手動で起動してください。"
}

$repoWin = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$wslPath = Convert-WindowsPathToWslPath $repoWin
$supportsCd = Test-WslSupportsCd

Write-Host ""
Write-Host "==> WSL 上で Docker Compose を起動します（WSL 未起動ならこのタイミングで VM が立ち上がります）。"
Write-Host "    Windows パス: $repoWin"
Write-Host "    WSL パス:     $wslPath"
Write-Host ""

$bootstrap = "set -euo pipefail; chmod +x scripts/wsl-up.sh scripts/wsl-down.sh scripts/wsl-restart.sh 2>/dev/null || true; ./scripts/wsl-up.sh"

if ($supportsCd) {
    Write-Host "==> wsl --cd ... bash -lc ..." 
    & wsl.exe --cd "$wslPath" -e bash -lc "$bootstrap"
} else {
    $q = $wslPath -replace "'", "'\''"
    $bashLc = 'cd ''{0}'' && {1}' -f $q, $bootstrap
    & wsl.exe -e bash -lc $bashLc
}

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "失敗しました（終了コード $LASTEXITCODE）。" -ForegroundColor Red
    if (-not $NoPause) {
        Write-Host "Enter キーで閉じます..."
        $null = Read-Host
    }
    exit $LASTEXITCODE
}

Write-Host ""
Write-Host "==> 完了: ブラウザで http://localhost:8080 を開いてください。"

if ($FollowLogs) {
    Write-Host ""
    Write-Host "==> app ログを追従します（終了は Ctrl+C）。コンテナは止まりません。"
    if ($supportsCd) {
        & wsl.exe --cd "$wslPath" -e bash -lc "docker compose logs -f app"
    } else {
        $q = $wslPath -replace "'", "'\''"
        $logLc = 'cd ''{0}'' && docker compose logs -f app' -f $q
        & wsl.exe -e bash -lc $logLc
    }
} elseif (-not $NoPause) {
    Write-Host ""
    Write-Host "Enter キーで閉じます（ログ例: wsl へ入り cd $wslPath して docker compose logs -f app）。"
    $null = Read-Host
}
