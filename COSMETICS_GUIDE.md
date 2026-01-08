# Руководство по добавлению косметики

## Обзор

Система косметики позволяет добавлять различные визуальные элементы (кепки, плащи, очки, значки и т.д.) для персонажей игроков.

## Структура косметики

Каждый элемент косметики представлен объектом JSON со следующими полями:

```json
{
    "id": "unique_id",
    "name": "Название косметики",
    "type": "тип_косметики",
    "icon": "URL_или_data_URI_иконки",
    "description": "Описание косметики"
}
```

### Поля:

- **id** (обязательно): Уникальный идентификатор косметики (например: `cap_blue`, `cape_red`)
- **name** (обязательно): Отображаемое название косметики
- **type** (обязательно): Тип косметики:
  - `hat` - кепки, шляпы
  - `cape` - плащи
  - `accessory` - аксессуары (очки, маски и т.д.)
  - `badge` - значки
  - `other` - прочее
- **icon** (обязательно): URL или data URI иконки (64x64 пикселей рекомендуется)
- **description** (опционально): Описание косметики

## Способы добавления косметики

### Способ 1: Через API сервер (рекомендуется)

1. Откройте файл `api-server/server.js`
2. Найдите функцию `getDefaultCosmetics()`
3. Добавьте новый элемент в массив:

```javascript
function getDefaultCosmetics() {
    return [
        // ... существующая косметика ...
        {
            id: 'my_new_cosmetic',
            name: 'Моя новая косметика',
            type: 'hat',
            icon: 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="64" height="64"><rect fill="%23FF0000" width="64" height="64"/></svg>',
            description: 'Описание моей косметики'
        }
    ];
}
```

### Способ 2: Через базу данных

Косметика хранится в таблице `users` в колонке `cosmetics` в формате JSON.

1. Откройте базу данных SQLite: `api-server/users.db`
2. Найдите пользователя в таблице `users`
3. Обновите колонку `cosmetics` с JSON массивом:

```sql
UPDATE users 
SET cosmetics = '[
    {
        "id": "my_cosmetic",
        "name": "Моя косметика",
        "type": "hat",
        "icon": "data:image/svg+xml,...",
        "description": "Описание"
    }
]'
WHERE username = 'имя_пользователя';
```

### Способ 3: Через API endpoint

Используйте POST запрос к `/api/user/cosmetics/add`:

```javascript
POST http://localhost:3000/api/user/cosmetics/add
Content-Type: application/json

{
    "username": "имя_пользователя",
    "cosmetic": {
        "id": "new_cosmetic",
        "name": "Новая косметика",
        "type": "hat",
        "icon": "data:image/svg+xml,...",
        "description": "Описание"
    }
}
```

## Создание иконок

### Вариант 1: SVG Data URI (рекомендуется для простых иконок)

```javascript
icon: 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="64" height="64"><rect fill="%23FF0000" width="64" height="64"/></svg>'
```

**Важно:** В data URI нужно URL-кодировать специальные символы:
- `#` → `%23`
- `<` → `%3C`
- `>` → `%3E`
- `"` → `%22`

### Вариант 2: Внешний URL

```javascript
icon: 'https://example.com/cosmetic-icon.png'
```

### Вариант 3: Локальный файл (требует настройки сервера)

```javascript
icon: '/static/cosmetics/my-cosmetic.png'
```

## Примеры косметики

### Кепка

```json
{
    "id": "cap_blue",
    "name": "Синяя кепка",
    "type": "hat",
    "icon": "data:image/svg+xml,<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"64\" height=\"64\"><rect fill=\"%230000FF\" width=\"64\" height=\"20\"/><rect y=\"20\" fill=\"%23FFFFFF\" width=\"64\" height=\"44\"/></svg>",
    "description": "Стильная синяя кепка"
}
```

### Плащ

```json
{
    "id": "cape_red",
    "name": "Красный плащ",
    "type": "cape",
    "icon": "data:image/svg+xml,<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"64\" height=\"64\"><rect fill=\"%23FF0000\" width=\"64\" height=\"64\"/></svg>",
    "description": "Элегантный красный плащ"
}
```

### Аксессуар

```json
{
    "id": "glasses_sunglasses",
    "name": "Солнечные очки",
    "type": "accessory",
    "icon": "data:image/svg+xml,<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"64\" height=\"64\"><rect fill=\"%23000000\" width=\"64\" height=\"20\" y=\"22\"/><rect fill=\"%23FFFFFF\" width=\"20\" height=\"20\" x=\"10\" y=\"22\"/><rect fill=\"%23FFFFFF\" width=\"20\" height=\"20\" x=\"34\" y=\"22\"/></svg>",
    "description": "Крутые солнечные очки"
}
```

## Применение косметики

В данный момент косметика отображается в разделе "Гардероб", но логика применения косметики к персонажу должна быть реализована на сервере Minecraft.

## Примечания

- Иконки должны быть размером 64x64 пикселей для лучшего отображения
- ID косметики должен быть уникальным
- Тип косметики используется для категоризации и фильтрации
- Базовая косметика автоматически добавляется новым пользователям при первом запросе

## Расширение функционала

Для добавления новых типов косметики:

1. Добавьте новый тип в список допустимых типов
2. Обновите функцию `displayCosmetics()` в `launcher/script.js` для поддержки нового типа
3. При необходимости обновите серверную логику для применения косметики








