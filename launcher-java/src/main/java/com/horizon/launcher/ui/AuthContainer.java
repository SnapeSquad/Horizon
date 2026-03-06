package com.horizon.launcher.ui;

import com.horizon.launcher.services.AuthService;
import com.horizon.launcher.ui.components.Toast;
import com.horizon.launcher.ui.components.ToastManager;
import com.horizon.launcher.utils.AccessibilityHelper;
import com.horizon.launcher.utils.AnimationHelper;
import com.horizon.launcher.utils.FieldValidator;
import com.horizon.launcher.utils.HWIDManager;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.net.URI;
import java.util.function.Consumer;

/**
 * Единый контейнер авторизации и регистрации в стиле Liquid Glass
 */
public class AuthContainer extends StackPane {
    private static final Logger logger = LoggerFactory.getLogger(AuthContainer.class);
    
    private final AuthService authService;
    private final HWIDManager hwidManager;
    
    // Формы
    private VBox loginForm;
    private VBox registerForm;
    private VBox twoFAForm;
    private VBox recoveryForm;
    
    // Поля ввода
    private TextField loginUsernameField;
    private PasswordField loginPasswordField;
    private TextField loginPasswordVisibleField; // Для показа пароля
    private TextField registerUsernameField;
    private PasswordField registerPasswordField;
    private TextField registerPasswordVisibleField;
    private PasswordField registerConfirmPasswordField;
    private TextField registerConfirmPasswordVisibleField;
    private TextField twoFACodeField;
    private TextField recoveryUsernameField;
    private TextField recoveryCodeField;
    private PasswordField recoveryNewPasswordField;
    private TextField recoveryNewPasswordVisibleField;
    
    // Кнопки
    private Button loginButton;
    private Button registerButton;
    private Button switchToRegisterButton;
    private Button switchToLoginButton;
    private Button linkTelegramButton;
    private Button verify2FAButton;
    private Button cancel2FAButton;
    private Button switchToRecoveryButton;
    private Button recoveryRequestCodeButton;
    private Button recoveryResetButton;
    private Button recoveryCancelButton;
    
    // Иконки показа пароля
    private Button loginPasswordToggle;
    private Button registerPasswordToggle;
    private Button registerConfirmPasswordToggle;
    private Button recoveryPasswordToggle;
    
    // Лейблы ошибок
    private Label loginErrorLabel;
    private Label registerErrorLabel;
    private Label recoveryErrorLabel;
    
    // Текущее состояние
    private String currentUsername;
    private boolean recoveryStep2 = false; // Этап восстановления пароля
    private Consumer<java.util.Map<String, String>> onAuthSuccess;
    
    // Контейнер для уведомлений
    private VBox toastContainer;
    private ToastManager toastManager;
    
    public AuthContainer(Consumer<java.util.Map<String, String>> onAuthSuccess) {
        this.authService = AuthService.getInstance();
        this.hwidManager = HWIDManager.getInstance();
        this.onAuthSuccess = onAuthSuccess;
        this.toastManager = ToastManager.getInstance();
        
        initializeContainer();
        createLoginForm();
        createRegisterForm();
        create2FAForm();
        createRecoveryForm();
        
        // Показываем форму логина по умолчанию
        showForm(loginForm);
        
        // Инициализируем ToastManager после добавления в Scene
        Platform.runLater(() -> {
            javafx.scene.Node parent = this.getParent();
            if (parent instanceof StackPane) {
                toastManager.initialize((StackPane) parent);
            } else if (parent != null && parent.getParent() instanceof StackPane) {
                toastManager.initialize((StackPane) parent.getParent());
            }
        });
    }
    
    /**
     * Создать поле пароля с функцией показа/скрытия
     */
    private HBox createPasswordFieldWithToggle(PasswordField passwordField, TextField visibleField, Button toggleButton) {
        HBox container = new HBox(5);
        container.setAlignment(Pos.CENTER);
        container.setPrefWidth(320);
        
        // Изначально показываем PasswordField
        passwordField.setVisible(true);
        visibleField.setVisible(false);
        
        // Настройка видимого поля
        visibleField.setStyle(getFieldStyle());
        visibleField.setPrefHeight(50);
        visibleField.setPrefWidth(320);
        visibleField.textProperty().bindBidirectional(passwordField.textProperty());
        
        // Кнопка переключения видимости
        toggleButton.setText("👁");
        toggleButton.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-text-fill: rgba(255, 255, 255, 0.7); " +
            "-fx-font-size: 18px; " +
            "-fx-cursor: hand; " +
            "-fx-padding: 5px;"
        );
        toggleButton.setPrefWidth(30);
        toggleButton.setPrefHeight(30);
        
        toggleButton.setOnAction(e -> {
            boolean isPasswordVisible = passwordField.isVisible();
            passwordField.setVisible(!isPasswordVisible);
            visibleField.setVisible(isPasswordVisible);
            if (isPasswordVisible) {
                visibleField.requestFocus();
            } else {
                passwordField.requestFocus();
            }
        });
        
        // Размещаем кнопку справа от поля
        StackPane fieldContainer = new StackPane();
        fieldContainer.getChildren().addAll(passwordField, visibleField);
        HBox.setHgrow(fieldContainer, Priority.ALWAYS);
        
        container.getChildren().addAll(fieldContainer, toggleButton);
        
