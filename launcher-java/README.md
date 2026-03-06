# 🎮 Horizon Launcher (JavaFX)

Профессиональный JavaFX лаунчер для Minecraft сервера Horizon.

## 🚀 Быстрый старт

### Требования

- **Java 21+** (JDK)
- **JavaFX 21+**
- **Maven 3.8+**

### Сборка

```bash
mvn clean package
```

### Запуск

```bash
# Через Maven
mvn javafx:run

# Через собранный JAR
java -jar target/horizon-launcher-1.0.0.jar
```

## 📁 Структура проекта

```
launcher-java/
├── src/main/java/
│   ├── com/horizon/launcher/
│   │   ├── LauncherApplication.java    # Точка входа
│   │   ├── network/                    # Сетевые компоненты
│   │   │   └── ApiClient.java          # HTTP клиент
│   │   ├── services/                   # Сервисы
│   │   │   ├── AuthService.java        # Авторизация
│   │   │   ├── StoreService.java       # Магазин
│   │   │   └── ForumService.java       # Форум
│   │   ├── ui/                         # UI компоненты
│   │   │   ├── AuthWindow.java         # Окно авторизации
│   │   │   ├── MainWindow.java         # Главное окно
│   │   │   ├── AuthContainer.java      # Контейнер форм
│   │   │   └── components/             # UI компоненты
│   │   ├── utils/                      # Утилиты
│   │   │   ├── SessionManager.java     # Управление сессией
│   │   │   ├── CryptoHelper.java       # Шифрование
│   │   │   └── ConfigLoader.java       # Загрузка конфигурации
│   │   ├── minecraft/                  # Minecraft компоненты
│   │   │   ├── GameLauncher.java       # Запуск игры
│   │   │   └── LaunchBuilder.java      # Построение команды запуска
│   │   └── runtime/                    # Управление Java Runtime
│   └── resources/
│       ├── config.properties           # Конфигурация
│       └── styles.css                  # Стили
└── pom.xml                             # Maven конфигурация
```

## 🔧 Конфигурация

Конфигурация находится в `src/main/resources/config.properties`:

```properties
# API Server
api.server.url=http://localhost:3000
api.timeout.connect=10
api.timeout.read=10

# Telegram
telegram.bot.url=https://t.me/your_bot_username

# Minecraft
minecraft.default.memory=4096
minecraft.default.version=1.21
```

## 🎨 Особенности UI

### Dark Liquid Glass Design

- **Цветовая палитра:** Background `#14141e`, Surface `rgba(30, 30, 45, 0.6)` с blur 50px
- **Primary Gradient:** `#667eea` → `#764ba2` (Violet Glow)
- **Accent Color:** `#00f2fe` (Cyber Cyan) для активных элементов
- Прозрачные элементы с размытием (Glass Panels)
- Плавные анимации переходов
- Кастомные шрифты "Minecraft Unicode" для заголовков
- **Эффекты:**
  - Shimmering анимация для Owner/Curator ролей
  - Pulse glow для кнопок "Log In" и "PLAY"
  - Backdrop blur для glass panels
  - Hover эффекты для карточек

### Компоненты

- **AuthContainer** - Единый контейнер для логина, регистрации и восстановления
- **Toast** - Всплывающие уведомления
- **SplashScreen** - Экран загрузки при старте
- **AnimationHelper** - Утилиты для анимаций

## 🔒 Безопасность

### Шифрование токенов

- Токены шифруются AES-256
- HWID используется как часть ключа шифрования
- Данные привязаны к конкретному ПК

### HWID

- Уникальный идентификатор устройства
- Используется для защиты от взлома
- Привязка аккаунтов к устройству

## 📚 Основные компоненты

### AuthService

Управление авторизацией:
- Регистрация
- Вход
- 2FA проверка
- Восстановление пароля
- Автоматический вход

### ApiClient

HTTP клиент для работы с API:
- Асинхронные запросы
- Обработка ошибок
- Таймауты
- JSON сериализация

### SessionManager

Управление сессией:
- Сохранение токенов
- Шифрование данных
- Загрузка сессии при старте

## 🛠️ Разработка

### Сборка для разработки

```bash
mvn clean compile
```

### Запуск с отладкой

```bash
# Через IDE
# Запустите LauncherApplication.main()

# Через Maven с параметрами JVM
mvn javafx:run -Djavafx.debug=true
```

### Зависимости

Основные зависимости (см. `pom.xml`):
- JavaFX 21.0.2
- OkHttp 4.12.0
- Gson 2.10.1
- SLF4J + Logback

## 📝 Лицензия

ISC
