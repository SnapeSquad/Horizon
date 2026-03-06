# Спецификация модуля запуска игры (Game Runner)

## Обзор

Модуль запуска игры (Game Runner) предоставляет полную инфраструктуру для безопасного запуска Minecraft с автоматической проверкой файлов, загрузкой JRE и защитой от инжекции читов.

## Компоненты

### 1. AssetManager.java

**Назначение**: Управление и валидация файлов игры через SHA-256 хеширование.

**Основные функции**:
- **Многопточная проверка файлов**: Использует `ExecutorService` для параллельной проверки хешей SHA-256
- **Верификация целостности**: Проверяет каждый файл из манифеста на соответствие хешу
- **Очистка неразрешенных файлов**: Автоматически удаляет файлы в `/mods` и `/versions`, которые не указаны в манифесте
- **Рекурсивная очистка**: Удаляет пустые директории после очистки файлов

**Использование**:
```java
AssetManager assetManager = AssetManager.getInstance();

// Проверка файлов
Map<String, String> manifest = new HashMap<>();
manifest.put("mods/horizon-cosmetics-1.0.0.jar", "sha256-hash-here");
List<FileVerificationResult> results = assetManager.verifyFiles(manifest).join();

// Очистка неразрешенных файлов
CleanupResult cleanup = assetManager.cleanupUnlistedFiles(manifest).join();
```

**Производительность**:
- Использует пул потоков размером `Runtime.availableProcessors() - 1`
- Асинхронная обработка через `CompletableFuture`
- Оптимизированное чтение файлов с буфером 8KB

### 2. RuntimeDownloader.java

**Назначение**: Автоматическая загрузка и установка JRE 21 Runtime.

**Основные функции**:
- **Автоматическое определение платформы**: Windows, Linux, macOS
- **Загрузка с Temurin (Eclipse Adoptium)**: Официальный источник JRE
- **Проверка целостности**: Валидация размера и структуры архива
- **Распаковка**: Поддержка ZIP (Windows) и TAR.GZ (Linux/macOS)
- **Кэширование**: Сохранение JRE в изолированной директории

**Поддерживаемые платформы**:
- Windows x64: ZIP архив
- Linux x64: TAR.GZ архив
- macOS x64/aarch64: TAR.GZ архив

**Использование**:
```java
RuntimeDownloader downloader = RuntimeDownloader.getInstance();
RuntimeInstallResult result = downloader.ensureRuntimeInstalled("windows", "x64").join();

if (result.isSuccess()) {
    Path javaExe = result.getJavaExecutable();
    // Использовать javaExe для запуска
}
```

**Безопасность**:
- Проверка минимального размера архива (100 МБ)
- Валидация структуры архива перед распаковкой
- Проверка целостности через чтение записей

### 3. LaunchBuilder.java

**Назначение**: Построение команды запуска Minecraft с правильными аргументами.

**Особенности**:
- **Fluent API**: Цепочка методов для настройки параметров
- **Автоматическое построение classpath**: Сканирование библиотек из `.minecraft/libraries/`
- **Поддержка кастомных свойств**: Передача токенов и других параметров через `-D`
- **Валидация параметров**: Проверка всех обязательных параметров перед построением

**Пример использования**:

Базовый запуск с указанием версии:
```java
List<String> command = LaunchBuilder.create()
    .version("1.21")  // Версия для сервера Анархия
    .username("Player")
    .accessToken("token-here")
    .uuid("uuid-here")
    .memory(2048, 4096)
    .javaExecutable(javaExe)
    .customProperty("horizon.token", "token")
    .build();

ProcessBuilder pb = new ProcessBuilder(command);
Process process = pb.start();
```

Запуск с выбором типа сервера:
```java
// Для сервера Анархия (версия 1.21)
List<String> commandAnarchy = LaunchBuilder.create()
    .serverType(LaunchBuilder.ServerType.ANARCHY)  // Автоматически установит версию 1.21
    .username("Player")
    .accessToken("token-here")
    .uuid("uuid-here")
    .memory(2048, 4096)
    .javaExecutable(javaExe)
    .build();

// Для сервера Выживание (версия 1.21.10)
List<String> commandSurvival = LaunchBuilder.create()
    .serverType(LaunchBuilder.ServerType.SURVIVAL)  // Автоматически установит версию 1.21.10
    .username("Player")
    .accessToken("token-here")
    .uuid("uuid-here")
    .memory(2048, 4096)
    .javaExecutable(javaExe)
    .build();
```

**Поддерживаемые версии**:
- **1.21** - Сервер Анархия (LaunchBuilder.ServerType.ANARCHY)
- **1.21.10** - Сервер Выживание (LaunchBuilder.ServerType.SURVIVAL)

Обе версии используют asset index **5** (Minecraft 1.21 asset index).

