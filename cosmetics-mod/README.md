# Horizon Cosmetics Mod

Forge мод для отображения косметики из Horizon Launcher в Minecraft.

## Возможности:
- 🪽 Крылья (различные типы)
- 🦸 Плащи
- ✨ Эффекты частиц
- 🔄 Автоматическая синхронизация с API каждые 30 секунд

## Сборка:
```bash
cd cosmetics-mod
gradlew build
```

Скомпилированный мод будет в `build/libs/horizoncosmetics-1.0.0.jar`

## Установка:
1. Скопируйте `.jar` в папку `mods` Minecraft
2. Запустите игру через Forge
3. Косметика автоматически загрузится из API

## API Endpoints:
- `GET /api/cosmetics/mods` - Получить список косметики
- `GET /api/cosmetics/animations` - Получить анимации

## Типы косметики:
### Крылья:
- dragon (огненные крылья дракона)
- angel (крылья ангела)
- demon (демонические крылья)

### Частицы:
- stars (звезды)
- flames (пламя)
- sparkles (блестки)

### Плащи:
- custom (кастомные текстуры)

