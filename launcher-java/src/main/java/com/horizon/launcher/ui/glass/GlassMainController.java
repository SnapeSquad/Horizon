package com.horizon.launcher.ui.glass;

import animatefx.animation.SlideInDown;
import com.horizon.launcher.api.AuthService;
import com.horizon.launcher.ui.StyledMainWindow;
import com.horizon.launcher.ui.HybridLauncherWindow;
import com.horizon.launcher.ui.RegisterWindow;
import com.horizon.launcher.util.ValidationUtils;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Главный контроллер для Sky-Turquoise Glass UI
 * 
 * Особенности:
 * - Draggable frameless window
 * - Elastic entrance animation
 * - Spring transitions
 */
public class GlassMainController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(GlassMainController.class);
    
    private Stage stage;
    private double xOffset = 0;
    private double yOffset = 0;
    
    @FXML
    private StackPane rootStackPane;
    
    @FXML
    private AnchorPane mainContainer;
    
    @FXML
    private StackPane glassLoginBox;
    
    @FXML
    private TextField usernameField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private TextField twoFactorField;
    
    @FXML
    private Button loginButton;
    
    @FXML
    private Hyperlink registerLink;
    
    @FXML
    private Rectangle backgroundGradient;
    
    @FXML
    private ToggleButton licenseToggle;
    
    @FXML
    private ToggleButton crackToggle;
    
    @FXML
    private Button microsoftButton;
    
    private AuthService authService;
    private boolean requires2FA = false;
    private boolean isLicenseMode = true;
    
    /**
     * Инициализация контроллера
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Инициализация GlassMainController");
        
        this.authService = new AuthService();
        
        // Устанавливаем градиент программно, если FXML не загрузил его
        setupBackgroundGradient();
        
        // Попытка загрузить фоновое изображение (опционально)
        loadBackgroundImage();
        
        // Настройка формы авторизации
        setupAuthForm();
        
        // Устанавливаем обработчики событий после загрузки FXML
        Platform.runLater(() -> {
            setupDragHandlers();
            playEntranceAnimation();
        });
    }
    
    /**
     * Настраивает градиентный фон программно (fallback если FXML не работает)
     */
    private void setupBackgroundGradient() {
        if (backgroundGradient == null) {
            return;
        }
        
        try {
            // Используем темный фон из Figma дизайна
            backgroundGradient.setFill(Color.web("#0F0F13"));
        } catch (Exception e) {
            logger.warn("Не удалось установить градиент, используется черный цвет", e);
            backgroundGradient.setFill(Color.BLACK);
        }
    }
    
    /**
     * Загружает фоновое изображение, если оно доступно
     */
    private void loadBackgroundImage() {
        if (rootStackPane == null) {
            return;
        }
        
        try {
            String imagePath = "/images/nebula-background.png";
            java.io.InputStream imageStream = getClass().getResourceAsStream(imagePath);
            
            if (imageStream == null) {
                // Изображение не найдено - используем gradient fallback
                logger.debug("Фоновое изображение не найдено (ресурс {} не существует), используется gradient", imagePath);
                return;
            }
            
            Image backgroundImage = new Image(imageStream);
            
            // Проверяем, что изображение загрузилось без ошибок
            if (!backgroundImage.isError() && backgroundImage.getWidth() > 0) {
                ImageView imageView = new ImageView(backgroundImage);
                imageView.setFitWidth(1400);
                imageView.setFitHeight(900);
                imageView.setPreserveRatio(false);
                
                // Вставляем изображение перед gradient (на нижний слой)
                rootStackPane.getChildren().add(0, imageView);
                logger.info("Фоновое изображение успешно загружено");
            } else {
                logger.debug("Ошибка загрузки фонового изображения, используется gradient");
            }
        } catch (Exception e) {
            // Изображение не найдено или ошибка загрузки - используем gradient fallback
            logger.debug("Фоновое изображение не найдено, используется gradient: {}", e.getMessage());
        }
    }
    
    /**
     * Настройка формы авторизации
     */
    private void setupAuthForm() {
        // Настройка переключателя лицензия/пиратка
        setupModeToggle();
        
        // Обработчик кнопки входа
        if (loginButton != null) {
            loginButton.setOnAction(e -> handleLogin());
        }
        
        // Обработчик ссылки регистрации
        if (registerLink != null) {
            registerLink.setOnAction(e -> handleRegister());
        }
        
        // Обработчик Enter в полях ввода
        if (usernameField != null) {
            usernameField.setOnAction(e -> passwordField.requestFocus());
        }
        
        if (passwordField != null) {
            passwordField.setOnAction(e -> {
                if (requires2FA && twoFactorField != null && twoFactorField.isVisible()) {
                    twoFactorField.requestFocus();
                } else {
                    handleLogin();
                }
            });
        }
        
        if (twoFactorField != null) {
            twoFactorField.setOnAction(e -> handleLogin());
            twoFactorField.textProperty().addListener((obs, oldVal, newVal) -> {
                // Форматирование: только цифры, максимум 6
                if (newVal != null && !newVal.matches("\\d{0,6}")) {
                    twoFactorField.setText(oldVal);
                }
            });
        }
    }
    
    /**
     * Обработка входа
     */
    private void handleLogin() {
        if (usernameField == null || passwordField == null) {
            return;
        }
        
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String twoFactorCode = requires2FA && twoFactorField != null ? 
                twoFactorField.getText().trim() : null;
        
        // Валидация
        ValidationUtils.ValidationResult usernameValidation = 
                ValidationUtils.validateUsername(username);
        if (!usernameValidation.isValid()) {
            showError(usernameValidation.getMessage());
            return;
        }
        
        if (password.isEmpty()) {
            showError("Пароль не может быть пустым");
            return;
        }
        
        if (requires2FA && twoFactorCode != null) {
            ValidationUtils.ValidationResult codeValidation = 
                    ValidationUtils.validate2FACode(twoFactorCode);
            if (!codeValidation.isValid()) {
                showError(codeValidation.getMessage());
                return;
            }
        }
        
        // Блокируем кнопку
        if (loginButton != null) {
            loginButton.setDisable(true);
            loginButton.setText("Вход...");
        }
        
        // Выполняем вход в отдельном потоке
        new Thread(() -> {
            AuthService.AuthResult result = authService.login(username, password, twoFactorCode);
            
            Platform.runLater(() -> {
                if (loginButton != null) {
                    loginButton.setDisable(false);
                    loginButton.setText("Sign In");
                }
                
                if (result.isSuccess()) {
                    showSuccess("Вход выполнен успешно!");
                    // Переход на главное окно
                    openMainWindow(result.getUsername());
                } else {
                    if (result.requires2FA()) {
                        requires2FA = true;
                        show2FAField();
                        showInfo("Введите код 2FA");
                    } else {
                        showError(result.getMessage());
                    }
                }
            });
        }).start();
    }
    
    /**
     * Обработка регистрации
     */
    private void handleRegister() {
        openRegisterWindow();
    }
    
    /**
     * Открывает главное окно лаунчера
     */
    private void openMainWindow(String username) {
        Platform.runLater(() -> {
            try {
                // Закрываем окно входа
                if (stage != null) {
                    stage.close();
                }
                
                // Открываем новый гибридный лаунчер с React UI
                logger.info("Запуск гибридного лаунчера (React + JavaFX)");
                HybridLauncherWindow hybridWindow = new HybridLauncherWindow(username);
                hybridWindow.show();
                
                logger.info("Гибридный лаунчер открыт для пользователя: {}", username);
            } catch (Exception e) {
                logger.error("Ошибка гибридного лаунчера, fallback на StyledMainWindow", e);
                
                // Fallback на старый интерфейс
                try {
                    StyledMainWindow mainWindow = new StyledMainWindow(username);
                    mainWindow.show();
                    logger.info("Fallback: Главное окно открыто для пользователя: {}", username);
                } catch (Exception ex) {
                    logger.error("Ошибка при открытии главного окна", ex);
                    showError("Ошибка при открытии главного окна: " + ex.getMessage());
                }
            }
        });
    }
    
    /**
     * Открывает окно регистрации
     */
    private void openRegisterWindow() {
        Platform.runLater(() -> {
            try {
                RegisterWindow registerWindow = new RegisterWindow(stage);
                registerWindow.show();
                logger.info("Окно регистрации открыто");
            } catch (Exception e) {
                logger.error("Ошибка при открытии окна регистрации", e);
                showError("Ошибка при открытии окна регистрации: " + e.getMessage());
            }
        });
    }
    
    /**
     * Показывает поле 2FA
     */
    private void show2FAField() {
        if (twoFactorField != null) {
            twoFactorField.setVisible(true);
            twoFactorField.setManaged(true);
            twoFactorField.requestFocus();
        }
    }
    
    /**
     * Показывает сообщение об ошибке
     */
    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            alert.showAndWait();
        });
    }
    
    /**
     * Показывает информационное сообщение
     */
    private void showInfo(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Информация");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            alert.showAndWait();
        });
    }
    
    /**
     * Показывает сообщение об успехе
     */
    private void showSuccess(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Успех");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            alert.showAndWait();
        });
    }
    
    /**
     * Устанавливает stage для управления окном
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }
    
    /**
     * Настройка обработчиков для перетаскивания окна
     */
    private void setupDragHandlers() {
        if (rootStackPane == null || stage == null) {
            return;
        }
        
        // Обработчик для начала перетаскивания
        rootStackPane.setOnMousePressed(event -> {
            if (isDraggableArea(event.getTarget())) {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            }
        });
        
        // Обработчик для перетаскивания
        rootStackPane.setOnMouseDragged(event -> {
            if (xOffset != 0 || yOffset != 0) {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            }
        });
        
        // Сброс offset при отпускании мыши
        rootStackPane.setOnMouseReleased(event -> {
            xOffset = 0;
            yOffset = 0;
        });
        
        // Также добавляем обработчики на mainContainer
        if (mainContainer != null) {
            mainContainer.setOnMousePressed(event -> {
                if (isDraggableArea(event.getTarget())) {
                    xOffset = event.getSceneX();
                    yOffset = event.getSceneY();
                }
            });
            
            mainContainer.setOnMouseDragged(event -> {
                if (xOffset != 0 || yOffset != 0) {
                    stage.setX(event.getScreenX() - xOffset);
                    stage.setY(event.getScreenY() - yOffset);
                }
            });
            
            mainContainer.setOnMouseReleased(event -> {
                xOffset = 0;
                yOffset = 0;
            });
        }
    }
    
    /**
     * Проверяет, можно ли перетаскивать окно с этой области
     */
    private boolean isDraggableArea(Object target) {
        if (target == null) return false;
        
        // Не позволяем перетаскивать с интерактивных элементов
        String className = target.getClass().getSimpleName();
        if (className.contains("Button") || 
            className.contains("TextField") || 
            className.contains("PasswordField") ||
            className.contains("Hyperlink") ||
            className.contains("Label")) {
            return false;
        }
        
        // Разрешаем перетаскивание с фоновых элементов
        return className.contains("Rectangle") || 
               className.contains("ImageView") ||
               className.contains("StackPane") ||
               className.contains("AnchorPane") ||
               className.contains("VBox") ||
               className.contains("Region");
    }
    
    /**
     * Воспроизводит входную анимацию с Elastic эффектом
     */
    private void playEntranceAnimation() {
        if (rootStackPane == null) {
            return;
        }
        
        // Начальное состояние: масштаб 0.9 и прозрачность 0
        rootStackPane.setScaleX(0.9);
        rootStackPane.setScaleY(0.9);
        rootStackPane.setOpacity(0.0);
        
        // Создаем Elastic анимацию масштаба
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(800), rootStackPane);
        scaleTransition.setFromX(0.9);
        scaleTransition.setFromY(0.9);
        scaleTransition.setToX(1.0);
        scaleTransition.setToY(1.0);
        scaleTransition.setInterpolator(Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0)); // Elastic-like
        
        // Fade in анимация
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(600), rootStackPane);
        fadeTransition.setFromValue(0.0);
        fadeTransition.setToValue(1.0);
        
        // Комбинируем анимации
        ParallelTransition parallelTransition = new ParallelTransition(scaleTransition, fadeTransition);
        parallelTransition.play();
        
        // Анимация для стеклянной карточки входа
        if (glassLoginBox != null) {
            Platform.runLater(() -> {
                new SlideInDown(glassLoginBox)
                    .setSpeed(0.8)
                    .setDelay(Duration.millis(200))
                    .play();
            });
        }
    }
    
    /**
     * Закрытие приложения с анимацией
     */
    /**
     * Настройка переключателя режимов (Лицензия/Пиратка)
     */
    private void setupModeToggle() {
        if (licenseToggle == null || crackToggle == null) {
            logger.warn("Переключатели режимов не найдены в FXML");
            return;
        }
        
        // Устанавливаем начальное состояние
        licenseToggle.setSelected(true);
        crackToggle.setSelected(false);
        updateUIForMode(true);
        
        // Логика переключения (только один может быть выбран)
        licenseToggle.setOnAction(e -> {
            if (licenseToggle.isSelected()) {
                crackToggle.setSelected(false);
                isLicenseMode = true;
                updateUIForMode(true);
                logger.info("Режим: Лицензия");
            } else {
                licenseToggle.setSelected(true);
            }
        });
        
        crackToggle.setOnAction(e -> {
            if (crackToggle.isSelected()) {
                licenseToggle.setSelected(false);
                isLicenseMode = false;
                updateUIForMode(false);
                logger.info("Режим: Пиратка");
            } else {
                crackToggle.setSelected(true);
            }
        });
        
        // Обработчик кнопки Microsoft
        if (microsoftButton != null) {
            microsoftButton.setOnAction(e -> handleMicrosoftLogin());
        }
    }
    
    /**
     * Обновляет UI в зависимости от выбранного режима
     */
    private void updateUIForMode(boolean isLicense) {
        if (isLicense) {
            // Режим лицензии
            usernameField.setPromptText("Email или логин");
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            if (microsoftButton != null) {
                microsoftButton.setVisible(true);
                microsoftButton.setManaged(true);
            }
            if (registerLink != null) {
                registerLink.setVisible(false);
                registerLink.setManaged(false);
            }
            if (loginButton != null) {
                loginButton.setText("Войти (свой сервер)");
            }
        } else {
            // Режим пиратки
            usernameField.setPromptText("Никнейм (3-16 символов)");
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordField.setPromptText("Пароль");
            if (microsoftButton != null) {
                microsoftButton.setVisible(false);
                microsoftButton.setManaged(false);
            }
            if (registerLink != null) {
                registerLink.setVisible(true);
                registerLink.setManaged(true);
            }
            if (loginButton != null) {
                loginButton.setText("Войти");
            }
        }
    }
    
    /**
     * Обработка входа через Microsoft
     */
    private void handleMicrosoftLogin() {
        logger.info("Начало входа через Microsoft OAuth");
        
        if (loginButton != null) {
            loginButton.setDisable(true);
        }
        if (microsoftButton != null) {
            microsoftButton.setDisable(true);
            microsoftButton.setText("Открытие браузера...");
        }
        
        Platform.runLater(() -> {
            try {
                com.horizon.launcher.auth.MicrosoftAuthService microsoftAuth = 
                        com.horizon.launcher.auth.MicrosoftAuthService.getInstance();
                
                microsoftAuth.authenticate().thenAccept(result -> {
                    Platform.runLater(() -> {
                        if (result.isSuccess()) {
                            logger.info("Microsoft авторизация успешна: {}", result.getMinecraftUsername());
                            openMainWindow(result.getMinecraftUsername());
                        } else {
                            showError("Ошибка Microsoft входа: " + result.getMessage());
                            if (microsoftButton != null) {
                                microsoftButton.setDisable(false);
                                microsoftButton.setText("Войти через Microsoft");
                            }
                        }
                        if (loginButton != null) {
                            loginButton.setDisable(false);
                        }
                    });
                }).exceptionally(ex -> {
                    Platform.runLater(() -> {
                        logger.error("Ошибка Microsoft авторизации", ex);
                        showError("Ошибка подключения к Microsoft: " + ex.getMessage());
                        if (microsoftButton != null) {
                            microsoftButton.setDisable(false);
                            microsoftButton.setText("Войти через Microsoft");
                        }
                        if (loginButton != null) {
                            loginButton.setDisable(false);
                        }
                    });
                    return null;
                });
            } catch (Exception e) {
                logger.error("Ошибка запуска Microsoft авторизации", e);
                showError("Не удалось запустить Microsoft авторизацию");
                if (microsoftButton != null) {
                    microsoftButton.setDisable(false);
                    microsoftButton.setText("Войти через Microsoft");
                }
                if (loginButton != null) {
                    loginButton.setDisable(false);
                }
            }
        });
    }
    
    @FXML
    private void handleClose() {
        if (rootStackPane == null || stage == null) {
            Platform.exit();
            return;
        }
        
        // Fade out анимация перед закрытием
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), rootStackPane);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            Platform.exit();
            System.exit(0);
        });
        fadeOut.play();
    }
    
    /**
     * Сворачивание окна
     */
    @FXML
    private void handleMinimize() {
        if (stage != null) {
            stage.setIconified(true);
        }
    }
    
    /**
     * Максимизация/восстановление окна
     */
    @FXML
    private void handleMaximize() {
        if (stage != null) {
            if (stage.isMaximized()) {
                stage.setMaximized(false);
            } else {
                stage.setMaximized(true);
            }
        }
    }
}
