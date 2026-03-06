# Руководство по системе 3D-косметики Horizon

## Обзор

Система 3D-косметики состоит из двух основных компонентов:
1. **Лаунчер (JavaFX)** - для предварительного просмотра косметики
2. **Игровой мод (Fabric 1.21-1.21.10)** - для отображения косметики в игре

## Структура проекта

### Лаунчер (`launcher-java/`)
```
src/main/java/com/horizon/launcher/
├── models/
│   ├── ModelData.java              # Класс данных модели
│   └── BlockbenchModelParser.java  # Парсер JSON моделей
└── ui/
    └── PlayerModelView.java        # 3D визуализация модели игрока
```

### Игровой мод (`cosmetics-mod/`)
```
src/main/java/com/horizon/cosmetics/
├── common/
│   └── ModelData.java              # Общий класс (идентичен версии лаунчера)
├── client/
│   ├── CosmeticManager.java        # Менеджер загрузки и кеширования
│   └── renderer/
│       └── PlayerCosmeticRenderer.java  # Рендерер косметики
└── mixin/
    └── PlayerEntityRendererMixin.java   # Mixin для рендеринга игрока
```

## Основные компоненты

### 1. BlockbenchModelParser
Парсер для моделей Blockbench в формате Bedrock Geometry (.json).

**Использование:**
```java
// Загрузка модели из файла
ModelData model = BlockbenchModelParser.parseFromFile(new File("model.json"));

// Загрузка модели из пути
ModelData model = BlockbenchModelParser.parseFromPath(Paths.get("model.json"));

// Валидация модели
boolean isValid = BlockbenchModelParser.validate(model);
```

### 2. PlayerModelView (JavaFX)
3D визуализация модели игрока для лаунчера.

**Использование:**
```java
// Создание вида модели
Image skinImage = new Image("skin.png");
PlayerModelView playerView = new PlayerModelView(400, 600, skinImage);

// Прикрепление аксессуара
ModelData accessoryModel = BlockbenchModelParser.parseFromFile(new File("hat.json"));
Image accessoryTexture = new Image("hat_texture.png");
Group accessory = playerView.attachAccessory("head", accessoryModel, accessoryTexture);

// Удаление аксессуара
playerView.removeAccessory("head");

// Управление вращением
playerView.startRotation();  // Автоматическое вращение
playerView.setRotation(45.0); // Установка угла вручную
```

### 3. CosmeticManager (Fabric Mod)
Менеджер для асинхронной загрузки и кеширования моделей и текстур.

**Использование:**
```java
CosmeticManager manager = CosmeticManager.getInstance();

// Асинхронная загрузка модели
CompletableFuture<ModelData> modelFuture = manager.loadModelAsync("cosmetic_id");
modelFuture.thenAccept(model -> {
    // Использование модели
});

// Асинхронная загрузка текстуры
CompletableFuture<Identifier> textureFuture = manager.loadTextureAsync("cosmetic_id");
textureFuture.thenAccept(texture -> {
    // Использование текстуры
});

// Получение косметики игрока
UUID playerUuid = player.getUuid();
CompletableFuture<Map<String, String>> cosmeticsFuture = manager.getPlayerCosmetics(playerUuid);
cosmeticsFuture.thenAccept(cosmetics -> {
    // cosmetics: Map<boneName, cosmeticId>
});
```

### 4. PlayerCosmeticRenderer (Fabric Mod)
Рендерер для отрисовки косметики в игре.

**Особенности:**
- Использует `MatrixStack` (PoseStack в 1.21+) для трансформации
- Использует стандартный шейдер `RenderLayer.getEntityCutout()`
- Поддерживает все основные кости: head, body, left_arm, right_arm, left_leg, right_leg

### 5. PlayerEntityRendererMixin
Mixin для интеграции рендеринга косметики в стандартный рендерер игрока.

**Точки инжекции:**
- После отрисовки модели игрока
- После отрисовки всех слоев

## Формат моделей

Модели должны быть в формате Bedrock Geometry, используемом BlockBench для Java-модов:

```json
{
  "format_version": "1.12.0",
  "minecraft:geometry": [
    {
      "description": {
        "identifier": "geometry.custom_cosmetic"
      },
      "bones": [
        {
          "name": "bone_name",
          "parent": "parent_bone",
          "pivot": [0, 0, 0],
          "rotation": [0, 0, 0],
          "cubes": [
            {
              "origin": [-8, 0, -8],
              "size": [16, 16, 16],
              "uv": [0, 0]
            }
          ]
        }
      ]
    }
  ]
}
```

## Поддерживаемые кости

- `head` - Голова
- `body` - Тело
- `left_arm` - Левая рука
- `right_arm` - Правая рука
- `left_leg` - Левая нога
- `right_leg` - Правая нога

## Особенности версии 1.21-1.21.10

1. **Использование MatrixStack вместо старых названий**:
   - `MatrixStack` вместо `PoseStack` (в некоторых версиях)
   - Современные маппинги Yarn/Mojang

2. **Поддержка высокодетализированных скинов**:
   - Слои скина (base layer, overlay layer)
   - Прозрачность и альфа-канал

3. **Современные методы рендеринга**:
   - `RenderLayer.getEntityCutout()` для стандартного шейдера
   - Вершинные консьюмеры через `VertexConsumerProvider`
   - Поддержка освещения через параметр `light`

## Кеширование

Все модели и текстуры кешируются в:
- `~/.horizon/assets/models/` - модели (.json)
- `~/.horizon/assets/textures/` - текстуры (.png)

## API интеграция

Мод загружает косметику с API сервера:
- `GET /api/cosmetics/{cosmeticId}/model` - получение модели
- `GET /api/cosmetics/{cosmeticId}/texture` - получение текстуры
- `GET /api/cosmetics/player/{playerUuid}` - получение косметики игрока

## Примечания

1. **Модульность**: Логика парсинга моделей вынесена в общие классы (`ModelData`, `BlockbenchModelParser`), которые должны быть идентичны в лаунчере и моде.

2. **Асинхронность**: Все загрузки выполняются асинхронно через `CompletableFuture` для предотвращения блокировки основного потока.

3. **Производительность**: Используется кеширование моделей и текстур для минимизации повторных загрузок.

4. **Совместимость**: Код написан с учетом специфики Minecraft 1.21-1.21.10 и использует современные API.
