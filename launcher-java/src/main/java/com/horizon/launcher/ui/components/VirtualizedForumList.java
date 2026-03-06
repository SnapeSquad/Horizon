package com.horizon.launcher.ui.components;

import com.horizon.launcher.models.ForumTopic;
import com.horizon.launcher.models.UserRole;
import com.horizon.launcher.ui.ForumController;
import com.horizon.launcher.utils.AnimationHelper;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Виртуализированный список тем форума
 * Использует ListView с кастомными ячейками для эффективного рендеринга больших списков
 */
public class VirtualizedForumList extends ListView<ForumTopic> {
    private static final Logger logger = LoggerFactory.getLogger(VirtualizedForumList.class);
    
    private final ForumController forumController;
    
    public VirtualizedForumList(ForumController forumController) {
        this.forumController = forumController;
        
        // Настраиваем ListView для виртуализации
        this.setCellFactory(param -> new ForumTopicCell());
        
        // Стилизация
        this.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-background-insets: 0; " +
            "-fx-padding: 10px;"
        );
        
        // ListView по умолчанию вертикальный
    }
    
    /**
     * Установить темы
     */
    public void setTopics(List<ForumTopic> topics) {
        if (topics == null || topics.isEmpty()) {
            this.getItems().clear();
            return;
        }
        
        this.getItems().setAll(topics);
        logger.debug("Установлено {} тем в виртуализированный список", topics.size());
    }
    
    /**
     * Кастомная ячейка для темы форума
     */
    private class ForumTopicCell extends ListCell<ForumTopic> {
        private VBox topicCard;
        private HBox titleBox;
        private Label titleLabel;
        private HBox metaBox;
        private Label authorLabel;
        private Label dateLabel;
        private HeadRenderer headRenderer;
        
        public ForumTopicCell() {
            // Создаем карточку темы (аналогично ForumController.createTopicCard)
            topicCard = new VBox(10);
            topicCard.setPadding(new Insets(15));
            topicCard.getStyleClass().add("forum-topic-card");
            
            // Заголовок
            titleBox = new HBox(10);
            titleBox.setAlignment(Pos.CENTER_LEFT);
            
            titleLabel = new Label();
            titleLabel.setStyle(
                "-fx-font-size: 16px; " +
                "-fx-font-weight: bold; " +
                "-fx-text-fill: #FFFFFF; " +
                "-fx-wrap-text: true;"
            );
            
            // Мета-информация
            metaBox = new HBox(15);
            metaBox.setAlignment(Pos.CENTER_LEFT);
            
            authorLabel = new Label();
            authorLabel.setStyle(
                "-fx-font-size: 12px; " +
                "-fx-text-fill: #A0A0B0;"
            );
            
            dateLabel = new Label();
            dateLabel.setStyle(
                "-fx-font-size: 12px; " +
                "-fx-text-fill: #A0A0B0;"
            );
            
            metaBox.getChildren().addAll(authorLabel, dateLabel);
            titleBox.getChildren().add(titleLabel);
            topicCard.getChildren().addAll(titleBox, metaBox);
            
            // Обработчик клика
            topicCard.setOnMouseClicked(e -> {
                ForumTopic topic = getItem();
                if (topic != null && forumController != null) {
                    Platform.runLater(() -> {
                        try {
                            java.lang.reflect.Method method = forumController.getClass()
                                .getDeclaredMethod("loadPosts", int.class);
                            method.setAccessible(true);
                            method.invoke(forumController, topic.getId());
                        } catch (Exception ex) {
                            logger.warn("Не удалось вызвать loadPosts", ex);
                        }
                    });
                }
            });
            
            // Анимация при наведении
            topicCard.setOnMouseEntered(e -> {
                topicCard.setStyle(topicCard.getStyle() + " -fx-background-color: rgba(255, 255, 255, 0.05);");
            });
            topicCard.setOnMouseExited(e -> {
                topicCard.setStyle(topicCard.getStyle().replaceAll("-fx-background-color:\\s*rgba\\(255,\\s*255,\\s*255,\\s*0\\.05\\);", ""));
            });
        }
        
        @Override
        protected void updateItem(ForumTopic topic, boolean empty) {
            super.updateItem(topic, empty);
            
            if (empty || topic == null) {
                setGraphic(null);
                return;
            }
            
            // Обновляем данные карточки
            titleLabel.setText(topic.getTitle());
            authorLabel.setText("Автор: " + topic.getAuthorUsername());
            
            // Форматируем дату
            if (topic.getCreatedAt() != null) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm");
                dateLabel.setText(sdf.format(topic.getCreatedAt()));
            } else {
                dateLabel.setText("");
            }
            
            // Обновляем стили для закрепленных/заблокированных тем
            topicCard.getStyleClass().removeAll("pinned", "locked");
            if (topic.isPinned()) {
                topicCard.getStyleClass().add("pinned");
                // Добавляем иконку закрепления
                if (!titleBox.getChildren().contains(titleBox.lookup(".pinned-icon"))) {
                    Label pinnedIcon = new Label("📌");
                    pinnedIcon.getStyleClass().add("forum-icon-pinned");
                    titleBox.getChildren().add(0, pinnedIcon);
                }
            }
            if (topic.isLocked()) {
                topicCard.getStyleClass().add("locked");
                // Добавляем иконку блокировки
                if (!titleBox.getChildren().contains(titleBox.lookup(".locked-icon"))) {
                    Label lockedIcon = new Label("🔒");
                    lockedIcon.getStyleClass().add("forum-icon-locked");
                    titleBox.getChildren().add(lockedIcon);
                }
            }
            
            setGraphic(topicCard);
        }
    }
}
