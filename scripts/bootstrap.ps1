$ErrorActionPreference = 'Stop'

Write-Host '== Horizon bootstrap =='

if (-not (Test-Path 'api-server/.env') -and (Test-Path 'api-server/.env.example')) {
  Copy-Item 'api-server/.env.example' 'api-server/.env'
  Write-Host 'Created api-server/.env from .env.example'
}

Write-Host 'Installing api-server dependencies...'
Push-Location 'api-server'
npm ci
Pop-Location

Write-Host 'Installing admin-panel dependencies...'
Push-Location 'admin-panel'
npm ci
Pop-Location

Write-Host 'Bootstrap complete.'
