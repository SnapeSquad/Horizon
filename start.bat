@echo off
echo ========================================
echo    HORIZON LAUNCHER - AUTO START
echo ========================================
echo.

REM Проверка установки Node.js
where node >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Node.js not found! Please install Node.js first.
    echo Download from: https://nodejs.org/
    pause
    exit /b 1
)

echo [1/4] Checking dependencies...
echo.

REM Проверка и установка зависимостей для API Server
if not exist "api-server\node_modules" (
    echo Installing API Server dependencies...
    cd api-server
    call npm install
    cd ..
    echo.
)

REM Проверка и установка зависимостей для React UI
if not exist "horizon-ui\node_modules" (
    echo Installing React UI dependencies...
    cd horizon-ui
    call npm install
    cd ..
    echo.
)

REM Проверка и установка зависимостей для Electron Launcher
if not exist "electron-launcher\node_modules" (
    echo Installing Electron Launcher dependencies...
    cd electron-launcher
    call npm install
    cd ..
    echo.
)

echo [2/4] Starting API Server (port 3000)...
start "Horizon API Server" cmd /k "cd api-server && npm start"
timeout /t 3 /nobreak >nul

echo [3/4] Starting React UI (port 5173/5174)...
start "Horizon React UI" cmd /k "cd horizon-ui && npm run dev"
timeout /t 5 /nobreak >nul

echo [4/4] Starting Electron Launcher...
echo.
echo ========================================
echo    ALL SERVICES STARTED!
echo ========================================
echo.
echo API Server:   http://localhost:3000
echo React UI:     http://localhost:5173 (or 5174)
echo Electron:     Opening window...
echo.
echo Close these windows to stop all services.
echo ========================================
echo.

cd electron-launcher
call npm run dev

echo.
echo Electron closed. You can close API and React windows now.
pause

