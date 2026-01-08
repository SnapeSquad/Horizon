package com.horizon.launcher.ui;

import com.horizon.launcher.minecraft.GameLauncher;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Главное окно лаунчера
 */
public class MainWindow {
    private static final Logger logger = LoggerFactory.getLogger(MainWindow.class);
    private Stage stage;
    private String username;
    private GameLauncher gameLauncher;
    private String selectedServer = null;
    private int ramInGB = 4;

    public MainWindow(String username) {
        this.username = username;
        this.gameLauncher = new GameLauncher();
        createWindow();
    }

    private void createWindow() {
        stage = new Stage();
        stage.setTitle("Horizon Launcher");
        stage.setWidth(1400);
        stage.setHeight(900);
        stage.setMinWidth(1200);
        stage.setMinHeight(800);
        stage.initStyle(StageStyle.UNDECORATED);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #87CEEB 0%, #B0E0E6 50%, #E0F6FF 100%);");

        // Боковая панель
        VBox sidebar = createSidebar();
        root.setLeft(sidebar);

        // Основной контент
        VBox mainContent = createMainContent();
        root.setCenter(mainContent);

        // Кнопки управления окном
        HBox windowControls = createWindowControls();
        root.setTop(windowControls);

        Scene scene = new Scene(root);
        stage.setScene(scene);
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.setPrefWidth(80);
        sidebar.setPadding(new Insets(20, 12, 20, 12));
        sidebar.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-backdrop-filter: blur(20px);");

        Button mainBtn = createNavButton("Главная", "🏠");
        Button serversBtn = createNavButton("Серверы", "🌐");
        Button wardrobeBtn = createNavButton("Гардероб", "👕");
        Button notificationsBtn = createNavButton("Уведомления", "🔔");

        sidebar.getChildren().addAll(mainBtn, serversBtn, wardrobeBtn, notificationsBtn);
        return sidebar;
    }

    private Button createNavButton(String tooltip, String icon) {
        Button btn = new Button(icon);
        btn.setPrefWidth(56);
        btn.setPrefHeight(56);
        btn.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 10; " +
                    "-fx-text-fill: white; -fx-font-size: 20px;");
        btn.setTooltip(new Tooltip(tooltip));
        return btn;
    }

    private HBox createWindowControls() {
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.TOP_RIGHT);
        controls.setPadding(new Insets(10));

        Button minimizeBtn = new Button("−");
        minimizeBtn.setStyle("-fx-background-color: rgba(0,0,0,0.1); -fx-text-fill: white; -fx-background-radius: 5;");
        minimizeBtn.setOnAction(e -> stage.setIconified(true));

        Button maximizeBtn = new Button("□");
        maximizeBtn.setStyle("-fx-background-color: rgba(0,0,0,0.1); -fx-text-fill: white; -fx-background-radius: 5;");
        maximizeBtn.setOnAction(e -> {
            if (stage.isMaximized()) {
                stage.setMaximized(false);
            } else {
                stage.setMaximized(true);
            }
        });

        Button closeBtn = new Button("×");
        closeBtn.setStyle("-fx-background-color: rgba(255,0,0,0.3); -fx-text-fill: white; -fx-background-radius: 5;");
        closeBtn.setOnAction(e -> Platform.exit());

        controls.getChildren().addAll(minimizeBtn, maximizeBtn, closeBtn);
        return controls;
    }

    private VBox createMainContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-backdrop-filter: blur(10px);");

        // Заголовок
        Label welcomeLabel = new Label("Добро пожаловать, " + username + "!");
        welcomeLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: white;");
        content.getChildren().add(welcomeLabel);

        // Карточки серверов
        HBox serversBox = new HBox(20);
        serversBox.setAlignment(Pos.CENTER);

        VBox anarchyCard = createServerCard("Анархия", "⚔️", "1.21", "anarchy");
        VBox survivalCard = createServerCard("Выживание", "🌲", "1.21.10", "survival");

        serversBox.getChildren().addAll(anarchyCard, survivalCard);
        content.getChildren().add(serversBox);

        // Кнопка запуска
        Button playButton = new Button("Играть");
        playButton.setPrefWidth(200);
        playButton.setPrefHeight(50);
        playButton.setStyle("-fx-background-color: linear-gradient(to right, #9B59B6, #8E44AD); " +
                          "-fx-background-radius: 10; -fx-text-fill: white; -fx-font-size: 18px; " +
                          "-fx-font-weight: bold;");
        playButton.setOnAction(e -> launchGame());
        content.getChildren().add(playButton);

        // Прогресс-бар (скрыт по умолчанию)
        ProgressBar progressBar = new ProgressBar();
        progressBar.setVisible(false);
        progressBar.setPrefWidth(400);
        content.getChildren().add(progressBar);

        return content;
    }

    private VBox createServerCard(String name, String icon, String version, String serverId) {
        VBox card = new VBox(15);
        card.setPrefWidth(300);
        card.setPrefHeight(200);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 15; " +
                     "-fx-backdrop-filter: blur(10px);");
        card.setAlignment(Pos.CENTER);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 48px;");

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label versionLabel = new Label("Версия: " + version);
        versionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(255,255,255,0.8);");

        Button selectBtn = new Button("Выбрать");
        selectBtn.setStyle("-fx-background-color: rgba(255,255,255,0.3); -fx-text-fill: white; " +
                          "-fx-background-radius: 10;");
        selectBtn.setOnAction(e -> {
            selectedServer = serverId;
            logger.info("Выбран сервер: {}", serverId);
        });

        card.getChildren().addAll(iconLabel, nameLabel, versionLabel, selectBtn);
        return card;
    }

    private void launchGame() {
        if (selectedServer == null) {
            showAlert("Пожалуйста, выберите сервер", true);
            return;
        }

        new Thread(() -> {
            try {
                Platform.runLater(() -> {
                    showAlert("Запуск игры...", false);
                });

                gameLauncher.setProgressCallback(message -> {
                    Platform.runLater(() -> {
                        logger.info("[GAME] {}", message);
                    });
                });

                gameLauncher.launch(selectedServer, username, ramInGB);
                
                Platform.runLater(() -> {
                    showAlert("Игра запущена!", false);
                });
            } catch (IOException e) {
                logger.error("Ошибка запуска игры", e);
                Platform.runLater(() -> {
                    showAlert("Ошибка запуска игры: " + e.getMessage(), true);
                });
            }
        }).start();
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

