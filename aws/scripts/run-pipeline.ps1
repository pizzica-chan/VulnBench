# Load deploy.env and start CodePipeline manually.
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
    $pipelineName = "$projectName-pipeline"

    Write-Host "Pipeline: $pipelineName"
    Write-Host "Region  : $region"
    Write-Host ""

    $json = aws codepipeline start-pipeline-execution `
        --name $pipelineName `
        --region $region `
        --query "pipelineExecutionId" `
        --output json

    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }

    $executionId = ($json | ConvertFrom-Json)
    Write-Host "Pipeline started"
    Write-Host "  ExecutionId : $executionId"
}
finally {
    Pop-Location
}
