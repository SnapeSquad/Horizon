package com.horizon.launcher;

import com.horizon.launcher.services.AuthService;
import com.horizon.launcher.ui.MainWindow;
import com.horizon.launcher.ui.AuthWindow;
import com.horizon.launcher.ui.SplashScreen;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Точка входа в приложение Horizon Launcher
 */
public class LauncherApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(LauncherApplication.class);
    
    // Сохраняем данные пользователя после успешной аутентификации
    private String username;
    private String accessToken;
    private String uuid;
    
    private SplashScreen splashScreen;

    @Override
    public void start(Stage primaryStage) {
        logger.info("Запуск Horizon Launcher...");
        
        try {
            // Показываем Splash экран
            splashScreen = new SplashScreen();
            splashScreen.show();
            
            // Проверяем наличие сохраненной сессии
            AuthService authService = AuthService.getInstance();
            
            if (authService.isAuthenticated()) {
                // Пытаемся автоматически войти
                splashScreen.setStatus("Автоматический вход...");
                
                authService.autoLogin(new AuthService.AutoLoginCallback() {
                    @Override
                    public void onSuccess(String username, String token) {
                        Platform.runLater(() -> {
                            logger.info("Автоматический вход успешен: {}", username);
                            splashScreen.setStatus("Вход выполнен!");
                            
                            // Закрываем Splash и открываем главное окно
                            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(0.5));
                            pause.setOnFinished(e -> {
                                splashScreen.close();
                                
                                LauncherApplication.this.username = username;
                                LauncherApplication.this.accessToken = token;
                                LauncherApplication.this.uuid = null;
                                
                                Stage mainStage = new Stage();
                                MainWindow mainWindow = new MainWindow(mainStage, username, accessToken, uuid);
                                mainWindow.show();
                            });
                            pause.play();
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        Platform.runLater(() -> {
                            logger.info("Автоматический вход не удался: {}", error);
                            splashScreen.setStatus("Сессия истекла");
                            
                            // Закрываем Splash и показываем окно авторизации
                            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1));
                            pause.setOnFinished(e -> {
                                splashScreen.close();
                                showAuthWindow();
                            });
                            pause.play();
                        });
                    }
                });
            } else {
                // Нет сохраненной сессии, показываем окно авторизации
                splashScreen.setStatus("Требуется авторизация");
                
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1));
                pause.setOnFinished(e -> {
                    splashScreen.close();
                    showAuthWindow();
                });
                pause.play();
            }
            
            logger.info("Приложение успешно запущено");
        } catch (Exception e) {
            logger.error("Ошибка при запуске приложения", e);
            if (splashScreen != null) {
                splashScreen.close();
            }
            throw new RuntimeException("Failed to start application", e);
        }
    }
    
    /**
     * Показать окно авторизации
     */
    private void showAuthWindow() {
        AuthWindow authWindow = new AuthWindow((userData) -> {
            logger.info("Авторизация успешна: {}", userData.get("username"));
            
            this.username = userData.get("username");
            this.accessToken = userData.get("accessToken");
            this.uuid = null;
            
            // Создаем и показываем главное окно
            Platform.runLater(() -> {
                Stage mainStage = new Stage();
                MainWindow mainWindow = new MainWindow(mainStage, username, accessToken, uuid);
                mainWindow.show();
            });
        });
        
        authWindow.show();
    }

    @Override
    public void stop() throws Exception {
        logger.info("Закрытие Horizon Launcher...");
        super.stop();
    }

    /**
     * Точка входа в приложение
     */
    public static void main(String[] args) {
        logger.info("Инициализация Horizon Launcher v1.0.0");
        launch(args);
    }
}
