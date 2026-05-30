# Load deploy.env and deploy the CloudFormation stack.
param(
    [string]$EnvFile = "aws/deploy.env"
)

$ErrorActionPreference = "Stop"
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "../..")

function Import-DotEnvFile {
    param(
        [Parameter(Mandatory = $true)][string]$Path
    )
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

    foreach ($required in @("GITHUB_CONNECTION_ARN", "REPOSITORY_ID")) {
        if (-not (Get-Item "env:$required" -ErrorAction SilentlyContinue) -or [string]::IsNullOrWhiteSpace((Get-Item "env:$required").Value)) {
            Write-Error "Missing required deploy.env variable: $required"
        }
    }

    $region = if ($env:AWS_REGION) { $env:AWS_REGION } else { "ap-northeast-1" }
    $stackName = if ($env:STACK_NAME) { $env:STACK_NAME } else { "secapp-demo" }
    $branchName = if ($env:BRANCH_NAME) { $env:BRANCH_NAME } else { "master" }

    $overrides = @(
        "GitHubConnectionArn=$env:GITHUB_CONNECTION_ARN",
        "RepositoryId=$env:REPOSITORY_ID",
        "BranchName=$branchName"
    )
    if ($env:PROJECT_NAME) { $overrides += "ProjectName=$env:PROJECT_NAME" }
    if ($env:MYSQL_ROOT_PASSWORD) { $overrides += "MysqlRootPassword=$env:MYSQL_ROOT_PASSWORD" }
    if ($env:MYSQL_APP_PASSWORD) { $overrides += "MysqlAppPassword=$env:MYSQL_APP_PASSWORD" }

    Write-Host "Stack : $stackName"
    Write-Host "Region: $region"
    Write-Host ""

    $env:PYTHONUTF8 = "1"
    $env:AWS_CLI_FILE_ENCODING = "UTF-8"

    aws cloudformation deploy `
        --template-file aws/cloudformation/demo-stack.yaml `
        --stack-name $stackName `
        --capabilities CAPABILITY_NAMED_IAM `
        --region $region `
        --parameter-overrides @overrides

    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}
finally {
    Pop-Location
}
