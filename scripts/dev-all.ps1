param(
  [switch]$NoAdminPanel,
  [switch]$NoLauncher
)

$ErrorActionPreference = 'Stop'

$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$statePath = Join-Path $PSScriptRoot 'dev-processes.json'
$logsDir = Join-Path $root 'logs/dev'

function Assert-CommandAvailable {
  param(
    [string]$CommandName
  )

  if (-not (Get-Command $CommandName -ErrorAction SilentlyContinue)) {
    throw "Required command '$CommandName' is not available in PATH."
  }
}

function Get-ActiveStateItems {
  if (-not (Test-Path $statePath)) {
    return @()
  }

  $raw = Get-Content $statePath -Encoding UTF8 | ConvertFrom-Json
  if ($null -eq $raw) {
    return @()
  }

  $items = @($raw | ForEach-Object { $_ })
  $alive = @()
  foreach ($item in $items) {
    if (-not $item -or -not $item.PSObject.Properties['pid']) {
      continue
    }
    if (Get-Process -Id $item.pid -ErrorAction SilentlyContinue) {
      $alive += $item
    }
  }

  if ($alive.Count -eq 0) {
    Remove-Item $statePath -Force
  }

  return $alive
}

function Start-DevProcess {
  param(
    [string]$Name,
    [string]$WorkingDir,
    [string]$Command
  )

  Write-Host "Starting $Name..."

  $stdoutPath = Join-Path $logsDir "$Name.stdout.log"
  $stderrPath = Join-Path $logsDir "$Name.stderr.log"
  if (Test-Path $stdoutPath) { Remove-Item $stdoutPath -Force }
  if (Test-Path $stderrPath) { Remove-Item $stderrPath -Force }

  $escapedWorkingDir = $WorkingDir.Replace("'", "''")
  $commandScript = "Set-Location '$escapedWorkingDir'; `$ErrorActionPreference='Stop'; $Command"

  $proc = Start-Process powershell `
    -ArgumentList '-NoProfile', '-ExecutionPolicy', 'Bypass', '-Command', $commandScript `
    -PassThru `
    -WindowStyle Hidden `
    -RedirectStandardOutput $stdoutPath `
    -RedirectStandardError $stderrPath

  [PSCustomObject]@{
    name = $Name
    pid = $proc.Id
    workdir = $WorkingDir
    command = $Command
    stdout = $stdoutPath
    stderr = $stderrPath
    startedAt = (Get-Date).ToString('s')
  }
}

Assert-CommandAvailable -CommandName 'npm'
if (-not $NoLauncher) {
  Assert-CommandAvailable -CommandName 'mvn'
}

if (-not (Test-Path $logsDir)) {
  New-Item -ItemType Directory -Path $logsDir -Force | Out-Null
}

$active = Get-ActiveStateItems
if ($active.Count -gt 0) {
  throw "Dev processes are already running. Stop them first with 'npm run dev:stop'."
}

$started = @()
$started += Start-DevProcess -Name 'api-server' -WorkingDir (Join-Path $root 'api-server') -Command 'npm start'

if (-not $NoAdminPanel) {
  $started += Start-DevProcess -Name 'admin-panel' -WorkingDir (Join-Path $root 'admin-panel') -Command 'npm run dev -- --host'
}

if (-not $NoLauncher) {
  $started += Start-DevProcess -Name 'launcher-java' -WorkingDir (Join-Path $root 'launcher-java') -Command 'mvn javafx:run'
}

$started | ConvertTo-Json -Depth 4 | Set-Content -Encoding UTF8 $statePath

Write-Output "Started $($started.Count) process(es)."
Write-Output "Use 'npm run dev:status' to check health/log paths."
Write-Output "Use 'npm run dev:stop' to stop all tracked processes."
