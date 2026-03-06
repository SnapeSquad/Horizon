# 🚀 Быстрый старт - Horizon Launcher

## 📋 Требования

- **Java 21+** (JDK)
- **Maven 3.8+** (для сборки)
- **API сервер** должен быть запущен

## 🔨 Сборка проекта

### Вариант 1: Быстрая сборка (рекомендуется)

```bash
cd launcher-java
mvn clean package
```

После сборки JAR файл будет в: `target/horizon-launcher-1.0.0.jar`

### Вариант 2: Только компиляция (для разработки)

```bash
cd launcher-java
mvn clean compile
```

## ▶️ Запуск лаунчера

### Вариант 1: Запуск через скрипт (рекомендуется)

**Windows (PowerShell):**
```powershell
cd launcher-java
.\run.ps1
```

**Windows (CMD):**
```cmd
cd launcher-java
run.bat
```

Скрипты автоматически настраивают JavaFX module-path и запускают приложение.

### Вариант 2: Запуск через Maven (для разработки)

```bash
cd launcher-java
mvn javafx:run
```

### Вариант 3: Ручной запуск (требует настройки module-path)

Если нужно запустить вручную, используйте:
```bash
cd launcher-java
java --module-path target/lib --add-modules javafx.controls,javafx.fxml,javafx.graphics -cp "target/horizon-launcher-1.0.0.jar;target/lib/*" com.horizon.launcher.LauncherApplication
```

## ⚙️ Перед запуском

### 1. Запустите API сервер

```bash
cd api-server
npm install
node server.js
```

API сервер должен быть доступен на `http://localhost:3000`

### 2. Проверьте конфигурацию

Убедитесь, что в `launcher-java/src/main/resources/config.properties` указан правильный URL API:

```properties
api.server.url=http://localhost:3000
telegram.bot.url=https://t.me/Horizon_Launcher_bot
```

## 🐛 Устранение проблем

### Ошибка "Could not find or load main class"

Убедитесь, что используете Java 21+:
```bash
java -version
```

Должно быть: `openjdk version "21"` или выше

### Ошибка подключения к API серверу

1. Проверьте, что API сервер запущен:
   ```bash
   curl http://localhost:3000/api/server/status
   ```

2. Проверьте конфигурацию в `config.properties`

### Ошибка при сборке

Очистите проект и пересоберите:
```bash
cd launcher-java
mvn clean
mvn dependency:resolve
mvn package
```

## 📝 Полная инструкция

Подробная инструкция по сборке находится в [BUILD.md](BUILD.md)
