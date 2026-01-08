package com.horizon.launcher.ui;

import com.horizon.launcher.api.*;
import com.horizon.launcher.minecraft.GameLauncher;
import javafx.application.Platform;
import javafx.util.Duration;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Главное окно лаунчера с современным Figma дизайном
 */
public class StyledMainWindow {
    private static final Logger logger = LoggerFactory.getLogger(StyledMainWindow.class);
    
    private Stage stage;
    private String username;
    private GameLauncher gameLauncher;
    private CurrencyService currencyService;
    private TelegramNewsService newsService;
    
    private VBox mainContent;
    private VBox wardrobeContent;
    private VBox shopContent;
    private VBox forumContent;
    private VBox settingsContent;
    
    private Button playButton;
    private Label currencyLabel;
    private String selectedServer = null;
    private int ramInGB = 4;
    private ToggleGroup modelToggleGroup;

    public StyledMainWindow(String username) {
        this.username = username;
        this.gameLauncher = new GameLauncher();
        this.currencyService = new CurrencyService();
        this.newsService = new TelegramNewsService();
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
        root.getStyleClass().add("animated-background");

        // Боковая панель
        VBox sidebar = createSidebar();
        root.setLeft(sidebar);

        // Верхняя панель
        HBox topBar = createTopBar();
        root.setTop(topBar);

        // Основной контент (StackPane для переключения страниц)
        StackPane contentPane = new StackPane();
        mainContent = createMainContent();
        wardrobeContent = createWardrobeContent();
        shopContent = createShopContent();
        forumContent = createForumContent();
        settingsContent = createSettingsContent();
        
        contentPane.getChildren().addAll(mainContent, wardrobeContent, shopContent, forumContent, settingsContent);
        mainContent.setVisible(true);
        wardrobeContent.setVisible(false);
        shopContent.setVisible(false);
        forumContent.setVisible(false);
        settingsContent.setVisible(false);
        
        root.setCenter(contentPane);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        stage.setScene(scene);
    }

    private HBox createTopBar() {
        HBox topBar = new HBox(15);
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.getStyleClass().add("glass-topbar");

        // Донат валюта
        HBox currencyBox = new HBox(8);
        currencyBox.setAlignment(Pos.CENTER);
        currencyBox.setPadding(new Insets(8, 16, 8, 16));
        currencyBox.getStyleClass().add("currency-label");
        
        currencyLabel = new Label();
        currencyLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: 600;");
        
        new Thread(() -> {
            int balance = currencyService.getBalance(username);
            Platform.runLater(() -> currencyLabel.setText("💎 " + balance));
        }).start();
        
        currencyBox.getChildren().add(currencyLabel);
        
        // Кнопки управления окном
        HBox windowControls = createWindowControls();
        
        topBar.getChildren().addAll(currencyBox, windowControls);
        return topBar;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(15);
        sidebar.setPrefWidth(100);
        sidebar.setPadding(new Insets(30, 15, 30, 15));
        sidebar.setAlignment(Pos.TOP_CENTER);
        sidebar.getStyleClass().add("glass-sidebar");

        Button homeBtn = createSidebarButton("🏠", "Главная", () -> showPage(mainContent));
        Button shopBtn = createSidebarButton("🛍️", "Магазин", () -> showPage(shopContent));
        Button wardrobeBtn = createSidebarButton("👕", "Гардероб", () -> showPage(wardrobeContent));
        Button forumBtn = createSidebarButton("💬", "Форум", () -> showPage(forumContent));
        Button settingsBtn = createSidebarButton("⚙️", "Настройки", () -> showPage(settingsContent));
        
        sidebar.getChildren().addAll(homeBtn, shopBtn, wardrobeBtn, forumBtn, settingsBtn);
        return sidebar;
    }

    private Button createSidebarButton(String icon, String tooltip, Runnable action) {
        Button btn = new Button(icon);
        btn.setPrefWidth(70);
        btn.setPrefHeight(70);
        btn.getStyleClass().add("sidebar-button");
        btn.setTooltip(new Tooltip(tooltip));
        btn.setOnAction(e -> {
            AnimationHelper.liquidPress(btn);
            action.run();
        });
        return btn;
    }

    private void showPage(VBox page) {
        Platform.runLater(() -> {
            mainContent.setVisible(page == mainContent);
            wardrobeContent.setVisible(page == wardrobeContent);
            shopContent.setVisible(page == shopContent);
            forumContent.setVisible(page == forumContent);
            settingsContent.setVisible(page == settingsContent);
            
            if (page.isVisible()) {
                AnimationHelper.fadeIn(page, Duration.millis(300));
            }
        });
    }

