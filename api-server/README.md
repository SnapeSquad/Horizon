# Horizon API Server

API сервер для Horizon Launcher на Node.js + Express.

## 🚀 Быстрый старт

### Требования
- Node.js 16+ 
- npm или yarn

### Установка и запуск

```bash
# Установка зависимостей (первый раз)
npm install

# Запуск сервера
node server.js
```

Сервер запустится на `http://localhost:3000`

### Настройка

1. Создайте файл `.env` в папке `api-server`:
```env
TELEGRAM_BOT_TOKEN=ваш_токен_бота_telegram
```

2. Если токен не указан, Telegram бот не будет работать, но остальные функции доступны.

## 📋 API Endpoints

### Авторизация
- `POST /api/auth/register` - Регистрация
- `POST /api/auth/login` - Вход
- `GET /api/auth/status` - Статус авторизации

### Сервер Minecraft
- `GET /api/server/status` - Статус сервера

### Косметика
- `GET /api/cosmetics` - Список косметики
- `POST /api/cosmetics/add` - Добавить косметику

### Скины
- `GET /api/skin/:username` - Получить скин
- `POST /api/skin/upload` - Загрузить скин

### Форум
- `GET /api/forum/categories` - Категории форума
- `GET /api/forum/topics/:categoryId` - Темы в категории
- `GET /api/forum/posts/:topicId` - Сообщения в теме

### Валюта
- `GET /api/currency` - Баланс валюты
- `POST /api/currency/add` - Добавить валюту

## 🗄️ База данных

Используется SQLite. База данных создается автоматически в файле `users.db`

### Схема базы данных

Основные таблицы:
- `users` - пользователи
- `forum_categories` - категории форума
- `forum_topics` - темы форума
- `forum_posts` - сообщения форума

Подробная схема в файле `forum-schema.sql`

## 🔧 Разработка

### Запуск в режиме разработки
```bash
node server.js
```

### Просмотр логов
Логи выводятся в консоль. Для production используйте PM2 или аналоги.

### Переменные окружения

Создайте `.env` файл:
```env
TELEGRAM_BOT_TOKEN=ваш_токен
PORT=3000
```

## 📝 Особенности

- ✅ SQLite база данных (легковесная, не требует установки)
- ✅ Telegram 2FA авторизация
- ✅ Интеграция с Minecraft сервером
- ✅ Система форума
- ✅ Косметика и скины
- ✅ Донат валюта

## 🐛 Решение проблем

**Ошибка: "Cannot find module"**
- Выполните `npm install`

**Ошибка: "Port 3000 already in use"**
- Измените PORT в `.env` или остановите другой процесс на порту 3000

**Telegram бот не работает**
- Проверьте токен в `.env`
- Убедитесь, что бот создан через @BotFather

## 📞 Поддержка

Логи выводятся в консоль. При проблемах проверьте вывод сервера.