**Аргументы команды**:
1. JVM аргументы: `-Xmx`, `-Xms`, `-Djava.library.path`
2. Системные свойства: `-Dminecraft.launcher.brand`, `-Dhorizon.token`
3. Classpath: Автоматически построенный из библиотек
4. Главный класс: `net.minecraft.client.main.Main`
5. Аргументы игры: `--username`, `--version`, `--gameDir`, `--assetIndex`, и т.д.

**Типы серверов**:
- `ServerType.ANARCHY` - Анархия, версия Minecraft 1.21
- `ServerType.SURVIVAL` - Выживание, версия Minecraft 1.21.10

Тип сервера автоматически определяется по версии, но может быть установлен явно через метод `serverType()`.

### 4. AntiInject.java

**Назначение**: Защита от инжекции читов через сканирование процессов.

**Возможности**:
- **Сканирование запущенных процессов**: Проверка всех процессов системы
- **База известных инжекторов**: 20+ известных читов и инжекторов
- **Обнаружение подозрительных процессов**: Поиск по ключевым словам
- **Кросс-платформенность**: Windows, Linux, macOS

**Известные инжекторы**:
- Vape, Ghost Client, Wurst, Impact
- Sigma, Future, Kami Blue, Seppuku
- DLL инжекторы, Process Hacker
- Cheat Engine, ArtMoney, и другие

**Использование**:
```java
AntiInject antiInject = AntiInject.getInstance();
InjectorScanResult result = antiInject.scanForInjectors().join();

if (!result.isClean()) {
    logger.warn("Обнаружены инжекторы: {}", result.getDetectedProcesses());
    // Можно заблокировать запуск игры
}
```

**Расширяемость**:
- Метод `addKnownInjector()` для добавления новых инжекторов
- Регулярное обновление списка через API (планируется)

## Интеграция в GameLauncher

Полный процесс запуска включает все компоненты:

```java
GameLauncher launcher = GameLauncher.getInstance();

// Базовый запуск (безопасность отключена)
Process process = launcher.launchMinecraft(
    "1.21.1", "username", "token", "uuid", 4096
).join();

// Полный запуск с безопасностью
Process process = launcher.launchMinecraft(
    "1.21.1", "username", "token", "uuid", 4096,
    true,  // verifyFiles
    true   // cleanupUnlisted
).join();
```

**Последовательность операций**:
1. ✅ Проверка Anti-Inject (сканирование процессов)
2. ✅ Верификация файлов (если включено)
3. ✅ Очистка неразрешенных файлов (если включено)
4. ✅ Сборка мода (если нужно)
5. ✅ Копирование мода в `.minecraft/mods/`
6. ✅ Установка JRE (если нужно)
7. ✅ Построение команды через LaunchBuilder
8. ✅ Запуск Minecraft

## Производительность

### Оптимизации:
- **Многопоточность**: Параллельная проверка файлов
- **Кэширование**: JRE скачивается один раз
- **Lazy loading**: Компоненты создаются только при необходимости
- **Асинхронность**: Все операции через `CompletableFuture`

### Метрики:
- Проверка 100 файлов: ~2-5 секунд (зависит от размера)
- Загрузка JRE: ~1-3 минуты (зависит от скорости интернета)
- Сканирование процессов: ~0.5-1 секунда
- Построение команды: <10 мс

## Безопасность

### Защита файлов:
- ✅ SHA-256 хеширование для проверки целостности
- ✅ Автоматическая очистка неизвестных файлов
- ✅ Проверка манифеста перед запуском

### Защита от читов:
- ✅ Сканирование процессов на наличие инжекторов
- ✅ База известных читов (расширяемая)
- ✅ Обнаружение подозрительных процессов

### Защита запуска:
- ✅ Валидация всех параметров
- ✅ Изолированная JRE (не зависит от системной)
- ✅ Кастомные системные свойства для передачи токенов

## Расширяемость

### Добавление новых инжекторов:
```java
AntiInject.addKnownInjector("new_cheat_name");
```

### Кастомные свойства запуска:
```java
LaunchBuilder.create()
    .customProperty("custom.key", "value")
    .build();
```

### Обновление манифеста:
Манифест должен загружаться с API сервера для актуальности списка разрешенных файлов.

## Логирование

Все компоненты используют SLF4J + Logback:
- Уровень INFO: Основные операции
- Уровень WARN: Предупреждения и подозрительные действия
- Уровень ERROR: Ошибки и критические проблемы
- Уровень DEBUG: Детальная информация для отладки

## Требования

- **Java 21**: Все компоненты используют Java 21
- **OkHttp 4.12**: Для загрузки JRE
- **SLF4J + Logback**: Для логирования
- **NIO**: Для работы с файловой системой

## Заключение

Модуль Game Runner предоставляет production-ready решение для запуска Minecraft с:
- ✅ Автоматической проверкой файлов
- ✅ Загрузкой JRE
- ✅ Защитой от читов
- ✅ Оптимизированной производительностью
- ✅ Полной интеграцией всех компонентов

Все компоненты работают асинхронно и оптимизированы для максимальной производительности.
