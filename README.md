# 🎮 Horizon Minecraft Launcher

Профессиональный лаунчер для Minecraft сервера Horizon с поддержкой Telegram 2FA, магазина косметики, форума и админ-панели.

## 📋 Описание

Horizon Launcher - это полнофункциональный лаунчер, построенный на JavaFX, который предоставляет:

- 🔐 **Безопасная авторизация** с поддержкой Telegram 2FA
- 🛍️ **Магазин косметики** - покупка и применение скинов, накидок и моделей
- 💬 **Форум** - обсуждения, темы, посты и лайки
- 👨‍💼 **Админ-панель** - управление пользователями, магазином и форумом
- 🎨 **Автоматический инжект модов** - косметика автоматически подключается при запуске
- 🔒 **HWID защита** - привязка аккаунтов к уникальному идентификатору устройства
- 🔑 **Шифрованное хранение токенов** - безопасное хранение данных сессии

## 🧭 Актуальный фокус проекта

- **Основной клиент (source of truth):** `launcher-java/` (JavaFX).
- **Legacy клиент:** `launcher/` и корневой Electron-конфиг оставлены для совместимости/истории и не являются основной веткой разработки.
- **Текущий рабочий план и статусы задач:** `PROJECT_BACKLOG.md`.

## 🚀 Быстрый старт

### Требования

- **Java 21+** (JDK)
- **JavaFX 21+**
- **Node.js 18+** (для API сервера)
- **Maven 3.8+** (для сборки Java лаунчера)

### Установка и запуск

#### 1. Клонирование репозитория

```bash
git clone https://github.com/your-repo/horizon-launcher.git
cd horizon-launcher
```

#### 2. Bootstrap (рекомендуется)

```powershell
powershell -ExecutionPolicy Bypass -File scripts/bootstrap.ps1
# или
npm run setup
```

#### 3. Настройка API сервера

```bash
cd api-server
npm install

# Создайте файл .env на основе .env.example
cp .env.example .env

# Отредактируйте .env и укажите:
# - TELEGRAM_BOT_TOKEN (получите у @BotFather)
# - ADMIN_TOKEN (для админ-панели)
# - JWT_SECRET (секретный ключ для JWT)
```

#### 4. Запуск API сервера

```bash
npm start
# или
node server.js
```

API сервер запустится на `http://localhost:3000`

#### 5. Сборка Java лаунчера

```bash
cd launcher-java
mvn clean package
```

После сборки JAR файл будет находиться в `target/horizon-launcher-1.0.0.jar`

#### 6. Запуск лаунчера

```bash
# Через Maven (для разработки)
mvn javafx:run

# Через собранный JAR
java -jar target/horizon-launcher-1.0.0.jar
```

**Примечание:** Перед запуском убедитесь, что API сервер запущен на `http://localhost:3000`

## ⚡ Запуск всего проекта и мгновенная проверка

### 1. Полная проверка проекта одной командой

```powershell
npm run verify
```

Что проверяется:
- API smoke (auth + cosmetics + payment + forum)
- сборка admin-panel
- компиляция launcher-java

### 2. Запуск всех сервисов для разработки

```powershell
npm run dev:all
```

Команда поднимет отдельные процессы:
- `api-server`
- `admin-panel` (Vite dev server)
- `launcher-java` (JavaFX)

Остановка всех поднятых процессов:

```powershell
npm run dev:stop
```

## 📁 Структура проекта

```
Horizon/
├── api-server/          # Node.js/Express API сервер
│   ├── server.js        # Главный файл сервера
│   ├── package.json     # Зависимости Node.js
│   └── uploads/         # Загруженные файлы (косметика)
│
├── launcher-java/       # JavaFX лаунчер
│   ├── src/main/java/   # Исходный код Java
│   ├── pom.xml          # Maven конфигурация
│   └── target/          # Скомпилированные файлы
│
├── cosmetics-mod/       # Minecraft мод для косметики
│   └── src/             # Исходный код мода
│
└── admin-panel/         # Веб-админ панель
    ├── index.html
    └── admin.js
```