    private VBox createMainContent() {
        VBox content = new VBox(30);
        content.setPadding(new Insets(40, 50, 40, 50));
        content.setAlignment(Pos.TOP_CENTER);

        // Заголовок Dashboard
        HBox headerBox = new HBox(20);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(10, 0, 20, 0));
        
        Label welcomeLabel = new Label("Dashboard");
        welcomeLabel.getStyleClass().add("title-text");
        welcomeLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: white;");
        AnimationHelper.slideUp(welcomeLabel, Duration.millis(200));
        
        Label usernameLabel = new Label("Привет, " + username + " 👋");
        usernameLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: rgba(255, 255, 255, 0.7);");
        
        headerBox.getChildren().addAll(welcomeLabel, new Region(), usernameLabel);
        HBox.setHgrow(headerBox.getChildren().get(1), Priority.ALWAYS);
        content.getChildren().add(headerBox);
        
        // Статистические карточки
        HBox statsBox = new HBox(20);
        statsBox.setAlignment(Pos.CENTER);
        statsBox.setPadding(new Insets(10, 0, 10, 0));
        
        VBox onlineCard = createStatCard("👥 Онлайн игроков", "342", "#7C4DFF");
        VBox rankCard = createStatCard("⭐ Ваш ранг", "VIP", "#00D4FF");
        VBox levelCard = createStatCard("🎯 Уровень", "25", "#B794F6");
        
        statsBox.getChildren().addAll(onlineCard, rankCard, levelCard);
        content.getChildren().add(statsBox);
        
        // Слайдер новостей
        Label newsTitle = new Label("📰 Новости и Обновления");
        newsTitle.getStyleClass().add("section-title");
        newsTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        content.getChildren().add(newsTitle);

        StackPane newsSlider = new StackPane();
        newsSlider.setPrefHeight(350);
        newsSlider.setPrefWidth(1200);
        newsSlider.getStyleClass().add("glass-card");
        newsSlider.setStyle("-fx-background-radius: 20; -fx-effect: dropshadow(gaussian, rgba(124, 77, 255, 0.4), 20, 0, 0, 0);");
        
        VBox newsContent = new VBox(15);
        newsContent.setPadding(new Insets(40));
        newsContent.setAlignment(Pos.CENTER);
        
        Label newsHeadline = new Label("🎉 Обновление 1.21.4 уже здесь!");
        newsHeadline.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        Label newsDescription = new Label("Новые биомы, мобы и улучшения производительности. Проверьте это сейчас!");
        newsDescription.setStyle("-fx-font-size: 18px; -fx-text-fill: rgba(255, 255, 255, 0.8); -fx-wrap-text: true;");
        newsDescription.setMaxWidth(900);
        newsDescription.setAlignment(Pos.CENTER);
        
        Button readMoreBtn = new Button("Читать далее →");
        readMoreBtn.getStyleClass().add("ios-button");
        readMoreBtn.setPrefWidth(200);
        
        newsContent.getChildren().addAll(newsHeadline, newsDescription, readMoreBtn);
        newsSlider.getChildren().add(newsContent);
        content.getChildren().add(newsSlider);

        // Карточки серверов
        Label serversTitle = new Label("🎮 Игровые Серверы");
        serversTitle.getStyleClass().add("section-title");
        serversTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        content.getChildren().add(serversTitle);

        HBox serversBox = new HBox(25);
        serversBox.setAlignment(Pos.CENTER);

        VBox anarchyCard = createEnhancedServerCard("Анархия", "⚔️", "1.21", "342/500", "HOT", "#FF4444", "anarchy");
        VBox survivalCard = createEnhancedServerCard("Выживание", "🌲", "1.21.10", "187/300", "NEW", "#44FF44", "survival");
        VBox creativeCard = createEnhancedServerCard("Творчество", "🎨", "1.21", "95/200", "", "", "creative");

        serversBox.getChildren().addAll(anarchyCard, survivalCard, creativeCard);
        AnimationHelper.scaleIn(serversBox, Duration.millis(400));
        content.getChildren().add(serversBox);

        // Кнопка запуска
        playButton = new Button("Играть");
        playButton.setPrefWidth(250);
        playButton.setPrefHeight(50);
        playButton.getStyleClass().add("ios-button");
        playButton.setOnAction(e -> {
            AnimationHelper.liquidPress(playButton);
            launchGame();
        });
        AnimationHelper.scaleIn(playButton, Duration.millis(600));
        content.getChildren().add(playButton);

