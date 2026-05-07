#requires -Version 5.1
<#
.SYNOPSIS
  one-click-wsl-up で立ち上げた MySQL / app コンテナを、Windows から WSL 経由で stop する。

.DESCRIPTION
  Docker デーモンが止まっている場合は Docker Desktop の起動を試みます（down を送るために API が必要）。

.PARAMETER NoPause
  終了時に Enter 待ちしない。
#>
param(
    [switch] $NoPause
)

$ErrorActionPreference = "Stop"

function Convert-WindowsPathToWslPath {
    param([Parameter(Mandatory)][string]$WindowsPath)
    $resolved = (Resolve-Path -LiteralPath $WindowsPath).Path
    if ($resolved -notmatch '^([A-Za-z]):\\(.*)$') {
        throw "WSL path: need drive letter path (e.g. D:\workspace\secure). Got: $resolved"
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

$repoWin = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

if (-not (Get-Command wsl.exe -ErrorAction SilentlyContinue)) {
    Write-Host "==> wsl.exe なし: Windows 側で docker compose down します"
    Set-Location $repoWin
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw "docker が PATH にありません。"
    }
    & docker compose down
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    Write-Host "==> 停止しました。"
    if (-not $NoPause) { $null = Read-Host "Enter で閉じます" }
    exit 0
}

. "$PSScriptRoot\Ensure-DockerDesktop.ps1"

if (-not (Test-DockerDaemon)) {
    Write-Host "==> Docker に接続できません。Docker Desktop の起動を試みます..."
    Start-DockerDesktopIfNeeded
}

$wslPath = Convert-WindowsPathToWslPath $repoWin
$supportsCd = Test-WslSupportsCd

Write-Host "==> WSL 上で docker compose down します"
Write-Host "    $wslPath"

$downCmd = "bash scripts/wsl-down.sh"
if ($supportsCd) {
    & wsl.exe --cd "$wslPath" -e bash -lc "$downCmd"
} else {
    $q = $wslPath -replace "'", "'\''"
    $bashLc = 'cd ''{0}'' && bash scripts/wsl-down.sh' -f $q
    & wsl.exe -e bash -lc $bashLc
}

if ($LASTEXITCODE -ne 0) {
    Write-Host "docker compose down が失敗しました (code $LASTEXITCODE)。リポジトリ直下で手動: docker compose down" -ForegroundColor Yellow
}

if (-not $NoPause) {
    $null = Read-Host "Enter で閉じます"
}

exit $LASTEXITCODE
