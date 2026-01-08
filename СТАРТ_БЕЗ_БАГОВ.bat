@echo off
title HORIZON LAUNCHER - АВТОЗАПУСК
color 0A

echo.
echo ============================================
echo    HORIZON LAUNCHER - АВТОЗАПУСК
echo ============================================
echo.

REM Проверка Node.js
where node >nul 2>nul
if errorlevel 1 (
    echo [X] Node.js НЕ НАЙДЕН!
    echo Установите Node.js: https://nodejs.org/
    pause
    exit
)

echo [+] Node.js найден: 
node --version
echo.

REM Переход в корневую папку
cd /d "%~dp0"

REM Установка зависимостей (если нужно)
echo ============================================
echo    [1/4] ПРОВЕРКА ЗАВИСИМОСТЕЙ
echo ============================================
echo.

if not exist "api-server\node_modules\" (
    echo [*] Установка зависимостей API Server...
    cd api-server
    call npm install
    cd ..
)

if not exist "horizon-ui\node_modules\" (
    echo [*] Установка зависимостей React UI...
    cd horizon-ui
    call npm install
    cd ..
)

if not exist "electron-launcher\node_modules\" (
    echo [*] Установка зависимостей Electron...
    cd electron-launcher
    call npm install
    cd ..
)

echo.
echo [+] Все зависимости установлены!
echo.

REM Запуск API Server
echo ============================================
echo    [2/4] ЗАПУСК API SERVER
echo ============================================
echo.
start "API Server" cmd /k "cd /d %~dp0api-server && npm start"
timeout /t 3 /nobreak >nul

REM Запуск React UI
echo ============================================
echo    [3/4] ЗАПУСК REACT UI
echo ============================================
echo.
start "React UI" cmd /k "cd /d %~dp0horizon-ui && npm run dev"
timeout /t 5 /nobreak >nul

REM Запуск Electron
echo ============================================
echo    [4/4] ЗАПУСК ELECTRON LAUNCHER
echo ============================================
echo.
cd electron-launcher
start "Electron Launcher" cmd /k "npm run dev"

echo.
echo ============================================
echo    ВСЁ ЗАПУЩЕНО!
echo ============================================
echo.
echo API Server:  http://localhost:3000
echo React UI:    http://localhost:5173
echo Electron:    Окно откроется автоматически
echo.
echo Чтобы остановить - закройте все окна CMD
echo.
pause


