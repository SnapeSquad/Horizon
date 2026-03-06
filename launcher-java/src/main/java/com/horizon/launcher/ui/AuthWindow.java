package com.horizon.launcher.ui;

import com.horizon.launcher.services.AuthService;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Окно авторизации в стиле iOS 26 Liquid Glass с поддержкой Telegram 2FA
 * Использует новый AuthContainer для единого интерфейса авторизации и регистрации
 */
public class AuthWindow extends Stage {
    private static final Logger logger = LoggerFactory.getLogger(AuthWindow.class);
    
    private final AuthService authService;
    private Scene scene;
    private AuthContainer authContainer;
    
    public AuthWindow() {
        this(null);
    }
    
    /**
     * Конструктор с колбэком для успешной авторизации
     */
    public AuthWindow(Consumer<Map<String, String>> onAuthSuccess) {
        this.authService = AuthService.getInstance();
        
        initStyle(StageStyle.TRANSPARENT);
        setTitle("Horizon Launcher - Авторизация");
        setWidth(450);
        setHeight(600);
        
        createUI(onAuthSuccess);
        setupScene();
    }
    
    /**
     * Создать UI
     */
    private void createUI(Consumer<Map<String, String>> onAuthSuccess) {
        // Используем новый AuthContainer
        authContainer = new AuthContainer((userData) -> {
            logger.info("Авторизация успешна: {}", userData.get("username"));
            if (onAuthSuccess != null) {
                onAuthSuccess.accept(userData);
            }
            close();
        });
        
    }
    
    /**
     * Настроить сцену
     */
    private void setupScene() {
        // Создаем корневой StackPane для ToastManager
        javafx.scene.layout.StackPane rootPane = new javafx.scene.layout.StackPane();
        rootPane.getChildren().add(authContainer);
        
        scene = new Scene(rootPane, 450, 600);
        scene.setFill(Color.TRANSPARENT);
        
        // Загружаем стили
        try {
            String cssPath = getClass().getResource("/styles.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
        } catch (Exception e) {
            logger.warn("Не удалось загрузить стили CSS", e);
        }
        
        setScene(scene);
        
        // Инициализируем ToastManager после создания Scene
        javafx.application.Platform.runLater(() -> {
            com.horizon.launcher.ui.components.ToastManager.getInstance().initialize(rootPane);
        });
        
        // Центрируем окно
        centerOnScreen();
    }
}
