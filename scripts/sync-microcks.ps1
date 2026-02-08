# Microcks Auto-Sync Script (PowerShell)
# This script runs every 10 minutes to check for spec changes and update Microcks

$ErrorActionPreference = "Continue"

# Configuration
$GitHubRepo = "https://github.com/sergeimeshe2-spec/user-service.git"
$LocalRepo = "C:\temp\user-service-specs"
$MicrocksUrl = "http://localhost:8080"
$SpecsDir = "$LocalRepo\src\main\resources\specs"
$LastHashFile = "C:\temp\user-service-specs-last-hash.txt"

function Log {
    param([string]$Message)
    Write-Host "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] $Message"
}

Log "Starting Microcks sync check..."

# Clone or update repository
if (Test-Path $LocalRepo) {
    Log "Updating local repository..."
    Set-Location $LocalRepo
    git fetch origin
    git reset --hard origin/main
} else {
    Log "Cloning repository..."
    New-Item -ItemType Directory -Path $LocalRepo -Force | Out-Null
    git clone -q --depth 1 $GitHubRepo $LocalRepo
    Set-Location $LocalRepo
}

# Get current hash of specs
$specFiles = Get-ChildItem -Path $SpecsDir -Filter "*.yaml" -File
$specContent = $specFiles | ForEach-Object { Get-Content $_.FullName -Raw }
$currentHash = (Get-Hash -InputStream ([System.IO.MemoryStream]::New([System.Text.Encoding]::UTF8.GetBytes($specContent))) -Algorithm MD5).Hash

# Check if specs changed
if (Test-Path $LastHashFile) {
    $lastHash = Get-Content $LastHashFile
    if ($currentHash -eq $lastHash) {
        Log "No changes detected in specifications"
        exit 0
    }
}

Log "Changes detected! Updating Microcks..."

# Wait for Microcks to be ready
$retryCount = 0
$maxRetries = 12
do {
    try {
        Invoke-WebRequest -Uri "$MicrocksUrl/api/health" -UseBasicParsing -TimeoutSec 5 | Out-Null
        break
    } catch {
        Log "Waiting for Microcks to be ready..."
        Start-Sleep -Seconds 5
        $retryCount++
    }
} while ($retryCount -lt $maxRetries)

if ($retryCount -ge $maxRetries) {
    Log "Error: Microcks is not available"
    exit 1
}

# Import OpenAPI spec
Log "Importing OpenAPI specification..."
$openapiSpec = "$SpecsDir\user-service-openapi.yaml"
if (Test-Path $openapiSpec) {
    try {
        $files = @{
            file = Get-Item $openapiSpec
        }
        Invoke-RestMethod -Uri "$MicrocksUrl/api/artifacts/resource" -Method Post -Form $files -ErrorAction SilentlyContinue
        Log "OpenAPI spec imported successfully"
    } catch {
        Log "Failed to import OpenAPI spec: $_"
    }
}

# Import AsyncAPI spec
Log "Importing AsyncAPI specification..."
$asyncapiSpec = "$SpecsDir\user-events-asyncapi.yaml"
if (Test-Path $asyncapiSpec) {
    try {
        $files = @{
            file = Get-Item $asyncapiSpec
        }
        Invoke-RestMethod -Uri "$MicrocksUrl/api/artifacts/resource" -Method Post -Form $files -ErrorAction SilentlyContinue
        Log "AsyncAPI spec imported successfully"
    } catch {
        Log "Failed to import AsyncAPI spec: $_"
    }
}

# Save current hash
$currentHash | Out-File -FilePath $LastHashFile -Force

Log "Microcks sync completed successfully!"
