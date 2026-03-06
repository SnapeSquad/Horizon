package com.horizon.cosmetics.client;

import com.google.gson.Gson;
import com.horizon.cosmetics.common.ModelData;
import com.horizon.cosmetics.common.BlockbenchModelParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Менеджер косметики для загрузки и кеширования моделей и текстур
 * Асинхронно скачивает ресурсы с API и кеширует их в .horizon/assets/
 */
public class CosmeticManager {
    private static final Logger logger = LoggerFactory.getLogger(CosmeticManager.class);
    private static CosmeticManager instance;
    
    private static final String ASSETS_DIR = ".horizon/assets";
    private static final String MODELS_DIR = ASSETS_DIR + "/models";
    private static final String TEXTURES_DIR = ASSETS_DIR + "/textures";
    private static final String API_BASE_URL = "http://localhost:3000/api/cosmetics";
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final ExecutorService executorService;
    private final Map<String, ModelData> modelCache;
    private final Map<String, Identifier> textureCache;
    
    private Path assetsPath;
    private Path modelsPath;
    private Path texturesPath;
    
    private CosmeticManager() {
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
        this.executorService = Executors.newFixedThreadPool(4);
        this.modelCache = new HashMap<>();
        this.textureCache = new HashMap<>();
        
        // Инициализация путей
        String userHome = System.getProperty("user.home");
        assetsPath = Paths.get(userHome, ASSETS_DIR);
        modelsPath = Paths.get(userHome, MODELS_DIR);
        texturesPath = Paths.get(userHome, TEXTURES_DIR);
        
        try {
            Files.createDirectories(modelsPath);
            Files.createDirectories(texturesPath);
            logger.info("Директории активов созданы: {}", assetsPath);
        } catch (IOException e) {
            logger.error("Не удалось создать директории активов", e);
        }
    }
    
    /**
     * Получить единственный экземпляр CosmeticManager
     */
    public static synchronized CosmeticManager getInstance() {
        if (instance == null) {
            instance = new CosmeticManager();
        }
        return instance;
    }
    