        return container;
    }
    
    /**
     * Инициализация основного контейнера с эффектом размытия
     */
    private void initializeContainer() {
        // Фоновый градиент
        this.setStyle(
            "-fx-background-color: linear-gradient(" +
            "from 0% 0% to 100% 100%, " +
            "rgba(102, 126, 234, 0.3) 0%, " +
            "rgba(118, 75, 162, 0.3) 100%" +
            ");"
        );
        this.setPrefSize(450, 600);
        this.setAlignment(Pos.CENTER);
        
        // Контейнер для форм с эффектом размытия
        StackPane glassPane = new StackPane();
        glassPane.setStyle(
            "-fx-background-color: rgba(255, 255, 255, 0.15); " +
            "-fx-background-radius: 20px; " +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.2), 20, 0, 0, 10);"
        );
        glassPane.setPrefSize(400, 550);
        glassPane.setMaxSize(400, 550);
        
        // Применяем эффект размытия
        GaussianBlur blur = new GaussianBlur(10);
        glassPane.setEffect(blur);
        
        // Контейнер для уведомлений
        toastContainer = new VBox(10);
        toastContainer.setAlignment(Pos.BOTTOM_CENTER);
        toastContainer.setPadding(new Insets(0, 0, 20, 0));
        toastContainer.setMouseTransparent(true);
        
        this.getChildren().addAll(glassPane, toastContainer);
    }
    
    /**
     * Создать форму логина
     */
    private void createLoginForm() {
        loginForm = new VBox(20);
        loginForm.setAlignment(Pos.CENTER);
        loginForm.setPadding(new Insets(40, 30, 40, 30));
        loginForm.setPrefWidth(400);
        
        // Заголовок
        Label titleLabel = new Label("Вход в Horizon");
        titleLabel.setStyle(
            "-fx-text-fill: white; " +
            "-fx-font-size: 28px; " +
            "-fx-font-weight: bold; " +
            "-fx-font-family: 'Minecraft Unicode', -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Segoe UI', Roboto, sans-serif;"
        );
        
        // Поле логина с валидацией
        loginUsernameField = new TextField();
        loginUsernameField.setPromptText("Логин");
        loginUsernameField.setStyle(getFieldStyle());
        loginUsernameField.setPrefHeight(50);
        loginUsernameField.setPrefWidth(320);
        
        // Валидация имени пользователя в реальном времени
        FieldValidator.setupRealtimeValidation(
            loginUsernameField,
            FieldValidator::validateUsername,
            loginErrorLabel
        );
        
        // Поле пароля с функцией показа
        loginPasswordField = new PasswordField();
        loginPasswordField.setPromptText("Пароль");
        loginPasswordField.setStyle(getFieldStyle());
        loginPasswordField.setPrefHeight(50);
        loginPasswordField.setPrefWidth(320);
        
        loginPasswordVisibleField = new TextField();
        loginPasswordVisibleField.setPromptText("Пароль");
        
        loginPasswordToggle = new Button();
        HBox passwordContainer = createPasswordFieldWithToggle(loginPasswordField, loginPasswordVisibleField, loginPasswordToggle);
        
        // Валидация пароля в реальном времени
        loginPasswordField.textProperty().addListener((observable, oldValue, newValue) -> {
            hideError(loginErrorLabel);
        });
        loginPasswordVisibleField.textProperty().addListener((observable, oldValue, newValue) -> {
            hideError(loginErrorLabel);
        });
        
        // Лейбл ошибки
        loginErrorLabel = new Label();
        loginErrorLabel.setStyle(
            "-fx-text-fill: rgba(255, 100, 100, 0.9); " +
            "-fx-font-size: 12px; " +
            "-fx-wrap-text: true; " +
            "-fx-max-width: 320px; " +
            "-fx-font-family: 'Minecraft Unicode', -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Segoe UI', Roboto, sans-serif;"
        );
        loginErrorLabel.setVisible(false);
        
        // Кнопка входа
        loginButton = new Button("Войти");
        loginButton.setStyle(getButtonStyle());
        loginButton.setPrefHeight(50);
        loginButton.setPrefWidth(320);
        loginButton.setOnAction(e -> handleLogin());
        AccessibilityHelper.setupButtonAccessibility(loginButton, "Войти в аккаунт", 
            "Нажмите для входа в систему. Убедитесь, что вы ввели правильный логин и пароль.");
        AnimationHelper.liquidPress(loginButton);
        
        // Кнопка переключения на регистрацию
        switchToRegisterButton = new Button("Нет аккаунта? Зарегистрироваться");
        switchToRegisterButton.setStyle(getLinkButtonStyle());
        switchToRegisterButton.setOnAction(e -> switchToRegister());
        AnimationHelper.liquidPress(switchToRegisterButton);
        
        // Кнопка восстановления пароля
        switchToRecoveryButton = new Button("Забыли пароль?");
        switchToRecoveryButton.setStyle(getLinkButtonStyle());
        switchToRecoveryButton.setOnAction(e -> switchToRecovery());
        AnimationHelper.liquidPress(switchToRecoveryButton);
        
        // Обработка Enter
        loginPasswordField.setOnAction(e -> handleLogin());
        loginPasswordVisibleField.setOnAction(e -> handleLogin());
        
        loginForm.getChildren().addAll(
            titleLabel,
            loginUsernameField,
            passwordContainer,
            loginErrorLabel,
            loginButton,
            switchToRegisterButton,
            switchToRecoveryButton
        );
    }
    
    /**
     * Создать форму регистрации
     */
    private void createRegisterForm() {
        registerForm = new VBox(20);
        registerForm.setAlignment(Pos.CENTER);
        registerForm.setPadding(new Insets(40, 30, 40, 30));
        registerForm.setPrefWidth(400);
        
        // Заголовок
        Label titleLabel = new Label("Регистрация");
        titleLabel.setStyle(
            "-fx-text-fill: white; " +
            "-fx-font-size: 28px; " +
            "-fx-font-weight: bold; " +
            "-fx-font-family: 'Minecraft Unicode', -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Segoe UI', Roboto, sans-serif;"
        );
        
        // Лейбл ошибки (создаем до использования в валидации)
        registerErrorLabel = new Label();
        registerErrorLabel.setStyle(
            "-fx-text-fill: rgba(255, 100, 100, 0.9); " +
            "-fx-font-size: 12px; " +
            "-fx-wrap-text: true; " +
            "-fx-max-width: 320px; " +
            "-fx-font-family: 'Minecraft Unicode', -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Segoe UI', Roboto, sans-serif;"
        );
        registerErrorLabel.setVisible(false);
        
        // Поле логина с валидацией
        registerUsernameField = new TextField();
        registerUsernameField.setPromptText("Логин");
        registerUsernameField.setStyle(getFieldStyle());
        registerUsernameField.setPrefHeight(50);
        registerUsernameField.setPrefWidth(320);
        
        // Валидация имени пользователя в реальном времени
        FieldValidator.setupRealtimeValidation(
            registerUsernameField,
            FieldValidator::validateUsername,
            registerErrorLabel
        );
        
        // Поле пароля с функцией показа
        registerPasswordField = new PasswordField();
        registerPasswordField.setPromptText("Пароль");
        registerPasswordField.setStyle(getFieldStyle());
        registerPasswordField.setPrefHeight(50);
        registerPasswordField.setPrefWidth(320);
        
        registerPasswordVisibleField = new TextField();
        registerPasswordVisibleField.setPromptText("Пароль");
        
        registerPasswordToggle = new Button();
        HBox registerPasswordContainer = createPasswordFieldWithToggle(registerPasswordField, registerPasswordVisibleField, registerPasswordToggle);
        
        // Поле подтверждения пароля с функцией показа
        registerConfirmPasswordField = new PasswordField();
        registerConfirmPasswordField.setPromptText("Подтверждение пароля");
        registerConfirmPasswordField.setStyle(getFieldStyle());
        registerConfirmPasswordField.setPrefHeight(50);
        registerConfirmPasswordField.setPrefWidth(320);
        
        registerConfirmPasswordVisibleField = new TextField();
        registerConfirmPasswordVisibleField.setPromptText("Подтверждение пароля");
        
        registerConfirmPasswordToggle = new Button();
        HBox registerConfirmPasswordContainer = createPasswordFieldWithToggle(registerConfirmPasswordField, registerConfirmPasswordVisibleField, registerConfirmPasswordToggle);
        
        // Валидация паролей в реальном времени
        registerPasswordField.textProperty().addListener((observable, oldValue, newValue) -> {
            FieldValidator.ValidationResult result = FieldValidator.validatePassword(newValue);
            if (!result.isValid() && !newValue.isEmpty()) {
                showError(registerErrorLabel, result.getErrorMessage());
            } else {
                hideError(registerErrorLabel);
            }
            // Проверяем совпадение паролей
            if (!registerConfirmPasswordField.getText().isEmpty()) {
                FieldValidator.ValidationResult confirmResult = FieldValidator.validatePasswordConfirmation(
                    newValue, registerConfirmPasswordField.getText()
                );
                if (!confirmResult.isValid()) {
                    showError(registerErrorLabel, confirmResult.getErrorMessage());
                }
            }
        });
        registerPasswordVisibleField.textProperty().addListener((observable, oldValue, newValue) -> {
            FieldValidator.ValidationResult result = FieldValidator.validatePassword(newValue);
            if (!result.isValid() && !newValue.isEmpty()) {
                showError(registerErrorLabel, result.getErrorMessage());
            } else {
                hideError(registerErrorLabel);
            }
        });
        registerConfirmPasswordField.textProperty().addListener((observable, oldValue, newValue) -> {
            FieldValidator.ValidationResult result = FieldValidator.validatePasswordConfirmation(
                registerPasswordField.getText(), newValue
            );
            if (!result.isValid() && !newValue.isEmpty()) {
                showError(registerErrorLabel, result.getErrorMessage());
            } else {
                hideError(registerErrorLabel);
            }
        });
        registerConfirmPasswordVisibleField.textProperty().addListener((observable, oldValue, newValue) -> {
            FieldValidator.ValidationResult result = FieldValidator.validatePasswordConfirmation(
                registerPasswordField.getText(), newValue
            );
            if (!result.isValid() && !newValue.isEmpty()) {
                showError(registerErrorLabel, result.getErrorMessage());
            } else {
                hideError(registerErrorLabel);
            }
        });
        
        // Лейбл ошибки (уже создан выше)
        registerErrorLabel.setStyle(
            "-fx-text-fill: rgba(255, 100, 100, 0.9); " +
            "-fx-font-size: 12px; " +
            "-fx-wrap-text: true; " +
            "-fx-max-width: 320px; " +
            "-fx-font-family: 'Minecraft Unicode', -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Segoe UI', Roboto, sans-serif;"
        );
        registerErrorLabel.setVisible(false);
        
        // Кнопка регистрации
        registerButton = new Button("Зарегистрироваться");
        registerButton.setStyle(getButtonStyle());
        registerButton.setPrefHeight(50);
        registerButton.setPrefWidth(320);
        registerButton.setOnAction(e -> handleRegister());
        AnimationHelper.liquidPress(registerButton);
        
        // Кнопка привязки Telegram
        linkTelegramButton = new Button("Связать с Telegram");
        linkTelegramButton.setStyle(getSecondaryButtonStyle());
        linkTelegramButton.setPrefHeight(45);
        linkTelegramButton.setPrefWidth(320);
        linkTelegramButton.setOnAction(e -> handleLinkTelegram());
        
        // Кнопка переключения на вход
        switchToLoginButton = new Button("Уже есть аккаунт? Войти");
        switchToLoginButton.setStyle(getLinkButtonStyle());
        switchToLoginButton.setOnAction(e -> switchToLogin());
        AnimationHelper.liquidPress(switchToLoginButton);
        
        // Обработка Enter
        registerConfirmPasswordField.setOnAction(e -> handleRegister());
        registerConfirmPasswordVisibleField.setOnAction(e -> handleRegister());
        
        registerForm.getChildren().addAll(
            titleLabel,
            registerUsernameField,
            registerPasswordContainer,
            registerConfirmPasswordContainer,
            registerErrorLabel,
            registerButton,
            linkTelegramButton,
            switchToLoginButton
        );
    }
    
    /**
     * Создать форму 2FA
     */
    private void create2FAForm() {
        twoFAForm = new VBox(20);
        twoFAForm.setAlignment(Pos.CENTER);
        twoFAForm.setPadding(new Insets(40, 30, 40, 30));
        twoFAForm.setPrefWidth(400);
        
        // Заголовок
        Label titleLabel = new Label("Подтверждение 2FA");
        titleLabel.setStyle(
            "-fx-text-fill: white; " +
            "-fx-font-size: 28px; " +
            "-fx-font-weight: bold; " +
            "-fx-font-family: 'Minecraft Unicode', -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Segoe UI', Roboto, sans-serif;"
        );
        
        // Подсказка
        Label hintLabel = new Label("Введите код из Telegram");
        hintLabel.setStyle(
            "-fx-text-fill: rgba(255, 255, 255, 0.8); " +
            "-fx-font-size: 14px; " +
            "-fx-font-family: 'Minecraft Unicode', -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Segoe UI', Roboto, sans-serif;"
        );
        
        // Поле кода 2FA
        twoFACodeField = new TextField();
        twoFACodeField.setPromptText("Код 2FA (6 цифр)");
        twoFACodeField.setStyle(getFieldStyle());
        twoFACodeField.setPrefHeight(50);
        twoFACodeField.setPrefWidth(320);
        twoFACodeField.setAlignment(Pos.CENTER);
        
        // Ограничение на 6 символов и только цифры
        twoFACodeField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > 6) {
                twoFACodeField.setText(oldValue);
            } else if (!newValue.matches("\\d*")) {
                twoFACodeField.setText(oldValue);
            }
            
            // Автоматическая отправка при вводе 6 цифр
            if (newValue.length() == 6) {
                Platform.runLater(() -> handleVerify2FA());
            }
        });
        
        // Кнопка проверки
        verify2FAButton = new Button("Проверить код");
        verify2FAButton.setStyle(getButtonStyle());
        verify2FAButton.setPrefHeight(50);
        verify2FAButton.setPrefWidth(320);
        verify2FAButton.setOnAction(e -> handleVerify2FA());
        AnimationHelper.liquidPress(verify2FAButton);
        
        // Кнопка отмены
        cancel2FAButton = new Button("Отмена");
        cancel2FAButton.setStyle(getSecondaryButtonStyle());
        cancel2FAButton.setPrefHeight(45);
        cancel2FAButton.setPrefWidth(320);
        cancel2FAButton.setOnAction(e -> switchToLogin());
        AnimationHelper.liquidPress(cancel2FAButton);
        
        // Обработка Enter
        twoFACodeField.setOnAction(e -> handleVerify2FA());
        
        twoFAForm.getChildren().addAll(
            titleLabel,
            hintLabel,
            twoFACodeField,
            verify2FAButton,
            cancel2FAButton
        );
    }
    
    /**
     * Переключение между формами с анимацией
     */
    private void showForm(VBox form) {
        // Удаляем все формы из контейнера
        StackPane glassPane = (StackPane) this.getChildren().get(0);
        glassPane.getChildren().clear();
        
        // Добавляем нужную форму
        glassPane.getChildren().add(form);
        
        // Анимация появления
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), form);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), form);
        slideIn.setFromX(50);
        slideIn.setToX(0);
        
        fadeIn.play();
        slideIn.play();
    }
    
    /**
     * Переключение на форму регистрации
     */
    private void switchToRegister() {
        // Анимация скрытия формы логина
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), loginForm);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            showForm(registerForm);
            registerUsernameField.requestFocus();
        });
        fadeOut.play();
    }
    
    /**
     * Переключение на форму логина
     */
    private void switchToLogin() {
        // Анимация скрытия текущей формы
        if (this.getChildren().isEmpty()) {
            showForm(loginForm);
            return;
        }
        
        StackPane glassPane = (StackPane) this.getChildren().get(0);
        if (glassPane.getChildren().isEmpty()) {
            showForm(loginForm);
            return;
        }
        
        VBox currentForm = (VBox) glassPane.getChildren().get(0);
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), currentForm);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            showForm(loginForm);
            loginUsernameField.requestFocus();
        });
        fadeOut.play();
    }
    
    /**
     * Переключение на форму 2FA
     */
    private void switchTo2FA() {
        VBox currentForm = (VBox) ((StackPane) this.getChildren().get(0)).getChildren().get(0);
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), currentForm);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            showForm(twoFAForm);
            twoFACodeField.clear();
            Platform.runLater(() -> twoFACodeField.requestFocus());
        });
        fadeOut.play();
    }
    
    /**
     * Обработка входа
     */
    private void handleLogin() {
        String username = loginUsernameField.getText().trim();
        String password = loginPasswordField.getText();
        
        // Валидация полей
        FieldValidator.ValidationResult usernameResult = FieldValidator.validateUsername(username);
        if (!usernameResult.isValid()) {
            showError(loginErrorLabel, usernameResult.getErrorMessage());
            AnimationHelper.shake(loginForm);
            return;
        }
        
        if (password.isEmpty()) {
            showError(loginErrorLabel, "Введите пароль");
            AnimationHelper.shake(loginForm);
            return;
        }
        
        hideError(loginErrorLabel);
        
        // Показываем индикатор загрузки
        loginButton.setDisable(true);
        loginButton.setText("Вход...");
        
        authService.attemptLogin(username, password, new AuthService.LoginCallback() {
            @Override
            public void onSuccess(String username, String token) {
                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    loginButton.setText("Войти");
                    
                    if (onAuthSuccess != null) {
                        java.util.Map<String, String> userData = new java.util.HashMap<>();
                        userData.put("username", username);
                        userData.put("accessToken", token);
                        onAuthSuccess.accept(userData);
                    }
                });
            }
            
            @Override
            public void onNeed2FA() {
                Platform.runLater(() -> {
                    currentUsername = username;
                    switchTo2FA();
                    loginButton.setDisable(false);
                    loginButton.setText("Войти");
                });
            }
            
            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    loginButton.setText("Войти");
                    String errorMessage = getErrorMessage(error);
                    showError(loginErrorLabel, errorMessage);
                    AnimationHelper.shake(loginForm);
                    showToast(errorMessage, Toast.ToastType.ERROR);
                });
            }
        });
    }
    
    /**
     * Обработка регистрации
     */
    private void handleRegister() {
        String username = registerUsernameField.getText().trim();
        String password = registerPasswordField.getText();
        String confirmPassword = registerConfirmPasswordField.getText();
        
        // Валидация всех полей
        FieldValidator.ValidationResult usernameResult = FieldValidator.validateUsername(username);
        if (!usernameResult.isValid()) {
            showError(registerErrorLabel, usernameResult.getErrorMessage());
            AnimationHelper.shake(registerForm);
            return;
        }
        
        FieldValidator.ValidationResult passwordResult = FieldValidator.validatePasswordConfirmation(password, confirmPassword);
        if (!passwordResult.isValid()) {
            showError(registerErrorLabel, passwordResult.getErrorMessage());
            AnimationHelper.shake(registerForm);
            return;
        }
        
        hideError(registerErrorLabel);
        
        registerButton.setDisable(true);
        registerButton.setText("Регистрация...");
        
        // Регистрация через AuthService
        authService.register(username, password, new AuthService.RegisterCallback() {
            @Override
            public void onSuccess(String username, String token) {
                Platform.runLater(() -> {
                    registerButton.setDisable(false);
                    registerButton.setText("Зарегистрироваться");
                    showToast("Регистрация успешна!", Toast.ToastType.SUCCESS);
                    
                    // Переключаемся на форму логина
                    switchToLogin();
                });
            }
            
            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    registerButton.setDisable(false);
                    registerButton.setText("Зарегистрироваться");
                    String errorMessage = getErrorMessage(error);
                    showError(registerErrorLabel, errorMessage);
                    AnimationHelper.shake(registerForm);
                    showToast(errorMessage, Toast.ToastType.ERROR);
                });
            }
        });
    }
    
    /**
     * Обработка привязки Telegram
     */
    private void handleLinkTelegram() {
        try {
            // Открываем ссылку на бота в браузере
            String botUrl = com.horizon.launcher.utils.ConfigLoader.getInstance()
                    .get("telegram.bot.url", "https://t.me/your_bot_username");
            
            // Проверяем поддержку Desktop API
            if (!Desktop.isDesktopSupported()) {
                showToast("Не удалось открыть браузер автоматически. Скопируйте ссылку: " + botUrl, Toast.ToastType.WARNING);
                logger.warn("Desktop API не поддерживается на этой платформе");
                return;
            }
            
            Desktop desktop = Desktop.getDesktop();
            if (!desktop.isSupported(Desktop.Action.BROWSE)) {
                showToast("Не удалось открыть браузер автоматически. Скопируйте ссылку: " + botUrl, Toast.ToastType.WARNING);
                logger.warn("Действие BROWSE не поддерживается");
                return;
            }
            
            desktop.browse(new URI(botUrl));
            showToast("Открыт Telegram бот в браузере", Toast.ToastType.INFO);
            logger.info("Открыт Telegram бот: {}", botUrl);
        } catch (java.net.URISyntaxException e) {
            logger.error("Неверный URL Telegram бота", e);
            showToast("Ошибка в настройках Telegram бота. Обратитесь к администратору.", Toast.ToastType.ERROR);
        } catch (java.io.IOException e) {
            logger.error("Ошибка при открытии браузера", e);
            showToast("Не удалось открыть браузер. Проверьте настройки системы.", Toast.ToastType.ERROR);
        } catch (Exception e) {
            logger.error("Ошибка при открытии Telegram бота", e);
            showToast("Не удалось открыть Telegram бота: " + e.getMessage(), Toast.ToastType.ERROR);
        }
    }
    
    /**
     * Обработка проверки 2FA
     */
    private void handleVerify2FA() {
        String code = twoFACodeField.getText().trim();
        
        // Валидация 2FA кода
        FieldValidator.ValidationResult codeResult = FieldValidator.validate2FACode(code);
        if (!codeResult.isValid()) {
            showToast(codeResult.getErrorMessage(), Toast.ToastType.ERROR);
            return;
        }
        
        if (currentUsername == null || currentUsername.isEmpty()) {
            showToast("Ошибка: имя пользователя не найдено", Toast.ToastType.ERROR);
            return;
        }
        
        verify2FAButton.setDisable(true);
        verify2FAButton.setText("Проверка...");
        
        authService.verify2FA(currentUsername, code, new AuthService.Verify2FACallback() {
            @Override
            public void onSuccess(String username, String token) {
                Platform.runLater(() -> {
                    verify2FAButton.setDisable(false);
                    verify2FAButton.setText("Проверить код");
                    
                    if (onAuthSuccess != null) {
                        java.util.Map<String, String> userData = new java.util.HashMap<>();
                        userData.put("username", username);
                        userData.put("accessToken", token);
                        onAuthSuccess.accept(userData);
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    verify2FAButton.setDisable(false);
                    verify2FAButton.setText("Проверить код");
                    showToast(getErrorMessage(error), Toast.ToastType.ERROR);
                    twoFACodeField.clear();
                });
            }
        });
    }
    
    /**
     * Показать уведомление через ToastManager
     */
    private void showToast(String message, Toast.ToastType type) {
        if (toastManager != null) {
            switch (type) {
                case SUCCESS:
                    toastManager.showSuccess(message);
                    break;
                case ERROR:
                    toastManager.showError(message);
                    break;
                case INFO:
                    toastManager.showInfo(message);
                    break;
                case WARNING:
                    toastManager.showWarning(message);
                    break;
            }
        } else {
            // Fallback на старую систему если ToastManager не инициализирован
            Platform.runLater(() -> {
                Toast toast = new Toast(message, type);
                toastContainer.getChildren().add(toast);
                
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(Duration.seconds(3));
                pause.setOnFinished(e -> {
                    toastContainer.getChildren().remove(toast);
                });
                pause.play();
            });
        }
    }
    
    /**
     * Получить понятное сообщение об ошибке с детальными причинами
     */
    private String getErrorMessage(String error) {
        if (error == null || error.isEmpty()) {
            return "Произошла неизвестная ошибка";
        }
        
        // Если сообщение уже детальное и понятное, возвращаем как есть
        if (error.length() > 30 && (error.contains(".") || error.contains(":") || error.contains("Проверьте"))) {
            return error;
        }
        
        String lowerError = error.toLowerCase();
        
        // Детальные сообщения об ошибках
        if (lowerError.contains("неверный логин") || lowerError.contains("неверный пароль") || 
            lowerError.contains("invalid password") || lowerError.contains("invalid login")) {
            return "Неверный логин или пароль. Проверьте правильность введенных данных.";
        } else if (lowerError.contains("логин занят") || lowerError.contains("username taken") || 
                   lowerError.contains("already exists") || lowerError.contains("уже существует")) {
            return "Пользователь с таким логином уже существует. Выберите другой логин.";
        } else if (lowerError.contains("код 2fa истек") || lowerError.contains("2fa code expired") ||
                   lowerError.contains("истек") || lowerError.contains("expired")) {
            return "Код 2FA истек. Запросите новый код.";
        } else if (lowerError.contains("неверный код") || lowerError.contains("invalid code") ||
                   lowerError.contains("код неверный")) {
            return "Неверный код 2FA. Проверьте код из Telegram и попробуйте снова.";
        } else if (lowerError.contains("пользователь не найден") || lowerError.contains("user not found") ||
                   lowerError.contains("не найден")) {
            return "Пользователь с таким логином не найден. Проверьте правильность ввода.";
        } else if (lowerError.contains("hwid заблокирован") || lowerError.contains("hwid blocked") || 
                   lowerError.contains("banned") || lowerError.contains("заблокирован")) {
            return "Ваш аккаунт или устройство заблокировано. Обратитесь к администратору.";
        } else if (lowerError.contains("telegram не привязан") || lowerError.contains("telegram not linked")) {
            return "Telegram не привязан к аккаунту. Сначала привяжите Telegram в настройках.";
        } else if (lowerError.contains("пароль должен") || lowerError.contains("password must")) {
            return error; // Уже детальное сообщение
        } else if (lowerError.contains("timeout") || lowerError.contains("превышено время")) {
            return "Превышено время ожидания ответа сервера. Проверьте подключение к интернету.";
        } else if (lowerError.contains("connection") || lowerError.contains("подключ")) {
            return "Не удалось подключиться к серверу. Убедитесь, что сервер запущен и доступен.";
        } else if (lowerError.contains("network") || lowerError.contains("сеть")) {
            return "Сеть недоступна. Проверьте подключение к интернету.";
        }
        
        // Если сообщение короткое и не содержит деталей, добавляем контекст
        if (error.length() < 30 && !error.contains(".")) {
            return error + ". Проверьте введенные данные и попробуйте снова.";
        }
        
        return error;
    }
    
    /**
     * Стиль для полей ввода
     */
    private String getFieldStyle() {
        return "-fx-background-color: rgba(255, 255, 255, 0.2); " +
               "-fx-background-radius: 15px; " +
               "-fx-border-color: rgba(255, 255, 255, 0.3); " +
               "-fx-border-width: 1px; " +
               "-fx-border-radius: 15px; " +
               "-fx-text-fill: white; " +
               "-fx-font-size: 16px; " +
               "-fx-padding: 15px 20px; " +
               "-fx-font-family: 'Minecraft Unicode', -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Segoe UI', Roboto, sans-serif;";
    }
    
    /**
     * Стиль для основных кнопок
     */
    private String getButtonStyle() {
        return "-fx-background-color: linear-gradient(to right, #667eea, #764ba2); " +
               "-fx-text-fill: white; " +
               "-fx-font-size: 16px; " +
               "-fx-font-weight: bold; " +
               "-fx-background-radius: 15px; " +
               "-fx-border-radius: 15px; " +
               "-fx-cursor: hand; " +
               "-fx-effect: dropshadow(gaussian, rgba(102, 126, 234, 0.5), 10, 0, 0, 5); " +
               "-fx-font-family: 'Minecraft Unicode', -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Segoe UI', Roboto, sans-serif;";
    }
    
    /**
     * Стиль для вторичных кнопок
     */
    private String getSecondaryButtonStyle() {
        return "-fx-background-color: rgba(255, 255, 255, 0.1); " +
               "-fx-text-fill: white; " +
               "-fx-font-size: 14px; " +
               "-fx-background-radius: 15px; " +
               "-fx-border-radius: 15px; " +
               "-fx-border-color: rgba(255, 255, 255, 0.3); " +
               "-fx-border-width: 1px; " +
               "-fx-cursor: hand; " +
               "-fx-font-family: 'Minecraft Unicode', -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Segoe UI', Roboto, sans-serif;";
    }
    
    /**
     * Стиль для ссылочных кнопок
     */
    private String getLinkButtonStyle() {
        return "-fx-background-color: transparent; " +
               "-fx-text-fill: rgba(255, 255, 255, 0.8); " +
               "-fx-font-size: 14px; " +
               "-fx-underline: true; " +
               "-fx-cursor: hand; " +
               "-fx-font-family: 'Minecraft Unicode', -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Segoe UI', Roboto, sans-serif;";
    }
    
    /**
     * Показать ошибку в лейбле
     */
    private void showError(Label errorLabel, String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        }
    }
    
    /**
     * Скрыть ошибку в лейбле
     */
    private void hideError(Label errorLabel) {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setText("");
        }
    }
    
    /**
     * Валидация сложности пароля
     */
    private void validatePasswordStrength(PasswordField passwordField, String password) {
        if (password == null || password.isEmpty()) {
            hideError(registerErrorLabel);
            return;
        }
        
        if (password.length() < 6) {
            showError(registerErrorLabel, "Пароль должен содержать минимум 6 символов");
        } else {
            hideError(registerErrorLabel);
        }
    }
    
    /**
     * Создать форму восстановления пароля
     */
    private void createRecoveryForm() {
        recoveryForm = new VBox(20);
        recoveryForm.setAlignment(Pos.CENTER);
        recoveryForm.setPadding(new Insets(40, 30, 40, 30));
        recoveryForm.setPrefWidth(400);
        
        // Заголовок
        Label titleLabel = new Label("Восстановление пароля");
        titleLabel.setStyle(
            "-fx-text-fill: white; " +
            "-fx-font-size: 28px; " +
            "-fx-font-weight: bold; " +
            "-fx-font-family: 'Minecraft Unicode', -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Segoe UI', Roboto, sans-serif;"
        );
        
        // Этап 1: Ввод логина
        recoveryUsernameField = new TextField();
        recoveryUsernameField.setPromptText("Логин");
        recoveryUsernameField.setStyle(getFieldStyle());
        recoveryUsernameField.setPrefHeight(50);
        recoveryUsernameField.setPrefWidth(320);
        
        // Кнопка запроса кода
        recoveryRequestCodeButton = new Button("Получить код в Telegram");
        recoveryRequestCodeButton.setStyle(getButtonStyle());
        recoveryRequestCodeButton.setPrefHeight(50);
        recoveryRequestCodeButton.setPrefWidth(320);
        recoveryRequestCodeButton.setOnAction(e -> handleRequestRecoveryCode());
        AnimationHelper.liquidPress(recoveryRequestCodeButton);
        
        // Этап 2: Ввод кода и нового пароля (изначально скрыт)
        Label step2Label = new Label("Введите код и новый пароль");
        step2Label.setStyle(
            "-fx-text-fill: rgba(255, 255, 255, 0.8); " +
            "-fx-font-size: 14px; " +
            "-fx-font-family: 'Minecraft Unicode', -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Segoe UI', Roboto, sans-serif;"
        );
        step2Label.setVisible(false);
        
        recoveryCodeField = new TextField();
        recoveryCodeField.setPromptText("Код из Telegram (6 цифр)");
        recoveryCodeField.setStyle(getFieldStyle());
        recoveryCodeField.setPrefHeight(50);
        recoveryCodeField.setPrefWidth(320);
        recoveryCodeField.setAlignment(Pos.CENTER);
        recoveryCodeField.setVisible(false);
        
        // Ограничение на 6 символов и только цифры
        recoveryCodeField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > 6) {
                recoveryCodeField.setText(oldValue);
            } else if (!newValue.matches("\\d*")) {
                recoveryCodeField.setText(oldValue);
            }
        });
        
        // Поле нового пароля
        recoveryNewPasswordField = new PasswordField();
        recoveryNewPasswordField.setPromptText("Новый пароль");
        recoveryNewPasswordField.setStyle(getFieldStyle());
        recoveryNewPasswordField.setPrefHeight(50);
        recoveryNewPasswordField.setPrefWidth(320);
        recoveryNewPasswordField.setVisible(false);
        
        recoveryNewPasswordVisibleField = new TextField();
        recoveryNewPasswordVisibleField.setPromptText("Новый пароль");
        recoveryPasswordToggle = new Button();
        HBox recoveryPasswordContainer = createPasswordFieldWithToggle(recoveryNewPasswordField, recoveryNewPasswordVisibleField, recoveryPasswordToggle);
        recoveryPasswordContainer.setVisible(false);
        
        // Лейбл ошибки
        recoveryErrorLabel = new Label();
        recoveryErrorLabel.setStyle(
            "-fx-text-fill: rgba(255, 100, 100, 0.9); " +
            "-fx-font-size: 12px; " +
            "-fx-wrap-text: true; " +
            "-fx-max-width: 320px; " +
            "-fx-font-family: 'Minecraft Unicode', -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Segoe UI', Roboto, sans-serif;"
        );
        recoveryErrorLabel.setVisible(false);
        
        // Кнопка сброса пароля
        recoveryResetButton = new Button("Восстановить пароль");
        recoveryResetButton.setStyle(getButtonStyle());
        recoveryResetButton.setPrefHeight(50);
        recoveryResetButton.setPrefWidth(320);
        recoveryResetButton.setVisible(false);
        recoveryResetButton.setOnAction(e -> handleResetPassword());
        AnimationHelper.liquidPress(recoveryResetButton);
        
        // Кнопка отмены
        recoveryCancelButton = new Button("Отмена");
        recoveryCancelButton.setStyle(getSecondaryButtonStyle());
        recoveryCancelButton.setPrefHeight(45);
        recoveryCancelButton.setPrefWidth(320);
        recoveryCancelButton.setOnAction(e -> switchToLogin());
        
        recoveryForm.getChildren().addAll(
            titleLabel,
            recoveryUsernameField,
            recoveryRequestCodeButton,
            step2Label,
            recoveryCodeField,
            recoveryPasswordContainer,
            recoveryErrorLabel,
            recoveryResetButton,
            recoveryCancelButton
        );
    }
    
    /**
     * Переключение на форму восстановления пароля
     */
    private void switchToRecovery() {
        if (this.getChildren().isEmpty()) {
            showForm(recoveryForm);
            recoveryUsernameField.requestFocus();
            return;
        }
        
        StackPane glassPane = (StackPane) this.getChildren().get(0);
        if (glassPane.getChildren().isEmpty()) {
            showForm(recoveryForm);
            recoveryUsernameField.requestFocus();
            return;
        }
        
        VBox currentForm = (VBox) glassPane.getChildren().get(0);
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), currentForm);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            showForm(recoveryForm);
            recoveryStep2 = false;
            resetRecoveryForm();
            recoveryUsernameField.requestFocus();
        });
        fadeOut.play();
    }
    
    /**
     * Сброс формы восстановления пароля
     */
    private void resetRecoveryForm() {
        recoveryStep2 = false;
        recoveryUsernameField.setVisible(true);
        recoveryRequestCodeButton.setVisible(true);
        recoveryRequestCodeButton.setDisable(false);
        recoveryRequestCodeButton.setText("Получить код в Telegram");
        
        // Скрываем этап 2
        for (javafx.scene.Node node : recoveryForm.getChildren()) {
            if (node instanceof Label && "step2Label".equals(node.getId())) {
                node.setVisible(false);
            }
        }
        recoveryCodeField.setVisible(false);
        recoveryNewPasswordField.setVisible(false);
        recoveryNewPasswordVisibleField.setVisible(false);
        recoveryPasswordToggle.setVisible(false);
        recoveryResetButton.setVisible(false);
        hideError(recoveryErrorLabel);
        
        recoveryUsernameField.clear();
        recoveryCodeField.clear();
        recoveryNewPasswordField.clear();
    }
    
    /**
     * Обработка запроса кода восстановления
     */
    private void handleRequestRecoveryCode() {
        String username = recoveryUsernameField.getText().trim();
        
        if (username.isEmpty()) {
            showError(recoveryErrorLabel, "Введите логин");
            AnimationHelper.shake(recoveryForm);
            return;
        }
        
        recoveryRequestCodeButton.setDisable(true);
        recoveryRequestCodeButton.setText("Отправка...");
        hideError(recoveryErrorLabel);
        
        authService.requestRecoveryCode(username, new AuthService.RequestRecoveryCodeCallback() {
            @Override
            public void onSuccess() {
                Platform.runLater(() -> {
                    recoveryRequestCodeButton.setDisable(false);
                    recoveryRequestCodeButton.setText("Получить код в Telegram");
                    
                    // Переходим на этап 2
                    recoveryStep2 = true;
                    recoveryUsernameField.setVisible(false);
                    recoveryRequestCodeButton.setVisible(false);
                    
                    // Показываем этап 2
                    for (javafx.scene.Node node : recoveryForm.getChildren()) {
                        if (node instanceof Label && "step2Label".equals(node.getId())) {
                            node.setVisible(true);
                        }
                    }
                    recoveryCodeField.setVisible(true);
                    recoveryNewPasswordField.setVisible(true);
                    recoveryPasswordToggle.setVisible(true);
                    recoveryResetButton.setVisible(true);
                    
                    recoveryCodeField.clear();
                    recoveryNewPasswordField.clear();
                    Platform.runLater(() -> recoveryCodeField.requestFocus());
                    
                    showToast("Код отправлен в Telegram", Toast.ToastType.SUCCESS);
                });
            }
            
            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    recoveryRequestCodeButton.setDisable(false);
                    recoveryRequestCodeButton.setText("Получить код в Telegram");
                    String errorMessage = getErrorMessage(error);
                    showError(recoveryErrorLabel, errorMessage);
                    AnimationHelper.shake(recoveryForm);
                    showToast(errorMessage, Toast.ToastType.ERROR);
                });
            }
        });
    }
    
    /**
     * Обработка восстановления пароля
     */
    private void handleResetPassword() {
        String username = recoveryUsernameField.getText().trim();
        String code = recoveryCodeField.getText().trim();
        String newPassword = recoveryNewPasswordField.getText();
        
        if (code.isEmpty()) {
            showError(recoveryErrorLabel, "Введите код из Telegram");
            AnimationHelper.shake(recoveryForm);
            recoveryCodeField.requestFocus();
            return;
        }
        
        if (code.length() != 6) {
            showError(recoveryErrorLabel, "Код должен содержать 6 цифр. Проверьте код из Telegram.");
            AnimationHelper.shake(recoveryForm);
            recoveryCodeField.requestFocus();
            return;
        }
        
        if (newPassword.isEmpty()) {
            showError(recoveryErrorLabel, "Введите новый пароль");
            AnimationHelper.shake(recoveryForm);
            recoveryNewPasswordField.requestFocus();
            return;
        }
        
        if (newPassword.length() < 6) {
            showError(recoveryErrorLabel, "Пароль должен содержать минимум 6 символов");
            AnimationHelper.shake(recoveryForm);
            recoveryNewPasswordField.requestFocus();
            return;
        }
        
        recoveryResetButton.setDisable(true);
        recoveryResetButton.setText("Восстановление...");
        hideError(recoveryErrorLabel);
        
        authService.resetPassword(username, code, newPassword, new AuthService.ResetPasswordCallback() {
            @Override
            public void onSuccess() {
                Platform.runLater(() -> {
                    recoveryResetButton.setDisable(false);
                    recoveryResetButton.setText("Восстановить пароль");
                    showToast("Пароль успешно восстановлен!", Toast.ToastType.SUCCESS);
                    
                    // Переключаемся на форму логина
                    switchToLogin();
                });
            }
            
            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    recoveryResetButton.setDisable(false);
                    recoveryResetButton.setText("Восстановить пароль");
                    String errorMessage = getErrorMessage(error);
                    showError(recoveryErrorLabel, errorMessage);
                    AnimationHelper.shake(recoveryForm);
                    showToast(errorMessage, Toast.ToastType.ERROR);
                    recoveryCodeField.clear();
                });
            }
        });
    }
}
