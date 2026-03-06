package com.horizon.launcher.ui.components;

import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Компонент для отображения новостей с parallax эффектом
 * Согласно tech.md: 4 большие прямоугольные карточки с parallax изображениями
 */
public class NewsSlider extends VBox {
    private static final Logger logger = LoggerFactory.getLogger(NewsSlider.class);
    
    private GridPane newsGrid;
    private List<NewsItem> newsItems;
    private int currentIndex = 0;
    
    public NewsSlider() {
        this.newsItems = new ArrayList<>();
        createUI();
    }
    
    /**
     * Создает UI компонента
     */
    private void createUI() {
        this.setSpacing(15);
        this.setPadding(new Insets(0));
        
        Label titleLabel = new Label("Новости");
        titleLabel.setStyle(
            "-fx-font-size: 24px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #FFFFFF; " + // Text Main из tech.md
            "-fx-font-family: 'Minecraft Unicode', monospace;"
        );
        
        // Grid для новостей (2x2)
        newsGrid = new GridPane();
        newsGrid.setHgap(15);
        newsGrid.setVgap(15);
        newsGrid.setAlignment(Pos.CENTER_LEFT);
        
        this.getChildren().addAll(titleLabel, newsGrid);
    }
    
    /**
     * Устанавливает новости
     */
    public void setNews(List<NewsItem> items) {
        this.newsItems = items != null ? items : new ArrayList<>();
        updateNewsDisplay();
    }
    
    /**
     * Обновляет отображение новостей
     */
    private void updateNewsDisplay() {
        newsGrid.getChildren().clear();
        
        // Показываем максимум 4 новости
        int count = Math.min(newsItems.size(), 4);
        
        for (int i = 0; i < count; i++) {
            NewsItem item = newsItems.get(i);
            VBox card = createNewsCard(item);
            newsGrid.add(card, i % 2, i / 2);
        }
        
        // Если новостей меньше 4, добавляем заглушки
        for (int i = count; i < 4; i++) {
            VBox placeholder = createPlaceholderCard("Новость " + (i + 1));
            newsGrid.add(placeholder, i % 2, i / 2);
        }
    }
    
    /**
     * Создает карточку новости с parallax эффектом
     */
    private VBox createNewsCard(NewsItem item) {
        VBox card = new VBox(0);
        card.setPrefSize(380, 200);
        card.setStyle(
            "-fx-background-color: rgba(30, 30, 45, 0.6); " + // Surface из tech.md
            "-fx-background-radius: 12px; " +
            "-fx-border-color: rgba(255, 255, 255, 0.1); " + // Borders из tech.md
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 12px; " +
            "-fx-overflow: hidden; " +
            "-fx-cursor: hand;"
        );
        
        // Контейнер для изображения с parallax эффектом
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefHeight(140);
        imageContainer.setStyle("-fx-background-color: rgba(102, 126, 234, 0.2);");
        
        ImageView imageView = new ImageView();
        imageView.setFitWidth(380);
        imageView.setFitHeight(200);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        
        // Загружаем изображение если есть URL
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            try {
                Image image = new Image(item.getImageUrl(), true); // true = фоновая загрузка
                imageView.setImage(image);
            } catch (Exception e) {
                logger.error("Ошибка при загрузке изображения новости: " + item.getImageUrl(), e);
            }
        }
        
        imageContainer.getChildren().add(imageView);
        
        // Parallax эффект при наведении
        card.setOnMouseEntered(e -> {
            TranslateTransition parallax = new TranslateTransition(Duration.millis(300), imageView);
            parallax.setByY(-10);
            parallax.play();
            
            card.setStyle(
                "-fx-background-color: rgba(30, 30, 45, 0.8); " +
                "-fx-background-radius: 12px; " +
                "-fx-border-color: rgba(255, 255, 255, 0.2); " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 12px; " +
                "-fx-overflow: hidden; " +
                "-fx-cursor: hand; " +
                "-fx-scale-x: 1.02; " +
                "-fx-scale-y: 1.02;"
            );
        });
        
        card.setOnMouseExited(e -> {
            TranslateTransition parallax = new TranslateTransition(Duration.millis(300), imageView);
            parallax.setByY(10);
            parallax.play();
            
            card.setStyle(
                "-fx-background-color: rgba(30, 30, 45, 0.6); " +
                "-fx-background-radius: 12px; " +
                "-fx-border-color: rgba(255, 255, 255, 0.1); " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 12px; " +
                "-fx-overflow: hidden; " +
                "-fx-cursor: hand; " +
                "-fx-scale-x: 1.0; " +
                "-fx-scale-y: 1.0;"
            );
        });
        
        // Overlay с текстом внизу
        VBox textOverlay = new VBox(5);
        textOverlay.setPadding(new Insets(10));
        textOverlay.setStyle(
            "-fx-background-color: linear-gradient(to top, rgba(0, 0, 0, 0.9), transparent);"
        );
        textOverlay.setAlignment(Pos.BOTTOM_LEFT);
        
        Label titleLabel = new Label(item.getTitle());
        titleLabel.setStyle(
            "-fx-font-size: 16px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #FFFFFF;"
        );
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(360);
        
        if (item.getDescription() != null && !item.getDescription().isEmpty()) {
            Label descLabel = new Label(item.getDescription());
            descLabel.setStyle(
                "-fx-font-size: 12px; " +
                "-fx-text-fill: #A0A0B0; " + // Text Muted
                "-fx-wrap-text: true;"
            );
            descLabel.setMaxWidth(360);
            textOverlay.getChildren().add(descLabel);
        }
        
        textOverlay.getChildren().add(0, titleLabel);
        
        // Stack для overlay поверх изображения
        StackPane cardContent = new StackPane();
        cardContent.getChildren().addAll(imageContainer, textOverlay);
        StackPane.setAlignment(textOverlay, Pos.BOTTOM_CENTER);
        
        card.getChildren().add(cardContent);
        
        // Добавляем liquidPress анимацию
        com.horizon.launcher.utils.AnimationHelper.liquidPress(card);
        
        return card;
    }
    
    /**
     * Создает заглушку для новости
     */
    private VBox createPlaceholderCard(String title) {
        NewsItem placeholder = new NewsItem(title, "Описание новости", null);
        return createNewsCard(placeholder);
    }
    
    /**
     * Обновить новости
     */
    public void refresh() {
        logger.info("Обновление новостей...");
        // Здесь можно добавить логику загрузки новых новостей с сервера
        // Пока просто перерисовываем существующие
        updateNewsDisplay();
    }
    
    /**
     * Класс для представления новости
     */
    public static class NewsItem {
        private String title;
        private String description;
        private String imageUrl;
        private String date;
        
        public NewsItem(String title, String description, String imageUrl) {
            this.title = title;
            this.description = description;
            this.imageUrl = imageUrl;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getImageUrl() {
            return imageUrl;
        }
        
        public String getDate() {
            return date;
        }
        
        public void setDate(String date) {
            this.date = date;
        }
    }
}
