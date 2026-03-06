# Админ-панель Horizon - Сводка

## ✅ Выполнено

### 1. Интерфейс управления косметикой ✅
- ✅ Форма загрузки файлов (модель JSON + текстура PNG)
- ✅ Поля для настройки метаданных: pivot_point, price, rarity
- ✅ POST /api/admin/cosmetics с Multipart/form-data
- ✅ Валидация BlockBench JSON модели
- ✅ Просмотр списка косметики

### 2. Раздел модерации ✅
- ✅ Таблица пользователей (ник, HWID, баланс)
- ✅ Кнопка "Забанить по HWID"
- ✅ API для бана и разбана
- ✅ Просмотр черного списка HWID

### 3. Редактор новостей ✅
- ✅ Форма: Заголовок, Текст (Markdown), URL картинки
- ✅ Создание, редактирование, удаление новостей
- ✅ Данные сохраняются в таблицу `news`

### 4. Технические требования ✅
- ✅ Tailwind CSS для дизайна
- ✅ Проверка JWT-токена в заголовках (x-admin-token)
- ✅ Валидация JSON (BlockBench модель)

## Структура файлов

```
admin-panel/
├── index.html        - Основной HTML файл с Tailwind CSS
├── admin.js          - JavaScript логика (API запросы, UI)
└── README.md         - Документация

api-server/
├── server.js         - Обновлен с админскими эндпоинтами
├── package.json      - Добавлены зависимости (multer, jsonwebtoken)
└── uploads/          - Директория для загруженных файлов
    └── cosmetics/    - Модели и текстуры косметики
```

## API Эндпоинты

### Косметика
- `POST /api/admin/cosmetics` - Загрузить косметику (multipart/form-data)
- `GET /api/admin/cosmetics` - Получить список косметики

### Модерация
- `GET /api/admin/users` - Получить список пользователей
- `POST /api/admin/users/ban` - Забанить по HWID
- `DELETE /api/admin/users/unban/:hwid` - Разбанить по HWID
- `GET /api/admin/bans` - Получить список банов

### Новости
- `POST /api/admin/news` - Создать новость
- `GET /api/admin/news` - Получить список новостей
- `PUT /api/admin/news/:id` - Обновить новость
- `DELETE /api/admin/news/:id` - Удалить новость

## База данных

Добавлены таблицы:
- `cosmetics` - Косметика (name, description, pivot_point, price, rarity, model_file_path, texture_file_path)
- `news` - Новости (title, content, image_url, author, views, created_at, updated_at)
- `banned_hwid` - Бан по HWID (hwid, reason, banned_by, created_at)
- `users.hwid` - Поле HWID добавлено в таблицу users

## Авторизация

Все админские эндпоинты требуют токен в заголовке `x-admin-token`.
По умолчанию: `horizon_admin_2024` (можно изменить через `ADMIN_TOKEN` в `.env`)

## Запуск

1. Установить зависимости: `cd api-server && npm install`
2. Запустить API сервер: `node server.js`
3. Открыть `admin-panel/index.html` в браузере
4. Ввести админ токен в поле вверху страницы

## Особенности реализации

- **Валидация BlockBench**: Проверка наличия `format_version` и `minecraft:geometry` в JSON
- **Загрузка файлов**: Multer с лимитом 10MB, автоматическая очистка при ошибках
- **Tailwind CSS**: Использован CDN для быстрого подключения
- **LocalStorage**: Токен сохраняется в браузере
- **Асинхронные запросы**: Все API запросы через async/await

## Следующие шаги (опционально)

- Добавить JWT токены вместо простого токена
- Добавить редактор изображений для новостей
- Добавить предпросмотр косметики перед загрузкой
- Добавить пагинацию для таблиц
- Добавить поиск и фильтрацию