## 🔧 Конфигурация

### API Сервер

Создайте файл `api-server/.env`:

```env
PORT=3000
ADMIN_TOKEN=your_admin_token_here
JWT_SECRET=your_jwt_secret_key_here
TELEGRAM_BOT_TOKEN=your_telegram_bot_token_here
```

### Java Лаунчер

Конфигурация находится в `launcher-java/src/main/resources/config.properties`:

```properties
api.server.url=http://localhost:3000
telegram.bot.url=https://t.me/your_bot_username
api.timeout.connect=10
api.timeout.read=10
minecraft.default.memory=4096
```

## 📚 Основные компоненты

### 🔐 Система авторизации

- **Регистрация** - создание нового аккаунта
- **Вход** - авторизация с паролем
- **Telegram 2FA** - двухфакторная аутентификация через Telegram
- **Восстановление пароля** - сброс пароля через Telegram код
- **Автоматический вход** - сохранение сессии с шифрованием

### 🛍️ Магазин косметики

- Покупка и применение скинов
- Управление накидками (capes)
- Загрузка кастомных моделей Blockbench
- Предпросмотр косметики в лаунчере

### 💬 Форум

- Категории тем
- Создание тем и постов
- Лайки и просмотры
- Модерация контента

### 👨‍💼 Админ-панель

- Управление пользователями
- Бан/разбан по HWID
- Управление магазином
- Модерация форума

## 🔒 Безопасность

- **HWID защита** - привязка аккаунтов к уникальному идентификатору устройства
- **Шифрование токенов** - AES-256 шифрование с использованием HWID как ключа
- **Валидация данных** - проверка всех входных данных на сервере
- **Telegram 2FA** - дополнительный уровень безопасности
- **Rate limiting** - защита от злоупотреблений

## 🛠️ Разработка

### Единая проверка проекта

```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify.ps1
```

### Запуск в режиме разработки

#### API сервер
```bash
cd api-server
npm install
node server.js
```

#### Java лаунчер
```bash
cd launcher-java
mvn clean compile
mvn javafx:run
```

### Тестирование

```bash
# Тестирование API сервера
cd api-server
npm test

# Компиляция Java лаунчера
cd launcher-java
mvn clean test
```

## 📝 API Документация

### Авторизация

- `POST /api/auth/register` - Регистрация
- `POST /api/auth/login` - Вход
- `POST /api/auth/verify-2fa` - Проверка 2FA кода
- `POST /api/auth/recovery/request` - Запрос кода восстановления
- `POST /api/auth/recovery/reset` - Сброс пароля
- `POST /api/auth/verify` - Проверка токена

### Магазин

- `GET /api/store/items` - Получить все предметы
- `POST /api/store/purchase` - Купить предмет
- `GET /api/store/my-items` - Мои покупки

### Форум

- `GET /api/forum/categories` - Категории
- `GET /api/forum/topics/:categoryId` - Темы категории
- `POST /api/forum/topics` - Создать тему
- `POST /api/forum/posts` - Создать пост

## 🤝 Вклад в проект

1. Fork проекта
2. Создайте ветку для функции (`git checkout -b feature/AmazingFeature`)
3. Зафиксируйте изменения (`git commit -m 'Add some AmazingFeature'`)
4. Запушьте в ветку (`git push origin feature/AmazingFeature`)
5. Откройте Pull Request

## 📄 Лицензия

Этот проект распространяется под лицензией ISC.

## 👥 Авторы

- **Isya** - Основной разработчик

## 🙏 Благодарности

- JavaFX Community
- Minecraft Launcher Core
- Node.js и Express.js сообщества

---

**Версия:** 1.0.0  
**Дата обновления:** 2026-01-11
