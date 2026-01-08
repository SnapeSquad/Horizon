# Исправления ошибок - FXML LinearGradient

## Проблема 1: LinearGradient is not a valid type

**Ошибка:**
```
javafx.fxml.LoadException: LinearGradient is not a valid type.
/C:/Users/skviz/Desktop/Horizon/launcher-java/target/classes/fxml/MainView.fxml:15
```

**Причина:**
В FXML файле использовался `LinearGradient` и `Stop` без импортов.

**Исправление:**
Добавлены импорты в `MainView.fxml`:
```xml
<?import javafx.scene.paint.LinearGradient?>
<?import javafx.scene.paint.Stop?>
```

## Проблема 2: Invalid resource: /images/nebula-background.png not found

**Ошибка:**
```
javafx.fxml.LoadException: Invalid resource: /images/nebula-background.png not found on the classpath
```

**Причина:**
ImageView в FXML пытался загрузить несуществующее изображение.

**Исправление:**
1. Убран ImageView из FXML
2. Добавлена опциональная загрузка изображения в контроллере через `loadBackgroundImage()`
3. Используется gradient fallback, если изображение не найдено

## Проблема 3: run.ps1 пытается запустить JAR без JavaFX модулей

**Ошибка:**
```
Error: JavaFX runtime components are missing, and are required to run this application
```

**Исправление:**
Изменен `run.ps1` для использования `mvnw.cmd javafx:run` вместо прямого запуска JAR.

## Статус

✅ Все ошибки исправлены  
✅ FXML валиден  
✅ Загрузка изображения опциональна  
✅ run.ps1 использует Maven для запуска  

Проект готов к запуску!


