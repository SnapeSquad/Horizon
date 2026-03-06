$ErrorActionPreference = 'Stop'

Write-Host '== Horizon verify =='

Write-Host '[1/3] API smoke tests'
Push-Location 'api-server'
npm test
Pop-Location

Write-Host '[2/3] Admin panel build'
Push-Location 'admin-panel'
npm run build
Pop-Location

Write-Host '[3/3] Java launcher compile'
Push-Location 'launcher-java'
mvn -q -DskipTests compile
Pop-Location

Write-Host 'Verification complete.'
