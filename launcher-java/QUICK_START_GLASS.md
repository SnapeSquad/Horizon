# 🚀 Быстрый старт - Sky-Turquoise Glass UI

## Запуск нового Glass UI

Новый UI уже настроен как основной. Просто запустите:

```powershell
cd launcher-java
.\build.ps1
.\run.ps1
```

## Структура файлов

### Основные файлы:
- `src/main/java/com/horizon/launcher/ui/glass/GlassLauncherApplication.java` - главный класс
- `src/main/java/com/horizon/launcher/ui/glass/GlassMainController.java` - контроллер MVC
- `src/main/resources/fxml/MainView.fxml` - FXML разметка
- `src/main/resources/styles/sky-turquoise-glass.css` - CSS тема

## Особенности дизайна

✅ **Frameless Window** - прозрачное окно без рамки  
✅ **Glassmorphism** - эффект размытого стекла  
✅ **Elastic Animation** - пружинная анимация при открытии  
✅ **Draggable** - перетаскивание окна  
✅ **Sky-Turquoise Theme** - цвета #40E0D0 и #87CEEB  

## Переключение между UI

В `LauncherApplication.java`:

```java
// Новый Glass UI (по умолчанию)
Application.launch(com.horizon.launcher.ui.glass.GlassLauncherApplication.class, args);

// Старый UI
// launch(args);
```

## Требования

- Java 21
- JavaFX 21
- AnimateFX (уже в pom.xml)
- Ikonli (уже в pom.xml)

Все зависимости установлены автоматически при сборке.

