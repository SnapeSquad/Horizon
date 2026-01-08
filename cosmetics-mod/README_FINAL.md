# Horizon Cosmetics Mod - ФИНАЛЬНАЯ ВЕРСИЯ

## ✅ Полностью реализовано:

### Функционал:
- 🪽 **Рендеринг крыльев** (Dragon, Angel, Demon wings)
- 🦸 **Плащи** (с кастомными текстурами)
- ✨ **Эффекты частиц** (Stars, Flames, Sparkles)
- 🔄 **Автосинхронизация** с API каждые 30 секунд
- 🎭 **Анимации** (взмахи крыльев, полет)

### Модели:
- `WingsModel.java` - Модели крыльев (Dragon/Angel)
- Анимация взмахов крыльев
- Динамическое масштабирование

### API Интеграция:
- Загрузка косметики из `http://localhost:3000/api/cosmetics/mods`
- Загрузка анимаций из `http://localhost:3000/api/cosmetics/animations`
- Кэширование данных

### Поддерживаемые типы:
#### Крылья:
- `dragon` - Огненные крылья дракона (20x16 блоков)
- `angel` - Крылья ангела (18x20 блоков)
- `demon` - Демонические крылья

#### Частицы:
- `stars` - Звезды (END_ROD particles)
- `flames` - Пламя (FLAME particles)
- `sparkles` - Блестки (GLOW particles)

#### Плащи:
- Custom textures support
- Движение на ветру

## 📦 Сборка:

```bash
cd cosmetics-mod
gradlew build
```

Готовый мод: `build/libs/horizoncosmetics-1.0.0.jar`

## 🎮 Установка:

1. Скопировать `horizoncosmetics-1.0.0.jar` в папку `mods`
2. Убедиться что установлен **Forge 1.20.1-47.2.0**
3. Запустить игру
4. Косметика загрузится автоматически из API

## ⚙️ Конфигурация:

В API сервере (`api-server/server.js`) добавлены endpoints:
- `GET /api/cosmetics/mods` - Получить список косметики
- `GET /api/cosmetics/animations` - Получить анимации

## 🔧 Требования:

- **Minecraft**: 1.20.1
- **Forge**: 47.2.0+
- **Java**: 17+
- **API Server**: Running on localhost:3000

## 📝 Примечания:

- Мод автоматически синхронизируется с сервером каждые 30 секунд
- Косметика применяется только если куплена в магазине
- Все данные хранятся в БД (`cosmetic_mods`, `cosmetic_animations`)
- Частицы спавнятся каждые 5 тиков для оптимизации

## 🎨 Добавление новой косметики:

```sql
INSERT INTO cosmetic_mods (name, version, description, author, file_path) 
VALUES ('Phoenix Wings', '1.0', 'Огненные крылья феникса', 'Admin', '/mods/phoenix_wings.jar');
```

## ✅ ГОТОВО К ИСПОЛЬЗОВАНИЮ!

