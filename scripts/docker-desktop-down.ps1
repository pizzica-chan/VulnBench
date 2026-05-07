#requires -Version 5.1
<#
.SYNOPSIS
  docker compose で起動した MySQL + アプリを停止する（Windows / WSL どちらから実行しても、同じプロジェクト名で動いていれば有効）。
#>
$ErrorActionPreference = "Stop"

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $RepoRoot

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "docker が PATH にありません。"
}

Write-Host "==> repo: $RepoRoot"
Write-Host "==> docker compose down"
& docker compose down
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "==> 停止しました。"
