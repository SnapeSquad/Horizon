$ErrorActionPreference = 'Stop'

$statePath = Join-Path $PSScriptRoot 'dev-processes.json'

if (-not (Test-Path $statePath)) {
  Write-Host 'No running process state file found. Nothing to stop.'
  exit 0
}

$items = Get-Content $statePath -Encoding UTF8 | ConvertFrom-Json

foreach ($item in $items) {
  try {
    $p = Get-Process -Id $item.pid -ErrorAction Stop
    Write-Host "Stopping $($item.name) (PID $($item.pid))"
    Stop-Process -Id $item.pid -Force
  } catch {
    Write-Host "$($item.name) (PID $($item.pid)) already stopped."
  }
}

Remove-Item $statePath -Force
Write-Host 'All tracked dev processes stopped.'
