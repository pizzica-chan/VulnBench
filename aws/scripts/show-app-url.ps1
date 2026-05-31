# Load deploy.env and print application URL from the running ECS task public IP.
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

function Invoke-AwsCliJson {
    param(
        [Parameter(Mandatory = $true)][string[]]$ArgumentList
    )
    $prev = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $raw = & aws @ArgumentList 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw ($raw | Out-String).Trim()
        }
        if ($raw -is [System.Array]) {
            return ($raw | Out-String).Trim()
        }
        return [string]$raw
    }
    finally {
        $ErrorActionPreference = $prev
    }
}

function Get-TaskPublicIp {
    param(
        [Parameter(Mandatory = $true)][string]$ClusterName,
        [Parameter(Mandatory = $true)][string]$ServiceName,
        [Parameter(Mandatory = $true)][string]$Region
    )

    $taskArn = Invoke-AwsCliJson @(
        "ecs", "list-tasks",
        "--cluster", $ClusterName,
        "--service-name", $ServiceName,
        "--desired-status", "RUNNING",
        "--region", $Region,
        "--query", "taskArns[0]",
        "--output", "text"
    )

    if ([string]::IsNullOrWhiteSpace($taskArn) -or $taskArn -eq "None") {
        return $null
    }

    $eniId = Invoke-AwsCliJson @(
        "ecs", "describe-tasks",
        "--cluster", $ClusterName,
        "--tasks", $taskArn,
        "--region", $Region,
        "--query", "tasks[0].attachments[?type=='ElasticNetworkInterface'] | [0].details[?name=='networkInterfaceId'].value | [0]",
        "--output", "text"
    )

    if ([string]::IsNullOrWhiteSpace($eniId) -or $eniId -eq "None") {
        return $null
    }

    $publicIp = Invoke-AwsCliJson @(
        "ec2", "describe-network-interfaces",
        "--network-interface-ids", $eniId,
        "--region", $Region,
        "--query", "NetworkInterfaces[0].Association.PublicIp",
        "--output", "text"
    )

    if ([string]::IsNullOrWhiteSpace($publicIp) -or $publicIp -eq "None") {
        return $null
    }

    return $publicIp
}

Push-Location $repoRoot
try {
    $envPath = Join-Path $repoRoot $EnvFile
    if (-not (Test-Path $envPath)) {
        Write-Error "Env file not found: $envPath. Run: copy aws\deploy.env.example aws\deploy.env"
    }

    Import-DotEnvFile -Path $envPath

    $region = if ($env:AWS_REGION) { $env:AWS_REGION } else { "ap-northeast-1" }
    $stackName = if ($env:STACK_NAME) { $env:STACK_NAME } else { "secapp-demo" }
    $projectName = if ($env:PROJECT_NAME) { $env:PROJECT_NAME } else { "secapp-demo" }
    $clusterName = "$projectName-cluster"
    $serviceName = "$projectName-service"
    $appPort = "8080"

    Write-Host "Stack  : $stackName"
    Write-Host "Region : $region"
    Write-Host ""

    $stackStatus = Invoke-AwsCliJson @(
        "cloudformation", "describe-stacks",
        "--stack-name", $stackName,
        "--region", $region,
        "--query", "Stacks[0].StackStatus",
        "--output", "text"
    )

    Write-Host "Stack status : $stackStatus"

    Write-Host ""
    Write-Host "ECS service:"

    $serviceJson = Invoke-AwsCliJson @(
        "ecs", "describe-services",
        "--cluster", $clusterName,
        "--services", $serviceName,
        "--region", $region,
        "--query", "services[0].{Status:status,DesiredCount:desiredCount,RunningCount:runningCount,PendingCount:pendingCount}",
        "--output", "json"
    )

    $service = $serviceJson | ConvertFrom-Json
    if ($null -eq $service -or [string]::IsNullOrWhiteSpace($service.Status)) {
        Write-Error "ECS service not found: $serviceName"
    }

    Write-Host "  Status  : $($service.Status)"
    Write-Host "  Desired : $($service.DesiredCount)"
    Write-Host "  Running : $($service.RunningCount)"
    Write-Host "  Pending : $($service.PendingCount)"

    if ($service.DesiredCount -eq 0) {
        Write-Host ""
        Write-Host "Note: desired-count is 0. Run 04_aws-ecs-start.bat before opening the URL."
        exit 0
    }

    if ($service.RunningCount -eq 0) {
        Write-Host ""
        Write-Host "Note: tasks are starting. Wait 2-3 minutes, then run this script again."
        exit 0
    }

    $publicIp = Get-TaskPublicIp -ClusterName $clusterName -ServiceName $serviceName -Region $region
    if ([string]::IsNullOrWhiteSpace($publicIp)) {
        Write-Error "Could not resolve task public IP. Check ECS task status and try again."
    }

    $applicationUrl = "http://${publicIp}:${appPort}/"

    Write-Host ""
    Write-Host "Application URL (task public IP):"
    Write-Host "  $applicationUrl"
    Write-Host ""
    Write-Host "Public IP:"
    Write-Host "  $publicIp"
    Write-Host ""
    Write-Host "Note: public IP changes when the ECS task is replaced (deploy / restart)."
}
finally {
    Pop-Location
}
