package com.horizon.launcher.ui.components;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Компонент для отображения всплывающих уведомлений (Toast)
 */
public class Toast extends StackPane {
    
    public enum ToastType {
        SUCCESS,
        ERROR,
        INFO,
        WARNING
    }
    
    private Label messageLabel;
    private ToastType type;
    
    public Toast(String message, ToastType type) {
        this.type = type;
        
        messageLabel = new Label(message);
        messageLabel.setStyle(
            "-fx-text-fill: white; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: bold; " +
            "-fx-font-family: 'Minecraft Unicode', -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Segoe UI', Roboto, sans-serif;"
        );
        
        this.getChildren().add(messageLabel);
        this.setAlignment(Pos.CENTER);
        this.setMaxWidth(350);
        this.setPrefHeight(50);
        
        // Применяем стиль в зависимости от типа
        applyStyle();
        
        // Анимация появления
        animateIn();
    }
    
    private void applyStyle() {
        String backgroundColor;
        String borderColor;
        switch (type) {
            case SUCCESS:
                backgroundColor = "rgba(46, 204, 113, 0.95)"; // Green из tech.md
                borderColor = "#2ecc71";
                break;
            case ERROR:
                backgroundColor = "rgba(255, 107, 107, 0.95)"; // Red из tech.md
                borderColor = "#FF6B6B";
                break;
            case INFO:
                backgroundColor = "rgba(52, 152, 219, 0.95)"; // Blue
                borderColor = "#3498db";
                break;
            case WARNING:
                backgroundColor = "rgba(255, 152, 0, 0.95)"; // Orange
                borderColor = "#FF9800";
                break;
            default:
                backgroundColor = "rgba(30, 30, 45, 0.95)"; // Surface из tech.md
                borderColor = "rgba(255, 255, 255, 0.1)";
        }
        
        this.setStyle(
            "-fx-background-color: " + backgroundColor + "; " +
            "-fx-background-radius: 12px; " +
            "-fx-border-color: " + borderColor + "; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 12px; " +
            "-fx-padding: 15px 20px; " +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.5), 15, 0, 0, 5), " +
                        "dropshadow(gaussian, " + borderColor + ", 20, 0, 0, 0);"
        );
    }
    
    private void animateIn() {
        this.setOpacity(0);
        this.setTranslateY(50);
        
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), this);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        
        TranslateTransition translateIn = new TranslateTransition(Duration.millis(300), this);
        translateIn.setFromY(50);
        translateIn.setToY(0);
        
        fadeIn.play();
        translateIn.play();
    }
    
    public void animateOut(Runnable onFinished) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), this);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        
        TranslateTransition translateOut = new TranslateTransition(Duration.millis(300), this);
        translateOut.setFromY(0);
        translateOut.setToY(-50);
        
        fadeOut.setOnFinished(e -> {
            if (onFinished != null) {
                onFinished.run();
            }
        });
        
        fadeOut.play();
        translateOut.play();
    }
}
