# Load deploy.env and delete the CloudFormation stack.
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
    $stackName = if ($env:STACK_NAME) { $env:STACK_NAME } else { "secapp-demo" }

    Write-Host "This will DELETE the entire AWS stack (ECS, ALB, Pipeline, ECR, etc.)."
    Write-Host "Stack : $stackName"
    Write-Host "Region: $region"
    Write-Host ""
    $confirm = Read-Host "Type the stack name to confirm deletion"
    if ($confirm -ne $stackName) {
        Write-Host "Cancelled (stack name did not match)."
        exit 1
    }

    Write-Host ""
    Write-Host "Scaling ECS service to 0..."
    $projectName = if ($env:PROJECT_NAME) { $env:PROJECT_NAME } else { "secapp-demo" }
    aws ecs update-service `
        --cluster "$projectName-cluster" `
        --service "$projectName-service" `
        --desired-count 0 `
        --region $region `
        2>$null | Out-Null

    $bucket = aws cloudformation describe-stack-resources `
        --stack-name $stackName `
        --region $region `
        --query "StackResources[?ResourceType=='AWS::S3::Bucket'].PhysicalResourceId" `
        --output text

    if ($bucket -and $bucket -ne "None") {
        Write-Host "Emptying pipeline artifact bucket: $bucket"
        aws s3 rm "s3://$bucket" --recursive --quiet --region $region
    }

    Write-Host "Deleting stack..."
    aws cloudformation delete-stack --stack-name $stackName --region $region
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }

    Write-Host "Waiting for stack deletion to complete (this may take several minutes)..."
    aws cloudformation wait stack-delete-complete --stack-name $stackName --region $region
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }

    Write-Host ""
    Write-Host "Stack deleted: $stackName"
}
finally {
    Pop-Location
}
