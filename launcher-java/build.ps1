# Horizon Launcher Build Script
# Automatically finds Java and sets JAVA_HOME

$ErrorActionPreference = "Stop"

Write-Host "=== Horizon Launcher Build Script ===" -ForegroundColor Cyan

# Find Java
Write-Host "`nSearching for Java..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    Write-Host "Java found: $javaVersion" -ForegroundColor Green
    
    # Try to find JDK
    $javaExe = (Get-Command java).Source
    $possiblePaths = @(
        "C:\Program Files\Java\jdk-25",
        "C:\Program Files\Java\jdk-21",
        "C:\Program Files\Java\jdk-17",
        "$env:ProgramFiles\Java\jdk-25",
        "$env:ProgramFiles\Java\jdk-21",
        "$env:ProgramFiles\Java\jdk-17",
        (Split-Path (Split-Path $javaExe))
    )
    
    $jdkFound = $false
    foreach ($path in $possiblePaths) {
        if (Test-Path "$path\bin\java.exe") {
            $env:JAVA_HOME = $path
            Write-Host "JAVA_HOME set: $env:JAVA_HOME" -ForegroundColor Green
            $jdkFound = $true
            break
        }
    }
    
    if (-not $jdkFound) {
        # Use JRE path as fallback
        $jrePath = Split-Path (Split-Path $javaExe)
        if (Test-Path "$jrePath\bin\java.exe") {
            $env:JAVA_HOME = $jrePath
            Write-Host "JAVA_HOME set (JRE): $env:JAVA_HOME" -ForegroundColor Yellow
        } else {
            Write-Host "Warning: Could not find JDK, using JRE path" -ForegroundColor Yellow
            $env:JAVA_HOME = Split-Path (Split-Path $javaExe)
        }
    }
} catch {
    Write-Host "Error: Java not found!" -ForegroundColor Red
    Write-Host "Install Java 17 or higher from https://adoptium.net/" -ForegroundColor Red
    exit 1
}

# Check Maven Wrapper
Write-Host "`nChecking Maven Wrapper..." -ForegroundColor Yellow
if (-not (Test-Path ".\mvnw.cmd")) {
    Write-Host "Error: mvnw.cmd not found!" -ForegroundColor Red
    exit 1
}

# Build project
Write-Host "`nBuilding project..." -ForegroundColor Yellow
& .\mvnw.cmd clean install

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n[SUCCESS] Build completed!" -ForegroundColor Green
    Write-Host "`nTo run the launcher:" -ForegroundColor Cyan
    Write-Host "  .\run.ps1" -ForegroundColor White
    Write-Host "  or" -ForegroundColor Gray
    Write-Host "  .\mvnw.cmd javafx:run" -ForegroundColor White
} else {
    Write-Host "`n[ERROR] Build failed!" -ForegroundColor Red
    exit 1
}
