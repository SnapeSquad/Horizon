$ErrorActionPreference = 'Stop'

param(
  [switch]$NoAdminPanel,
  [switch]$NoLauncher
)

$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$statePath = Join-Path $PSScriptRoot 'dev-processes.json'

function Start-DevProcess {
  param(
    [string]$Name,
    [string]$WorkingDir,
    [string]$Command
  )

  Write-Host "Starting $Name..."
  $proc = Start-Process powershell -ArgumentList '-NoExit', '-Command', "Set-Location '$WorkingDir'; $Command" -PassThru
  [PSCustomObject]@{
    name = $Name
    pid = $proc.Id
  }
}

$started = @()

$started += Start-DevProcess -Name 'api-server' -WorkingDir (Join-Path $root 'api-server') -Command 'npm start'

if (-not $NoAdminPanel) {
  $started += Start-DevProcess -Name 'admin-panel' -WorkingDir (Join-Path $root 'admin-panel') -Command 'npm run dev -- --host'
}

if (-not $NoLauncher) {
  $started += Start-DevProcess -Name 'launcher-java' -WorkingDir (Join-Path $root 'launcher-java') -Command 'mvn javafx:run'
}

$started | ConvertTo-Json | Set-Content -Encoding UTF8 $statePath
Write-Host "Started $($started.Count) process(es). Use 'npm run dev:stop' to stop them."
