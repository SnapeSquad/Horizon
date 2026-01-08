package com.horizon.launcher.ui;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Вспомогательный класс для анимаций в стиле iOS 26
 */
public class AnimationHelper {
    
    /**
     * Плавное появление элемента (fade in)
     */
    public static void fadeIn(Node node, Duration duration) {
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }
    
    /**
     * Появление с движением вверх (slide up)
     */
    public static void slideUp(Node node, Duration duration) {
        node.setTranslateY(20);
        node.setOpacity(0);
        
        ParallelTransition parallel = new ParallelTransition();
        
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        
        TranslateTransition translate = new TranslateTransition(duration, node);
        translate.setFromY(20);
        translate.setToY(0);
        
        parallel.getChildren().addAll(fade, translate);
        parallel.play();
    }
    
    /**
     * Появление с масштабированием (scale in)
     */
    public static void scaleIn(Node node, Duration duration) {
        node.setScaleX(0.9);
        node.setScaleY(0.9);
        node.setOpacity(0);
        
        ParallelTransition parallel = new ParallelTransition();
        
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        
        ScaleTransition scale = new ScaleTransition(duration, node);
        scale.setFromX(0.9);
        scale.setFromY(0.9);
        scale.setToX(1.0);
        scale.setToY(1.0);
        
        parallel.getChildren().addAll(fade, scale);
        parallel.play();
    }
    
    /**
     * Пульсация (для кнопок)
     */
    public static void pulse(Node node) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(150), node);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(1.05);
        scale.setToY(1.05);
        scale.setAutoReverse(true);
        scale.setCycleCount(2);
        scale.play();
    }
    
    /**
     * Жидкое нажатие (liquid press)
     */
    public static void liquidPress(Node node) {
        ScaleTransition press = new ScaleTransition(Duration.millis(100), node);
        press.setFromX(1.0);
        press.setFromY(1.0);
        press.setToX(0.95);
        press.setToY(0.95);
        
        ScaleTransition release = new ScaleTransition(Duration.millis(100), node);
        release.setFromX(0.95);
        release.setFromY(0.95);
        release.setToX(1.0);
        release.setToY(1.0);
        
        SequentialTransition sequence = new SequentialTransition(press, release);
        sequence.play();
    }
    
    /**
     * Плавное скрытие
     */
    public static void fadeOut(Node node, Duration duration, Runnable onFinished) {
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(1);
        fade.setToValue(0);
        fade.setOnFinished(e -> {
            if (onFinished != null) onFinished.run();
        });
        fade.play();
    }
    
    /**
     * Анимация успеха (зеленая вспышка)
     */
    public static void successFlash(Node node) {
        String originalStyle = node.getStyle();
        node.setStyle(originalStyle + "; -fx-background-color: rgba(52, 199, 89, 0.3);");
        
        FadeTransition flash = new FadeTransition(Duration.millis(300), node);
        flash.setFromValue(0.3);
        flash.setToValue(0);
        flash.setOnFinished(e -> node.setStyle(originalStyle));
        flash.play();
    }
    
    /**
     * Анимация ошибки (красная вспышка)
     */
    public static void errorFlash(Node node) {
        String originalStyle = node.getStyle();
        node.setStyle(originalStyle + "; -fx-background-color: rgba(255, 59, 48, 0.3);");
        
        FadeTransition flash = new FadeTransition(Duration.millis(300), node);
        flash.setFromValue(0.3);
        flash.setToValue(0);
        flash.setOnFinished(e -> node.setStyle(originalStyle));
        flash.play();
    }
}








