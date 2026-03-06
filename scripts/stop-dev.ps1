$ErrorActionPreference = 'Stop'

$statePath = Join-Path $PSScriptRoot 'dev-processes.json'

if (-not (Test-Path $statePath)) {
  Write-Host 'No running process state file found. Nothing to stop.'
  exit 0
}

$parsed = Get-Content $statePath -Encoding UTF8 | ConvertFrom-Json
$items = @($parsed | ForEach-Object { $_ })

foreach ($item in $items) {
  if (-not $item -or -not $item.PSObject.Properties['pid']) {
    continue
  }
  if (Get-Process -Id $item.pid -ErrorAction SilentlyContinue) {
    Write-Host "Stopping $($item.name) (PID $($item.pid))"
    cmd /c "taskkill /PID $($item.pid) /T /F" | Out-Null
  } else {
    Write-Host "$($item.name) (PID $($item.pid)) already stopped."
  }
}

Remove-Item $statePath -Force
Write-Host 'All tracked dev processes stopped.'
