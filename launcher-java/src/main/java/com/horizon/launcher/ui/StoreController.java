package com.horizon.launcher.ui;

import com.horizon.launcher.models.BlockbenchModelParser;
import com.horizon.launcher.models.ModelData;
import com.horizon.launcher.models.StoreItem;
import com.horizon.launcher.network.ApiClient;
import com.horizon.launcher.services.StoreService;
import com.horizon.launcher.utils.AnimationHelper;
import com.horizon.launcher.utils.DataCache;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Контроллер магазина с карточками товаров
 */
public class StoreController extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(StoreController.class);
    
    private final String username;
    private final StoreService storeService;
    
    // UI компоненты
    private javafx.scene.layout.Pane itemsFlowPane; // Может быть FlowPane или TilePane
    private HBox categoryContainer;
    private Label balanceLabel;
    private Label balanceValueLabel;
    private PlayerModelView previewModelView;
    private VBox previewContainer;
    private Button topUpButton;
    
    // Данные
    private List<StoreItem> allItems;
    private List<StoreItem> currentItems;
    private String selectedCategory;
    private int currentBalance;
    private StoreItem previewingItem; // Текущий товар в предпросмотре
    
    public StoreController(String username) {
        this.username = username;
        this.storeService = StoreService.getInstance();
        this.allItems = new ArrayList<>();
        this.currentItems = new ArrayList<>();
        this.selectedCategory = "Все";
        this.currentBalance = 0;
        
        createUI();
        loadBalance();
        loadStoreItems();
    }
    
    /**
     * Создать UI
     */
    private void createUI() {
        this.setStyle("-fx-background-color: transparent;");
        
        // Верхняя панель с балансом и категориями
        VBox topPanel = createTopPanel();
        this.setTop(topPanel);
        
        // Центральная область с товарами
        ScrollPane scrollPane = createItemsScrollPane();
        this.setCenter(scrollPane);
        
        // Боковая панель с предпросмотром
        VBox sidebar = createPreviewSidebar();
        this.setRight(sidebar);
        
        // Применяем стили
        this.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
    }
    
    /**
     * Создать верхнюю панель с балансом и категориями
     */
    private VBox createTopPanel() {
        VBox topPanel = new VBox(15);
        topPanel.setPadding(new Insets(20));
        topPanel.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 10px;");
        
        // Панель с балансом
        HBox balancePanel = new HBox(15);
        balancePanel.setAlignment(Pos.CENTER_LEFT);
        
        Label balanceLabelText = new Label("Баланс:");
        balanceLabelText.getStyleClass().add("store-balance-label");
        
        balanceValueLabel = new Label("0");
        balanceValueLabel.getStyleClass().add("store-balance");
        
        topUpButton = new Button("Пополнить");
        topUpButton.getStyleClass().add("button");
        topUpButton.setOnAction(e -> handleTopUp());
        
        balancePanel.getChildren().addAll(balanceLabelText, balanceValueLabel, topUpButton);
        
        // Категории
        categoryContainer = new HBox(10);
        categoryContainer.setAlignment(Pos.CENTER_LEFT);
        createCategoryButtons();
        
        topPanel.getChildren().addAll(balancePanel, categoryContainer);
        
        return topPanel;
    }
    
    /**
     * Создать кнопки категорий
     */
    private void createCategoryButtons() {
        categoryContainer.getChildren().clear();
        
        List<String> categories = new ArrayList<>();
        categories.add("Все");
        
        // Собираем уникальные категории из товаров
        Map<String, Boolean> categoryMap = new HashMap<>();
        for (StoreItem item : allItems) {
            if (item.getCategory() != null && !item.getCategory().isEmpty()) {
                categoryMap.put(item.getCategory(), true);
            }
        }
        categories.addAll(categoryMap.keySet());
        
        ToggleGroup categoryGroup = new ToggleGroup();
        
        for (String category : categories) {
            ToggleButton categoryButton = new ToggleButton(category);
            categoryButton.getStyleClass().add("store-category-button");
            categoryButton.setToggleGroup(categoryGroup);
            categoryButton.setSelected(category.equals(selectedCategory));
            categoryButton.setOnAction(e -> {
                selectedCategory = category;
                filterItemsByCategory();
            });
            
            categoryContainer.getChildren().add(categoryButton);
        }
    }
    
    /**
     * Создать ScrollPane с товарами
     * Использует TilePane для сетки товаров (оптимизировано для производительности)
     */
    private ScrollPane createItemsScrollPane() {
        // Используем TilePane вместо FlowPane для лучшей производительности
        // TilePane более эффективен для больших списков
        javafx.scene.layout.TilePane tilePane = new javafx.scene.layout.TilePane();
        tilePane.getStyleClass().add("store-items-pane");
        tilePane.setHgap(15);
        tilePane.setVgap(15);
        tilePane.setPadding(new Insets(20));
        tilePane.setPrefColumns(4); // 4 колонки по умолчанию
        tilePane.setPrefTileWidth(200);
        tilePane.setPrefTileHeight(280);
        
        // Сохраняем ссылку для обновления
        itemsFlowPane = tilePane;
        
        ScrollPane scrollPane = new ScrollPane(tilePane);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        // Оптимизация: кэширование для лучшей производительности
        tilePane.setCache(true);
        tilePane.setCacheShape(true);
        
        return scrollPane;
    }
    
    /**
     * Создать боковую панель с предпросмотром
     */
    private VBox createPreviewSidebar() {
        previewContainer = new VBox(20);
        previewContainer.setPadding(new Insets(20));
        previewContainer.setPrefWidth(300);
        previewContainer.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 10px;");
        
        Label previewLabel = new Label("Предпросмотр");
        previewLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        // 3D модель для предпросмотра
        previewModelView = new PlayerModelView(260, 260, null);
        
        // Загружаем скин пользователя
        loadUserSkin();
        
        previewContainer.getChildren().addAll(previewLabel, previewModelView);
        
        return previewContainer;
    }
    
    /**
     * Загрузить скин пользователя
     */
    private void loadUserSkin() {
        if (username != null && !username.isEmpty()) {
            try {
                // Загружаем скин асинхронно
                Task<Image> loadTask = new Task<Image>() {
                    @Override
                    protected Image call() throws Exception {
                        String skinUrl = "https://crafatar.com/skins/" + username;
                        return new Image(skinUrl, true); // true = фоновая загрузка
                    }
                    
                    @Override
                    protected void succeeded() {
                        Image skinImage = getValue();
                        if (skinImage != null && !skinImage.isError()) {
                            previewModelView.setSkin(skinImage);
                        }
                    }
                };
                new Thread(loadTask).start();
            } catch (Exception e) {
                logger.error("Ошибка при загрузке скина пользователя", e);
            }
        }
    }
    
    /**
     * Загрузить баланс
     */
    private void loadBalance() {
        storeService.getBalance(username, new StoreService.BalanceCallback() {
            @Override
            public void onSuccess(int balance) {
                Platform.runLater(() -> {
                    currentBalance = balance;
                    balanceValueLabel.setText(String.valueOf(balance) + " Хорики");
                });
            }
            
            @Override
            public void onError(String error) {
                logger.error("Ошибка при загрузке баланса: " + error);
            }
        });
    }
    
    /**
     * Загрузить товары из магазина
     */
    private void loadStoreItems() {
        storeService.getAvailableItems(new StoreService.StoreItemsCallback() {
            @Override
            public void onSuccess(List<StoreItem> items) {
                Platform.runLater(() -> {
                    allItems = items;
                    createCategoryButtons();
                    filterItemsByCategory();
                });
            }
            
            @Override
            public void onError(String error) {
                logger.error("Ошибка при загрузке товаров: " + error);
                Platform.runLater(() -> {
                    Label errorLabel = new Label("Ошибка загрузки товаров: " + error);
                    errorLabel.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 14px;");
                    itemsFlowPane.getChildren().add(errorLabel);
                });
            }
        });
    }
    
    /**
     * Фильтровать товары по категории
     */
    private void filterItemsByCategory() {
        currentItems.clear();
        
        if ("Все".equals(selectedCategory)) {
            currentItems.addAll(allItems);
        } else {
            for (StoreItem item : allItems) {
                if (selectedCategory.equals(item.getCategory())) {
                    currentItems.add(item);
                }
            }
        }
        
        renderItems();
    }
    
    /**
     * Отрисовать товары
     */
    private void renderItems() {
        itemsFlowPane.getChildren().clear();
        
        for (StoreItem item : currentItems) {
            VBox itemCard = createItemCard(item);
            itemsFlowPane.getChildren().add(itemCard);
            AnimationHelper.fadeIn(itemCard);
        }
    }
    
    /**
     * Создать карточку товара
     */
    private VBox createItemCard(StoreItem item) {
        VBox card = new VBox(10);
        card.getStyleClass().add("store-item-card");
        card.setPrefWidth(200);
        card.setPrefHeight(280);
        
        // Бейджи (новинка, скидка)
        HBox badgesBox = new HBox(5);
        badgesBox.setAlignment(Pos.TOP_LEFT);
        
        if (item.isNew()) {
            Label newBadge = new Label("НОВИНКА");
            newBadge.getStyleClass().addAll("store-item-badge", "store-item-badge-new");
            badgesBox.getChildren().add(newBadge);
        }
        
        if (item.isDiscounted()) {
            Label discountBadge = new Label("-" + item.getDiscountPercentage() + "%");
            discountBadge.getStyleClass().addAll("store-item-badge", "store-item-badge-discount");
            badgesBox.getChildren().add(discountBadge);
        }
        
        // Изображение товара
        ImageView imageView = new ImageView();
        imageView.setFitWidth(176);
        imageView.setFitHeight(176);
        imageView.setPreserveRatio(true);
        imageView.getStyleClass().add("store-item-image");
        
        // Асинхронная загрузка изображения
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            loadItemImageAsync(item.getImageUrl(), imageView);
        } else {
            // Заглушка, если нет изображения
            imageView.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1);");
        }
        
        // Название
        Label nameLabel = new Label(item.getName());
        nameLabel.getStyleClass().add("store-item-name");
        
        // Описание
        Label descLabel = new Label(item.getDescription() != null ? item.getDescription() : "");
        descLabel.getStyleClass().add("store-item-description");
        descLabel.setMaxHeight(40);
        descLabel.setWrapText(true);
        
        // Цена
        HBox priceBox = new HBox(10);
        priceBox.setAlignment(Pos.CENTER_LEFT);
        
        int displayPrice = item.isDiscounted() && item.getDiscountPercentage() > 0 
            ? item.getPrice() - (item.getPrice() * item.getDiscountPercentage() / 100)
            : item.getPrice();
        Label priceLabel = new Label(String.valueOf(displayPrice) + " Хорики");
        priceLabel.getStyleClass().add("store-item-price");
        
        if (item.isDiscounted() && item.getDiscountPercentage() > 0) {
            Label oldPriceLabel = new Label(String.valueOf(item.getPrice()));
            oldPriceLabel.getStyleClass().add("store-item-price-old");
            priceBox.getChildren().addAll(oldPriceLabel, priceLabel);
        } else {
            priceBox.getChildren().add(priceLabel);
        }
        
        // Кнопка "Примерить"
        Button previewButton = new Button("Примерить");
        previewButton.getStyleClass().add("store-item-preview-button");
        previewButton.setOnAction(e -> handlePreviewItem(item));
        
        // Кнопка "Купить"
        Button buyButton = new Button("Купить");
        buyButton.getStyleClass().add("button");
        buyButton.setStyle(buyButton.getStyle() + "-fx-background-color: rgba(76, 175, 80, 0.7);");
        buyButton.setOnAction(e -> handleBuyItem(item));
        
        HBox buttonsBox = new HBox(5);
        buttonsBox.getChildren().addAll(previewButton, buyButton);
        
        card.getChildren().addAll(badgesBox, imageView, nameLabel, descLabel, priceBox, buttonsBox);
        
        // Применяем стиль редкости
        String rarityClass = item.getRarityCssClass();
        card.getStyleClass().add(rarityClass);
        
        return card;
    }
    
    /**
     * Асинхронная загрузка изображения товара с кэшированием
     */
    private void loadItemImageAsync(String imageUrl, ImageView imageView) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }
        
        // Проверяем кэш
        DataCache cache = DataCache.getInstance();
        Image cachedImage = cache.getImage(imageUrl);
        if (cachedImage != null) {
            Platform.runLater(() -> imageView.setImage(cachedImage));
            return;
        }
        
        // Загружаем изображение асинхронно
        Task<Image> loadTask = new Task<Image>() {
            @Override
            protected Image call() throws Exception {
                try {
                    Image image = new Image(imageUrl, true); // true = фоновая загрузка
                    // Ждем завершения загрузки
                    while (!image.isError() && image.getProgress() < 1.0) {
                        Thread.sleep(50);
                    }
                    return image;
                } catch (Exception e) {
                    logger.error("Ошибка при загрузке изображения: " + imageUrl, e);
                    return null;
                }
            }
            
            @Override
            protected void succeeded() {
                Image image = getValue();
                if (image != null && !image.isError()) {
                    // Сохраняем в кэш
                    cache.putImage(imageUrl, image);
                    Platform.runLater(() -> imageView.setImage(image));
                }
            }
        };
        
        new Thread(loadTask).start();
    }
    
    /**
     * Обработка предпросмотра товара
     */
    private void handlePreviewItem(StoreItem item) {
        logger.info("Предпросмотр товара: {}", item.getName());
        previewingItem = item;
        
        // Здесь нужно применить аксессуар к 3D модели
        // Сохраняем текущее состояние и добавляем временный аксессуар
        if (item.getPivotPoint() != null) {
            try {
                // Загружаем модель и текстуру аксессуара асинхронно
                if (item.getModelPath() != null && item.getTexturePath() != null) {
                    loadAccessoryForPreview(item);
                } else {
                    logger.debug("Товар {} не имеет модели или текстуры", item.getName());
                }
            } catch (Exception e) {
                logger.error("Ошибка при применении аксессуара", e);
            }
        }
    }
    
    /**
     * Загрузить аксессуар для предпросмотра
     */
    private void loadAccessoryForPreview(StoreItem item) {
        Task<PreviewAccessoryData> task = new Task<PreviewAccessoryData>() {
            @Override
            protected PreviewAccessoryData call() {
                ModelData modelData = null;
                Image textureImage = null;

                try {
                    modelData = loadModelData(item.getModelPath());
                } catch (Exception e) {
                    logger.warn("Не удалось загрузить модель аксессуара {}", item.getModelPath(), e);
                }

                try {
                    textureImage = loadTextureImage(item.getTexturePath());
                } catch (Exception e) {
                    logger.warn("Не удалось загрузить текстуру аксессуара {}", item.getTexturePath(), e);
                }

                return new PreviewAccessoryData(modelData, textureImage);
            }

            @Override
            protected void succeeded() {
                PreviewAccessoryData data = getValue();
                previewModelView.attachAccessory(item.getPivotPoint(), data.modelData, data.textureImage);
                logger.debug("Применен аксессуар {} к кости {}", item.getName(), item.getPivotPoint());
            }

            @Override
            protected void failed() {
                logger.error("Ошибка при загрузке аксессуара для предпросмотра {}", item.getName(), getException());
            }
        };

        Thread loaderThread = new Thread(task, "store-preview-loader");
        loaderThread.setDaemon(true);
        loaderThread.start();
    }

    private ModelData loadModelData(String modelPath) throws Exception {
        if (modelPath == null || modelPath.isBlank()) {
            return null;
        }

        // Абсолютный URL
        if (modelPath.startsWith("http://") || modelPath.startsWith("https://")) {
            try (InputStream is = new URL(modelPath).openStream()) {
                return BlockbenchModelParser.parseFromStream(is);
            }
        }

        // Локальный файл
        Path localPath = Paths.get(modelPath);
        if (localPath.toFile().exists()) {
            return BlockbenchModelParser.parseFromPath(localPath);
        }

        // Относительный путь API (например, /uploads/...)
        if (modelPath.startsWith("/")) {
            String url = ApiClient.getInstance().getBaseUrl() + modelPath;
            try (InputStream is = new URL(url).openStream()) {
                return BlockbenchModelParser.parseFromStream(is);
            }
        }

        return null;
    }

    private Image loadTextureImage(String texturePath) {
        if (texturePath == null || texturePath.isBlank()) {
            return null;
        }

        String imageUrl = texturePath;
        if (texturePath.startsWith("/")) {
            imageUrl = ApiClient.getInstance().getBaseUrl() + texturePath;
        } else if (!texturePath.startsWith("http://") && !texturePath.startsWith("https://")) {
            Path localPath = Paths.get(texturePath);
            if (localPath.toFile().exists()) {
                imageUrl = localPath.toUri().toString();
            }
        }

        Image image = new Image(imageUrl, true);
        if (image.isError()) {
            logger.warn("Не удалось загрузить текстуру: {}", texturePath);
            return null;
        }
        return image;
    }

    private static class PreviewAccessoryData {
        private final ModelData modelData;
        private final Image textureImage;

        private PreviewAccessoryData(ModelData modelData, Image textureImage) {
            this.modelData = modelData;
            this.textureImage = textureImage;
        }
    }
    
    /**
     * Обработка покупки товара
     */
    private void handleBuyItem(StoreItem item) {
        int price = item.isDiscounted() && item.getDiscountPercentage() > 0 
            ? item.getPrice() - (item.getPrice() * item.getDiscountPercentage() / 100)
            : item.getPrice();
        
        if (currentBalance < price) {
            showAlert("Недостаточно средств", 
                     "Вам нужно " + price + " Хориков для покупки этого товара.",
                     Alert.AlertType.WARNING);
            return;
        }
        
        storeService.purchaseItem(username, item.getId(), price, 
            new StoreService.PurchaseCallback() {
                @Override
                public void onSuccess(int newBalance) {
                    Platform.runLater(() -> {
                        currentBalance = newBalance;
                        balanceValueLabel.setText(String.valueOf(newBalance) + " Хорики");
                        showAlert("Успешно!", "Товар \"" + item.getName() + "\" успешно куплен!", 
                                 Alert.AlertType.INFORMATION);
                    });
                }
                
                @Override
                public void onError(String error) {
                    Platform.runLater(() -> {
                        showAlert("Ошибка", "Не удалось купить товар: " + error, 
                                 Alert.AlertType.ERROR);
                    });
                }
            });
    }
    
    /**
     * Обработка пополнения баланса
     */
    private void handleTopUp() {
        storeService.generatePaymentLink(username, 100, new StoreService.PaymentLinkCallback() {
            @Override
            public void onSuccess(String paymentUrl) {
                Platform.runLater(() -> {
                    try {
                        // Открываем ссылку в системном браузере
                        Desktop desktop = Desktop.getDesktop();
                        if (desktop.isSupported(Desktop.Action.BROWSE)) {
                            desktop.browse(new URI(paymentUrl));
                        } else {
                            // Альтернативный способ для некоторых систем
                            Runtime.getRuntime().exec("xdg-open " + paymentUrl);
                        }
                    } catch (Exception e) {
                        logger.error("Ошибка при открытии ссылки на оплату", e);
                        showAlert("Ошибка", "Не удалось открыть ссылку на оплату", 
                                 Alert.AlertType.ERROR);
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    showAlert("Ошибка", "Не удалось сгенерировать ссылку на оплату: " + error, 
                             Alert.AlertType.ERROR);
                });
            }
        });
    }
    
    /**
     * Показать диалоговое окно
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Обновить данные магазина
     */
    public void refresh() {
        logger.info("Обновление магазина...");
        loadBalance();
        loadStoreItems();
    }
}
