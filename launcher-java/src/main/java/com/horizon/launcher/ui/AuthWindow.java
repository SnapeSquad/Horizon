package com.horizon.launcher.ui;

import com.horizon.launcher.api.AuthService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Окно авторизации
 */
public class AuthWindow {
    private static final Logger logger = LoggerFactory.getLogger(AuthWindow.class);
    private Stage stage;
    private AuthService authService;
    private TextField usernameField;
    private PasswordField passwordField;
    private TextField twoFactorCodeField;
    private VBox twoFactorContainer;
    private boolean requires2FA = false;

    public AuthWindow() {
        this.authService = new AuthService();
        createWindow();
    }

    private void createWindow() {
        stage = new Stage();
        stage.setTitle("Horizon Launcher - Авторизация");
        stage.setWidth(500);
        stage.setHeight(650);
        stage.setResizable(false);
        stage.initStyle(StageStyle.UNDECORATED);

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #87CEEB 0%, #B0E0E6 50%, #E0F6FF 100%);");

        // Заголовок
        Label titleLabel = new Label("Вход");
        titleLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: white;");
        root.getChildren().add(titleLabel);

        // Поле имени пользователя
        usernameField = new TextField();
        usernameField.setPromptText("Имя пользователя");
        usernameField.setPrefWidth(300);
        usernameField.setPrefHeight(40);
        usernameField.setStyle("-fx-background-color: rgba(255,255,255,0.3); -fx-background-radius: 10; -fx-text-fill: white;");
        root.getChildren().add(usernameField);

        // Поле пароля
        passwordField = new PasswordField();
        passwordField.setPromptText("Пароль");
        passwordField.setPrefWidth(300);
        passwordField.setPrefHeight(40);
        passwordField.setStyle("-fx-background-color: rgba(255,255,255,0.3); -fx-background-radius: 10; -fx-text-fill: white;");
        root.getChildren().add(passwordField);

        // Контейнер для 2FA кода (скрыт по умолчанию)
        twoFactorContainer = new VBox(10);
        twoFactorContainer.setVisible(false);
        twoFactorCodeField = new TextField();
        twoFactorCodeField.setPromptText("Код 2FA (6 цифр)");
        twoFactorCodeField.setPrefWidth(300);
        twoFactorCodeField.setPrefHeight(40);
        twoFactorCodeField.setStyle("-fx-background-color: rgba(255,255,255,0.3); -fx-background-radius: 10; -fx-text-fill: white;");
        twoFactorCodeField.textProperty().addListener((obs, oldVal, newVal) -> {
            // Оставляем только цифры
            if (!newVal.matches("\\d*")) {
                twoFactorCodeField.setText(newVal.replaceAll("[^\\d]", ""));
            }
            // Ограничиваем до 6 цифр
            if (twoFactorCodeField.getText().length() > 6) {
                twoFactorCodeField.setText(twoFactorCodeField.getText().substring(0, 6));
            }
        });
        twoFactorContainer.getChildren().add(twoFactorCodeField);
        root.getChildren().add(twoFactorContainer);

        // Кнопка входа
        Button loginButton = new Button("Войти");
        loginButton.setPrefWidth(300);
        loginButton.setPrefHeight(50);
        loginButton.setStyle("-fx-background-color: linear-gradient(to right, #9B59B6, #8E44AD); " +
                           "-fx-background-radius: 10; -fx-text-fill: white; -fx-font-size: 16px; " +
                           "-fx-font-weight: bold;");
        loginButton.setOnAction(e -> handleLogin());
        root.getChildren().add(loginButton);

        // Подсказка о 2FA
        Label tfaHint = new Label("💡 Рекомендуем включить двухфакторную аутентификацию для защиты аккаунта");
        tfaHint.setStyle("-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 12px; -fx-wrap-text: true;");
        tfaHint.setMaxWidth(300);
        root.getChildren().add(tfaHint);

        // Ссылка на регистрацию
        Hyperlink registerLink = new Hyperlink("Нет аккаунта? Регистрация");
        registerLink.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        registerLink.setOnAction(e -> showRegisterDialog());
        root.getChildren().add(registerLink);

        // Кнопки управления окном
        HBox windowControls = new HBox(10);
        windowControls.setAlignment(Pos.TOP_RIGHT);
        windowControls.setPadding(new Insets(10));

        Button minimizeBtn = new Button("−");
        minimizeBtn.setStyle("-fx-background-color: rgba(0,0,0,0.1); -fx-text-fill: white; -fx-background-radius: 5;");
        minimizeBtn.setOnAction(e -> stage.setIconified(true));

        Button closeBtn = new Button("×");
        closeBtn.setStyle("-fx-background-color: rgba(255,0,0,0.3); -fx-text-fill: white; -fx-background-radius: 5;");
        closeBtn.setOnAction(e -> Platform.exit());

        windowControls.getChildren().addAll(minimizeBtn, closeBtn);
        root.getChildren().add(0, windowControls);

        Scene scene = new Scene(root);
        stage.setScene(scene);
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String twoFactorCode = requires2FA ? twoFactorCodeField.getText().trim() : null;

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Пожалуйста, введите имя пользователя и пароль", true);
            return;
        }

        if (requires2FA && (twoFactorCode == null || twoFactorCode.length() != 6)) {
            showAlert("Пожалуйста, введите 6-значный код 2FA", true);
            return;
        }

        // Выполняем вход в отдельном потоке
        new Thread(() -> {
            AuthService.AuthResult result = authService.login(username, password, twoFactorCode);
            
            Platform.runLater(() -> {
                if (result.isSuccess()) {
                    // Закрываем окно авторизации и открываем главное
                    stage.close();
                    MainWindow mainWindow = new MainWindow(result.getUsername());
                    mainWindow.show();
                } else if (result.requires2FA()) {
                    // Показываем поле для 2FA кода
                    requires2FA = true;
                    twoFactorContainer.setVisible(true);
                    showAlert("Введите код двухфакторной аутентификации", false);
                } else {
                    showAlert(result.getMessage(), true);
                }
            });
        }).start();
    }

    private void showRegisterDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Регистрация");
        dialog.setHeaderText("Создание нового аккаунта");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        TextField regUsername = new TextField();
        regUsername.setPromptText("Имя пользователя");
        PasswordField regPassword = new PasswordField();
        regPassword.setPromptText("Пароль");

        content.getChildren().addAll(
            new Label("Имя пользователя:"), regUsername,
            new Label("Пароль:"), regPassword
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return regUsername.getText() + "|" + regPassword.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            String[] parts = result.split("\\|");
            if (parts.length == 2) {
                new Thread(() -> {
                    AuthService.AuthResult regResult = authService.register(parts[0], parts[1]);
                    Platform.runLater(() -> {
                        if (regResult.isSuccess()) {
                            showAlert("Регистрация успешна! Теперь вы можете войти.", false);
                        } else {
                            showAlert(regResult.getMessage(), true);
                        }
                    });
                }).start();
            }
        });
    }

    private void showAlert(String message, boolean isError) {
        Alert alert = new Alert(isError ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
        alert.setTitle(isError ? "Ошибка" : "Информация");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void show() {
        stage.show();
    }
}

