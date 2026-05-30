# Load deploy.env and open interactive MySQL client on ECS via ECS Exec.
param(
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
    $dbPasswordHint = if ($env:MYSQL_ROOT_PASSWORD) { $env:MYSQL_ROOT_PASSWORD } else { "secapp_demo_pass" }

    Write-Host "Cluster : $clusterName"
    Write-Host "Service : $serviceName"
    Write-Host "Region  : $region"
    Write-Host ""

    $taskArn = aws ecs list-tasks `
        --cluster $clusterName `
        --service-name $serviceName `
        --desired-status RUNNING `
        --region $region `
        --query "taskArns[0]" `
        --output text

    if (-not $taskArn -or $taskArn -eq "None") {
        Write-Error "No running task. Start ECS with 04_aws-ecs-start.bat first."
    }

    Write-Host "Opening MySQL client (ECS Exec)..."
    Write-Host "Database : secapp"
    Write-Host "Password : container env MYSQL_ROOT_PASSWORD (deploy.env hint: $dbPasswordHint)"
    Write-Host "Quit     : exit or \q"
    Write-Host ""

    $mysqlCommand = '/bin/sh -lc ''mysql -uroot -p"$MYSQL_ROOT_PASSWORD" secapp'''

    aws ecs execute-command `
        --cluster $clusterName `
        --task $taskArn `
        --container mysql `
        --interactive `
        --command $mysqlCommand `
        --region $region

    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}
finally {
    Pop-Location
}
