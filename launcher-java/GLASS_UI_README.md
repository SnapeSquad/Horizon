# Sky-Turquoise Glass UI

Современный UI для Horizon Launcher в стиле iOS 26 с эффектами Glassmorphism.

## 🎨 Особенности дизайна

- **Sky-Turquoise Theme**: Основные цвета #40E0D0 (Turquoise) и #87CEEB (Sky Blue)
- **Glassmorphism**: Прозрачные панели с эффектом размытого стекла
- **Frameless Window**: Прозрачное окно без рамки с возможностью перетаскивания
- **Elastic Animations**: Плавные spring-анимации при открытии
- **3D Depth**: Многослойные тени для эффекта глубины

## 🚀 Запуск нового UI

### Вариант 1: Через GlassLauncherApplication (новый UI)
```java
// В LauncherApplication.java измените main:
public static void main(String[] args) {
    // Запуск нового Glass UI
    Application.launch(GlassLauncherApplication.class, args);
}
```

### Вариант 2: Прямой запуск
```powershell
cd launcher-java
.\mvnw.cmd clean install
java -cp target/classes com.horizon.launcher.ui.glass.GlassLauncherApplication
```

## 📁 Структура файлов

```
launcher-java/src/main/
├── java/com/horizon/launcher/ui/glass/
│   ├── GlassLauncherApplication.java  # Главный класс
│   └── GlassMainController.java       # Контроллер MVC
├── resources/
│   ├── fxml/
│   │   └── MainView.fxml              # FXML разметка
│   ├── styles/
│   │   └── sky-turquoise-glass.css    # CSS тема
│   └── images/
│       └── nebula-background.png      # Фоновое изображение (опционально)
```

## 🎯 Основные компоненты

### GlassMainController
- Draggable window logic
- Elastic entrance animation
- Form validation
- Auth integration

### MainView.fxml
- Многослойная структура (StackPane)
- Layer 1: Background image/gradient
- Layer 2: Dark overlay
- Layer 3: Main UI with glass panel

### sky-turquoise-glass.css
- Glassmorphism стили
- iOS-style inputs
- Градиентные кнопки
- Window controls
- Анимации

## 💡 Как это работает

1. **Transparent Stage**: `StageStyle.TRANSPARENT` создает frameless окно
2. **Scene Fill**: `scene.setFill(Color.TRANSPARENT)` убирает белые углы
3. **Glass Effect**: Достигается через `rgba` фоны + многослойные тени
4. **Draggable**: Перетаскивание окна по фоновым элементам
5. **Elastic Animation**: Spring-интерполятор для bounce-эффекта

## 🎨 Цветовая палитра

- **Primary**: #40E0D0 (Turquoise)
- **Secondary**: #87CEEB (Sky Blue)  
- **Background**: #0a0e27 → #1a2f4f (Gradient)
- **Glass**: rgba(255, 255, 255, 0.08) + blur
- **Border**: rgba(255, 255, 255, 0.18)

## 🔧 Настройка

### Изменить цвета
Отредактируйте `sky-turquoise-glass.css`:
```css
:root {
    --turquoise-primary: #40E0D0;  /* Ваш цвет */
}
```

### Добавить фоновое изображение
Поместите `nebula-background.png` в `src/main/resources/images/`

### Изменить анимации
Настройте параметры в `GlassMainController.playEntranceAnimation()`

## ⚠️ Важно

1. **Scene Fill**: Должен быть `Color.TRANSPARENT` для корректной работы rounded corners
2. **HiDPI**: Используйте векторные иконки (Ikonli) для четкости на 4K
3. **Performance**: Glassmorphism требует GPU, проверьте производительность

## 📚 Зависимости

- JavaFX 21
- AnimateFX 1.2.1
- Ikonli 12.3.1
- Lombok 1.18.30

Все зависимости уже добавлены в `pom.xml`.

