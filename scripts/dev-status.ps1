$ErrorActionPreference = 'Stop'

$statePath = Join-Path $PSScriptRoot 'dev-processes.json'

if (-not (Test-Path $statePath)) {
  Write-Host 'No active dev process state found.'
  exit 0
}

$parsed = Get-Content $statePath -Encoding UTF8 | ConvertFrom-Json
$items = @($parsed | ForEach-Object { $_ })

$rows = foreach ($item in $items) {
  if (-not $item -or -not $item.PSObject.Properties['pid']) {
    continue
  }
  $isAlive = [bool](Get-Process -Id $item.pid -ErrorAction SilentlyContinue)
  [PSCustomObject]@{
    Name = $item.name
    PID = $item.pid
    Status = if ($isAlive) { 'running' } else { 'stopped' }
    StartedAt = $item.startedAt
    StdOut = $item.stdout
    StdErr = $item.stderr
  }
}

$rows | Format-Table -AutoSize

if ($rows.Status -contains 'stopped') {
  Write-Host "One or more tracked processes are no longer running. Use 'npm run dev:stop' to clear stale state."
}
