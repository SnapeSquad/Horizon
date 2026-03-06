$ErrorActionPreference = 'Stop'

Write-Host '== Horizon verify =='

Write-Host '[1/3] API smoke tests'
Push-Location 'api-server'
try {
  npm test
  if ($LASTEXITCODE -ne 0) {
    throw 'API smoke tests failed.'
  }
} finally {
  Pop-Location
}

Write-Host '[2/3] Admin panel build'
Push-Location 'admin-panel'
try {
  npm run build
  if ($LASTEXITCODE -ne 0) {
    throw 'Admin panel build failed.'
  }
} finally {
  Pop-Location
}

Write-Host '[3/3] Java launcher compile'
Push-Location 'launcher-java'
try {
  mvn -q -DskipTests compile
  if ($LASTEXITCODE -ne 0) {
    throw 'Java launcher compile failed.'
  }
} finally {
  Pop-Location
}

Write-Host 'Verification complete.'
