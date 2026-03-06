# Скрипт запуска Horizon Launcher
# Правильно настраивает JavaFX module-path

$ErrorActionPreference = "Stop"

# Получаем директорию скрипта
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$LauncherDir = $ScriptDir
$TargetDir = Join-Path $LauncherDir "target"
$JarFile = Join-Path $TargetDir "horizon-launcher-1.0.0.jar"
$LibDir = Join-Path $TargetDir "lib"

# Проверяем наличие JAR файла
if (-not (Test-Path $JarFile)) {
    Write-Host "[ERROR] JAR файл не найден: $JarFile" -ForegroundColor Red
    Write-Host "Сначала выполните сборку: mvn clean package" -ForegroundColor Yellow
    exit 1
}

# Проверяем наличие библиотек
if (-not (Test-Path $LibDir)) {
    Write-Host "[INFO] Копируем зависимости..." -ForegroundColor Yellow
    Push-Location $LauncherDir
    mvn dependency:copy-dependencies -DoutputDirectory=target/lib
    Pop-Location
}

# Находим JavaFX модули
$JavaFXModules = @()
$JavaFXJars = Get-ChildItem -Path $LibDir -Filter "javafx-*.jar" | Where-Object { 
    $_.Name -notmatch "-win\.jar$" -and 
    $_.Name -notmatch "-linux\.jar$" -and 
    $_.Name -notmatch "-mac\.jar$" 
}

foreach ($jar in $JavaFXJars) {
    # Преобразуем имя файла в имя модуля
    # javafx-controls-21.0.2.jar -> javafx.controls
    $baseName = $jar.BaseName
    if ($baseName -match "^javafx-(.+?)(?:-\d+\.\d+\.\d+)?$") {
        $moduleName = "javafx.$($Matches[1])"
        $JavaFXModules += $moduleName
    }
}

# Формируем module-path (все JAR файлы из lib)
$ModulePath = $LibDir
$AllJars = Get-ChildItem -Path $LibDir -Filter "*.jar" | ForEach-Object { $_.FullName }
$ClassPath = ($AllJars | Where-Object { $_ -notmatch "javafx-" }) -join ";"

# Формируем команду запуска
$JavaFXModulesList = $JavaFXModules -join ","

Write-Host "[INFO] Запуск Horizon Launcher..." -ForegroundColor Green
Write-Host "[INFO] Module Path: $ModulePath" -ForegroundColor Cyan
Write-Host "[INFO] JavaFX Modules: $JavaFXModulesList" -ForegroundColor Cyan

# Запускаем приложение
java `
    --module-path $ModulePath `
    --add-modules $JavaFXModulesList `
    -cp "$JarFile;$ClassPath" `
    com.horizon.launcher.LauncherApplication

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Ошибка при запуске приложения" -ForegroundColor Red
    exit $LASTEXITCODE
}
