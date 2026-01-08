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
import javafx.util.Duration;

/**
 * Окно авторизации с iOS 26 стилем и Liquid Glass эффектами
 */
public class StyledAuthWindow {
    private Stage stage;
    private AuthService authService;
    private TextField usernameField;
    private PasswordField passwordField;
    private TextField twoFactorCodeField;
    private VBox twoFactorContainer;
    private Button loginButton;
    private boolean requires2FA = false;

    public StyledAuthWindow() {
        this.authService = new AuthService();
        createWindow();
    }

    private void createWindow() {
        stage = new Stage();
        stage.setTitle("Horizon Launcher - Авторизация");
        stage.setWidth(500);
        stage.setHeight(700);
        stage.setResizable(false);
        stage.initStyle(StageStyle.UNDECORATED);

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.getStyleClass().add("animated-background");

        // Кнопки управления окном
        HBox windowControls = new HBox(10);
        windowControls.setAlignment(Pos.TOP_RIGHT);
        windowControls.setPadding(new Insets(10));

        Button minimizeBtn = new Button("−");
        minimizeBtn.getStyleClass().add("window-control");
        minimizeBtn.setOnAction(e -> stage.setIconified(true));

        Button closeBtn = new Button("×");
        closeBtn.getStyleClass().addAll("window-control", "window-control-close");
        closeBtn.setOnAction(e -> Platform.exit());

        windowControls.getChildren().addAll(minimizeBtn, closeBtn);
        root.getChildren().add(windowControls);

        // Glass Card для формы
        VBox glassCard = new VBox(20);
        glassCard.getStyleClass().add("glass-card");
        glassCard.setPadding(new Insets(40));
        glassCard.setMaxWidth(400);

        // Заголовок
        Label titleLabel = new Label("Вход");
        titleLabel.getStyleClass().add("title-text");
        AnimationHelper.slideUp(titleLabel, Duration.millis(400));
        glassCard.getChildren().add(titleLabel);

        // Поле имени пользователя
        usernameField = new TextField();
        usernameField.setPromptText("Имя пользователя");
        usernameField.setPrefWidth(320);
        usernameField.setPrefHeight(45);
        usernameField.getStyleClass().add("glass-input");
        AnimationHelper.slideUp(usernameField, Duration.millis(500));
        glassCard.getChildren().add(usernameField);

        // Поле пароля
        passwordField = new PasswordField();
        passwordField.setPromptText("Пароль");
        passwordField.setPrefWidth(320);
        passwordField.setPrefHeight(45);
        passwordField.getStyleClass().add("glass-input");
        AnimationHelper.slideUp(passwordField, Duration.millis(600));
        glassCard.getChildren().add(passwordField);

        // Контейнер для 2FA кода
        twoFactorContainer = new VBox(10);
        twoFactorContainer.setVisible(false);
        twoFactorCodeField = new TextField();
        twoFactorCodeField.setPromptText("Код 2FA (6 цифр)");
        twoFactorCodeField.setPrefWidth(320);
        twoFactorCodeField.setPrefHeight(45);
        twoFactorCodeField.getStyleClass().add("glass-input");
        twoFactorCodeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                twoFactorCodeField.setText(newVal.replaceAll("[^\\d]", ""));
            }
            if (twoFactorCodeField.getText().length() > 6) {
                twoFactorCodeField.setText(twoFactorCodeField.getText().substring(0, 6));
            }
        });
        twoFactorContainer.getChildren().add(twoFactorCodeField);
        glassCard.getChildren().add(twoFactorContainer);

        // Кнопка входа
        loginButton = new Button("Войти");
        loginButton.setPrefWidth(320);
        loginButton.setPrefHeight(50);
        loginButton.getStyleClass().add("ios-button");
        loginButton.setOnAction(e -> {
            AnimationHelper.liquidPress(loginButton);
            handleLogin();
        });
        AnimationHelper.scaleIn(loginButton, Duration.millis(700));
        glassCard.getChildren().add(loginButton);

        // Подсказка о 2FA
        VBox hintBox = new VBox(8);
        hintBox.setAlignment(Pos.CENTER);
        hintBox.setPadding(new Insets(10));
        hintBox.getStyleClass().add("glass-card");
        hintBox.setStyle("-fx-background-color: rgba(102, 126, 234, 0.2); -fx-background-radius: 12;");
        
        Label tfaHint = new Label("💡 Рекомендуем включить двухфакторную аутентификацию");
        tfaHint.setStyle("-fx-text-fill: rgba(255,255,255,0.9); -fx-font-size: 13px; -fx-wrap-text: true;");
        tfaHint.setMaxWidth(320);
        tfaHint.setAlignment(Pos.CENTER);
        hintBox.getChildren().add(tfaHint);
        glassCard.getChildren().add(hintBox);

        // Ссылка на регистрацию
        HBox registerBox = new HBox(5);
        registerBox.setAlignment(Pos.CENTER);
        
        Label noAccountLabel = new Label("Нет аккаунта?");
        noAccountLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 14px;");
        
        Hyperlink registerLink = new Hyperlink("Регистрация");
        registerLink.setStyle("-fx-text-fill: rgba(102, 126, 234, 1); -fx-font-size: 14px; -fx-underline: false; -fx-font-weight: bold;");
        registerLink.setOnAction(e -> {
            AnimationHelper.pulse(registerLink);
            showRegisterDialog();
        });
        
        registerBox.getChildren().addAll(noAccountLabel, registerLink);
        glassCard.getChildren().add(registerBox);

        root.getChildren().add(glassCard);
        AnimationHelper.fadeIn(glassCard, Duration.millis(300));

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        stage.setScene(scene);
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String twoFactorCode = requires2FA ? twoFactorCodeField.getText().trim() : null;

        if (username.isEmpty() || password.isEmpty()) {
            AnimationHelper.errorFlash(loginButton);
            showAlert("Пожалуйста, введите имя пользователя и пароль", true);
            return;
        }

        if (requires2FA && (twoFactorCode == null || twoFactorCode.length() != 6)) {
            AnimationHelper.errorFlash(twoFactorCodeField);
            showAlert("Пожалуйста, введите 6-значный код 2FA", true);
            return;
        }

        loginButton.setDisable(true);
        loginButton.setText("Вход...");

        // Создаем диалог прогресса
        ProgressDialog progressDialog = new ProgressDialog("Вход в систему");
        progressDialog.show();
        progressDialog.updateProgress(0.2, "Проверка данных...");

        new Thread(() -> {
            try {
                // Имитация прогресса входа
                Thread.sleep(300);
                progressDialog.updateProgress(0.4, "Подключение к серверу...");
                Thread.sleep(300);
                progressDialog.updateProgress(0.6, "Проверка учетных данных...");
                
                AuthService.AuthResult result = authService.login(username, password, twoFactorCode);
                
                Thread.sleep(200);
                progressDialog.updateProgress(0.8, "Завершение входа...");
                Thread.sleep(200);
                
                Platform.runLater(() -> {
                    progressDialog.close();
                    loginButton.setDisable(false);
                    loginButton.setText("Войти");
                    
                    if (result.isSuccess()) {
                        progressDialog.updateProgress(1.0, "Вход выполнен успешно!");
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                        
                        Platform.runLater(() -> {
                            progressDialog.close();
                            AnimationHelper.successFlash(loginButton);
                            stage.close();
                            StyledMainWindow mainWindow = new StyledMainWindow(result.getUsername());
                            mainWindow.show();
                        });
                    } else if (result.requires2FA()) {
                        requires2FA = true;
                        twoFactorContainer.setVisible(true);
                        AnimationHelper.slideUp(twoFactorContainer, Duration.millis(300));
                        showAlert("Введите код двухфакторной аутентификации", false);
                    } else {
                        AnimationHelper.errorFlash(loginButton);
                        showAlert(result.getMessage(), true);
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Platform.runLater(() -> {
                    progressDialog.close();
                    loginButton.setDisable(false);
                    loginButton.setText("Войти");
                });
            }
        }).start();
    }

    private void showRegisterDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Регистрация");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.getStyleClass().add("glass-card");

        TextField regUsername = new TextField();
        regUsername.setPromptText("Имя пользователя");
        regUsername.getStyleClass().add("glass-input");
        regUsername.setPrefWidth(300);

        PasswordField regPassword = new PasswordField();
        regPassword.setPromptText("Пароль");
        regPassword.getStyleClass().add("glass-input");
        regPassword.setPrefWidth(300);

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
                            AnimationHelper.successFlash(loginButton);
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
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        alert.showAndWait();
    }

    public void show() {
        stage.show();
    }
}





