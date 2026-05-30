# Optional: port-forward to an ECS container via SSM (ECS Exec).

param(
    [string]$Cluster = "secapp-demo-cluster",
    [string]$Service = "secapp-demo-service",
    [string]$Container = "app",
    [int]$RemotePort = 8080,
    [int]$LocalPort = 8080,
    [string]$Region = ""
)

$ErrorActionPreference = "Stop"

if (-not $Region) {
    $Region = (aws configure get region 2>$null)
    if (-not $Region) { $Region = "ap-northeast-1" }
}

Write-Host "Cluster : $Cluster"
Write-Host "Service : $Service"
Write-Host "Forward : localhost:$LocalPort -> ${Container}:$RemotePort"
Write-Host ""

$taskArn = aws ecs list-tasks `
    --cluster $Cluster `
    --service-name $Service `
    --desired-status RUNNING `
    --region $Region `
    --query "taskArns[0]" `
    --output text

if (-not $taskArn -or $taskArn -eq "None") {
    Write-Error "No running task. Set desired-count to 1 (04_aws-ecs-start.bat) after Pipeline deploy."
}

$taskId = ($taskArn -split "/")[-1]

$taskJson = aws ecs describe-tasks `
    --cluster $Cluster `
    --tasks $taskArn `
    --region $Region `
    --output json

if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$task = ($taskJson | ConvertFrom-Json).tasks[0]
$containerObj = $task.containers | Where-Object { $_.name -eq $Container } | Select-Object -First 1
$runtimeId = if ($containerObj) { $containerObj.runtimeId } else { $null }

if (-not $runtimeId -or $runtimeId -eq "None") {
    Write-Error "Could not get runtimeId for container '$Container'."
}

$target = "ecs:${Cluster}_${taskId}_${runtimeId}"

Write-Host "SSM target: $target"
Write-Host "Local URL: http://localhost:$LocalPort"
Write-Host "Stop: Ctrl+C"
Write-Host ""

aws ssm start-session `
    --region $Region `
    --target $target `
    --document-name AWS-StartPortForwardingSession `
    --parameters "portNumber=$RemotePort,localPortNumber=$LocalPort"
