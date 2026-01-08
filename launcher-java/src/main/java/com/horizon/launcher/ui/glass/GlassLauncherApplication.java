package com.horizon.launcher.ui.glass;

import com.horizon.launcher.util.ConfigManager;
import com.horizon.launcher.util.DiscordLogger;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Главный класс приложения с Sky-Turquoise Glass UI
 * 
 * Особенности:
 * - Frameless прозрачное окно
 * - Glassmorphism дизайн
 * - Плавные анимации
 */
public class GlassLauncherApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(GlassLauncherApplication.class);
    
    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Запуск Horizon Launcher (Sky-Turquoise Glass UI)...");
            
            // Устанавливаем глобальный обработчик исключений
            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                logger.error("Необработанное исключение в потоке: {}", thread.getName(), throwable);
                try {
                    DiscordLogger.getInstance().logException(throwable, 
                            "Необработанное исключение в потоке: " + thread.getName());
                } catch (Exception e) {
                    logger.error("Ошибка при отправке исключения в Discord", e);
                }
            });
            
            // Инициализация конфигурации
            try {
                ConfigManager.getInstance().initialize();
                logger.debug("Конфигурация инициализирована");
            } catch (Exception e) {
                logger.error("Ошибка инициализации конфигурации", e);
            }
            
            // Загружаем FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
            Parent root = loader.load();
            
            // Получаем контроллер и устанавливаем stage
            GlassMainController controller = loader.getController();
            controller.setStage(primaryStage);
            
            // Создаем сцену с прозрачным фоном (ВАЖНО для frameless!)
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT); // Критично для корректной работы rounded corners
            
            // Загружаем CSS темы (Figma дизайн загружается первым для переопределения базовых стилей)
            scene.getStylesheets().add(getClass().getResource("/styles/figma-theme.css").toExternalForm());
            // Оригинальный glass стиль загружается вторым для дополнительных эффектов
            scene.getStylesheets().add(getClass().getResource("/styles/sky-turquoise-glass.css").toExternalForm());
            
            // Настраиваем stage
            primaryStage.setTitle("Horizon Launcher");
            primaryStage.setScene(scene);
            primaryStage.setWidth(1400);
            primaryStage.setHeight(900);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            
            // Frameless стиль (прозрачное окно)
            primaryStage.initStyle(StageStyle.TRANSPARENT);
            
            // Центрируем окно на экране
            primaryStage.centerOnScreen();
            
            // Показываем окно
            primaryStage.show();
            
            logger.info("Лаунчер успешно запущен");
            
        } catch (Exception e) {
            logger.error("Критическая ошибка при запуске лаунчера", e);
            try {
                DiscordLogger.getInstance().logException(e, "Критическая ошибка при запуске лаунчера");
            } catch (Exception ex) {
                logger.error("Не удалось отправить ошибку в Discord", ex);
            }
            System.exit(1);
        }
    }
    
    @Override
    public void stop() {
        logger.info("Закрытие лаунчера...");
        System.exit(0);
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}

