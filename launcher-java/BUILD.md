# Инструкция по сборке Horizon Launcher

## Требования

- **Java 21+** (JDK)
- **Maven 3.8+**
- **Git** (для клонирования репозитория)

## Быстрая сборка

### 1. Клонирование репозитория

```bash
git clone <repository-url>
cd Horizon/launcher-java
```

### 2. Сборка проекта

```bash
mvn clean package
```

После успешной сборки JAR файл будет находиться в:
```
target/horizon-launcher-1.0.0.jar
```

### 3. Запуск

**Важно:** JavaFX требует модульную систему Java, поэтому простой запуск через `java -jar` не работает. Используйте один из вариантов ниже:

**Вариант 1: Через скрипт запуска (рекомендуется)**

Windows (PowerShell):
```powershell
.\run.ps1
```

Windows (CMD):
```cmd
run.bat
```

**Вариант 2: Через Maven (для разработки)**
```bash
mvn javafx:run
```

**Вариант 3: Ручной запуск с module-path**
```bash
java --module-path target/lib --add-modules javafx.controls,javafx.fxml,javafx.graphics -cp "target/horizon-launcher-1.0.0.jar;target/lib/*" com.horizon.launcher.LauncherApplication
```

**Примечание:** Скрипты `run.ps1` и `run.bat` автоматически настраивают module-path и копируют зависимости при необходимости.

## Детальная сборка

### Проверка зависимостей

Перед сборкой убедитесь, что все зависимости загружены:

```bash
mvn dependency:resolve
```

### Компиляция

```bash
mvn clean compile
```

### Сборка JAR

```bash
mvn clean package
```

Maven Shade Plugin автоматически создаст исполняемый JAR со всеми зависимостями.

## Структура собранного JAR

JAR файл содержит:
- Все исходные классы проекта
- JavaFX библиотеки (21.0.2)
- OkHttp для HTTP запросов
- Gson для JSON парсинга
- SLF4J + Logback для логирования
- Apache Commons Compress для работы с архивами

## Конфигурация перед запуском

Перед запуском лаунчера убедитесь, что:

1. **API сервер запущен** на `http://localhost:3000` (или измените URL в `config.properties`)
2. **Telegram бот настроен** (токен в `.env` файле API сервера)
3. **Конфигурация обновлена** в `src/main/resources/config.properties`:
   ```properties
   api.server.url=http://localhost:3000
   telegram.bot.url=https://t.me/Horizon_Launcher_bot
   ```

## Размер JAR файла

Ожидаемый размер JAR файла: ~50-70 MB (включая все зависимости JavaFX)

## Устранение проблем

### Ошибка компиляции

Если возникают ошибки компиляции:
```bash
mvn clean
mvn dependency:resolve
mvn compile
```

### Проблемы с JavaFX

**Ошибка "JavaFX runtime components are missing":**

JavaFX требует модульную систему Java. Не используйте простой `java -jar`. Вместо этого:

1. Используйте скрипт запуска:
   ```powershell
   .\run.ps1
   ```

2. Или используйте Maven:
   ```bash
   mvn javafx:run
   ```

3. Или настройте module-path вручную (см. раздел "Запуск" выше)

Убедитесь, что используется Java 21+ и JavaFX 21+:
```bash
java -version
mvn -version
```

### Проблемы с зависимостями

Очистите локальный репозиторий Maven:
```bash
mvn dependency:purge-local-repository
mvn clean install
```

## Сборка для релиза

Для создания релизной версии:

1. Обновите версию в `pom.xml`:
   ```xml
   <version>1.0.0</version>
   ```

2. Соберите проект:
   ```bash
   mvn clean package
   ```

3. Проверьте JAR файл:
   ```powershell
   .\run.ps1
   ```
   
   Или через Maven:
   ```bash
   mvn javafx:run
   ```

## Дополнительные команды

### Запуск тестов (если есть)

```bash
mvn test
```

### Генерация документации

```bash
mvn javadoc:javadoc
```

### Очистка проекта

```bash
mvn clean
```

## Поддержка

При возникновении проблем проверьте:
- Логи в `logs/` директории
- Конфигурацию в `config.properties`
- Статус API сервера