    /**
     * Загрузить модель косметики асинхронно
     * @param cosmeticId ID косметики
     * @return CompletableFuture с данными модели
     */
    public CompletableFuture<ModelData> loadModelAsync(String cosmeticId) {
        if (cosmeticId == null || cosmeticId.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        
        // Проверяем кеш
        ModelData cached = modelCache.get(cosmeticId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Проверяем локальный кеш
                Path localModelPath = modelsPath.resolve(cosmeticId + ".json");
                if (Files.exists(localModelPath)) {
                    logger.info("Загрузка модели из локального кеша: {}", cosmeticId);
                    ModelData model = loadModelFromFile(localModelPath);
                    if (model != null) {
                        modelCache.put(cosmeticId, model);
                        return model;
                    }
                }
                
                // Загружаем с API
                logger.info("Загрузка модели с API: {}", cosmeticId);
                String modelUrl = API_BASE_URL + "/" + cosmeticId + "/model";
                ModelData model = downloadModel(modelUrl, cosmeticId);
                
                if (model != null) {
                    modelCache.put(cosmeticId, model);
                }
                
                return model;
            } catch (Exception e) {
                logger.error("Ошибка при загрузке модели: {}", cosmeticId, e);
                return null;
            }
        }, executorService);
    }
    
    /**
     * Загрузить текстуру косметики асинхронно
     * @param cosmeticId ID косметики
     * @return CompletableFuture с Identifier текстуры
     */
    public CompletableFuture<Identifier> loadTextureAsync(String cosmeticId) {
        if (cosmeticId == null || cosmeticId.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        
        // Проверяем кеш
        Identifier cached = textureCache.get(cosmeticId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Проверяем локальный кеш
                Path localTexturePath = texturesPath.resolve(cosmeticId + ".png");
                if (Files.exists(localTexturePath)) {
                    logger.info("Загрузка текстуры из локального кеша: {}", cosmeticId);
                    Identifier texture = loadTextureFromFile(localTexturePath, cosmeticId);
                    if (texture != null) {
                        textureCache.put(cosmeticId, texture);
                        return texture;
                    }
                }
                
                // Загружаем с API
                logger.info("Загрузка текстуры с API: {}", cosmeticId);
                String textureUrl = API_BASE_URL + "/" + cosmeticId + "/texture";
                Identifier texture = downloadTexture(textureUrl, cosmeticId);
                
                if (texture != null) {
                    textureCache.put(cosmeticId, texture);
                }
                
                return texture;
            } catch (Exception e) {
                logger.error("Ошибка при загрузке текстуры: {}", cosmeticId, e);
                return null;
            }
        }, executorService);
    }
    
    /**
     * Загрузить модель из файла через общий BlockbenchModelParser
     */
    private ModelData loadModelFromFile(Path path) {
        try {
            if (path == null || !Files.exists(path)) {
                logger.error("Путь к модели не существует: {}", path);
                return null;
            }
            
            // Используем общий парсер с лаунчером
            ModelData modelData = BlockbenchModelParser.parseFromPath(path);
            
            if (modelData != null && BlockbenchModelParser.validate(modelData)) {
                logger.debug("Модель успешно загружена из файла: {}", path);
                return modelData;
            } else {
                logger.warn("Модель не прошла валидацию: {}", path);
                return null;
            }
        } catch (Exception e) {
            logger.error("Ошибка при чтении файла модели: {}", path, e);
            return null;
        }
    }
    
    /**
     * Загрузить текстуру из файла
     * В реальной реализации нужно регистрировать текстуру через ResourceManager Minecraft
     */
    private Identifier loadTextureFromFile(Path path, String cosmeticId) {
        try {
            if (path == null || !Files.exists(path)) {
                logger.error("Путь к текстуре не существует: {}", path);
                return null;
            }
            
            // Загружаем изображение для валидации
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) {
                logger.warn("Не удалось загрузить изображение: {}", path);
                return null;
            }
            
            logger.debug("Изображение загружено: {}x{} пикселей", image.getWidth(), image.getHeight());
            
            // Создаем Identifier для текстуры
            // В реальной реализации нужно регистрировать текстуру через ResourceManager
            // Например: MinecraftClient.getInstance().getTextureManager().registerTexture(textureId, dynamicTexture)
            Identifier textureId = new Identifier("horizon", "cosmetics/" + cosmeticId);
            
            logger.info("Текстура загружена: {}", textureId);
            return textureId;
        } catch (IOException e) {
            logger.error("Ошибка при чтении файла текстуры: {}", path, e);
            return null;
        } catch (Exception e) {
            logger.error("Неожиданная ошибка при загрузке текстуры: {}", path, e);
            return null;
        }
    }
    
    /**
     * Скачать модель с API
     */
    private ModelData downloadModel(String url, String cosmeticId) {
        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.warn("Неуспешный ответ при загрузке модели: {} - {}", url, response.code());
                return null;
            }
            
            ResponseBody body = response.body();
            if (body == null) {
                logger.warn("Пустое тело ответа при загрузке модели: {}", url);
                return null;
            }
            
            String json = body.string();
            
            // Сохраняем в локальный кеш
            Path localPath = modelsPath.resolve(cosmeticId + ".json");
            Files.writeString(localPath, json);
            logger.info("Модель сохранена в кеш: {}", localPath);
            
            // Парсим модель
            return parseModelJson(json);
        } catch (IOException e) {
            logger.error("Ошибка при скачивании модели: {}", url, e);
            return null;
        }
    }
    
    /**
     * Скачать текстуру с API
     */
    private Identifier downloadTexture(String url, String cosmeticId) {
        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.warn("Неуспешный ответ при загрузке текстуры: {} - {}", url, response.code());
                return null;
            }
            
            ResponseBody body = response.body();
            if (body == null) {
                logger.warn("Пустое тело ответа при загрузке текстуры: {}", url);
                return null;
            }
            
            byte[] imageData = body.bytes();
            
            // Сохраняем в локальный кеш
            Path localPath = texturesPath.resolve(cosmeticId + ".png");
            Files.write(localPath, imageData);
            logger.info("Текстура сохранена в кеш: {}", localPath);
            
            // Загружаем текстуру в Minecraft
            return loadTextureFromFile(localPath, cosmeticId);
        } catch (IOException e) {
            logger.error("Ошибка при скачивании текстуры: {}", url, e);
            return null;
        }
    }
    
    /**
     * Парсинг JSON модели через общий BlockbenchModelParser
     */
    private ModelData parseModelJson(String json) {
        try {
            if (json == null || json.trim().isEmpty()) {
                logger.error("JSON модель пуста");
                return null;
            }
            
            // Используем общий парсер с лаунчером
            ModelData modelData = BlockbenchModelParser.parseFromString(json);
            
            if (modelData != null && BlockbenchModelParser.validate(modelData)) {
                logger.debug("Модель успешно распарсена и валидирована");
                return modelData;
            } else {
                logger.warn("Модель не прошла валидацию");
                return null;
            }
        } catch (Exception e) {
            logger.error("Ошибка при парсинге JSON модели", e);
            return null;
        }
    }
    
    /**
     * Получить косметику для игрока
     * @param playerUuid UUID игрока
     * @return Map с косметикой (boneName -> cosmeticId)
     */
    public CompletableFuture<Map<String, String>> getPlayerCosmetics(UUID playerUuid) {
        String url = API_BASE_URL + "/player/" + playerUuid.toString();
        
        return CompletableFuture.supplyAsync(() -> {
            Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.warn("Неуспешный ответ при получении косметики: {} - {}", url, response.code());
                    return new HashMap<>();
                }
                
                ResponseBody body = response.body();
                if (body == null) {
                    return new HashMap<>();
                }
                
                String json = body.string();
                // Парсим JSON ответ с косметикой
                @SuppressWarnings("unchecked")
                Map<String, String> cosmetics = gson.fromJson(json, Map.class);
                
                return cosmetics != null ? cosmetics : new HashMap<>();
            } catch (IOException e) {
                logger.error("Ошибка при получении косметики игрока: {}", playerUuid, e);
                return new HashMap<>();
            }
        }, executorService);
    }
    
    /**
     * Очистить кеш
     */
    public void clearCache() {
        modelCache.clear();
        textureCache.clear();
        logger.info("Кеш косметики очищен");
    }
    
    /**
     * Закрыть ресурсы
     */
    public void shutdown() {
        executorService.shutdown();
        httpClient.dispatcher().executorService().shutdown();
        logger.info("CosmeticManager остановлен");
    }
}
