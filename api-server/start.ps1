# Скрипт запуска API сервера

Write-Host "=== Horizon API Server ===" -ForegroundColor Cyan

# Проверка Node.js
try {
    $nodeVersion = node --version
    Write-Host "Node.js найден: $nodeVersion" -ForegroundColor Green
} catch {
    Write-Host "Ошибка: Node.js не найден!" -ForegroundColor Red
    Write-Host "Установите Node.js с https://nodejs.org/" -ForegroundColor Red
    exit 1
}

# Проверка зависимостей
if (-not (Test-Path "node_modules")) {
    Write-Host "`nУстановка зависимостей..." -ForegroundColor Yellow
    npm install
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Ошибка при установке зависимостей!" -ForegroundColor Red
        exit 1
    }
}

# Запуск сервера
Write-Host "`nЗапуск API сервера..." -ForegroundColor Yellow
Write-Host "Сервер будет доступен на http://localhost:3000" -ForegroundColor Cyan
Write-Host "Для остановки нажмите Ctrl+C`n" -ForegroundColor Gray

node server.js

