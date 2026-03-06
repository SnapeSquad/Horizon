package com.horizon.launcher.ui.components;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Менеджер для управления Toast уведомлениями
 */
public class ToastManager {
    private static ToastManager instance;
    private VBox toastContainer;
    private List<Toast> activeToasts;
    private static final int MAX_TOASTS = 5;
    private static final int TOAST_DURATION = 3000; // 3 секунды
    
    private ToastManager() {
        this.activeToasts = new ArrayList<>();
    }
    
    public static synchronized ToastManager getInstance() {
        if (instance == null) {
            instance = new ToastManager();
        }
        return instance;
    }
    
    /**
     * Инициализирует контейнер для Toast уведомлений
     */
    public void initialize(StackPane rootPane) {
        toastContainer = new VBox(10);
        toastContainer.setAlignment(Pos.TOP_CENTER);
        toastContainer.setPickOnBounds(false); // Позволяет кликать сквозь контейнер
        toastContainer.setMouseTransparent(true);
        toastContainer.setSpacing(10);
        toastContainer.setPadding(new javafx.geometry.Insets(20));
        
        rootPane.getChildren().add(toastContainer);
    }
    
    /**
     * Показать Toast уведомление
     */
    public void showToast(String message, Toast.ToastType type) {
        Platform.runLater(() -> {
            if (toastContainer == null) {
                logger.warn("ToastManager не инициализирован. Вызовите initialize() перед использованием.");
                return;
            }
            
            // Удаляем старые Toast если их слишком много
            if (activeToasts.size() >= MAX_TOASTS) {
                Toast oldestToast = activeToasts.remove(0);
                removeToast(oldestToast);
            }
            
            // Создаем новый Toast
            Toast toast = new Toast(message, type);
            activeToasts.add(toast);
            toastContainer.getChildren().add(toast);
            
            // Автоматически удаляем через указанное время
            PauseTransition pause = new PauseTransition(Duration.millis(TOAST_DURATION));
            pause.setOnFinished(e -> {
                removeToast(toast);
            });
            pause.play();
        });
    }
    
    /**
     * Показать успешное уведомление
     */
    public void showSuccess(String message) {
        showToast(message, Toast.ToastType.SUCCESS);
    }
    
    /**
     * Показать ошибку
     */
    public void showError(String message) {
        showToast(message, Toast.ToastType.ERROR);
    }
    
    /**
     * Показать информационное уведомление
     */
    public void showInfo(String message) {
        showToast(message, Toast.ToastType.INFO);
    }
    
    /**
     * Показать предупреждение
     */
    public void showWarning(String message) {
        showToast(message, Toast.ToastType.WARNING);
    }
    
    /**
     * Удалить Toast уведомление
     */
    private void removeToast(Toast toast) {
        if (toast == null || !activeToasts.contains(toast)) {
            return;
        }
        
        toast.animateOut(() -> {
            Platform.runLater(() -> {
                activeToasts.remove(toast);
                if (toastContainer != null) {
                    toastContainer.getChildren().remove(toast);
                }
            });
        });
    }
    
    /**
     * Очистить все Toast уведомления
     */
    public void clearAll() {
        Platform.runLater(() -> {
            List<Toast> toastsToRemove = new ArrayList<>(activeToasts);
            for (Toast toast : toastsToRemove) {
                removeToast(toast);
            }
        });
    }
    
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ToastManager.class);
}
