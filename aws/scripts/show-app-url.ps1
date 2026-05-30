# Load deploy.env and print ALB ApplicationUrl from CloudFormation stack outputs.
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

function Get-StackOutputValue {
    param(
        [Parameter(Mandatory = $true)]$Outputs,
        [Parameter(Mandatory = $true)][string]$Key
    )
    foreach ($item in $Outputs) {
        if ($item.OutputKey -eq $Key) {
            return $item.OutputValue
        }
    }
    return $null
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

    Write-Host "Stack  : $stackName"
    Write-Host "Region : $region"
    Write-Host ""

    $stackJson = aws cloudformation describe-stacks `
        --stack-name $stackName `
        --region $region `
        --query "Stacks[0].{Status:StackStatus,Outputs:Outputs}" `
        --output json 2>&1

    if ($LASTEXITCODE -ne 0) {
        Write-Error "Stack not found or inaccessible: $stackName (region: $region). Run 02_aws-deploy.bat first."
    }

    $stack = $stackJson | ConvertFrom-Json
    Write-Host "Stack status : $($stack.Status)"

    $applicationUrl = Get-StackOutputValue -Outputs $stack.Outputs -Key "ApplicationUrl"
    $albDns = Get-StackOutputValue -Outputs $stack.Outputs -Key "AlbDnsName"

    if ([string]::IsNullOrWhiteSpace($applicationUrl)) {
        Write-Error "ApplicationUrl output not found on stack: $stackName"
    }

    Write-Host ""
    Write-Host "Application URL (ALB):"
    Write-Host "  $applicationUrl"
    if (-not [string]::IsNullOrWhiteSpace($albDns)) {
        Write-Host ""
        Write-Host "ALB DNS name:"
        Write-Host "  $albDns"
    }

    Write-Host ""
    Write-Host "ECS service:"

    $serviceJson = aws ecs describe-services `
        --cluster $clusterName `
        --services $serviceName `
        --region $region `
        --query "services[0].{Status:status,DesiredCount:desiredCount,RunningCount:runningCount,PendingCount:pendingCount}" `
        --output json 2>&1

    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($serviceJson)) {
        Write-Host "  (could not describe $serviceName)"
    }
    else {
        $service = $serviceJson | ConvertFrom-Json
        if ($null -eq $service -or [string]::IsNullOrWhiteSpace($service.Status)) {
            Write-Host "  (service not found)"
        }
        else {
            Write-Host "  Status  : $($service.Status)"
            Write-Host "  Desired : $($service.DesiredCount)"
            Write-Host "  Running : $($service.RunningCount)"
            Write-Host "  Pending : $($service.PendingCount)"
            if ($service.DesiredCount -eq 0) {
                Write-Host ""
                Write-Host "Note: desired-count is 0. Run 04_aws-ecs-start.bat before opening the URL."
            }
            elseif ($service.RunningCount -eq 0) {
                Write-Host ""
                Write-Host "Note: tasks are starting. Wait 2-3 minutes for ALB targets to become healthy."
            }
        }
    }
}
finally {
    Pop-Location
}
