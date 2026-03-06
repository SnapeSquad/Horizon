# 🌐 Horizon API Server

Backend API сервер для Horizon Minecraft Launcher.

## 🚀 Быстрый старт

### Установка

```bash
npm install
```

### Настройка

Создайте файл `.env` на основе `.env.example`:

```bash
cp .env.example .env
```

Отредактируйте `.env`:

```env
PORT=3000
ADMIN_TOKEN=your_secure_admin_token_here
JWT_SECRET=your_jwt_secret_key_here
TELEGRAM_BOT_TOKEN=your_telegram_bot_token_from_botfather
```

### Запуск

```bash
# Development
npm start

# Production (с PM2)
pm2 start server.js --name horizon-api
```

## 📡 API Эндпоинты

### Авторизация

- `POST /api/auth/register` - Регистрация нового пользователя
- `POST /api/auth/login` - Вход в систему
- `POST /api/auth/verify-2fa` - Проверка 2FA кода
- `POST /api/auth/recovery/request` - Запрос кода восстановления пароля
- `POST /api/auth/recovery/reset` - Сброс пароля с кодом
- `POST /api/auth/verify` - Проверка валидности токена

### Магазин

- `GET /api/cosmetics/available` - Получить доступную косметику
- `GET /api/user/currency?username=...` - Получить баланс
- `POST /api/user/currency/purchase` - Купить косметику за валюту
- `POST /api/payment/generate` - Сгенерировать ссылку пополнения

### Форум

- `GET /api/forum/categories` - Получить категории
- `GET /api/forum/topics?category_id=...` - Получить темы категории
- `POST /api/forum/topics` - Создать новую тему
- `GET /api/forum/posts?topic_id=...&username=...` - Получить посты темы
- `POST /api/forum/posts` - Создать новый пост
- `POST /api/forum/posts/like` - Лайкнуть/снять лайк с поста

### Админ (требуется токен)

- `GET /api/admin/users` - Получить всех пользователей
- `POST /api/admin/users/ban` - Забанить HWID
- `DELETE /api/admin/users/unban/:hwid` - Разбанить HWID
- `POST /api/admin/currency/give` - Выдать валюту
- `GET /api/admin/news` / `POST /api/admin/news` - Управление новостями
- `GET /api/admin/cosmetics` / `POST /api/admin/cosmetics` - Управление косметикой

## 🔒 Безопасность

### HWID Защита

- Все запросы проверяют HWID на наличие бана
- HWID привязывается к аккаунту при регистрации/входе

### Telegram 2FA

- Двухфакторная аутентификация через Telegram бота
- Коды для входа и восстановления пароля отправляются в Telegram

### JWT Токены

- Токены подписываются секретным ключом
- Срок действия токена: 7 дней
- Токены проверяются при каждом запросе

## 📊 База данных

Используется SQLite3. База данных создается автоматически при первом запуске.

### Таблицы

- `users` - Пользователи
- `forum_categories` - Категории форума
- `forum_topics` - Темы форума
- `forum_posts` - Посты
- `forum_likes` - Лайки постов
- `banned_hwid` - Забаненные HWID
- `cosmetics` - Загруженные косметические предметы
- `news` - Новости лаунчера

## 🤖 Telegram Bot

Для работы 2FA и восстановления пароля требуется настроить Telegram бота:

1. Создайте бота через [@BotFather](https://t.me/botfather)
2. Получите токен бота
3. Добавьте токен в `.env` как `TELEGRAM_BOT_TOKEN`
4. Перезапустите сервер

## 📁 Структура

```
api-server/
├── server.js          # Главный файл сервера
├── package.json       # Зависимости
├── .env              # Локальная конфигурация (не хранить в git)
├── .env.example      # Пример конфигурации
├── users.db          # База данных SQLite (создается автоматически)
└── uploads/          # Загруженные файлы
    └── cosmetics/    # Косметика (модели и текстуры)
```

## 🛠️ Разработка

### Запуск в режиме разработки

```bash
# Базовый запуск
npm start

# Smoke-проверка API
npm test
```

### Логирование

Все важные события логируются в консоль с префиксами:
- `[REGISTER]` - Регистрация
- `[LOGIN]` - Вход
- `[TG MSG]` - Сообщения Telegram
- `[ERROR]` - Ошибки
- `[ADMIN]` - Действия админа

## 🐛 Обработка ошибок

Сервер имеет централизованную обработку ошибок:
- Ошибки валидации: 400
- Ошибки авторизации: 401
- Ошибки доступа: 403
- Не найдено: 404
- Внутренние ошибки: 500

## 📝 Лицензия

ISC
