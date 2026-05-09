#requires -Version 5.1
<#
.SYNOPSIS
  WSL 上の Docker Compose でアプリを再ビルドし、Windows からワンクリックでソース変更を反映する。

.DESCRIPTION
  - エクスプローラーから scripts\one-click-wsl-restart.cmd をダブルクリックして実行する想定。
  - `docker compose up -d --build --force-recreate app`（Dockerfile から app イメージを再ビルドし app コンテナを作り直す）。
  - MySQL コンテナは載せ替えない（データは tmpfs だが、初回起動からの状態は維持）。DB まで初期化し直す場合は wsl-up.sh を使う。

.PARAMETER NoPause
  成功時に Enter 待ちをしない。
#>
param(
    [switch] $NoPause
)

$ErrorActionPreference = "Stop"

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

if (-not (Get-Command wsl.exe -ErrorAction SilentlyContinue)) {
    throw "wsl.exe が見つかりません。このスクリプトは Windows 上で実行してください。"
}

Write-Host "==> WSL がコマンドを実行できるか確認しています..."
$null = wsl.exe -e sh -c "exit 0" 2>&1
if ($LASTEXITCODE -ne 0) {
    throw "WSL でコマンドを実行できませんでした。`nwsl --install などで WSL を有効にしてください。"
}

. "$PSScriptRoot\Ensure-DockerDesktop.ps1"

if (-not (Test-DockerDaemon)) {
    Write-Host "==> Docker に接続できません。Docker Desktop の起動を試みます..."
    Start-DockerDesktopIfNeeded
}

$repoWin = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$wslPath = Convert-WindowsPathToWslPath $repoWin
$supportsCd = Test-WslSupportsCd

Write-Host ""
Write-Host "==> WSL 上でアプリを再ビルドして反映します（compose up --build app）。"
Write-Host "    Windows パス: $repoWin"
Write-Host "    WSL パス:     $wslPath"
Write-Host ""

$bootstrap = 'set -euo pipefail; chmod +x scripts/wsl-restart.sh 2>/dev/null || true; ./scripts/wsl-restart.sh'

if ($supportsCd) {
    & wsl.exe --cd "$wslPath" -e bash -lc "$bootstrap"
} else {
    $q = $wslPath -replace "'", "'\''"
    $bashLc = 'cd ''{0}'' && {1}' -f $q, $bootstrap
    & wsl.exe -e bash -lc $bashLc
}

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "失敗しました（終了コード $LASTEXITCODE）。スタックが未起動なら one-click-wsl-up を先に実行してください。" -ForegroundColor Red
    if (-not $NoPause) {
        Write-Host "Enter キーで閉じます..."
        $null = Read-Host
    }
    exit $LASTEXITCODE
}

Write-Host ""
Write-Host "==> 完了: http://localhost:8080"

if (-not $NoPause) {
    Write-Host ""
    Write-Host "Enter キーで閉じます..."
    $null = Read-Host
}