        return content;
    }

    private VBox createStatCard(String title, String value, String color) {
        VBox card = new VBox(10);
        card.setPrefWidth(250);
        card.setPrefHeight(120);
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("glass-card");
        card.setStyle("-fx-background-radius: 15; -fx-effect: dropshadow(gaussian, " + color + "80, 15, 0, 0, 0);");
        
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: rgba(255, 255, 255, 0.8);");
        
        card.getChildren().addAll(valueLabel, titleLabel);
        
        card.setOnMouseEntered(e -> AnimationHelper.scaleIn(card, Duration.millis(200)));
        card.setOnMouseExited(e -> {
            card.setScaleX(1.0);
            card.setScaleY(1.0);
        });

        return card;
    }

    private VBox createEnhancedServerCard(String name, String icon, String version, String players, String badge, String badgeColor, String serverId) {
        VBox card = new VBox(15);
        card.setPrefWidth(350);
        card.setPrefHeight(280);
        card.setPadding(new Insets(25));
        card.getStyleClass().add("server-card");
        card.setAlignment(Pos.TOP_CENTER);
        card.setStyle("-fx-background-radius: 20; -fx-cursor: hand;");
        
        StackPane iconPane = new StackPane();
        iconPane.setPrefWidth(100);
        iconPane.setPrefHeight(100);
        iconPane.setStyle("-fx-background-color: rgba(124, 77, 255, 0.2); -fx-background-radius: 50;");

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 56px;");
        iconPane.getChildren().add(iconLabel);
        
        if (badge != null && !badge.isEmpty()) {
            Label badgeLabel = new Label(badge);
            badgeLabel.setStyle("-fx-background-color: " + badgeColor + "; -fx-text-fill: white; " +
                              "-fx-padding: 5 10; -fx-background-radius: 12; -fx-font-size: 12px; -fx-font-weight: bold;");
            badgeLabel.setTranslateX(35);
            badgeLabel.setTranslateY(-10);
            iconPane.getChildren().add(badgeLabel);
        }
        
        card.getChildren().add(iconPane);

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        card.getChildren().add(nameLabel);
        
        HBox infoBox = new HBox(15);
        infoBox.setAlignment(Pos.CENTER);
        
        Label versionLabel = new Label("v" + version);
        versionLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.6); -fx-font-size: 14px;");
        
        Label playersLabel = new Label(players);
        playersLabel.setStyle("-fx-text-fill: #00D4FF; -fx-font-size: 14px; -fx-font-weight: bold;");
        
        infoBox.getChildren().addAll(versionLabel, new Label("|"), playersLabel);
        card.getChildren().add(infoBox);
        
        ProgressBar progressBar = new ProgressBar();
        String[] parts = players.split("/");
        if (parts.length == 2) {
            try {
                double current = Double.parseDouble(parts[0]);
                double max = Double.parseDouble(parts[1]);
                progressBar.setProgress(current / max);
            } catch (NumberFormatException e) {
                progressBar.setProgress(0.5);
            }
        } else {
            progressBar.setProgress(0.5);
        }
        progressBar.setPrefWidth(280);
        progressBar.setStyle("-fx-accent: #7C4DFF;");
        card.getChildren().add(progressBar);
        
        Button connectBtn = new Button("Подключиться");
        connectBtn.getStyleClass().add("ios-button");
        connectBtn.setPrefWidth(250);
        connectBtn.setOnAction(e -> {
            AnimationHelper.liquidPress(connectBtn);
            selectedServer = serverId;
        });
        card.getChildren().add(connectBtn);

        card.setOnMouseEntered(e -> card.setStyle("-fx-background-radius: 20; -fx-cursor: hand; -fx-scale-x: 1.05; -fx-scale-y: 1.05;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-radius: 20; -fx-cursor: hand; -fx-scale-x: 1.0; -fx-scale-y: 1.0;"));
        
        return card;
    }

    private VBox createWardrobeContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("👕 Гардероб");
        title.getStyleClass().add("title-text");
        title.setStyle("-fx-font-size: 28px;");
        content.getChildren().add(title);

        Label placeholder = new Label("Раздел в разработке...");
        placeholder.setStyle("-fx-font-size: 20px; -fx-text-fill: white;");
        content.getChildren().add(placeholder);
        
        return content;
    }
    
    private VBox createShopContent() {
        VBox content = new VBox(25);
        content.setPadding(new Insets(40));
        content.setAlignment(Pos.TOP_CENTER);
        
        // Заголовок
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label("🛒 Магазин");
        title.getStyleClass().add("title-text");
        title.setStyle("-fx-font-size: 32px;");
        
        // Баланс
        HBox currencyDisplay = new HBox(8);
        currencyDisplay.setAlignment(Pos.CENTER);
        currencyDisplay.setPadding(new Insets(8, 16, 8, 16));
        currencyDisplay.getStyleClass().add("currency-label");
        
        Label currencyValue = new Label();
        currencyValue.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: 600;");
        
        new Thread(() -> {
            int balance = currencyService.getBalance(username);
            Platform.runLater(() -> currencyValue.setText("💎 " + balance));
        }).start();
        
        currencyDisplay.getChildren().add(currencyValue);
        header.getChildren().addAll(title, new Region(), currencyDisplay);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        content.getChildren().add(header);
        
        Label desc = new Label("Покупайте косметику, скины, плащи и другие улучшения за донат-валюту");
        desc.getStyleClass().add("subtitle-text");
        desc.setStyle("-fx-font-size: 15px; -fx-text-fill: rgba(255, 255, 255, 0.7);");
        desc.setWrapText(true);
        desc.setMaxWidth(800);
        content.getChildren().add(desc);
        
        // Категории товаров
        HBox categoriesBox = new HBox(15);
        categoriesBox.setAlignment(Pos.CENTER);
        categoriesBox.setPadding(new Insets(10, 0, 10, 0));
        
        Button allCategoryBtn = createCategoryButton("Все", true);
        Button skinsCategoryBtn = createCategoryButton("Скины", false);
        Button capesCategoryBtn = createCategoryButton("Плащи", false);
        Button particlesCategoryBtn = createCategoryButton("Частицы", false);
        Button ranksCategoryBtn = createCategoryButton("Ранги", false);
        
        categoriesBox.getChildren().addAll(allCategoryBtn, skinsCategoryBtn, capesCategoryBtn, particlesCategoryBtn, ranksCategoryBtn);
        content.getChildren().add(categoriesBox);
        
        // Список товаров
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setPrefHeight(500);
        
        FlowPane shopPane = new FlowPane(20, 20);
        shopPane.setPadding(new Insets(20));
        shopPane.setAlignment(Pos.CENTER);
        shopPane.setPrefWrapLength(1100);
        
        loadShopItems(shopPane);
        scrollPane.setContent(shopPane);
        content.getChildren().add(scrollPane);

        return content;
    }

    private Button createCategoryButton(String text, boolean selected) {
        Button btn = new Button(text);
        btn.setPrefWidth(120);
        btn.setPrefHeight(40);
        
        if (selected) {
            btn.setStyle("-fx-background-color: #7C4DFF; -fx-text-fill: white; -fx-background-radius: 20; " +
                        "-fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;");
                        } else {
            btn.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); -fx-text-fill: rgba(255, 255, 255, 0.7); " +
                        "-fx-background-radius: 20; -fx-font-size: 14px; -fx-cursor: hand;");
        }
        
        btn.setOnMouseEntered(e -> {
            if (!selected) {
                btn.setStyle("-fx-background-color: rgba(124, 77, 255, 0.3); -fx-text-fill: white; " +
                            "-fx-background-radius: 20; -fx-font-size: 14px; -fx-cursor: hand;");
            }
        });
        
        btn.setOnMouseExited(e -> {
            if (!selected) {
                btn.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); -fx-text-fill: rgba(255, 255, 255, 0.7); " +
                            "-fx-background-radius: 20; -fx-font-size: 14px; -fx-cursor: hand;");
            }
        });
        
        return btn;
    }
    
    private void loadShopItems(FlowPane pane) {
        new Thread(() -> {
            CosmeticService cosmeticService = new CosmeticService();
            List<CosmeticService.Cosmetic> availableCosmetics = cosmeticService.getAvailableCosmetics();
            
            Platform.runLater(() -> {
                // Добавляем популярные товары
                pane.getChildren().add(createShopItemCard("Драконьи крылья", "🐉", 500, "LEGENDARY", "#FFD700", true));
                pane.getChildren().add(createShopItemCard("Радужный плащ", "🌈", 250, "EPIC", "#9B59B6", false));
                pane.getChildren().add(createShopItemCard("Огненные частицы", "🔥", 150, "RARE", "#FF6B6B", false));
                pane.getChildren().add(createShopItemCard("Скин Киборга", "🤖", 300, "EPIC", "#00D4FF", true));
                pane.getChildren().add(createShopItemCard("VIP Ранг", "⭐", 1000, "FEATURED", "#FFD700", true));
                pane.getChildren().add(createShopItemCard("Звёздная аура", "✨", 200, "RARE", "#B794F6", false));
                
                // Добавляем товары из API
                for (CosmeticService.Cosmetic cosmetic : availableCosmetics) {
                    pane.getChildren().add(createShopItemCard(
                        cosmetic.getName(), 
                        "✨", 
                        cosmetic.getPrice(), 
                        "COMMON", 
                        "#7C4DFF",
                        false
                    ));
                }
            });
        }).start();
    }

    private VBox createShopItemCard(String name, String icon, int price, String rarity, String color, boolean featured) {
        VBox card = new VBox(15);
        card.setPrefWidth(240);
        card.setPrefHeight(320);
        card.setPadding(new Insets(25));
        card.getStyleClass().add("glass-card");
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-radius: 20; -fx-cursor: hand;");
        
        // Иконка товара
        StackPane iconPane = new StackPane();
        iconPane.setPrefWidth(160);
        iconPane.setPrefHeight(160);
        iconPane.setStyle("-fx-background-color: " + color + "40; -fx-background-radius: 16;");
        
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 64px;");
        iconPane.getChildren().add(iconLabel);
        
        // Бейдж редкости
        if (featured) {
            Label featuredBadge = new Label("FEATURED");
            featuredBadge.setStyle("-fx-background-color: #FFD700; -fx-text-fill: black; " +
                                  "-fx-padding: 5 10; -fx-background-radius: 12; -fx-font-size: 10px; -fx-font-weight: bold;");
            featuredBadge.setTranslateX(50);
            featuredBadge.setTranslateY(-60);
            iconPane.getChildren().add(featuredBadge);
        }
        
        Label rarityLabel = new Label(rarity);
        rarityLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        rarityLabel.setTranslateY(-50);
        iconPane.getChildren().add(rarityLabel);
        
        card.getChildren().add(iconPane);
        
        // Название товара
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white; -fx-wrap-text: true;");
        nameLabel.setMaxWidth(200);
        nameLabel.setAlignment(Pos.CENTER);
        card.getChildren().add(nameLabel);
        
        // Цена
        HBox priceBox = new HBox(5);
        priceBox.setAlignment(Pos.CENTER);
        
        Label priceLabel = new Label("💎 " + price);
        priceLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #00D4FF;");
        priceBox.getChildren().add(priceLabel);
        card.getChildren().add(priceBox);
        
        // Кнопка покупки
        Button buyBtn = new Button("Купить");
        buyBtn.getStyleClass().add("ios-button");
        buyBtn.setPrefWidth(180);
        buyBtn.setOnAction(e -> {
            AnimationHelper.liquidPress(buyBtn);
            purchaseItem(name, price);
        });
        card.getChildren().add(buyBtn);
        
        // Shine эффект при наведении
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-radius: 20; -fx-cursor: hand; -fx-scale-x: 1.05; -fx-scale-y: 1.05; " +
                         "-fx-effect: dropshadow(gaussian, " + color + ", 25, 0, 0, 0);");
        });
        
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-radius: 20; -fx-cursor: hand; -fx-scale-x: 1.0; -fx-scale-y: 1.0;");
        });
        
        return card;
    }
    
    private void purchaseItem(String itemName, int price) {
            new Thread(() -> {
            int balance = currencyService.getBalance(username);
            
            if (balance < price) {
                Platform.runLater(() -> showAlert("Недостаточно средств! У вас: 💎 " + balance + ", нужно: 💎 " + price, true));
                return;
            }
            
            boolean success = currencyService.deductBalance(username, price);
            
                Platform.runLater(() -> {
                    if (success) {
                    showAlert("✅ Покупка успешна! Вы приобрели: " + itemName, false);
                    // Обновляем баланс
                    int newBalance = currencyService.getBalance(username);
                    if (currencyLabel != null) {
                        currencyLabel.setText("💎 " + newBalance);
                    }
                } else {
                    showAlert("❌ Ошибка покупки. Попробуйте позже.", true);
                }
            });
        }).start();
    }

    private VBox createForumContent() {
        VBox content = new VBox(25);
        content.setPadding(new Insets(40));
        content.setAlignment(Pos.TOP_CENTER);

        // Заголовок с поиском
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPrefWidth(1200);
        
        Label title = new Label("💬 Форум");
        title.getStyleClass().add("title-text");
        title.setStyle("-fx-font-size: 32px;");
        
        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Поиск тем...");
        searchField.setPrefWidth(300);
        searchField.setPrefHeight(40);
        searchField.getStyleClass().add("ios-input");
        
        Button newTopicBtn = new Button("+ Новая тема");
        newTopicBtn.getStyleClass().add("ios-button");
        newTopicBtn.setPrefWidth(150);
        
        header.getChildren().addAll(title, new Region(), searchField, newTopicBtn);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        content.getChildren().add(header);
        
        // Категории форума
        Label categoriesTitle = new Label("Категории");
        categoriesTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        content.getChildren().add(categoriesTitle);
        
        GridPane categoriesGrid = new GridPane();
        categoriesGrid.setHgap(20);
        categoriesGrid.setVgap(20);
        categoriesGrid.setAlignment(Pos.CENTER);
        categoriesGrid.setPrefWidth(1200);
        
        categoriesGrid.add(createForumCategory("📢 Объявления", 45, "#FFD700"), 0, 0);
        categoriesGrid.add(createForumCategory("💬 Общение", 238, "#7C4DFF"), 1, 0);
        categoriesGrid.add(createForumCategory("❓ Вопросы", 89, "#00D4FF"), 2, 0);
        categoriesGrid.add(createForumCategory("🐛 Баги", 34, "#FF6B6B"), 0, 1);
        categoriesGrid.add(createForumCategory("💡 Предложения", 67, "#B794F6"), 1, 1);
        categoriesGrid.add(createForumCategory("🎮 Гайды", 52, "#44FF44"), 2, 1);
        
        content.getChildren().add(categoriesGrid);
        
        // Горячие темы
        Label hotTopicsTitle = new Label("🔥 Горячие темы");
        hotTopicsTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        content.getChildren().add(hotTopicsTitle);
        
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        
        VBox topicsBox = new VBox(15);
        topicsBox.setPadding(new Insets(10));
        topicsBox.setPrefWidth(1180);
        
        topicsBox.getChildren().addAll(
            createForumTopic("Обновление 1.21.4 - что нового?", "Admin", 156, 24, true, false),
            createForumTopic("Как получить VIP ранг?", "Player123", 89, 12, false, true),
            createForumTopic("Лучшие моды для Horizon", "ModLover", 234, 45, false, false),
            createForumTopic("[ГАЙД] Фарм ресурсов на Анархии", "ProGamer", 445, 78, false, false),
            createForumTopic("Баг с плащами - РЕШЕНО", "Tester", 67, 8, false, false)
        );
        
        scrollPane.setContent(topicsBox);
        content.getChildren().add(scrollPane);

        return content;
    }

    private VBox createForumCategory(String name, int topics, String color) {
        VBox card = new VBox(15);
        card.setPrefWidth(360);
        card.setPrefHeight(140);
        card.setPadding(new Insets(25));
        card.getStyleClass().add("glass-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-radius: 15; -fx-cursor: hand;");
        
        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        Label badge = new Label(String.valueOf(topics));
        badge.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-padding: 5 12; " +
                      "-fx-background-radius: 15; -fx-font-size: 14px; -fx-font-weight: bold;");
        
        titleBox.getChildren().addAll(nameLabel, badge);
        
        Label desc = new Label(topics + " " + (topics == 1 ? "тема" : topics < 5 ? "темы" : "тем"));
        desc.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.6); -fx-font-size: 14px;");
        
        card.getChildren().addAll(titleBox, desc);
        
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-radius: 15; -fx-cursor: hand; -fx-scale-x: 1.03; -fx-scale-y: 1.03; " +
                         "-fx-effect: dropshadow(gaussian, " + color + ", 20, 0, 0, 0);");
        });
        
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-radius: 15; -fx-cursor: hand; -fx-scale-x: 1.0; -fx-scale-y: 1.0;");
        });
        
        return card;
    }
    
    private HBox createForumTopic(String title, String author, int views, int replies, boolean pinned, boolean hot) {
        HBox topic = new HBox(20);
        topic.setPrefHeight(80);
        topic.setPadding(new Insets(15, 20, 15, 20));
        topic.getStyleClass().add("glass-card");
        topic.setAlignment(Pos.CENTER_LEFT);
        topic.setStyle("-fx-background-radius: 12; -fx-cursor: hand;");
        
        // Иконка
        StackPane iconPane = new StackPane();
        iconPane.setPrefWidth(50);
        iconPane.setPrefHeight(50);
        iconPane.setStyle("-fx-background-color: rgba(124, 77, 255, 0.2); -fx-background-radius: 25;");
        
        Label icon = new Label(pinned ? "📌" : hot ? "🔥" : "💬");
        icon.setStyle("-fx-font-size: 24px;");
        iconPane.getChildren().add(icon);
        
        // Информация о теме
        VBox infoBox = new VBox(8);
        infoBox.setPrefWidth(700);
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        Label authorLabel = new Label("Автор: " + author);
        authorLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255, 255, 255, 0.6);");
        
        infoBox.getChildren().addAll(titleLabel, authorLabel);
        
        // Статистика
        HBox statsBox = new HBox(30);
        statsBox.setAlignment(Pos.CENTER);
        
        VBox viewsBox = new VBox(5);
        viewsBox.setAlignment(Pos.CENTER);
        Label viewsLabel = new Label(String.valueOf(views));
        viewsLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #00D4FF;");
        Label viewsText = new Label("просмотров");
        viewsText.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255, 255, 255, 0.6);");
        viewsBox.getChildren().addAll(viewsLabel, viewsText);
        
        VBox repliesBox = new VBox(5);
        repliesBox.setAlignment(Pos.CENTER);
        Label repliesLabel = new Label(String.valueOf(replies));
        repliesLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #7C4DFF;");
        Label repliesText = new Label("ответов");
        repliesText.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255, 255, 255, 0.6);");
        repliesBox.getChildren().addAll(repliesLabel, repliesText);
        
        statsBox.getChildren().addAll(viewsBox, repliesBox);
        
        topic.getChildren().addAll(iconPane, infoBox, new Region(), statsBox);
        HBox.setHgrow(topic.getChildren().get(2), Priority.ALWAYS);
        
        topic.setOnMouseEntered(e -> {
            topic.setStyle("-fx-background-radius: 12; -fx-cursor: hand; -fx-scale-x: 1.01; -fx-scale-y: 1.01; " +
                          "-fx-effect: dropshadow(gaussian, rgba(124, 77, 255, 0.4), 15, 0, 0, 0);");
        });
        
        topic.setOnMouseExited(e -> {
            topic.setStyle("-fx-background-radius: 12; -fx-cursor: hand; -fx-scale-x: 1.0; -fx-scale-y: 1.0;");
        });
        
        return topic;
    }
    
    private VBox createSettingsContent() {
        VBox content = new VBox(30);
        content.setPadding(new Insets(40, 80, 40, 80));
        content.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("⚙️ Настройки");
        title.getStyleClass().add("title-text");
        title.setStyle("-fx-font-size: 32px;");
        content.getChildren().add(title);

        // Секция производительности
        VBox performanceSection = createSettingsSection(
            "🖥️ Производительность",
            createRamSlider(),
            createResolutionSelector()
        );
        content.getChildren().add(performanceSection);
        
        // Секция Java
        VBox javaSection = createSettingsSection(
            "☕ Java",
            createJavaVersionSelector()
        );
        content.getChildren().add(javaSection);
        
        // Секция языка
        VBox languageSection = createSettingsSection(
            "🌐 Язык",
            createLanguageSelector()
        );
        content.getChildren().add(languageSection);
        
        // Кнопки сохранения
        HBox buttonsBox = new HBox(15);
        buttonsBox.setAlignment(Pos.CENTER);
        buttonsBox.setPadding(new Insets(20, 0, 0, 0));
        
        Button saveBtn = new Button("💾 Сохранить");
        saveBtn.getStyleClass().add("ios-button");
        saveBtn.setPrefWidth(200);
        saveBtn.setOnAction(e -> {
            AnimationHelper.liquidPress(saveBtn);
            saveSettings();
        });
        
        Button cancelBtn = new Button("❌ Отменить");
        cancelBtn.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); -fx-text-fill: white; " +
                          "-fx-background-radius: 12; -fx-cursor: hand; -fx-font-size: 14px;");
        cancelBtn.setPrefWidth(200);
        cancelBtn.setPrefHeight(40);
        cancelBtn.setOnAction(e -> showPage(mainContent));
        
        buttonsBox.getChildren().addAll(saveBtn, cancelBtn);
        content.getChildren().add(buttonsBox);
        
        return content;
    }
    
    private VBox createSettingsSection(String title, javafx.scene.Node... controls) {
        VBox section = new VBox(20);
        section.setPadding(new Insets(30));
        section.setPrefWidth(1000);
        section.getStyleClass().add("glass-card");
        section.setStyle("-fx-background-radius: 20;");
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");
        section.getChildren().add(titleLabel);
        
        section.getChildren().addAll(controls);
        
        return section;
    }
    
    private VBox createRamSlider() {
        VBox box = new VBox(15);
        
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label label = new Label("Выделенная оперативная память (RAM)");
        label.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
        
        Label valueLabel = new Label(ramInGB + " GB");
        valueLabel.setStyle("-fx-text-fill: #00D4FF; -fx-font-size: 18px; -fx-font-weight: bold;");
        
        header.getChildren().addAll(label, new Region(), valueLabel);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        
        Slider ramSlider = new Slider(2, 16, ramInGB);
        ramSlider.setMajorTickUnit(2);
        ramSlider.setMinorTickCount(1);
        ramSlider.setShowTickLabels(true);
        ramSlider.setShowTickMarks(true);
        ramSlider.setSnapToTicks(true);
        ramSlider.setPrefWidth(900);
        ramSlider.setStyle("-fx-control-inner-background: rgba(255, 255, 255, 0.1);");
        
        ramSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            ramInGB = newVal.intValue();
            valueLabel.setText(ramInGB + " GB");
        });
        
        box.getChildren().addAll(header, ramSlider);
        return box;
    }
    
    private VBox createResolutionSelector() {
        VBox box = new VBox(15);
        
        Label label = new Label("Разрешение окна");
        label.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
        
        HBox resolutionsBox = new HBox(15);
        resolutionsBox.setAlignment(Pos.CENTER_LEFT);
        
        ToggleGroup resolutionGroup = new ToggleGroup();
        
        ToggleButton res1 = createResolutionButton("1280x720", resolutionGroup, true);
        ToggleButton res2 = createResolutionButton("1920x1080", resolutionGroup, false);
        ToggleButton res3 = createResolutionButton("2560x1440", resolutionGroup, false);
        ToggleButton res4 = createResolutionButton("Полный экран", resolutionGroup, false);
        
        resolutionsBox.getChildren().addAll(res1, res2, res3, res4);
        
        box.getChildren().addAll(label, resolutionsBox);
        return box;
    }
    
    private ToggleButton createResolutionButton(String text, ToggleGroup group, boolean selected) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(group);
        btn.setSelected(selected);
        btn.setPrefWidth(150);
        btn.setPrefHeight(50);
        btn.getStyleClass().add("mode-toggle-button");
        return btn;
    }
    
    private VBox createJavaVersionSelector() {
        VBox box = new VBox(15);
        
        Label label = new Label("Версия Java");
        label.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
        
        HBox javaBox = new HBox(15);
        javaBox.setAlignment(Pos.CENTER_LEFT);
        
        ToggleGroup javaGroup = new ToggleGroup();
        
        ToggleButton java17 = createResolutionButton("Java 17", javaGroup, false);
        ToggleButton java21 = createResolutionButton("Java 21", javaGroup, true);
        ToggleButton javaCustom = createResolutionButton("Своя Java", javaGroup, false);
        
        javaBox.getChildren().addAll(java17, java21, javaCustom);
        
        box.getChildren().addAll(label, javaBox);
        return box;
    }
    
    private VBox createLanguageSelector() {
        VBox box = new VBox(15);
        
        Label label = new Label("Язык интерфейса");
        label.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
        
        FlowPane languagesPane = new FlowPane(15, 15);
        languagesPane.setAlignment(Pos.CENTER_LEFT);
        
        languagesPane.getChildren().addAll(
            createLanguageCard("🇷🇺", "Русский", true),
            createLanguageCard("🇺🇸", "English", false),
            createLanguageCard("🇩🇪", "Deutsch", false),
            createLanguageCard("🇫🇷", "Français", false),
            createLanguageCard("🇪🇸", "Español", false),
            createLanguageCard("🇨🇳", "中文", false)
        );
        
        box.getChildren().addAll(label, languagesPane);
        return box;
    }
    
    private VBox createLanguageCard(String flag, String name, boolean selected) {
        VBox card = new VBox(10);
        card.setPrefWidth(140);
        card.setPrefHeight(120);
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: " + (selected ? "#7C4DFF" : "rgba(255, 255, 255, 0.1)") + 
                     "; -fx-background-radius: 15; -fx-cursor: hand;");
        
        Label flagLabel = new Label(flag);
        flagLabel.setStyle("-fx-font-size: 48px;");
        
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        
        card.getChildren().addAll(flagLabel, nameLabel);
        
        card.setOnMouseEntered(e -> {
            if (!selected) {
                card.setStyle("-fx-background-color: rgba(124, 77, 255, 0.3); -fx-background-radius: 15; -fx-cursor: hand;");
            }
        });
        
        card.setOnMouseExited(e -> {
            if (!selected) {
                card.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); -fx-background-radius: 15; -fx-cursor: hand;");
            }
        });
        
        return card;
    }
    
    private void saveSettings() {
        logger.info("Сохранение настроек: RAM={} GB", ramInGB);
        showAlert("✅ Настройки успешно сохранены!", false);
    }
    
    private HBox createWindowControls() {
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_RIGHT);
        
        Button minimizeBtn = new Button("−");
        minimizeBtn.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
        minimizeBtn.setOnAction(e -> stage.setIconified(true));
        
        Button maximizeBtn = new Button("□");
        maximizeBtn.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1); -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
        maximizeBtn.setOnAction(e -> stage.setMaximized(!stage.isMaximized()));
        
        Button closeBtn = new Button("×");
        closeBtn.setStyle("-fx-background-color: rgba(255, 0, 0, 0.3); -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> Platform.exit());
        
        controls.getChildren().addAll(minimizeBtn, maximizeBtn, closeBtn);
        return controls;
    }

    private void launchGame() {
        if (selectedServer == null) {
            showAlert("Пожалуйста, выберите сервер", true);
            return;
        }

        new Thread(() -> {
            try {
                Platform.runLater(() -> showAlert("Запуск игры...", false));
                
                gameLauncher.setProgressCallback(message -> Platform.runLater(() -> logger.info("[GAME] {}", message)));
                gameLauncher.launch(selectedServer, username, ramInGB);
                
                Platform.runLater(() -> showAlert("Игра запущена!", false));
            } catch (IOException e) {
                logger.error("Ошибка запуска игры", e);
                Platform.runLater(() -> showAlert("Ошибка запуска игры: " + e.getMessage(), true));
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
