package com.horizon.launcher;

import javafx.application.Application;
import javafx.stage.Stage;
import com.horizon.launcher.ui.StyledAuthWindow;
import com.horizon.launcher.util.ConfigManager;
import com.horizon.launcher.util.DiscordLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Главный класс приложения Horizon Launcher
 */
public class LauncherApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(LauncherApplication.class);

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Запуск Horizon Launcher v1.0.0...");
            
            // Устанавливаем глобальный обработчик необработанных исключений (AAA-уровень)
            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                logger.error("Необработанное исключение в потоке: {}", thread.getName(), throwable);
                try {
                    DiscordLogger.getInstance().logException(throwable, 
                            "Необработанное исключение в потоке: " + thread.getName());
                } catch (Exception e) {
                    logger.error("Ошибка при отправке исключения в Discord", e);
                }
            });
            
            // Инициализация конфигурации с обработкой ошибок
            try {
                ConfigManager.getInstance().initialize();
                logger.debug("Конфигурация инициализирована");
            } catch (Exception e) {
                logger.error("Ошибка инициализации конфигурации", e);
                // Продолжаем работу с дефолтными настройками
            }
            
            // Валидация критических настроек
            String apiUrl = ConfigManager.getInstance().getApiUrl();
            if (apiUrl == null || apiUrl.trim().isEmpty()) {
                logger.warn("API URL не настроен, используется значение по умолчанию");
            }
            
            // Открываем окно авторизации
            try {
                StyledAuthWindow authWindow = new StyledAuthWindow();
                authWindow.show();
                logger.info("Окно авторизации открыто");
            } catch (Exception e) {
                logger.error("Ошибка при создании окна авторизации", e);
                throw e; // Критическая ошибка, не можем продолжить
            }
            
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
        // Можно переключаться между UI:
        // - Старый UI: launch(args);
        // - Новый Glass UI: Application.launch(com.horizon.launcher.ui.glass.GlassLauncherApplication.class, args);
        
        // По умолчанию используем новый Glass UI (Sky-Turquoise Glass)
        javafx.application.Application.launch(com.horizon.launcher.ui.glass.GlassLauncherApplication.class, args);
        
        // Для старого UI раскомментируйте:
        // launch(args);
    }
}

