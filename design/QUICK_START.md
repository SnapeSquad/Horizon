# Быстрый старт: Подключение дизайна из Figma

## Шаг 1: Экспорт из Figma (5 минут)

1. **Откройте ваш дизайн в Figma**

2. **Экспортируйте иконки:**
   - Выберите иконку → Правый клик → Export
   - Формат: SVG
   - Сохраните в `design/assets/icons/`

3. **Экспортируйте изображения:**
   - Выберите изображение → Правый клик → Export
   - Формат: PNG @2x
   - Сохраните в `design/assets/images/`

4. **Скачайте шрифты:**
   - Определите используемые шрифты
   - Скачайте файлы (.ttf, .woff2)
   - Сохраните в `design/assets/fonts/`

## Шаг 2: Извлечение токенов (10 минут)

### Цвета
1. Откройте панель Design → Colors в Figma
2. Скопируйте HEX значения цветов
3. Откройте `design/tokens/colors.json`
4. Замените значения на ваши цвета

### Типографика
1. Выберите текстовые элементы в Figma
2. Запишите размеры, веса, межстрочные интервалы
3. Обновите `design/tokens/typography.json`

### Отступы
1. Измерьте отступы между элементами
2. Обновите `design/tokens/spacing.json`

## Шаг 3: Применение стилей (5 минут)

### Для Electron версии

1. **Подключите стили в `index.html`:**
```html
<link rel="stylesheet" href="design/styles/electron/figma-theme.css">
```

2. **Используйте классы:**
```html
<button class="btn btn-primary">Кнопка</button>
<div class="card">Карточка</div>
<input class="input" placeholder="Введите текст">
```

### Для JavaFX версии

1. **Скопируйте стили:**
```bash
# Скопируйте design/styles/javafx/figma-theme.css
# в launcher-java/src/main/resources/styles/
```

2. **Подключите в Java коде:**
```java
scene.getStylesheets().add(
    getClass().getResource("/styles/figma-theme.css").toExternalForm()
);
```

3. **Используйте классы в FXML:**
```xml
<Button styleClass="btn-primary" text="Кнопка" />
<Pane styleClass="card">...</Pane>
```

## Шаг 4: Проверка (5 минут)

1. Запустите Electron версию:
```bash
npm start
```

2. Запустите JavaFX версию:
```bash
cd launcher-java
./run.ps1
```

3. Проверьте, что стили применяются корректно

## Готово! 🎉

Теперь ваш дизайн из Figma подключен к проекту.

## Дополнительная помощь

- **Подробная инструкция**: См. `FIGMA_INTEGRATION.md`
- **Гайдлайны**: См. `guidelines/`
- **Примеры токенов**: См. `tokens/`

## Обновление дизайна

При обновлении дизайна в Figma:
1. Экспортируйте новые ресурсы
2. Обновите токены
3. Обновите стили
4. Проверьте обе версии

