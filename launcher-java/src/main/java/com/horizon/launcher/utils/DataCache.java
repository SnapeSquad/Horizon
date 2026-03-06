package com.horizon.launcher.utils;

import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Кэш для данных приложения (изображения, API ответы)
 * Использует LRU стратегию и TTL для управления памятью
 */
public class DataCache {
    private static final Logger logger = LoggerFactory.getLogger(DataCache.class);
    private static final DataCache instance = new DataCache();
    
    // Кэш изображений (URL -> Image)
    private final Map<String, CacheEntry<Image>> imageCache = new ConcurrentHashMap<>();
    
    // Кэш API ответов (endpoint + params -> JSON string)
    private final Map<String, CacheEntry<String>> apiCache = new ConcurrentHashMap<>();
    
    // Максимальный размер кэша изображений
    private static final int MAX_IMAGE_CACHE_SIZE = 100;
    
    // Максимальный размер кэша API ответов
    private static final int MAX_API_CACHE_SIZE = 50;
    
    // TTL для изображений (1 час)
    private static final long IMAGE_CACHE_TTL = TimeUnit.HOURS.toMillis(1);
    
    // TTL для API ответов (5 минут)
    private static final long API_CACHE_TTL = TimeUnit.MINUTES.toMillis(5);
    
    private DataCache() {
        // Запускаем периодическую очистку устаревших записей
        startCleanupTask();
    }
    
    public static DataCache getInstance() {
        return instance;
    }
    
    /**
     * Получить изображение из кэша
     */
    public Image getImage(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        CacheEntry<Image> entry = imageCache.get(url);
        if (entry != null && !entry.isExpired()) {
            logger.debug("Изображение найдено в кэше: {}", url);
            entry.updateAccessTime();
            return entry.getValue();
        }
        
        if (entry != null && entry.isExpired()) {
            imageCache.remove(url);
        }
        
        return null;
    }
    
    /**
     * Сохранить изображение в кэш
     */
    public void putImage(String url, Image image) {
        if (url == null || url.isEmpty() || image == null) {
            return;
        }
        
        // Проверяем размер кэша
        if (imageCache.size() >= MAX_IMAGE_CACHE_SIZE) {
            evictOldestImage();
        }
        
        imageCache.put(url, new CacheEntry<>(image, IMAGE_CACHE_TTL));
        logger.debug("Изображение добавлено в кэш: {}", url);
    }
    
    /**
     * Получить API ответ из кэша
     */
    public String getApiResponse(String cacheKey) {
        if (cacheKey == null || cacheKey.isEmpty()) {
            return null;
        }
        
        CacheEntry<String> entry = apiCache.get(cacheKey);
        if (entry != null && !entry.isExpired()) {
            logger.debug("API ответ найден в кэше: {}", cacheKey);
            entry.updateAccessTime();
            return entry.getValue();
        }
        
        if (entry != null && entry.isExpired()) {
            apiCache.remove(cacheKey);
        }
        
        return null;
    }
    
    /**
     * Сохранить API ответ в кэш
     */
    public void putApiResponse(String cacheKey, String response) {
        if (cacheKey == null || cacheKey.isEmpty() || response == null) {
            return;
        }
        
        // Проверяем размер кэша
        if (apiCache.size() >= MAX_API_CACHE_SIZE) {
            evictOldestApiResponse();
        }
        
        apiCache.put(cacheKey, new CacheEntry<>(response, API_CACHE_TTL));
        logger.debug("API ответ добавлен в кэш: {}", cacheKey);
    }
    
    /**
     * Создать ключ кэша для API запроса
     */
    public static String createApiCacheKey(String endpoint, Map<String, String> params) {
        StringBuilder key = new StringBuilder(endpoint);
        if (params != null && !params.isEmpty()) {
            key.append("?");
            params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> key.append(entry.getKey()).append("=").append(entry.getValue()).append("&"));
            key.setLength(key.length() - 1); // Удаляем последний &
        }
        return key.toString();
    }
    
    /**
     * Удалить самую старую запись из кэша изображений
     */
    private void evictOldestImage() {
        String oldestKey = null;
        long oldestTime = Long.MAX_VALUE;
        
        for (Map.Entry<String, CacheEntry<Image>> entry : imageCache.entrySet()) {
            long accessTime = entry.getValue().getLastAccessTime();
            if (accessTime < oldestTime) {
                oldestTime = accessTime;
                oldestKey = entry.getKey();
            }
        }
        
        if (oldestKey != null) {
            imageCache.remove(oldestKey);
            logger.debug("Удалена самая старая запись из кэша изображений: {}", oldestKey);
        }
    }
    
    /**
     * Удалить самую старую запись из кэша API ответов
     */
    private void evictOldestApiResponse() {
        String oldestKey = null;
        long oldestTime = Long.MAX_VALUE;
        
        for (Map.Entry<String, CacheEntry<String>> entry : apiCache.entrySet()) {
            long accessTime = entry.getValue().getLastAccessTime();
            if (accessTime < oldestTime) {
                oldestTime = accessTime;
                oldestKey = entry.getKey();
            }
        }
        
        if (oldestKey != null) {
            apiCache.remove(oldestKey);
            logger.debug("Удалена самая старая запись из кэша API ответов: {}", oldestKey);
        }
    }
    
    /**
     * Запустить задачу периодической очистки
     */
    private void startCleanupTask() {
        javafx.concurrent.Task<Void> cleanupTask = new javafx.concurrent.Task<Void>() {
            @Override
            protected Void call() throws Exception {
                while (!isCancelled()) {
                    try {
                        Thread.sleep(TimeUnit.MINUTES.toMillis(5)); // Очистка каждые 5 минут
                        
                        // Очищаем устаревшие изображения
                        imageCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
                        
                        // Очищаем устаревшие API ответы
                        apiCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
                        
                        logger.debug("Очистка кэша завершена. Изображений: {}, API ответов: {}", 
                            imageCache.size(), apiCache.size());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                return null;
            }
        };
        
        Thread cleanupThread = new Thread(cleanupTask);
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }
    
    /**
     * Очистить весь кэш
     */
    public void clearAll() {
        imageCache.clear();
        apiCache.clear();
        logger.info("Весь кэш очищен");
    }
    
    /**
     * Очистить кэш изображений
     */
    public void clearImageCache() {
        imageCache.clear();
        logger.info("Кэш изображений очищен");
    }
    
    /**
     * Очистить кэш API ответов
     */
    public void clearApiCache() {
        apiCache.clear();
        logger.info("Кэш API ответов очищен");
    }
    
    /**
     * Запись в кэше с TTL
     */
    private static class CacheEntry<T> {
        private final T value;
        private final long ttl;
        private long createdAt;
        private long lastAccessTime;
        
        public CacheEntry(T value, long ttl) {
            this.value = value;
            this.ttl = ttl;
            this.createdAt = System.currentTimeMillis();
            this.lastAccessTime = this.createdAt;
        }
        
        public T getValue() {
            return value;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - createdAt > ttl;
        }
        
        public void updateAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }
        
        public long getLastAccessTime() {
            return lastAccessTime;
        }
    }
}
