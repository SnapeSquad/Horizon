package com.horizon.launcher.ui.components;

import com.horizon.launcher.models.StoreItem;
import com.horizon.launcher.ui.StoreController;
import com.horizon.launcher.utils.AnimationHelper;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Виртуализированный список товаров для магазина
 * Использует ListView с кастомными ячейками для эффективного рендеринга больших списков
 */
public class VirtualizedStoreList extends ListView<StoreItem> {
    private static final Logger logger = LoggerFactory.getLogger(VirtualizedStoreList.class);
    
    private final StoreController storeController;
    
    public VirtualizedStoreList(StoreController storeController) {
        this.storeController = storeController;
        
        // Настраиваем ListView для виртуализации
        this.setCellFactory(param -> new StoreItemCell());
        
        // Стилизация
        this.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-background-insets: 0; " +
            "-fx-padding: 20px;"
        );
        
        // ListView по умолчанию вертикальный
    }
    
    /**
     * Установить товары
     */
    public void setItems(List<StoreItem> items) {
        if (items == null || items.isEmpty()) {
            this.getItems().clear();
            return;
        }
        
        this.getItems().setAll(items);
        logger.debug("Установлено {} товаров в виртуализированный список", items.size());
    }
    
    /**
     * Кастомная ячейка для товара
     */
    private class StoreItemCell extends ListCell<StoreItem> {
        private HBox cellContainer;
        private VBox itemCard;
        private ImageView imageView;
        private Label nameLabel;
        private Label priceLabel;
        private HBox badgesBox;
        
        public StoreItemCell() {
            // Создаем контейнер для карточки товара
            cellContainer = new HBox();
            cellContainer.setAlignment(Pos.CENTER);
            cellContainer.setPadding(new Insets(5));
            
            // Создаем карточку товара (аналогично StoreController.createItemCard)
            itemCard = new VBox(10);
            itemCard.getStyleClass().add("store-item-card");
            itemCard.setPrefWidth(200);
            itemCard.setPrefHeight(280);
            itemCard.setMinWidth(200);
            itemCard.setMinHeight(280);
            
            // Бейджи
            badgesBox = new HBox(5);
            badgesBox.setAlignment(Pos.TOP_LEFT);
            
            // Изображение
            imageView = new ImageView();
            imageView.setFitWidth(176);
            imageView.setFitHeight(176);
            imageView.setPreserveRatio(true);
            imageView.getStyleClass().add("store-item-image");
            
            // Название
            nameLabel = new Label();
            nameLabel.setStyle(
                "-fx-font-size: 14px; " +
                "-fx-font-weight: bold; " +
                "-fx-text-fill: #FFFFFF; " +
                "-fx-wrap-text: true;"
            );
            nameLabel.setMaxWidth(176);
            
            // Цена
            priceLabel = new Label();
            priceLabel.setStyle(
                "-fx-font-size: 16px; " +
                "-fx-font-weight: bold; " +
                "-fx-text-fill: #00f2fe;"
            );
            
            itemCard.getChildren().addAll(badgesBox, imageView, nameLabel, priceLabel);
            cellContainer.getChildren().add(itemCard);
            
            // Добавляем анимацию при наведении
            itemCard.setOnMouseEntered(e -> {
                itemCard.setStyle(itemCard.getStyle() + " -fx-scale-x: 1.05; -fx-scale-y: 1.05;");
            });
            itemCard.setOnMouseExited(e -> {
                itemCard.setStyle(itemCard.getStyle().replaceAll("-fx-scale-[xy]:\\s*1\\.05;", ""));
            });
            
            // Обработчик клика
            itemCard.setOnMouseClicked(e -> {
                StoreItem item = getItem();
                if (item != null && storeController != null) {
                    // Вызываем метод предпросмотра из StoreController
                    Platform.runLater(() -> {
                        try {
                            java.lang.reflect.Method method = storeController.getClass()
                                .getDeclaredMethod("handlePreviewItem", StoreItem.class);
                            method.setAccessible(true);
                            method.invoke(storeController, item);
                        } catch (Exception ex) {
                            logger.warn("Не удалось вызвать handlePreviewItem", ex);
                        }
                    });
                }
            });
        }
        
        @Override
        protected void updateItem(StoreItem item, boolean empty) {
            super.updateItem(item, empty);
            
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            
            // Обновляем данные карточки
            nameLabel.setText(item.getName());
            priceLabel.setText(item.getPrice() + " Horikov");
            
            // Обновляем бейджи
            badgesBox.getChildren().clear();
            if (item.isNew()) {
                Label newBadge = new Label("НОВИНКА");
                newBadge.setStyle(
                    "-fx-background-color: #2ecc71; " +
                    "-fx-text-fill: white; " +
                    "-fx-padding: 2px 6px; " +
                    "-fx-font-size: 10px; " +
                    "-fx-font-weight: bold;"
                );
                badgesBox.getChildren().add(newBadge);
            }
            if (item.isDiscounted()) {
                Label discountBadge = new Label("-" + item.getDiscountPercentage() + "%");
                discountBadge.setStyle(
                    "-fx-background-color: #e74c3c; " +
                    "-fx-text-fill: white; " +
                    "-fx-padding: 2px 6px; " +
                    "-fx-font-size: 10px; " +
                    "-fx-font-weight: bold;"
                );
                badgesBox.getChildren().add(discountBadge);
            }
            
            // Загружаем изображение асинхронно
            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                loadImageAsync(item.getImageUrl());
            } else {
                imageView.setImage(null);
                imageView.setStyle("-fx-background-color: rgba(255, 255, 255, 0.1);");
            }
            
            setGraphic(cellContainer);
        }
        
        private void loadImageAsync(String imageUrl) {
            javafx.concurrent.Task<Image> loadTask = new javafx.concurrent.Task<Image>() {
                @Override
                protected Image call() throws Exception {
                    return new Image(imageUrl, true);
                }
                
                @Override
                protected void succeeded() {
                    Image image = getValue();
                    if (image != null && !image.isError()) {
                        Platform.runLater(() -> {
                            imageView.setImage(image);
                        });
                    }
                }
            };
            new Thread(loadTask).start();
        }
    }
}
