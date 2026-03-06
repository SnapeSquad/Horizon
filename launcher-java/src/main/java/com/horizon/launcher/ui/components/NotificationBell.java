package com.horizon.launcher.ui.components;

import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Компонент колокольчика уведомлений с индикатором количества
 */
public class NotificationBell extends StackPane {
    private static final Logger logger = LoggerFactory.getLogger(NotificationBell.class);
    
    private Circle bellIcon;
    private Circle notificationBadge;
    private Label notificationCountLabel;
    private int notificationCount;
    
    public NotificationBell() {
        super();
        this.notificationCount = 0;
        
        createBell();
    }
    
    /**
     * Создать иконку колокольчика
     */
    private void createBell() {
        // Иконка колокольчика (простой круг для демонстрации)
        // В реальном проекте здесь должна быть SVG или изображение
        bellIcon = new Circle(12);
        bellIcon.setFill(Color.TRANSPARENT);
        bellIcon.setStroke(Color.web("#667EEA"));
        bellIcon.setStrokeWidth(2);
        
        // Бейдж с количеством уведомлений
        notificationBadge = new Circle(8);
        notificationBadge.setFill(Color.web("#FF3B30")); // Красный цвет
        notificationBadge.setVisible(false);
        notificationBadge.setTranslateX(8);
        notificationBadge.setTranslateY(-8);
        
        // Текст с количеством
        notificationCountLabel = new Label("0");
        notificationCountLabel.setFont(Font.font("System", FontWeight.BOLD, 10));
        notificationCountLabel.setTextFill(Color.WHITE);
        notificationCountLabel.setTranslateX(8);
        notificationCountLabel.setTranslateY(-8);
        notificationCountLabel.setVisible(false);
        
        // Используем Unicode символ колокольчика
        Label bellLabel = new Label("\uD83D\uDD14"); // 🔔
        bellLabel.setStyle("-fx-font-size: 20px;");
        
        Group bellGroup = new Group();
        bellGroup.getChildren().addAll(bellLabel, notificationBadge, notificationCountLabel);
        
        this.getChildren().add(bellGroup);
        this.setPrefSize(32, 32);
    }
    
    /**
     * Установить количество уведомлений
     */
    public void setNotificationCount(int count) {
        this.notificationCount = Math.max(0, count);
        
        if (this.notificationCount > 0) {
            notificationBadge.setVisible(true);
            notificationCountLabel.setVisible(true);
            notificationCountLabel.setText(this.notificationCount > 99 ? "99+" : String.valueOf(this.notificationCount));
            
            // Анимация пульсации
            javafx.animation.ScaleTransition pulse = new javafx.animation.ScaleTransition(
                javafx.util.Duration.millis(200), notificationBadge
            );
            pulse.setToX(1.2);
            pulse.setToY(1.2);
            pulse.setAutoReverse(true);
            pulse.setCycleCount(2);
            pulse.play();
        } else {
            notificationBadge.setVisible(false);
            notificationCountLabel.setVisible(false);
        }
    }
    
    /**
     * Получить текущее количество уведомлений
     */
    public int getNotificationCount() {
        return notificationCount;
    }
    
    /**
     * Увеличить количество уведомлений
     */
    public void incrementNotificationCount() {
        setNotificationCount(notificationCount + 1);
    }
    
    /**
     * Сбросить количество уведомлений
     */
    public void resetNotificationCount() {
        setNotificationCount(0);
    }
}
