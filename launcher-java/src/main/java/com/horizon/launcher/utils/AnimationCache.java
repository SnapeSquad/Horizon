package com.horizon.launcher.utils;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Кэш для анимаций, чтобы избежать создания новых объектов при каждом вызове
 */
public class AnimationCache {
    private static final Logger logger = LoggerFactory.getLogger(AnimationCache.class);
    
    private static final AnimationCache instance = new AnimationCache();
    
    // Кэш для fadeIn/fadeOut анимаций (слабая ссылка на Node, чтобы не мешать GC)
    private final Map<Node, FadeTransition> fadeInCache = new WeakHashMap<>();
    private final Map<Node, FadeTransition> fadeOutCache = new WeakHashMap<>();
    
    // Кэш для shimmer и pulseGlow (бесконечные анимации)
    private final Map<Node, Timeline> shimmerCache = new WeakHashMap<>();
    private final Map<Node, Timeline> pulseGlowCache = new WeakHashMap<>();
    
    // Кэш для liquidPress анимаций
    private final Map<Node, ScaleTransition> scaleDownCache = new WeakHashMap<>();
    private final Map<Node, ScaleTransition> scaleUpCache = new WeakHashMap<>();
    
    private AnimationCache() {
    }
    
    public static AnimationCache getInstance() {
        return instance;
    }
    
    /**
     * Получить или создать fadeIn анимацию для узла
     */
    public FadeTransition getFadeIn(Node node) {
        FadeTransition fadeIn = fadeInCache.get(node);
        if (fadeIn == null) {
            fadeIn = new FadeTransition(Duration.millis(300), node);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeInCache.put(node, fadeIn);
        }
        return fadeIn;
    }
    
    /**
     * Получить или создать fadeOut анимацию для узла
     */
    public FadeTransition getFadeOut(Node node) {
        FadeTransition fadeOut = fadeOutCache.get(node);
        if (fadeOut == null) {
            fadeOut = new FadeTransition(Duration.millis(300), node);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOutCache.put(node, fadeOut);
        }
        return fadeOut;
    }
    
    /**
     * Получить или создать shimmer анимацию для узла
     */
    public Timeline getShimmer(Node node) {
        Timeline shimmer = shimmerCache.get(node);
        if (shimmer == null) {
            javafx.scene.effect.Glow glow = new javafx.scene.effect.Glow();
            glow.setLevel(0.5);
            
            shimmer = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(glow.levelProperty(), 0.3)),
                new KeyFrame(Duration.millis(1500), new KeyValue(glow.levelProperty(), 0.8)),
                new KeyFrame(Duration.millis(3000), new KeyValue(glow.levelProperty(), 0.3))
            );
            shimmer.setCycleCount(Timeline.INDEFINITE);
            shimmer.setAutoReverse(true);
            
            node.setEffect(glow);
            shimmerCache.put(node, shimmer);
        }
        return shimmer;
    }
    
    /**
     * Получить или создать pulseGlow анимацию для узла
     */
    public Timeline getPulseGlow(Node node) {
        Timeline pulseGlow = pulseGlowCache.get(node);
        if (pulseGlow == null) {
            javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
            glow.setColor(javafx.scene.paint.Color.rgb(102, 126, 234, 0.5));
            glow.setRadius(20);
            
            pulseGlow = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(glow.radiusProperty(), 15)),
                new KeyFrame(Duration.millis(1000), new KeyValue(glow.radiusProperty(), 25)),
                new KeyFrame(Duration.millis(2000), new KeyValue(glow.radiusProperty(), 15))
            );
            pulseGlow.setCycleCount(Timeline.INDEFINITE);
            pulseGlow.setAutoReverse(true);
            
            node.setEffect(glow);
            pulseGlowCache.put(node, pulseGlow);
        }
        return pulseGlow;
    }
    
    /**
     * Получить или создать scaleDown анимацию для узла
     */
    public ScaleTransition getScaleDown(Node node) {
        ScaleTransition scaleDown = scaleDownCache.get(node);
        if (scaleDown == null) {
            scaleDown = new ScaleTransition(Duration.millis(100), node);
            scaleDown.setToX(0.95);
            scaleDown.setToY(0.95);
            scaleDownCache.put(node, scaleDown);
        }
        return scaleDown;
    }
    
    /**
     * Получить или создать scaleUp анимацию для узла
     */
    public ScaleTransition getScaleUp(Node node) {
        ScaleTransition scaleUp = scaleUpCache.get(node);
        if (scaleUp == null) {
            scaleUp = new ScaleTransition(Duration.millis(100), node);
            scaleUp.setToX(1.0);
            scaleUp.setToY(1.0);
            scaleUpCache.put(node, scaleUp);
        }
        return scaleUp;
    }
    
    /**
     * Остановить все анимации для узла
     */
    public void stopAllAnimations(Node node) {
        FadeTransition fadeIn = fadeInCache.get(node);
        if (fadeIn != null) fadeIn.stop();
        
        FadeTransition fadeOut = fadeOutCache.get(node);
        if (fadeOut != null) fadeOut.stop();
        
        Timeline shimmer = shimmerCache.get(node);
        if (shimmer != null) shimmer.stop();
        
        Timeline pulseGlow = pulseGlowCache.get(node);
        if (pulseGlow != null) pulseGlow.stop();
        
        ScaleTransition scaleDown = scaleDownCache.get(node);
        if (scaleDown != null) scaleDown.stop();
        
        ScaleTransition scaleUp = scaleUpCache.get(node);
        if (scaleUp != null) scaleUp.stop();
    }
    
    /**
     * Очистить кэш (для тестирования или при необходимости)
     */
    public void clearCache() {
        fadeInCache.clear();
        fadeOutCache.clear();
        shimmerCache.clear();
        pulseGlowCache.clear();
        scaleDownCache.clear();
        scaleUpCache.clear();
    }
}
