[CmdletBinding()]
param(
    [switch]$RunPostman,
    [switch]$RunBrowserSmoke
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$evidenceDir = Join-Path $projectRoot 'target\verification'
New-Item -ItemType Directory -Force -Path $evidenceDir | Out-Null
Set-Location $projectRoot

function Invoke-LoggedCommand {
    param(
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][scriptblock]$Command
    )

    $logPath = Join-Path $evidenceDir "$Name.log"
    Write-Host "`n=== $Name ===" -ForegroundColor Cyan
    & $Command 2>&1 | Tee-Object -FilePath $logPath
    if ($LASTEXITCODE -ne 0) {
        throw "$Name failed with exit code $LASTEXITCODE. See $logPath"
    }
}

Write-Host 'Holiday Planner final verification' -ForegroundColor Green
Write-Host "Project: $projectRoot"

Invoke-LoggedCommand -Name 'java-version' -Command { java -version }
Invoke-LoggedCommand -Name 'maven-version' -Command { mvn -version }
Invoke-LoggedCommand -Name 'mvn-clean-test' -Command { mvn --batch-mode --no-transfer-progress clean test }
Invoke-LoggedCommand -Name 'mvn-clean-package' -Command { mvn --batch-mode --no-transfer-progress clean package }

if ($RunPostman) {
    $newman = Get-Command newman -ErrorAction SilentlyContinue
    if (-not $newman) {
        throw 'Newman was requested but is not installed or not available in PATH.'
    }
    Invoke-LoggedCommand -Name 'postman-newman' -Command {
        newman run 'postman/HolidayPlanner.postman_collection.json' --reporters cli
    }
}

if ($RunBrowserSmoke) {
    $required = 'HP_USER_EMAIL', 'HP_USER_PASSWORD', 'HP_ADMIN_EMAIL', 'HP_ADMIN_PASSWORD'
    $missing = $required | Where-Object { -not [Environment]::GetEnvironmentVariable($_) }
    if ($missing) {
        throw "Browser smoke requires environment variables: $($missing -join ', ')"
    }
    Invoke-LoggedCommand -Name 'browser-smoke' -Command { node 'scripts/browser-smoke.mjs' }
}

Write-Host "`n=== git-status ===" -ForegroundColor Cyan
git status --short
git status

Write-Host "`nVerification completed. Raw logs are in $evidenceDir (ignored by Git)." -ForegroundColor Green
