package com.horizon.launcher.ui;

import com.horizon.launcher.api.ApiClient;
import com.horizon.launcher.util.ValidationUtils;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Окно регистрации нового пользователя
 */
public class RegisterWindow {
    private static final Logger logger = LoggerFactory.getLogger(RegisterWindow.class);
    
    private Stage stage;
    private Stage parentStage;
    private TextField usernameField;
    private PasswordField passwordField;
    private PasswordField confirmPasswordField;
    private TextField emailField;
    private Button registerButton;
    private Label statusLabel;
    
    public RegisterWindow(Stage parentStage) {
        this.parentStage = parentStage;
        createWindow();
    }
    
    private void createWindow() {
        stage = new Stage();
        stage.setTitle("Регистрация - Horizon Launcher");
        stage.setWidth(500);
        stage.setHeight(600);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(parentStage);
        stage.initStyle(StageStyle.UNDECORATED);
        
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #0a0e27 0%, #1a1f3a 100%);");
        
        // Заголовок
        Label titleLabel = new Label("Регистрация");
        titleLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; " +
                           "-fx-text-fill: linear-gradient(to right, #40E0D0, #87CEEB);");
        root.getChildren().add(titleLabel);
        
        // Поля ввода
        usernameField = new TextField();
        usernameField.setPromptText("Имя пользователя");
        usernameField.setPrefWidth(400);
        usernameField.setPrefHeight(45);
        usernameField.setStyle("-fx-background-color: rgba(255, 255, 255, 0.12); " +
                              "-fx-background-radius: 15px; -fx-border-color: rgba(255, 255, 255, 0.2); " +
                              "-fx-border-width: 1.5px; -fx-border-radius: 15px; -fx-text-fill: #FFFFFF; " +
                              "-fx-prompt-text-fill: rgba(255, 255, 255, 0.5); -fx-font-size: 16px; " +
                              "-fx-padding: 15px 20px;");
        
        emailField = new TextField();
        emailField.setPromptText("Email");
        emailField.setPrefWidth(400);
        emailField.setPrefHeight(45);
        emailField.setStyle("-fx-background-color: rgba(255, 255, 255, 0.12); " +
                           "-fx-background-radius: 15px; -fx-border-color: rgba(255, 255, 255, 0.2); " +
                           "-fx-border-width: 1.5px; -fx-border-radius: 15px; -fx-text-fill: #FFFFFF; " +
                           "-fx-prompt-text-fill: rgba(255, 255, 255, 0.5); -fx-font-size: 16px; " +
                           "-fx-padding: 15px 20px;");
        
        passwordField = new PasswordField();
        passwordField.setPromptText("Пароль");
        passwordField.setPrefWidth(400);
        passwordField.setPrefHeight(45);
        passwordField.setStyle("-fx-background-color: rgba(255, 255, 255, 0.12); " +
                              "-fx-background-radius: 15px; -fx-border-color: rgba(255, 255, 255, 0.2); " +
                              "-fx-border-width: 1.5px; -fx-border-radius: 15px; -fx-text-fill: #FFFFFF; " +
                              "-fx-prompt-text-fill: rgba(255, 255, 255, 0.5); -fx-font-size: 16px; " +
                              "-fx-padding: 15px 20px;");
        
        confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Подтвердите пароль");
        confirmPasswordField.setPrefWidth(400);
        confirmPasswordField.setPrefHeight(45);
        confirmPasswordField.setStyle("-fx-background-color: rgba(255, 255, 255, 0.12); " +
                                     "-fx-background-radius: 15px; -fx-border-color: rgba(255, 255, 255, 0.2); " +
                                     "-fx-border-width: 1.5px; -fx-border-radius: 15px; -fx-text-fill: #FFFFFF; " +
                                     "-fx-prompt-text-fill: rgba(255, 255, 255, 0.5); -fx-font-size: 16px; " +
                                     "-fx-padding: 15px 20px;");
        
