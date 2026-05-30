# Load deploy.env and change ECS service desired-count.
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet(0, 1)]
    [int]$DesiredCount,
    [string]$EnvFile = "aws/deploy.env"
)

$ErrorActionPreference = "Stop"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "../..")

function Import-DotEnvFile {
    param([Parameter(Mandatory = $true)][string]$Path)
    if (-not (Test-Path $Path)) { return }
    Get-Content $Path | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq "" -or $line.StartsWith("#")) { return }
        $eq = $line.IndexOf("=")
        if ($eq -lt 1) { return }
        $name = $line.Substring(0, $eq).Trim()
        $value = $line.Substring($eq + 1).Trim()
        if ((Get-Item "env:$name" -ErrorAction SilentlyContinue) -and -not [string]::IsNullOrWhiteSpace((Get-Item "env:$name").Value)) {
            return
        }
        Set-Item -Path "env:$name" -Value $value
    }
}

Push-Location $repoRoot
try {
    $envPath = Join-Path $repoRoot $EnvFile
    if (-not (Test-Path $envPath)) {
        Write-Error "Env file not found: $envPath. Run: copy aws\deploy.env.example aws\deploy.env"
    }

    Import-DotEnvFile -Path $envPath

    $region = if ($env:AWS_REGION) { $env:AWS_REGION } else { "ap-northeast-1" }
    $projectName = if ($env:PROJECT_NAME) { $env:PROJECT_NAME } else { "secapp-demo" }
    $clusterName = "$projectName-cluster"
    $serviceName = "$projectName-service"

    Write-Host "Cluster : $clusterName"
    Write-Host "Service : $serviceName"
    Write-Host "Region  : $region"
    Write-Host "Desired : $DesiredCount"
    Write-Host ""

    $json = aws ecs update-service `
        --cluster $clusterName `
        --service $serviceName `
        --desired-count $DesiredCount `
        --region $region `
        --query "service.{ServiceName:serviceName,Status:status,DesiredCount:desiredCount,RunningCount:runningCount,PendingCount:pendingCount}" `
        --output json

    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }

    $service = $json | ConvertFrom-Json
    Write-Host "Updated ECS service"
    Write-Host "  Service : $($service.ServiceName)"
    Write-Host "  Status  : $($service.Status)"
    Write-Host "  Desired : $($service.DesiredCount)"
    Write-Host "  Running : $($service.RunningCount)"
    Write-Host "  Pending : $($service.PendingCount)"
}
finally {
    Pop-Location
}
