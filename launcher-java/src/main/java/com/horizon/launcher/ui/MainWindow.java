package com.horizon.launcher.ui;

import com.horizon.launcher.minecraft.GameLauncher;
import com.horizon.launcher.minecraft.LaunchBuilder;
import com.horizon.launcher.ui.components.NewsSlider;
import com.horizon.launcher.utils.AccessibilityHelper;
import com.horizon.launcher.utils.AnimationHelper;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.*;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Главное окно лаунчера с полным интерфейсом согласно tech.md
 * Включает: боковую панель навигации, новости, магазин, форум, гардероб, выбор серверов
 */
public class MainWindow {
    private static final Logger logger = LoggerFactory.getLogger(MainWindow.class);
    
    private Stage stage;
    private Scene scene;
    private BorderPane rootContainer;
    
    // Боковая панель навигации
    private VBox sidebar;
    private Button homeButton, storeButton, wardrobeButton, forumButton, settingsButton;
    private String activeSection = "home";
    
    // Центральная область - секции
    private StackPane contentArea;
    private VBox homeSection;
    private StoreController storeSection;
    private VBox wardrobeSection;
    private ForumController forumSection;
    private VBox settingsSection;
    
    // Флаги для lazy loading
    private boolean homeSectionInitialized = false;
    private boolean storeSectionInitialized = false;
    private boolean wardrobeSectionInitialized = false;
    private boolean forumSectionInitialized = false;
    private boolean settingsSectionInitialized = false;
    
    // Компоненты Home секции
    private NewsSlider newsSlider;
    private PlayerModelView playerPreview;
    
    // Выбор сервера
    private LaunchBuilder.ServerType selectedServerType = LaunchBuilder.ServerType.ANARCHY;
    private HBox serverSelector;
    
    // Кнопка PLAY
    private Button playButton;
    private ProgressIndicator progressIndicator;
    private Label statusLabel;
    private TextField memoryField;
    
    // Данные пользователя
    private String username;
    private String accessToken;
    private String uuid;
    
    private final GameLauncher gameLauncher;
    
    public MainWindow(Stage stage, String username, String accessToken, String uuid) {
        this.stage = stage;
        this.username = username;
        this.accessToken = accessToken;
        this.uuid = uuid;
        this.gameLauncher = GameLauncher.getInstance();
        
        createUI();
    }
    
    private void createUI() {
        // Корневой контейнер
        rootContainer = new BorderPane();
        rootContainer.setStyle("-fx-background-color: #14141e;"); // Background из tech.md
        
        // Создаем боковую панель
        createSidebar();
        
        // Создаем центральную область
        createContentArea();
        
        // Создаем кнопку PLAY
        createPlayButton();
        
        // Применяем структуру
        rootContainer.setLeft(sidebar);
        rootContainer.setCenter(contentArea);
        
        // Создаем сцену
        scene = new Scene(rootContainer, 1400, 900);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        
        // Настраиваем окно
        stage.setTitle("Horizon Launcher");
        stage.setScene(scene);
        stage.initStyle(StageStyle.DECORATED);
        stage.setMinWidth(1200);
        stage.setMinHeight(800);
        
        // Показываем Home секцию по умолчанию
        showSection("home");
        
        // Настраиваем keyboard shortcuts
        setupKeyboardShortcuts();
    }
    