        // Кнопка регистрации
        registerButton = new Button("Зарегистрироваться");
        registerButton.setPrefWidth(400);
        registerButton.setPrefHeight(50);
        registerButton.setStyle("-fx-background-color: linear-gradient(to right, #40E0D0 0%, #87CEEB 100%); " +
                               "-fx-background-radius: 15px; -fx-text-fill: #0a0e27; -fx-font-size: 18px; " +
                               "-fx-font-weight: bold; -fx-cursor: hand;");
        registerButton.setOnAction(e -> handleRegister());
        
        // Статус
        statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 14px;");
        statusLabel.setWrapText(true);
        
        // Кнопка закрытия
        Button closeButton = new Button("Отмена");
        closeButton.setPrefWidth(200);
        closeButton.setPrefHeight(40);
        closeButton.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); " +
                            "-fx-background-radius: 10px; -fx-text-fill: #FFFFFF; -fx-font-size: 14px; " +
                            "-fx-cursor: hand;");
        closeButton.setOnAction(e -> stage.close());
        
        root.getChildren().addAll(
            usernameField,
            emailField,
            passwordField,
            confirmPasswordField,
            registerButton,
            statusLabel,
            closeButton
        );
        
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.centerOnScreen();
    }
    
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        // Валидация
        if (username.isEmpty()) {
            showError("Введите имя пользователя");
            return;
        }
        
        if (!ValidationUtils.isValidUsername(username)) {
            showError("Недопустимое имя пользователя. Используйте только буквы, цифры и _");
            return;
        }
        
        if (email.isEmpty()) {
            showError("Введите email");
            return;
        }
        
        if (!ValidationUtils.isValidEmail(email)) {
            showError("Недопустимый формат email");
            return;
        }
        
        if (password.isEmpty()) {
            showError("Введите пароль");
            return;
        }
        
        if (password.length() < 6) {
            showError("Пароль должен быть не менее 6 символов");
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            showError("Пароли не совпадают");
            return;
        }
        
        // Отправка запроса на регистрацию
        registerButton.setDisable(true);
        registerButton.setText("Регистрация...");
        statusLabel.setText("Регистрация...");
        
        new Thread(() -> {
            try {
                com.google.gson.JsonObject data = new com.google.gson.JsonObject();
                data.addProperty("username", username);
                data.addProperty("email", email);
                data.addProperty("password", password);
                
                ApiClient client = ApiClient.getInstance();
                ApiClient.ApiResponse response = client.post("/api/auth/register", data);
                
                Platform.runLater(() -> {
                    registerButton.setDisable(false);
                    registerButton.setText("Зарегистрироваться");
                    
                    if (response.getStatusCode() == 200 || response.getStatusCode() == 201) {
                        showSuccess("Регистрация успешна! Теперь вы можете войти.");
                        // Закрываем окно через 2 секунды
                        new Thread(() -> {
                            try {
                                Thread.sleep(2000);
                                Platform.runLater(() -> stage.close());
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }).start();
                    } else {
                        String errorMsg = "Ошибка регистрации";
                        try {
                            com.google.gson.JsonObject body = response.getBody();
                            if (body != null && body.has("message")) {
                                errorMsg = body.get("message").getAsString();
                            } else if (body != null && body.has("error")) {
                                errorMsg = body.get("error").getAsString();
                            }
                        } catch (Exception e) {
                            logger.debug("Не удалось распарсить ответ", e);
                        }
                        showError(errorMsg);
                    }
                });
            } catch (Exception e) {
                logger.error("Ошибка при регистрации", e);
                Platform.runLater(() -> {
                    registerButton.setDisable(false);
                    registerButton.setText("Зарегистрироваться");
                    showError("Ошибка при регистрации: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private void showError(String message) {
        statusLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 14px;");
        statusLabel.setText(message);
    }
    
    private void showSuccess(String message) {
        statusLabel.setStyle("-fx-text-fill: #44ff44; -fx-font-size: 14px;");
        statusLabel.setText(message);
    }
    
    public void show() {
        stage.show();
    }
}

