package com.horizon.launcher.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Централизованный обработчик ошибок (AAA-уровень)
 * Обеспечивает единообразную обработку всех ошибок в приложении
 */
public class ErrorHandler {
    private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);
    private static ErrorHandler instance;
    
    private ErrorHandler() {
    }
    
    public static ErrorHandler getInstance() {
        if (instance == null) {
            instance = new ErrorHandler();
        }
        return instance;
    }
    
    /**
     * Обрабатывает критическую ошибку
     */
    public void handleCriticalError(Throwable error, String context) {
        logger.error("КРИТИЧЕСКАЯ ОШИБКА [{}]: {}", context, error.getMessage(), error);
        
        // Отправляем в Discord асинхронно, чтобы не блокировать
        CompletableFuture.runAsync(() -> {
            try {
                DiscordLogger.getInstance().logException(error, context);
            } catch (Exception e) {
                logger.error("Не удалось отправить ошибку в Discord", e);
            }
        });
    }
    
    /**
     * Обрабатывает некритическую ошибку
     */
    public void handleError(Throwable error, String context) {
        logger.warn("Ошибка [{}]: {}", context, error.getMessage(), error);
        
        // Для некритических ошибок не отправляем в Discord
    }
    
    /**
     * Обрабатывает предупреждение
     */
    public void handleWarning(String message, String context) {
        logger.warn("Предупреждение [{}]: {}", context, message);
    }
    
    /**
     * Обрабатывает ошибку с возвратом результата
     */
    public <T> T handleErrorWithFallback(Throwable error, String context, T fallback) {
        handleError(error, context);
        return fallback;
    }
    
    /**
     * Обрабатывает ошибку сети
     */
    public void handleNetworkError(Throwable error, String endpoint) {
        logger.error("Ошибка сети при обращении к {}: {}", endpoint, error.getMessage(), error);
        // Можно добавить специальную логику для сетевых ошибок
    }
    
    /**
     * Обрабатывает ошибку валидации
     */
    public void handleValidationError(String message, String field) {
        logger.warn("Ошибка валидации поля {}: {}", field, message);
    }
}

