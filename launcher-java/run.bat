@echo off
REM Скрипт запуска Horizon Launcher для Windows
REM Правильно настраивает JavaFX module-path

setlocal enabledelayedexpansion

REM Получаем директорию скрипта
set "SCRIPT_DIR=%~dp0"
set "LAUNCHER_DIR=%SCRIPT_DIR%"
set "TARGET_DIR=%LAUNCHER_DIR%target"
set "JAR_FILE=%TARGET_DIR%\horizon-launcher-1.0.0.jar"
set "LIB_DIR=%TARGET_DIR%\lib"

REM Проверяем наличие JAR файла
if not exist "%JAR_FILE%" (
    echo [ERROR] JAR файл не найден: %JAR_FILE%
    echo Сначала выполните сборку: mvn clean package
    exit /b 1
)

REM Проверяем наличие библиотек
if not exist "%LIB_DIR%" (
    echo [INFO] Копируем зависимости...
    cd /d "%LAUNCHER_DIR%"
    call mvn dependency:copy-dependencies -DoutputDirectory=target/lib
    if errorlevel 1 (
        echo [ERROR] Ошибка при копировании зависимостей
        exit /b 1
    )
)

REM Формируем module-path (все JAR файлы из lib)
set "MODULE_PATH=%LIB_DIR%"

REM Используем известные модули JavaFX из проекта
REM Модули: javafx.controls, javafx.fxml, javafx.graphics
set "JAVAFX_MODULES=javafx.controls,javafx.fxml,javafx.graphics"

REM Формируем classpath (все JAR кроме JavaFX)
set "CLASSPATH=%JAR_FILE%"
for %%f in ("%LIB_DIR%\*.jar") do (
    echo %%~nf | findstr /R "^javafx-" >nul
    if errorlevel 1 (
        set "CLASSPATH=!CLASSPATH!;%%f"
    )
)

echo [INFO] Запуск Horizon Launcher...
echo [INFO] Module Path: %MODULE_PATH%
echo [INFO] JavaFX Modules: %JAVAFX_MODULES%

REM Запускаем приложение
java --module-path "%MODULE_PATH%" --add-modules "%JAVAFX_MODULES%" -cp "%CLASSPATH%" com.horizon.launcher.LauncherApplication

if errorlevel 1 (
    echo [ERROR] Ошибка при запуске приложения
    exit /b %ERRORLEVEL%
)
