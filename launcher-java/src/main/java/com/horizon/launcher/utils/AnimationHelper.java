package com.horizon.launcher.utils;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Утилиты для анимаций в JavaFX с оптимизацией производительности
 * Использует кэширование анимаций для избежания создания новых объектов
 */
public class AnimationHelper {
    private static final Logger logger = LoggerFactory.getLogger(AnimationHelper.class);
    private static final AnimationCache cache = AnimationCache.getInstance();
    
    /**
     * Эффект liquidPress - плавное уменьшение при нажатии
     * Оптимизировано: использует кэшированные анимации
     */
    public static void liquidPress(Node node) {
        if (node == null) return;
        
        ScaleTransition scaleDown = cache.getScaleDown(node);
        ScaleTransition scaleUp = cache.getScaleUp(node);
        
        // Настраиваем связь между анимациями
        scaleDown.setOnFinished(e -> {
            if (!scaleUp.getStatus().equals(Animation.Status.RUNNING)) {
                scaleUp.play();
            }
        });
        
        // Удаляем старые обработчики, чтобы избежать дублирования
        node.setOnMousePressed(e -> {
            if (scaleDown.getStatus().equals(Animation.Status.RUNNING)) {
                scaleDown.stop();
            }
            scaleDown.play();
        });
        
        node.setOnMouseReleased(e -> {
            scaleDown.stop();
            if (!scaleUp.getStatus().equals(Animation.Status.RUNNING)) {
                scaleUp.play();
            }
        });
    }
    
    /**
     * Анимация тряски (Shake) для ошибок валидации
     * Оптимизировано: останавливает предыдущую анимацию перед запуском новой
     */
    public static void shake(Node node) {
        if (node == null) return;
        
        // Останавливаем предыдущую shake анимацию, если она есть
        TranslateTransition existingShake = (TranslateTransition) node.getProperties().get("shakeAnimation");
        if (existingShake != null && existingShake.getStatus().equals(Animation.Status.RUNNING)) {
            existingShake.stop();
        }
        
        TranslateTransition shake = new TranslateTransition(Duration.millis(50), node);
        shake.setFromX(0);
        shake.setByX(10);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        
        // Сохраняем ссылку для возможности остановки
        node.getProperties().put("shakeAnimation", shake);
        
        shake.setOnFinished(e -> {
            node.getProperties().remove("shakeAnimation");
            node.setTranslateX(0); // Сбрасываем позицию
        });
        
        shake.play();
    }
    
    /**
     * Плавное появление (Fade In)
     * Оптимизировано: использует кэшированную анимацию
     */
    public static void fadeIn(Node node) {
        if (node == null) return;
        
        FadeTransition fadeIn = cache.getFadeIn(node);
        
        // Останавливаем fadeOut, если он запущен
        FadeTransition fadeOut = cache.getFadeOut(node);
        if (fadeOut.getStatus().equals(Animation.Status.RUNNING)) {
            fadeOut.stop();
        }
        
        // Устанавливаем начальное значение, если нужно
        if (node.getOpacity() < 1.0) {
            fadeIn.setFromValue(node.getOpacity());
        } else {
            fadeIn.setFromValue(0.0);
        }
        fadeIn.setToValue(1.0);
        
        fadeIn.play();
    }
    
    /**
     * Плавное исчезновение (Fade Out)
     * Оптимизировано: использует кэшированную анимацию
     */
    public static void fadeOut(Node node) {
        if (node == null) return;
        
        FadeTransition fadeOut = cache.getFadeOut(node);
        
        // Останавливаем fadeIn, если он запущен
        FadeTransition fadeIn = cache.getFadeIn(node);
        if (fadeIn.getStatus().equals(Animation.Status.RUNNING)) {
            fadeIn.stop();
        }
        
        // Устанавливаем начальное значение
        fadeOut.setFromValue(node.getOpacity());
        fadeOut.setToValue(0.0);
        
        fadeOut.play();
    }
    
    /**
     * Shimmering эффект для Owner/Curator ролей
     * Оптимизировано: использует кэшированную анимацию, не создает дубликаты
     */
    public static void shimmer(Node node) {
        if (node == null) return;
        
        Timeline shimmer = cache.getShimmer(node);
        
        // Останавливаем предыдущую shimmer анимацию, если она запущена
        if (shimmer.getStatus().equals(Animation.Status.RUNNING)) {
            shimmer.stop();
        }
        
        shimmer.play();
    }
    
    /**
     * Pulse glow эффект для кнопок (особенно "Log In" и "PLAY")
     * Оптимизировано: использует кэшированную анимацию, не создает дубликаты
     */
    public static void pulseGlow(Node node) {
        if (node == null) return;
        
        Timeline pulseGlow = cache.getPulseGlow(node);
        
        // Останавливаем предыдущую pulseGlow анимацию, если она запущена
        if (pulseGlow.getStatus().equals(Animation.Status.RUNNING)) {
            pulseGlow.stop();
        }
        
        pulseGlow.play();
    }
    
    /**
     * Остановить все анимации для узла
     */
    public static void stopAllAnimations(Node node) {
        if (node == null) return;
        cache.stopAllAnimations(node);
    }
}
