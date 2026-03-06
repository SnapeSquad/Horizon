$ErrorActionPreference = 'Stop'

$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$apiEnvPath = Join-Path $root 'api-server/.env'

$hasErrors = $false

function Write-Check {
  param(
    [string]$Label,
    [bool]$Ok,
    [string]$Details
  )

  if ($Ok) {
    Write-Host "[OK]   $Label - $Details"
  } else {
    Write-Host "[FAIL] $Label - $Details"
    $script:hasErrors = $true
  }
}

function Get-VersionLine {
  param(
    [string]$CommandName,
    [string]$VersionArg
  )

  try {
    $output = cmd /c "$CommandName $VersionArg 2>&1"
    $line = $output | Where-Object { $_ -and $_.ToString().Trim().Length -gt 0 } | Select-Object -First 1
    return $line
  } catch {
    return $null
  }
}

function Get-CommandCheck {
  param(
    [string]$CommandName,
    [string]$VersionArg
  )

  $cmd = Get-Command $CommandName -ErrorAction SilentlyContinue
  if (-not $cmd) {
    return [PSCustomObject]@{
      ok = $false
      details = "$CommandName not found"
    }
  }

  $line = Get-VersionLine -CommandName $CommandName -VersionArg $VersionArg
  if (-not $line) {
    return [PSCustomObject]@{
      ok = $false
      details = "$CommandName found but version check failed"
    }
  }

  return [PSCustomObject]@{
    ok = $true
    details = [string]$line
  }
}

Write-Host '== Horizon doctor =='

$nodeCheck = Get-CommandCheck -CommandName 'node' -VersionArg '--version'
$npmCheck = Get-CommandCheck -CommandName 'npm' -VersionArg '--version'
$javaCheck = Get-CommandCheck -CommandName 'java' -VersionArg '-version'
$mvnCheck = Get-CommandCheck -CommandName 'mvn' -VersionArg '-version'

Write-Check -Label 'node in PATH' -Ok $nodeCheck.ok -Details $nodeCheck.details
Write-Check -Label 'npm in PATH' -Ok $npmCheck.ok -Details $npmCheck.details
Write-Check -Label 'java in PATH' -Ok $javaCheck.ok -Details $javaCheck.details
Write-Check -Label 'mvn in PATH' -Ok $mvnCheck.ok -Details $mvnCheck.details

$apiNodeModules = Test-Path (Join-Path $root 'api-server/node_modules')
$adminNodeModules = Test-Path (Join-Path $root 'admin-panel/node_modules')
$apiDepsDetails = 'run npm run setup'
if ($apiNodeModules) { $apiDepsDetails = 'node_modules present' }
$adminDepsDetails = 'run npm run setup'
if ($adminNodeModules) { $adminDepsDetails = 'node_modules present' }
Write-Check -Label 'api-server dependencies' -Ok $apiNodeModules -Details $apiDepsDetails
Write-Check -Label 'admin-panel dependencies' -Ok $adminNodeModules -Details $adminDepsDetails

$envExists = Test-Path $apiEnvPath
$envDetails = 'copy api-server/.env.example to api-server/.env'
if ($envExists) { $envDetails = '.env found' }
Write-Check -Label 'api-server/.env' -Ok $envExists -Details $envDetails

if ($envExists) {
  $envContent = Get-Content $apiEnvPath -Raw
  foreach ($requiredVar in @('ADMIN_TOKEN', 'JWT_SECRET')) {
    $present = $envContent -match "(?m)^\s*$requiredVar\s*=\s*.+$"
    $requiredDetails = 'missing value'
    if ($present) { $requiredDetails = 'set' }
    Write-Check -Label "api-server/.env:$requiredVar" -Ok $present -Details $requiredDetails
  }
}

if ($hasErrors) {
  Write-Host 'Doctor found issues. Fix failed checks and re-run `npm run doctor`.'
  exit 1
}

Write-Host 'Doctor checks passed.'
