# Инструкция по запуску Horizon Launcher

## Проблема с JavaFX

JavaFX требует модульную систему Java, поэтому простой запуск через `java -jar` **не работает** и выдает ошибку:
```
Error: JavaFX runtime components are missing, and are required to run this application
```

## Решение: Используйте скрипты запуска

### Windows (PowerShell) - Рекомендуется

```powershell
cd launcher-java
.\run.ps1
```

### Windows (CMD)

```cmd
cd launcher-java
run.bat
```

### Альтернатива: Через Maven

```bash
cd launcher-java
mvn javafx:run
```

## Что делают скрипты?

1. **Проверяют наличие JAR файла** - если его нет, предлагают выполнить сборку
2. **Копируют зависимости** - автоматически копируют JavaFX и другие библиотеки в `target/lib`
3. **Настраивают module-path** - правильно настраивают JavaFX модули для запуска
4. **Запускают приложение** - запускают лаунчер с правильными параметрами

## Перед запуском

1. **Соберите проект:**
   ```bash
   cd launcher-java
   mvn clean package
   ```

2. **Убедитесь, что API сервер запущен:**
   ```bash
   cd api-server
   npm install
   node server.js
   ```

3. **Проверьте конфигурацию** в `src/main/resources/config.properties`

## Устранение проблем

### Скрипт не запускается

Убедитесь, что используете правильную директорию:
```powershell
cd C:\Users\skviz\Desktop\Horizon\launcher-java
.\run.ps1
```

### Ошибка "JAR файл не найден"

Выполните сборку проекта:
```bash
mvn clean package
```

### Ошибка "JavaFX runtime components are missing"

Это означает, что вы пытаетесь запустить через `java -jar`. Используйте скрипты `run.ps1` или `run.bat` вместо этого.

### Ошибка подключения к API серверу

1. Проверьте, что API сервер запущен
2. Проверьте URL в `config.properties`
3. Проверьте файрвол и сетевые настройки
