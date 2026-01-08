package com.horizon.launcher.ui;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Гибридный лаунчер, использующий React UI через WebView
 * 
 * Особенности:
 * - Отображение React приложения через JavaFX WebView
 * - Двусторонняя связь между Java и JavaScript через JSBridge
 * - Обработка запусков игры из веб-интерфейса
 * - Доступ к Java API из React (авторизация, скины, настройки)
 */
public class HybridLauncherWindow {
    private static final Logger logger = LoggerFactory.getLogger(HybridLauncherWindow.class);
    
    private Stage stage;
    private WebView webView;
    private WebEngine webEngine;
    private String username;
    
    private double xOffset = 0;
    private double yOffset = 0;
    
    public HybridLauncherWindow(String username) {
        this.username = username;
        createWindow();
    }
    
    private void createWindow() {
        stage = new Stage();
        stage.setTitle("Horizon Launcher - " + username);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setWidth(1280);
        stage.setHeight(800);
        
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: transparent;");
        
        // Создаем WebView для React приложения
        webView = new WebView();
        webEngine = webView.getEngine();
        
        // Включаем JavaScript
        webEngine.setJavaScriptEnabled(true);
        
        // Отключаем контекстное меню
        webView.setContextMenuEnabled(false);
        
        // Логирование JavaScript ошибок
        webEngine.setOnError(event -> {
            logger.error("WebView JavaScript ERROR: {}", event.getMessage());
        });
        
        // Логирование консольных сообщений
        webEngine.setOnAlert(event -> {
            logger.info("WebView Alert: {}", event.getData());
        });
        
        // Обработка состояния загрузки
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                logger.info("React UI загружен успешно");
                logger.info("Текущий URL: {}", webEngine.getLocation());
                injectJavaBridge();
            } else if (newState == Worker.State.FAILED) {
                logger.error("Ошибка загрузки React UI");
                Throwable exception = webEngine.getLoadWorker().getException();
                if (exception != null) {
                    logger.error("Exception при загрузке:", exception);
                }
            }
        });
        
        // Загружаем React UI
        loadReactUI();
        
        root.setCenter(webView);
        
        // Draggable window
        setupDraggableWindow(root);
        
        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        
        stage.setScene(scene);
    }
    
    /**
     * Загружаем React UI из dev сервера или production build.
     * Важно: выполняем сетевую проверку в отдельном потоке, чтобы не фризить JavaFX.
     */
    private void loadReactUI() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            // ПРИОРИТЕТ 1: Dev сервер (проверяем порты 5173, 5174, 5175)
            String[] devPorts = {"5173", "5174", "5175"};
            for (String port : devPorts) {
                String devServerUrl = "http://localhost:" + port;
                if (isDevServerAvailable(devServerUrl)) {
                    String finalUrl = devServerUrl + "/#/dashboard";
                    Platform.runLater(() -> {
                        logger.info("🚀 ПОДКЛЮЧЕНО к dev серверу: {}", finalUrl);
                        webEngine.load(finalUrl);
                    });
                    executor.shutdown();
                    return;
                }
            }
            
            logger.warn("Dev сервер недоступен на портах 5173-5175, пробуем production build...");

            // ПРИОРИТЕТ 2: Production build (для дистрибуции)
            File distFile = new File("horizon-ui/dist/index.html");
            if (!distFile.exists()) {
                distFile = new File("../horizon-ui/dist/index.html");
            }
            
            if (distFile.exists()) {
                String url = "file:///" + distFile.getAbsolutePath().replace("\\", "/");
                final String finalUrl = url;
                Platform.runLater(() -> {
                    logger.info("📦 Загружаем production build: {}", finalUrl);
                    webEngine.load(finalUrl);
                });
                executor.shutdown();
                return;
            }

            // ОШИБКА: ничего не найдено
            Platform.runLater(() -> {
                logger.error("❌ НИЧЕГО НЕ НАЙДЕНО!");
                logger.error("Production build путь: {}", new File("horizon-ui/dist/index.html").getAbsolutePath());
                logger.error("Dev сервер: http://localhost:5173/ (недоступен)");
                
                showError("⚠️ React UI НЕ НАЙДЕН!\n\n" +
                         "РЕШЕНИЕ:\n\n" +
                         "1) Откройте НОВЫЙ терминал\n\n" +
                         "2) Выполните команды:\n" +
                         "   cd C:\\Users\\skviz\\Desktop\\Horizon\\horizon-ui\n" +
                         "   npm run dev\n\n" +
                         "3) Дождитесь сообщения:\n" +
                         "   VITE v6.3.5  ready in XXX ms\n" +
                         "   ➜  Local:   http://localhost:5173/\n\n" +
                         "4) Перезапустите лаунчер\n\n" +
                         "ВАЖНО: Dev сервер должен работать ДО запуска лаунчера!");
            });
            executor.shutdown();
        });
    }

    private boolean isDevServerAvailable(String urlString) {
        try {
            java.net.URL url = new java.net.URL(urlString + "/");
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(1500);
            connection.setReadTimeout(1500);
            connection.connect();
            int code = connection.getResponseCode();
            connection.disconnect();
            return code == 200;
        } catch (Exception e) {
            logger.debug("Dev сервер недоступен: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Внедряем Java мост для связи с JavaScript
     */
    private void injectJavaBridge() {
        JSObject window = (JSObject) webEngine.executeScript("window");
        window.setMember("javaBridge", new JavaBridge());
        
        // Передаем username в React
        webEngine.executeScript(String.format(
            "window.username = '%s'; console.log('Username set:', window.username);",
            username
        ));
        
        logger.info("Java Bridge внедрен");
    }
    
    /**
     * Мост между Java и JavaScript
     */
    public class JavaBridge {
        
        /**
         * Авторизация через Microsoft/Mojang (лицензия)
         */
        public boolean authenticateLicense(String email, String password) {
            try {
                logger.info("Попытка авторизации Microsoft для: {}", email);
                // TODO: Интеграция с MicrosoftAuthService
                // Временно возвращаем true для тестирования
                return true;
            } catch (Exception e) {
                logger.error("Ошибка авторизации Microsoft", e);
                return false;
            }
        }
        
        /**
         * Авторизация пиратская (никнейм)
         */
        public boolean authenticateCracked(String username) {
            try {
                logger.info("Попытка авторизации Cracked для: {}", username);
                // TODO: Интеграция с AuthService для пиратских аккаунтов
                // Временно возвращаем true для тестирования
                return true;
            } catch (Exception e) {
                logger.error("Ошибка пиратской авторизации", e);
                return false;
            }
        }
        
        /**
         * Запуск игры с указанным сервером
         */
        public void launchGame(String serverName) {
            Platform.runLater(() -> {
                logger.info("Запуск игры на сервере: {}", serverName);
                // TODO: Интеграция с MinecraftLauncher
            });
        }
        
        /**
         * Загрузка скина пользователя
         */
        public void uploadSkin(String skinPath, String model) {
            Platform.runLater(() -> {
                logger.info("Загрузка скина: {} (модель: {})", skinPath, model);
                // TODO: Интеграция с SkinService
            });
        }
        
        /**
         * Покупка товара в магазине
         */
        public void purchaseItem(int itemId, int price) {
            Platform.runLater(() -> {
                logger.info("Покупка товара #{} за {} монет", itemId, price);
                // TODO: Интеграция с ShopService
            });
        }

        /**
         * Создание темы на форуме
         */
        public void createForumThread(String title, String body) {
            Platform.runLater(() -> {
                logger.info("Создание темы форума: {} -> {}", title, body);
                // TODO: Интеграция с ForumService
            });
        }
        
        /**
         * Сохранение настроек
         */
        public void saveSettings(String settingsJson) {
            Platform.runLater(() -> {
                logger.info("Сохранение настроек: {}", settingsJson);
                // TODO: Парсинг JSON и сохранение в ConfigManager
            });
        }
        
        /**
         * Выход из аккаунта
         */
        public void logout() {
            Platform.runLater(() -> {
                logger.info("Выход из аккаунта");
                stage.close();
                // TODO: Открыть окно авторизации
            });
        }
        
        /**
         * Закрыть лаунчер
         */
        public void closeWindow() {
            Platform.runLater(() -> {
                stage.close();
            });
        }
        
        /**
         * Минимизировать лаунчер
         */
        public void minimizeWindow() {
            Platform.runLater(() -> {
                stage.setIconified(true);
            });
        }
        
        /**
         * Развернуть/свернуть лаунчер
         */
        public void toggleMaximize() {
            Platform.runLater(() -> {
                stage.setMaximized(!stage.isMaximized());
            });
        }
        
        /**
         * Логирование из JavaScript
         */
        public void log(String message) {
            logger.info("[JS] {}", message);
        }
    }
    
    /**
     * Настройка перетаскивания окна
     */
    private void setupDraggableWindow(BorderPane root) {
        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        
        root.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }
    
    private void showError(String message) {
        Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR
            );
            alert.setTitle("Ошибка");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    public void show() {
        stage.show();
    }
    
    public Stage getStage() {
        return stage;
    }
}

