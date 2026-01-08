# Horizon Launcher

Лаунчер Minecraft для сервера Horizon на Java + JavaFX.

## 🚀 Быстрый старт

### Требования
- Java 21 или выше
- Maven (или используйте Maven Wrapper - `mvnw.cmd`)

### Запуск лаунчера

**Вариант 1: Через PowerShell скрипт (рекомендуется)**
```powershell
.\build.ps1    # Сборка проекта
.\run.ps1      # Запуск лаунчера
```

**Вариант 2: Через Maven Wrapper**
```powershell
.\mvnw.cmd clean install    # Сборка
.\mvnw.cmd javafx:run      # Запуск
```

**Вариант 3: Через Maven (если установлен)**
```powershell
mvn clean install
mvn javafx:run
```

**Вариант 4: Запуск готового JAR**
```powershell
java -jar target\launcher-1.0.0.jar
```

## 🖥️ Запуск API сервера

Перейдите в папку `api-server`:

```powershell
cd ..\api-server
npm install    # Первый раз - установка зависимостей
node server.js # Запуск сервера
```

Сервер запустится на `http://localhost:3000`

### Настройка API сервера

1. Создайте файл `.env` в папке `api-server`:
```
TELEGRAM_BOT_TOKEN=ваш_токен_бота
```

2. Если токен не указан, Telegram бот не будет работать, но остальные функции будут доступны.

## 📁 Структура проекта

```
launcher-java/
├── src/main/java/          # Исходный код
├── src/main/resources/     # Ресурсы (стили, локализация)
├── pom.xml                 # Maven конфигурация
├── mvnw.cmd               # Maven Wrapper
├── build.ps1              # Скрипт сборки
└── run.ps1                # Скрипт запуска
```

## ⚙️ Конфигурация

Конфигурация лаунчера хранится в `%USERPROFILE%\.horizon-launcher\config.properties`

Основные настройки:
- `api.url` - URL API сервера (по умолчанию: `http://localhost:3000`)
- `ram.gb` - Выделение ОЗУ для игры (по умолчанию: 4)
- `discord.webhook.url` - URL Discord Webhook для логов (опционально)

## 🔧 Разработка

### Сборка проекта
```powershell
.\mvnw.cmd clean install
```

### Запуск в режиме разработки
```powershell
.\mvnw.cmd javafx:run
```

### Просмотр логов
Логи сохраняются в `%USERPROFILE%\.horizon-launcher\launcher.log`

## 📝 Основные функции

- ✅ Авторизация (MySQL через Backend + Microsoft OAuth2)
- ✅ Защита файлов через хеширование (Asset Guard)
- ✅ Изолированная Java 21
- ✅ Локализация (RU/EN/TT)
- ✅ 3D предпросмотр персонажа
- ✅ Автообновление
- ✅ Telegram новости

## 🐛 Решение проблем

**Ошибка: "Java не найдена"**
- Установите Java 21 с https://adoptium.net/
- Убедитесь, что Java добавлена в PATH

**Ошибка: "Maven не найден"**
- Используйте `mvnw.cmd` вместо `mvn`
- Или установите Maven с https://maven.apache.org/

**Лаунчер не подключается к серверу**
- Проверьте, что API сервер запущен на порту 3000
- Проверьте настройку `api.url` в конфиге

## 📞 Поддержка

При возникновении проблем проверьте логи в `%USERPROFILE%\.horizon-launcher\launcher.log`
