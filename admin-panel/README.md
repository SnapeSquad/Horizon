# 🎮 Horizon Admin Panel

Веб-панель администратора для управления форумом и магазином Horizon Launcher.

## ⚡ Быстрый старт

### 1. Установка зависимостей

```bash
npm install
```

### 2. Запуск сервера

**Режим разработки с автоперезагрузкой:**
```bash
npm run dev
```

**Продакшн:**
```bash
npm start
```

Панель будет доступна по адресу: **http://localhost:3000**

## 🔐 Учетные данные

**Логин:** `admin`  
**Пароль:** `admin123`

## 🛠️ Возможности

### 📊 Dashboard (Панель управления)
- Общая статистика: пользователи, товары, темы форума
- Быстрые действия для управления
- Визуализация данных

### 🛒 Управление магазином
- Добавление/удаление товаров
- Редактирование цен и параметров
- Категории: скины, плащи, частицы, ранги
- Система редкости (COMMON, RARE, EPIC, LEGENDARY, FEATURED)
- Featured товары (в топе)

### 💬 Управление форумом
- Создание/удаление категорий
- Управление темами
- Закрепление тем (📌 pin)
- Блокировка тем (🔒 lock)
- Удаление тем

### 👥 Управление пользователями
- Просмотр всех пользователей
- Изменение баланса донат-валюты
- Бан/разбан пользователей
- Управление рангами

## 🎨 Технологии

- **Backend:** Node.js + Express
- **Frontend:** Vanilla JavaScript + HTML/CSS
- **Аутентификация:** JWT tokens
- **Стиль:** Figma дизайн с glassmorphism

## 📁 Структура проекта

```
admin-panel/
├── server.js           # Express сервер с API
├── package.json        # Зависимости
├── .env               # Конфигурация
└── public/            # Фронтенд
    ├── index.html     # Главная страница
    ├── styles.css     # Стили
    └── app.js         # JavaScript логика
```

## 🔒 Безопасность

⚠️ **ВАЖНО для продакшена:**

1. Измените `SECRET_KEY` в `.env` на уникальный ключ
2. Используйте настоящую базу данных (PostgreSQL, MongoDB)
3. Храните пароли в хешированном виде (bcrypt)
4. Настройте HTTPS
5. Добавьте rate limiting
6. Настройте CORS для нужных доменов

## 📡 API Endpoints

### Авторизация
- `POST /api/admin/login` - Вход в систему

### Магазин
- `GET /api/admin/shop/items` - Список товаров
- `POST /api/admin/shop/items` - Добавить товар
- `PUT /api/admin/shop/items/:id` - Редактировать товар
- `DELETE /api/admin/shop/items/:id` - Удалить товар

### Форум
- `GET /api/admin/forum/categories` - Категории
- `POST /api/admin/forum/categories` - Добавить категорию
- `DELETE /api/admin/forum/categories/:id` - Удалить категорию
- `GET /api/admin/forum/topics` - Темы
- `PUT /api/admin/forum/topics/:id/pin` - Закрепить тему
- `PUT /api/admin/forum/topics/:id/lock` - Заблокировать тему
- `DELETE /api/admin/forum/topics/:id` - Удалить тему

### Пользователи
- `GET /api/admin/users` - Список пользователей
- `PUT /api/admin/users/:username/balance` - Изменить баланс
- `PUT /api/admin/users/:username/ban` - Бан/разбан

### Статистика
- `GET /api/admin/stats` - Общая статистика

## 🚀 Развертывание

### Docker (рекомендуется)

```bash
docker build -t horizon-admin .
docker run -p 3000:3000 horizon-admin
```

### VPS/Dedicated Server

1. Установите Node.js 18+
2. Склонируйте проект
3. `npm install --production`
4. Настройте `.env`
5. Используйте PM2 для запуска:
   ```bash
   npm install -g pm2
   pm2 start server.js --name horizon-admin
   ```

## 📝 Лицензия

MIT License - используйте свободно!

## 💡 Поддержка

Если возникли вопросы, создайте issue в репозитории проекта.

---

**Сделано с ❤️ для Horizon Launcher**

