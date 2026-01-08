# Horizon Launcher Run Script
# Automatically finds Java and sets JAVA_HOME

$ErrorActionPreference = "Stop"

Write-Host "=== Horizon Launcher ===" -ForegroundColor Cyan

# Find Java
try {
    $javaExe = (Get-Command java).Source
    $possiblePaths = @(
        (Split-Path (Split-Path $javaExe)),
        "C:\Program Files\Java\jdk-25",
        "C:\Program Files\Java\jdk-21",
        "C:\Program Files\Java\jdk-17"
    )
    
    $jdkFound = $false
    foreach ($path in $possiblePaths) {
        if (Test-Path "$path\bin\java.exe") {
            $env:JAVA_HOME = $path
            $jdkFound = $true
            break
        }
    }
    
    if (-not $jdkFound) {
        $env:JAVA_HOME = Split-Path (Split-Path $javaExe)
    }
} catch {
    Write-Host "Error: Java not found!" -ForegroundColor Red
    Write-Host "Install Java 17 or higher from https://adoptium.net/" -ForegroundColor Red
    exit 1
}

# Always use Maven to run (JavaFX requires module path configuration)
Write-Host "Running launcher through Maven..." -ForegroundColor Yellow
& .\mvnw.cmd javafx:run
