# Load deploy.env and port-forward ECS MySQL (3306) to localhost.
param(
    [string]$EnvFile = "aws/deploy.env",
    [int]$LocalPort = 13306
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
    $dbPasswordHint = if ($env:MYSQL_ROOT_PASSWORD) { $env:MYSQL_ROOT_PASSWORD } else { "secapp_demo_pass" }

    Write-Host "Cluster : $clusterName"
    Write-Host "Service : $serviceName"
    Write-Host "Region  : $region"
    Write-Host "Forward : localhost:$LocalPort -> mysql:3306"
    Write-Host ""
    Write-Host "In another terminal:"
    Write-Host "  mysql -h 127.0.0.1 -P $LocalPort -uroot -p secapp"
    Write-Host "Password hint (from deploy.env): $dbPasswordHint"
    Write-Host "Stop forward: Ctrl+C"
    Write-Host ""

    & (Join-Path $PSScriptRoot "port-forward.ps1") `
        -Cluster $clusterName `
        -Service $serviceName `
        -Container mysql `
        -RemotePort 3306 `
        -LocalPort $LocalPort `
        -Region $region

    exit $LASTEXITCODE
}
finally {
    Pop-Location
}