    /**
     * Настроить keyboard shortcuts для навигации и действий
     */
    private void setupKeyboardShortcuts() {
        scene.setOnKeyPressed(event -> {
            // Навигация по секциям
            if (event.isControlDown()) {
                switch (event.getCode()) {
                    case DIGIT1:
                        showSection("home");
                        event.consume();
                        break;
                    case DIGIT2:
                        showSection("store");
                        event.consume();
                        break;
                    case DIGIT3:
                        showSection("wardrobe");
                        event.consume();
                        break;
                    case DIGIT4:
                        showSection("forum");
                        event.consume();
                        break;
                    case DIGIT5:
                        showSection("settings");
                        event.consume();
                        break;
                }
            }
            
            // Обновление (F5)
            if (event.getCode() == javafx.scene.input.KeyCode.F5) {
                refreshCurrentSection();
                event.consume();
            }
            
            // Поиск (Ctrl+F)
            if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.F) {
                // Можно добавить открытие поиска
                event.consume();
            }
        });
    }
    
    /**
     * Обновить текущую секцию
     */
    private void refreshCurrentSection() {
        switch (activeSection) {
            case "home":
                // Обновить новости
                if (newsSlider != null) {
                    newsSlider.refresh();
                }
                break;
            case "store":
                // Обновить магазин
                if (storeSection != null) {
                    storeSection.refresh();
                }
                break;
            case "forum":
                // Обновить форум
                if (forumSection != null) {
                    forumSection.refresh();
                }
                break;
        }
    }
    
    /**
     * Создает боковую панель навигации
     */
    private void createSidebar() {
        sidebar = new VBox(10);
        sidebar.setPrefWidth(80);
        sidebar.setPadding(new Insets(20, 10, 20, 10));
        sidebar.setStyle(
            "-fx-background-color: rgba(30, 30, 45, 0.6); " + // Surface из tech.md
            "-fx-background-radius: 0 15px 15px 0;"
        );
        
        // Иконки навигации (используем текст как иконки, можно заменить на реальные иконки)
        homeButton = createNavButton("🏠", "Главная (Ctrl+1)");
        storeButton = createNavButton("🛍️", "Магазин (Ctrl+2)");
        wardrobeButton = createNavButton("👕", "Гардероб (Ctrl+3)");
        forumButton = createNavButton("💬", "Форум (Ctrl+4)");
        settingsButton = createNavButton("⚙️", "Настройки (Ctrl+5)");
        
        sidebar.getChildren().addAll(homeButton, storeButton, wardrobeButton, forumButton, settingsButton);
        
        // Обработчики кликов
        homeButton.setOnAction(e -> showSection("home"));
        storeButton.setOnAction(e -> showSection("store"));
        wardrobeButton.setOnAction(e -> showSection("wardrobe"));
        forumButton.setOnAction(e -> showSection("forum"));
        settingsButton.setOnAction(e -> showSection("settings"));
    }
    
    /**
     * Создает кнопку навигации
     */
    private Button createNavButton(String icon, String tooltip) {
        Button button = new Button(icon);
        button.setPrefSize(60, 60);
        button.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-background-radius: 10px; " +
            "-fx-text-fill: #A0A0B0; " + // Text Muted из tech.md
            "-fx-font-size: 24px; " +
            "-fx-cursor: hand;"
        );
        
        button.setOnMouseEntered(e -> {
            if (!button.getStyle().contains("#00f2fe")) {
                button.setStyle(
                    "-fx-background-color: rgba(0, 242, 254, 0.1); " +
                    "-fx-background-radius: 10px; " +
                    "-fx-text-fill: #A0A0B0; " +
                    "-fx-font-size: 24px; " +
                    "-fx-cursor: hand;"
                );
            }
        });
        
        button.setOnMouseExited(e -> {
            if (!button.getStyle().contains("#00f2fe")) {
                button.setStyle(
                    "-fx-background-color: transparent; " +
                    "-fx-background-radius: 10px; " +
                    "-fx-text-fill: #A0A0B0; " +
                    "-fx-font-size: 24px; " +
                    "-fx-cursor: hand;"
                );
            }
        });
        
        Tooltip.install(button, new Tooltip(tooltip));
        
        // Настраиваем accessibility
        AccessibilityHelper.setupButtonAccessibility(button, tooltip, 
            "Используйте эту кнопку для навигации. Также можно использовать клавиатурные сокращения.");
        
        // Добавляем liquidPress анимацию
        AnimationHelper.liquidPress(button);
        
        return button;
    }
    
    /**
     * Обновляет активную кнопку навигации
     */
    private void updateActiveNavButton(Button activeButton) {
        // Сбрасываем все кнопки
        homeButton.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-background-radius: 10px; " +
            "-fx-text-fill: #A0A0B0; " +
            "-fx-font-size: 24px; " +
            "-fx-cursor: hand;"
        );
        storeButton.setStyle(homeButton.getStyle());
        wardrobeButton.setStyle(homeButton.getStyle());
        forumButton.setStyle(homeButton.getStyle());
        settingsButton.setStyle(homeButton.getStyle());
        
        // Устанавливаем активную кнопку
        activeButton.setStyle(
            "-fx-background-color: rgba(0, 242, 254, 0.2); " + // Accent из tech.md
            "-fx-background-radius: 10px; " +
            "-fx-text-fill: #00f2fe; " + // Cyber Cyan
            "-fx-font-size: 24px; " +
            "-fx-cursor: hand;"
        );
        
        // Добавляем glow эффект
        Glow glow = new Glow(0.8);
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.web("#00f2fe"));
        shadow.setRadius(20);
        shadow.setSpread(0.5);
        glow.setInput(shadow);
        activeButton.setEffect(glow);
    }
    
    /**
     * Создает центральную область с секциями
     */
    private void createContentArea() {
        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: transparent;");
        
        // Секции будут создаваться лениво при первом обращении
        // Создаем только Home секцию сразу, так как она показывается по умолчанию
        ensureHomeSection();
    }
    
    /**
     * Убедиться, что секция создана (lazy loading)
     */
    private void ensureHomeSection() {
        if (!homeSectionInitialized) {
            createHomeSection();
            homeSection.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            VBox.setVgrow(homeSection, Priority.ALWAYS);
            contentArea.getChildren().add(homeSection);
            homeSectionInitialized = true;
        }
    }
    
    private void ensureStoreSection() {
        if (!storeSectionInitialized) {
            createStoreSection();
            storeSection.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            StackPane.setAlignment(storeSection, Pos.CENTER);
            contentArea.getChildren().add(storeSection);
            storeSectionInitialized = true;
        }
    }
    
    private void ensureWardrobeSection() {
        if (!wardrobeSectionInitialized) {
            createWardrobeSection();
            wardrobeSection.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            VBox.setVgrow(wardrobeSection, Priority.ALWAYS);
            contentArea.getChildren().add(wardrobeSection);
            wardrobeSectionInitialized = true;
        }
    }
    
    private void ensureForumSection() {
        if (!forumSectionInitialized) {
            createForumSection();
            forumSection.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            StackPane.setAlignment(forumSection, Pos.CENTER);
            contentArea.getChildren().add(forumSection);
            forumSectionInitialized = true;
        }
    }
    
    private void ensureSettingsSection() {
        if (!settingsSectionInitialized) {
            createSettingsSection();
            settingsSection.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            VBox.setVgrow(settingsSection, Priority.ALWAYS);
            contentArea.getChildren().add(settingsSection);
            settingsSectionInitialized = true;
        }
    }
    
    /**
     * Создает Home секцию с новостями и 3D превью
     */
    private void createHomeSection() {
        homeSection = new VBox(20);
        homeSection.setPadding(new Insets(30));
        homeSection.setStyle("-fx-background-color: transparent;");
        
        // Заголовок
        Label titleLabel = new Label("Главная");
        titleLabel.setStyle(
            "-fx-font-size: 32px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #FFFFFF; " + // Text Main из tech.md
            "-fx-font-family: 'Minecraft Unicode', monospace;"
        );
        
        // Контейнер для новостей и превью
        HBox mainContent = new HBox(20);
        mainContent.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(mainContent, Priority.ALWAYS);
        
        // Новости через NewsSlider
        newsSlider = new NewsSlider();
        newsSlider.setPrefWidth(800);
        newsSlider.setMinWidth(600);
        HBox.setHgrow(newsSlider, Priority.ALWAYS);
        
        // Загружаем новости (пока заглушка, будет интеграция с API)
        loadNews();
        
        // 3D превью персонажа
        VBox previewContainer = new VBox(10);
        previewContainer.setPrefWidth(400);
        previewContainer.setMinWidth(350);
        previewContainer.setAlignment(Pos.CENTER);
        
        Label previewLabel = new Label("3D Превью персонажа");
        previewLabel.setStyle(
            "-fx-font-size: 18px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #FFFFFF;"
        );
        
        // Создаем PlayerModelView для 3D превью
        try {
            final PlayerModelView preview = new PlayerModelView(350, 350);
            playerPreview = preview;
            // Убеждаемся, что вращение запущено
            Platform.runLater(() -> {
                if (preview != null) {
                    preview.startRotation();
                }
            });
            // Загружаем скин пользователя асинхронно
            loadUserSkin();
        } catch (Exception e) {
            logger.error("Ошибка при создании 3D превью", e);
            Label errorLabel = new Label("3D превью недоступно");
            errorLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #A0A0B0;");
            previewContainer.getChildren().add(errorLabel);
        }
        
        if (playerPreview != null) {
            previewContainer.getChildren().addAll(previewLabel, playerPreview);
        }
        
        mainContent.getChildren().addAll(newsSlider, previewContainer);
        
        // Выбор сервера
        createServerSelector();
        
        // Обертка для прокрутки если нужно
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(new VBox(20, titleLabel, mainContent, serverSelector));
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        homeSection.getChildren().add(scrollPane);
    }
    
    /**
     * Загружает новости (заглушка, будет интеграция с API)
     */
    private void loadNews() {
        List<NewsSlider.NewsItem> news = new ArrayList<>();
        
        // Заглушки новостей (будет заменено на реальную загрузку из API)
        news.add(new NewsSlider.NewsItem(
            "Обновление сервера",
            "Новое обновление с улучшениями и исправлениями",
            null
        ));
        news.add(new NewsSlider.NewsItem(
            "Новый ивент",
            "Королевская битва начинается!",
            null
        ));
        news.add(new NewsSlider.NewsItem(
            "Новая косметика",
            "Добавлены новые предметы в магазин",
            null
        ));
        news.add(new NewsSlider.NewsItem(
            "Обновление форума",
            "Улучшения интерфейса форума",
            null
        ));
        
        newsSlider.setNews(news);
    }
    
    /**
     * Загружает скин пользователя для 3D превью
     */
    private void loadUserSkin() {
        if (username == null || username.isEmpty() || playerPreview == null) {
            return;
        }
        
        // Загружаем скин асинхронно
        javafx.concurrent.Task<javafx.scene.image.Image> loadTask = new javafx.concurrent.Task<javafx.scene.image.Image>() {
            @Override
            protected javafx.scene.image.Image call() throws Exception {
                String skinUrl = "https://crafatar.com/skins/" + username;
                return new javafx.scene.image.Image(skinUrl, true);
            }
            
            @Override
            protected void succeeded() {
                javafx.scene.image.Image skinImage = getValue();
                if (skinImage != null && !skinImage.isError()) {
                    Platform.runLater(() -> {
                        playerPreview.setSkin(skinImage);
                    });
                }
            }
        };
        
        new Thread(loadTask).start();
    }
    
    /**
     * Создает выбор сервера (Анархия/Выживание)
     */
    private void createServerSelector() {
        serverSelector = new HBox(20);
        serverSelector.setAlignment(Pos.CENTER);
        serverSelector.setPadding(new Insets(20, 0, 0, 0));
        
        Label serverLabel = new Label("Выберите сервер:");
        serverLabel.setStyle(
            "-fx-font-size: 16px; " +
            "-fx-text-fill: #FFFFFF;"
        );
        
        // Карточка Анархия
        VBox anarchyCard = createServerCard(
            "Анархия",
            "Версия: 1.21",
            LaunchBuilder.ServerType.ANARCHY
        );
        
        // Карточка Выживание
        VBox survivalCard = createServerCard(
            "Выживание",
            "Версия: 1.21.10",
            LaunchBuilder.ServerType.SURVIVAL
        );
        
        serverSelector.getChildren().addAll(serverLabel, anarchyCard, survivalCard);
    }
    
    /**
     * Создает карточку сервера
     */
    private VBox createServerCard(String name, String version, LaunchBuilder.ServerType serverType) {
        VBox card = new VBox(10);
        card.setPrefSize(200, 120);
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.CENTER);
        card.setStyle(
            "-fx-background-color: rgba(30, 30, 45, 0.6); " +
            "-fx-background-radius: 12px; " +
            "-fx-border-color: rgba(255, 255, 255, 0.1); " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 12px; " +
            "-fx-cursor: hand;"
        );
        
        Label nameLabel = new Label(name);
        nameLabel.setStyle(
            "-fx-font-size: 20px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #FFFFFF;"
        );
        
        Label versionLabel = new Label(version);
        versionLabel.setStyle(
            "-fx-font-size: 14px; " +
            "-fx-text-fill: #A0A0B0;"
        );
        
        card.getChildren().addAll(nameLabel, versionLabel);
        
        // Добавляем liquidPress анимацию
        AnimationHelper.liquidPress(card);
        
        // Выделение выбранного сервера
        if (selectedServerType == serverType) {
            card.setStyle(
                "-fx-background-color: rgba(102, 126, 234, 0.3); " + // Primary gradient start
                "-fx-background-radius: 12px; " +
                "-fx-border-color: #00f2fe; " + // Accent
                "-fx-border-width: 2px; " +
                "-fx-border-radius: 12px; " +
                "-fx-cursor: hand;"
            );
        }
        
        // Обработчик клика
        card.setOnMouseClicked(e -> {
            selectedServerType = serverType;
            createServerSelector(); // Пересоздаем для обновления выделения
        });
        
        // Hover эффект
        card.setOnMouseEntered(e -> {
            if (selectedServerType != serverType) {
                card.setStyle(
                    "-fx-background-color: rgba(30, 30, 45, 0.8); " +
                    "-fx-background-radius: 12px; " +
                    "-fx-border-color: rgba(255, 255, 255, 0.2); " +
                    "-fx-border-width: 1px; " +
                    "-fx-border-radius: 12px; " +
                    "-fx-cursor: hand;"
                );
            }
        });
        
        card.setOnMouseExited(e -> {
            if (selectedServerType != serverType) {
                card.setStyle(
                    "-fx-background-color: rgba(30, 30, 45, 0.6); " +
                    "-fx-background-radius: 12px; " +
                    "-fx-border-color: rgba(255, 255, 255, 0.1); " +
                    "-fx-border-width: 1px; " +
                    "-fx-border-radius: 12px; " +
                    "-fx-cursor: hand;"
                );
            }
        });
        
        return card;
    }
    
    /**
     * Создает Store секцию
     */
    private void createStoreSection() {
        storeSection = new StoreController(username);
        storeSection.setVisible(false);
        // Убеждаемся, что BorderPane заполняет все пространство
        storeSection.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        storeSection.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    }
    
    /**
     * Создает Wardrobe секцию с 3D превью, выбором плащей и косметики
     */
    private void createWardrobeSection() {
        wardrobeSection = new VBox(20);
        wardrobeSection.setPadding(new Insets(30));
        wardrobeSection.setStyle("-fx-background-color: transparent;");
        wardrobeSection.setVisible(false);
        
        Label titleLabel = new Label("Гардероб");
        titleLabel.setStyle(
            "-fx-font-size: 32px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #FFFFFF; " +
            "-fx-font-family: 'Minecraft Unicode', monospace;"
        );
        
        // Основной контейнер с превью и опциями
        HBox mainContainer = new HBox(30);
        mainContainer.setAlignment(Pos.TOP_LEFT);
        
        // Левая часть - 3D превью
        VBox previewContainer = new VBox(15);
        previewContainer.setPrefWidth(400);
        previewContainer.setAlignment(Pos.CENTER);
        
        Label previewLabel = new Label("3D Превью");
        previewLabel.setStyle(
            "-fx-font-size: 18px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #FFFFFF;"
        );
        
        // 3D превью скина
        PlayerModelView wardrobePreview = null;
        try {
            final PlayerModelView preview = new PlayerModelView(350, 350);
            wardrobePreview = preview;
            // Убеждаемся, что вращение запущено
            Platform.runLater(() -> {
                if (preview != null) {
                    preview.startRotation();
                }
            });
            loadUserSkinForWardrobe(preview);
        } catch (Exception e) {
            logger.error("Ошибка при создании 3D превью для гардероба", e);
            wardrobePreview = null;
        }
        
        if (wardrobePreview != null) {
            previewContainer.getChildren().addAll(previewLabel, wardrobePreview);
        } else {
            Label errorLabel = new Label("3D превью недоступно");
            errorLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #A0A0B0;");
            previewContainer.getChildren().addAll(previewLabel, errorLabel);
        }
        
        // Правая часть - выбор плащей и косметики
        VBox optionsContainer = new VBox(20);
        optionsContainer.setPrefWidth(600);
        
        // Выбор плащей
        Label capesLabel = new Label("Плащи");
        capesLabel.setStyle(
            "-fx-font-size: 20px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #FFFFFF;"
        );
        
        FlowPane capesPane = new FlowPane();
        capesPane.setHgap(15);
        capesPane.setVgap(15);
        capesPane.setPrefWrapLength(600);
        
        // Заглушки для плащей (будет интеграция с API)
        for (int i = 1; i <= 6; i++) {
            VBox capeCard = createCapeCard("Плащ " + i);
            capesPane.getChildren().add(capeCard);
        }
        
        // Косметика
        Label cosmeticsLabel = new Label("Косметика");
        cosmeticsLabel.setStyle(
            "-fx-font-size: 20px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #FFFFFF;"
        );
        
        FlowPane cosmeticsPane = new FlowPane();
        cosmeticsPane.setHgap(15);
        cosmeticsPane.setVgap(15);
        cosmeticsPane.setPrefWrapLength(600);
        
        Label cosmeticsPlaceholder = new Label("Ваша косметика будет отображаться здесь");
        cosmeticsPlaceholder.setStyle("-fx-font-size: 14px; -fx-text-fill: #A0A0B0;");
        cosmeticsPane.getChildren().add(cosmeticsPlaceholder);
        
        ScrollPane cosmeticsScroll = new ScrollPane(cosmeticsPane);
        cosmeticsScroll.setFitToWidth(true);
        cosmeticsScroll.setPrefHeight(200);
        cosmeticsScroll.setStyle("-fx-background-color: transparent;");
        
        optionsContainer.getChildren().addAll(
            capesLabel,
            capesPane,
            cosmeticsLabel,
            cosmeticsScroll
        );
        
        mainContainer.getChildren().addAll(previewContainer, optionsContainer);
        
        wardrobeSection.getChildren().addAll(titleLabel, mainContainer);
    }
    
    /**
     * Создает карточку плаща
     */
    private VBox createCapeCard(String capeName) {
        VBox card = new VBox(10);
        card.setPrefSize(120, 150);
        card.setPadding(new Insets(10));
        card.setAlignment(Pos.CENTER);
        card.setStyle(
            "-fx-background-color: rgba(30, 30, 45, 0.6); " +
            "-fx-background-radius: 12px; " +
            "-fx-border-color: rgba(255, 255, 255, 0.1); " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 12px; " +
            "-fx-cursor: hand;"
        );
        
        // Изображение плаща (заглушка)
        Region capeImage = new Region();
        capeImage.setPrefSize(100, 100);
        capeImage.setStyle(
            "-fx-background-color: rgba(102, 126, 234, 0.3); " +
            "-fx-background-radius: 8px;"
        );
        
        Label nameLabel = new Label(capeName);
        nameLabel.setStyle(
            "-fx-font-size: 12px; " +
            "-fx-text-fill: #FFFFFF; " +
            "-fx-wrap-text: true;"
        );
        nameLabel.setMaxWidth(100);
        
        card.getChildren().addAll(capeImage, nameLabel);
        
        // Добавляем liquidPress анимацию
        AnimationHelper.liquidPress(card);
        
        // Hover эффект
        card.setOnMouseEntered(e -> {
            card.setStyle(
                "-fx-background-color: rgba(30, 30, 45, 0.8); " +
                "-fx-background-radius: 12px; " +
                "-fx-border-color: #00f2fe; " +
                "-fx-border-width: 2px; " +
                "-fx-border-radius: 12px; " +
                "-fx-cursor: hand; " +
                "-fx-scale-x: 1.05; " +
                "-fx-scale-y: 1.05;"
            );
        });
        
        card.setOnMouseExited(e -> {
            card.setStyle(
                "-fx-background-color: rgba(30, 30, 45, 0.6); " +
                "-fx-background-radius: 12px; " +
                "-fx-border-color: rgba(255, 255, 255, 0.1); " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 12px; " +
                "-fx-cursor: hand; " +
                "-fx-scale-x: 1.0; " +
                "-fx-scale-y: 1.0;"
            );
        });
        
        return card;
    }
    
    /**
     * Загружает скин для гардероба
     */
    private void loadUserSkinForWardrobe(PlayerModelView preview) {
        if (username == null || username.isEmpty() || preview == null) {
            return;
        }
        
        javafx.concurrent.Task<javafx.scene.image.Image> loadTask = new javafx.concurrent.Task<javafx.scene.image.Image>() {
            @Override
            protected javafx.scene.image.Image call() throws Exception {
                String skinUrl = "https://crafatar.com/skins/" + username;
                return new javafx.scene.image.Image(skinUrl, true);
            }
            
            @Override
            protected void succeeded() {
                javafx.scene.image.Image skinImage = getValue();
                if (skinImage != null && !skinImage.isError()) {
                    Platform.runLater(() -> {
                        preview.setSkin(skinImage);
                    });
                }
            }
        };
        
        new Thread(loadTask).start();
    }
    
    /**
     * Создает Forum секцию
     */
    private void createForumSection() {
        forumSection = new ForumController(username);
        forumSection.setVisible(false);
        // Убеждаемся, что BorderPane заполняет все пространство
        forumSection.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        forumSection.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    }
    
    /**
     * Создает Settings секцию
     */
    private void createSettingsSection() {
        settingsSection = new VBox(25);
        settingsSection.setPadding(new Insets(30));
        settingsSection.setStyle("-fx-background-color: transparent;");
        settingsSection.setVisible(false);
        
        Label titleLabel = new Label("Настройки");
        titleLabel.setStyle(
            "-fx-font-size: 32px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #FFFFFF; " +
            "-fx-font-family: 'Minecraft Unicode', monospace;"
        );
        
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        VBox contentBox = new VBox(20);
        contentBox.setPadding(new Insets(10));
        
        // Группа: Minecraft настройки
        VBox minecraftGroup = createSettingsGroup("Minecraft", createMinecraftSettings());
        
        // Группа: API настройки
        VBox apiGroup = createSettingsGroup("API Сервер", createApiSettings());
        
        // Группа: Уведомления
        VBox notificationsGroup = createSettingsGroup("Уведомления", createNotificationSettings());
        
        // Кнопка "О программе"
        Button aboutButton = new Button("О программе");
        aboutButton.getStyleClass().add("button-secondary");
        aboutButton.setOnAction(e -> showAboutDialog());
        AnimationHelper.liquidPress(aboutButton);
        
        contentBox.getChildren().addAll(minecraftGroup, apiGroup, notificationsGroup, aboutButton);
        scrollPane.setContent(contentBox);
        
        settingsSection.getChildren().addAll(titleLabel, scrollPane);
    }
    
    /**
     * Создает группу настроек с заголовком
     */
    private VBox createSettingsGroup(String title, VBox content) {
        VBox group = new VBox(15);
        group.setPadding(new Insets(20));
        group.setStyle(
            "-fx-background-color: rgba(30, 30, 45, 0.6); " +
            "-fx-background-radius: 12px; " +
            "-fx-border-color: rgba(255, 255, 255, 0.1); " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 12px;"
        );
        
        Label groupTitle = new Label(title);
        groupTitle.setStyle(
            "-fx-font-size: 20px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #FFFFFF;"
        );
        
        group.getChildren().addAll(groupTitle, content);
        return group;
    }
    
    /**
     * Создает настройки Minecraft
     */
    private VBox createMinecraftSettings() {
        VBox settings = new VBox(15);
        
        // Память
        HBox memoryContainer = new HBox(10);
        memoryContainer.setAlignment(Pos.CENTER_LEFT);
        
        Label memoryLabel = new Label("Память (МБ):");
        memoryLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #FFFFFF;");
        memoryLabel.setPrefWidth(200);
        
        memoryField = new TextField("4096");
        memoryField.setPrefWidth(150);
        memoryField.getStyleClass().add("text-field");
        
        memoryContainer.getChildren().addAll(memoryLabel, memoryField);
        
        // Java Runtime путь (заглушка для будущей реализации)
        HBox javaContainer = new HBox(10);
        javaContainer.setAlignment(Pos.CENTER_LEFT);
        
        Label javaLabel = new Label("Java Runtime:");
        javaLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #FFFFFF;");
        javaLabel.setPrefWidth(200);
        
        Label javaPathLabel = new Label("Автоматически");
        javaPathLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #A0A0B0;");
        
        javaContainer.getChildren().addAll(javaLabel, javaPathLabel);
        
        settings.getChildren().addAll(memoryContainer, javaContainer);
        return settings;
    }
    
    /**
     * Создает настройки API
     */
    private VBox createApiSettings() {
        VBox settings = new VBox(15);
        
        com.horizon.launcher.utils.ConfigLoader config = com.horizon.launcher.utils.ConfigLoader.getInstance();
        
        // URL API сервера
        HBox apiUrlContainer = new HBox(10);
        apiUrlContainer.setAlignment(Pos.CENTER_LEFT);
        
        Label apiUrlLabel = new Label("API URL:");
        apiUrlLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #FFFFFF;");
        apiUrlLabel.setPrefWidth(200);
        
        TextField apiUrlField = new TextField(config.get("api.server.url", "http://localhost:3000"));
        apiUrlField.setPrefWidth(300);
        apiUrlField.getStyleClass().add("text-field");
        
        apiUrlContainer.getChildren().addAll(apiUrlLabel, apiUrlField);
        
        // Timeout настройки
        HBox timeoutContainer = new HBox(10);
        timeoutContainer.setAlignment(Pos.CENTER_LEFT);
        
        Label timeoutLabel = new Label("Timeout (сек):");
        timeoutLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #FFFFFF;");
        timeoutLabel.setPrefWidth(200);
        
        TextField timeoutField = new TextField(String.valueOf(config.getInt("api.timeout.connect", 10)));
        timeoutField.setPrefWidth(150);
        timeoutField.getStyleClass().add("text-field");
        
        timeoutContainer.getChildren().addAll(timeoutLabel, timeoutField);
        
        settings.getChildren().addAll(apiUrlContainer, timeoutContainer);
        return settings;
    }
    
    /**
     * Создает настройки уведомлений
     */
    private VBox createNotificationSettings() {
        VBox settings = new VBox(15);
        
        // Уведомления о новостях
        HBox newsContainer = new HBox(10);
        newsContainer.setAlignment(Pos.CENTER_LEFT);
        
        Label newsLabel = new Label("Уведомления о новостях:");
        newsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #FFFFFF;");
        newsLabel.setPrefWidth(200);
        
        CheckBox newsCheckbox = new CheckBox("Включить");
        newsCheckbox.setSelected(true);
        newsCheckbox.setStyle("-fx-text-fill: #FFFFFF;");
        
        newsContainer.getChildren().addAll(newsLabel, newsCheckbox);
        
        // Уведомления о сообщениях форума
        HBox forumContainer = new HBox(10);
        forumContainer.setAlignment(Pos.CENTER_LEFT);
        
        Label forumLabel = new Label("Уведомления форума:");
        forumLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #FFFFFF;");
        forumLabel.setPrefWidth(200);
        
        CheckBox forumCheckbox = new CheckBox("Включить");
        forumCheckbox.setSelected(true);
        forumCheckbox.setStyle("-fx-text-fill: #FFFFFF;");
        
        forumContainer.getChildren().addAll(forumLabel, forumCheckbox);
        
        settings.getChildren().addAll(newsContainer, forumContainer);
        return settings;
    }
    
    /**
     * Показывает диалог "О программе"
     */
    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("О программе");
        alert.setHeaderText("Horizon Launcher");
        
        com.horizon.launcher.utils.ConfigLoader config = com.horizon.launcher.utils.ConfigLoader.getInstance();
        String version = config.get("launcher.version", "1.0.0");
        
        alert.setContentText(
            "Версия: " + version + "\n\n" +
            "Лаунчер для серверов Horizon\n" +
            "Поддержка Minecraft 1.21 - 1.21.10"
        );
        
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle(
            "-fx-background-color: rgba(30, 30, 45, 0.95); " +
            "-fx-background-radius: 10px; " +
            "-fx-text-fill: #FFFFFF;"
        );
        
        alert.showAndWait();
    }
    
    /**
     * Создает кнопку PLAY
     */
    private void createPlayButton() {
        // Контейнер для кнопки PLAY (внизу справа)
        AnchorPane playButtonContainer = new AnchorPane();
        playButtonContainer.setPrefHeight(100);
        playButtonContainer.setMinHeight(100);
        playButtonContainer.setMaxHeight(100);
        
        playButton = new Button("ИГРАТЬ");
        playButton.setPrefSize(200, 60);
        playButton.setStyle(
            "-fx-background-color: linear-gradient(to right, #667eea, #764ba2); " + // Primary Gradient из tech.md
            "-fx-background-radius: 15px; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 20px; " +
            "-fx-font-weight: bold; " +
            "-fx-font-family: 'Minecraft Unicode', monospace; " +
            "-fx-cursor: hand;"
        );
        
        // Neon glow эффект
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web("#667eea"));
        glow.setRadius(30);
        glow.setSpread(0.5);
        playButton.setEffect(glow);
        
        // Pulse glow анимация
        AnimationHelper.pulseGlow(playButton);
        
        playButton.setOnAction(e -> launchGame());
        AnimationHelper.liquidPress(playButton);
        
        // Размещаем кнопку внизу справа
        AnchorPane.setRightAnchor(playButton, 30.0);
        AnchorPane.setBottomAnchor(playButton, 20.0);
        
        playButtonContainer.getChildren().add(playButton);
        
        // Индикатор прогресса и статус
        progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(40, 40);
        progressIndicator.setVisible(false);
        AnchorPane.setRightAnchor(progressIndicator, 250.0);
        AnchorPane.setBottomAnchor(progressIndicator, 30.0);
        
        statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #A0A0B0;");
        statusLabel.setVisible(false);
        AnchorPane.setRightAnchor(statusLabel, 250.0);
        AnchorPane.setBottomAnchor(statusLabel, 75.0);
        
        playButtonContainer.getChildren().addAll(progressIndicator, statusLabel);
        
        rootContainer.setBottom(playButtonContainer);
    }
    
    /**
     * Показывает указанную секцию с плавным переходом
     */
    private void showSection(String section) {
        if (activeSection.equals(section)) {
            return; // Уже показана
        }
        
        // Получаем текущую видимую секцию для fadeOut
        Node currentVisibleSection = null;
        switch (activeSection) {
            case "home":
                if (homeSectionInitialized) {
                    currentVisibleSection = homeSection;
                }
                break;
            case "store":
                if (storeSectionInitialized) {
                    currentVisibleSection = storeSection;
                }
                break;
            case "wardrobe":
                if (wardrobeSectionInitialized) {
                    currentVisibleSection = wardrobeSection;
                }
                break;
            case "forum":
                if (forumSectionInitialized) {
                    currentVisibleSection = forumSection;
                }
                break;
            case "settings":
                if (settingsSectionInitialized) {
                    currentVisibleSection = settingsSection;
                }
                break;
        }
        
        // Получаем целевую секцию (создаем лениво, если нужно)
        Node targetSection = null;
        Button targetButton = null;
        switch (section) {
            case "home":
                ensureHomeSection();
                targetSection = homeSection;
                targetButton = homeButton;
                break;
            case "store":
                ensureStoreSection();
                targetSection = storeSection;
                targetButton = storeButton;
                break;
            case "wardrobe":
                ensureWardrobeSection();
                targetSection = wardrobeSection;
                targetButton = wardrobeButton;
                break;
            case "forum":
                ensureForumSection();
                targetSection = forumSection;
                targetButton = forumButton;
                break;
            case "settings":
                ensureSettingsSection();
                targetSection = settingsSection;
                targetButton = settingsButton;
                break;
        }
        
        if (targetSection == null) {
            return;
        }
        
        // Сохраняем final копии для использования в lambda
        final Node finalTargetSection = targetSection;
        final Button finalTargetButton = targetButton;
        final String finalSection = section;
        
        // Если есть текущая видимая секция и она отличается от целевой, делаем плавный переход
        if (currentVisibleSection != null && currentVisibleSection.isVisible() && currentVisibleSection != finalTargetSection) {
            // Fade out текущей секции
            AnimationHelper.fadeOut(currentVisibleSection);
            
            // После fadeOut показываем новую секцию
            javafx.animation.Timeline switchTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(300), e -> {
                    // Скрываем все секции
                    homeSection.setVisible(false);
                    storeSection.setVisible(false);
                    wardrobeSection.setVisible(false);
                    forumSection.setVisible(false);
                    settingsSection.setVisible(false);
                    
                    // Показываем целевую секцию с fadeIn
                    finalTargetSection.setVisible(true);
                    finalTargetSection.setOpacity(0);
                    AnimationHelper.fadeIn(finalTargetSection);
                    
                    if (finalTargetButton != null) {
                        updateActiveNavButton(finalTargetButton);
                    }
                    
                    activeSection = finalSection;
                })
            );
            switchTimeline.play();
        } else {
            // Если нет текущей секции или это первый запуск, просто показываем целевую
            homeSection.setVisible(false);
            storeSection.setVisible(false);
            wardrobeSection.setVisible(false);
            forumSection.setVisible(false);
            settingsSection.setVisible(false);
            
            finalTargetSection.setVisible(true);
            finalTargetSection.setOpacity(1.0);
            
            if (finalTargetButton != null) {
                updateActiveNavButton(finalTargetButton);
            }
            
            activeSection = finalSection;
        }
    }
    
    /**
     * Запускает игру с выбранным сервером
     */
    private void launchGame() {
        String version = selectedServerType.getMinecraftVersion();
        
        int memory;
        try {
            memory = Integer.parseInt(memoryField.getText());
            if (memory < 1024 || memory > 8192) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            showError("Память должна быть от 1024 до 8192 МБ");
            return;
        }
        
        // Обновляем UI
        playButton.setDisable(true);
        progressIndicator.setVisible(true);
        statusLabel.setVisible(true);
        statusLabel.setText("Подготовка к запуску...");
        
        // Запускаем игру асинхронно
        CompletableFuture<Process> launchFuture = gameLauncher.launchMinecraft(
                version, username, accessToken, uuid, memory
        );
        
        launchFuture.thenAccept(process -> {
            Platform.runLater(() -> {
                statusLabel.setText("Minecraft запущен! Закрытие лаунчера...");
                progressIndicator.setVisible(false);
                
                // Закрываем лаунчер через 3 секунды
                new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                        Platform.runLater(() -> {
                            stage.close();
                        });
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            });
        }).exceptionally(throwable -> {
            Platform.runLater(() -> {
                logger.error("Ошибка при запуске Minecraft", throwable);
                playButton.setDisable(false);
                progressIndicator.setVisible(false);
                
                String errorMessage = throwable.getMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "Неизвестная ошибка при запуске Minecraft";
                }
                
                statusLabel.setText("Ошибка: " + errorMessage);
                statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #FF6B6B;");
                showError("Ошибка запуска: " + errorMessage);
            });
            return null;
        });
    }
    
    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setHeaderText(null);
            alert.setContentText(message);
            
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setStyle(
                "-fx-background-color: rgba(30, 30, 45, 0.95); " +
                "-fx-background-radius: 10px; " +
                "-fx-text-fill: #FFFFFF;"
            );
            
            alert.showAndWait();
        });
    }
    
    public void show() {
        stage.show();
        AnimationHelper.fadeIn(rootContainer);
    }
    
    public void close() {
        stage.close();
    }
}
