# Horizon Cosmetics Mod (Fabric 1.21-1.21.10)

Мод для Minecraft Java Edition, который добавляет систему 3D-косметики.

## Структура проекта

```
cosmetics-mod/
├── src/main/java/com/horizon/cosmetics/
│   ├── client/
│   │   ├── CosmeticManager.java       # Менеджер косметики
│   │   └── renderer/
│   │       └── PlayerCosmeticRenderer.java  # Рендерер косметики
│   ├── mixin/
│   │   └── PlayerEntityRendererMixin.java   # Mixin для рендеринга игрока
│   └── common/
│       ├── ModelData.java             # Общие классы моделей
│       └── BlockbenchModelParser.java # Парсер моделей (общий с лаунчером)
├── src/main/resources/
│   ├── fabric.mod.json                # Конфигурация мода
│   └── horizon.cosmetics.mixins.json  # Конфигурация Mixin
└── build.gradle                       # Gradle конфигурация
```

## Установка

1. Установите Fabric Loader для Minecraft 1.21.x
2. Установите Fabric API
3. Поместите мод в папку `mods`
4. Запустите игру

## Использование

Косметика автоматически загружается с API сервера и отображается на игроках.
