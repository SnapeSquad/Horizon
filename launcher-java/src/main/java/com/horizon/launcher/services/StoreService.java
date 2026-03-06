package com.horizon.launcher.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.horizon.launcher.models.ApiResponse;
import com.horizon.launcher.models.StoreItem;
import com.horizon.launcher.network.ApiClient;
import com.horizon.launcher.utils.DataCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Сервис для взаимодействия с магазином и валютой
 */
public class StoreService {
    private static final Logger logger = LoggerFactory.getLogger(StoreService.class);
    private static StoreService instance;
    private final ApiClient apiClient;
    private final Gson gson;
    private final DataCache dataCache;

    private StoreService() {
        this.apiClient = ApiClient.getInstance();
        this.gson = new Gson();
        this.dataCache = DataCache.getInstance();
    }

    public static synchronized StoreService getInstance() {
        if (instance == null) {
            instance = new StoreService();
        }
        return instance;
    }

    /**
     * Колбэк для получения товаров
     */
    public interface StoreItemsCallback {
        void onSuccess(List<StoreItem> items);
        void onError(String error);
    }
    
    /**
     * Колбэк для получения баланса
     */
    public interface BalanceCallback {
        void onSuccess(int balance);
        void onError(String error);
    }
    
    /**
     * Колбэк для покупки товара
     */
    public interface PurchaseCallback {
        void onSuccess(int newBalance);
        void onError(String error);
    }
    
    /**
     * Колбэк для генерации ссылки на оплату
     */
    public interface PaymentLinkCallback {
        void onSuccess(String paymentUrl);
        void onError(String error);
    }

    /**
     * Получает список доступных товаров в магазине
     * Использует кэширование для оптимизации производительности
     * @param callback Колбэк для результата
     */
    public void getAvailableItems(StoreItemsCallback callback) {
        String cacheKey = DataCache.createApiCacheKey("/api/cosmetics/available", null);
        
        // Проверяем кэш
        String cachedResponse = dataCache.getApiResponse(cacheKey);
        if (cachedResponse != null) {
            try {
                JsonObject data = gson.fromJson(cachedResponse, JsonObject.class);
                List<StoreItem> items = parseItemsFromJson(data);
                callback.onSuccess(items);
                return;
            } catch (Exception e) {
                logger.warn("Ошибка при парсинге кэшированного ответа", e);
            }
        }
        
        apiClient.get("/api/cosmetics/available", JsonObject.class, new ApiClient.Callback<JsonObject>() {
            @Override
            public void onSuccess(boolean success, String message, JsonObject data) {
                if (success && data != null) {
                    try {
                        List<StoreItem> items = parseItemsFromJson(data);
                        
                        // Сохраняем в кэш
                        dataCache.putApiResponse(cacheKey, data.toString());
                        
                        callback.onSuccess(items);
                    } catch (Exception e) {
                        logger.error("Ошибка при парсинге товаров магазина", e);
                        callback.onError("Ошибка при загрузке товаров: " + e.getMessage());
                    }
                } else {
                    callback.onError(message != null ? message : "Не удалось загрузить товары");
                }
            }

            @Override
            public void onError(Throwable error) {
                logger.error("Ошибка при запросе товаров магазина", error);
                callback.onError("Ошибка сети: " + error.getMessage());
            }
        });
    }

    /**
     * Парсить StoreItem из JSON объекта
     */
    private StoreItem parseStoreItem(JsonObject json) {
        try {
            return StoreItem.fromJson(json);
        } catch (Exception e) {
            logger.error("Ошибка при парсинге товара", e);
            return null;
        }
    }
    
    /**
     * Парсить список товаров из JSON объекта
     */
    private List<StoreItem> parseItemsFromJson(JsonObject data) {
        List<StoreItem> items = new ArrayList<>();
        
        // API может вернуть либо массив напрямую, либо объект с полем cosmetics
        JsonArray itemsArray = null;
        if (data.has("cosmetics") && data.get("cosmetics").isJsonArray()) {
            itemsArray = data.getAsJsonArray("cosmetics");
        } else if (data.isJsonArray()) {
            itemsArray = data.getAsJsonArray();
        }
        
        if (itemsArray != null) {
            for (JsonElement element : itemsArray) {
                if (element.isJsonObject()) {
                    JsonObject itemObj = element.getAsJsonObject();
                    StoreItem item = parseStoreItem(itemObj);
                    if (item != null) {
                        items.add(item);
                    }
                }
            }
        }
        
        return items;
    }

    /**
     * Получает текущий баланс "Хорики" пользователя
     * @param username Имя пользователя
     * @param callback Колбэк для результата
     */
    public void getBalance(String username, BalanceCallback callback) {
        apiClient.get("/api/user/currency?username=" + username, JsonObject.class, new ApiClient.Callback<JsonObject>() {
            @Override
            public void onSuccess(boolean success, String message, JsonObject data) {
                if (success && data != null) {
                    try {
                        int balance = 0;
                        if (data.has("balance")) {
                            balance = data.get("balance").getAsInt();
                        } else if (data.has("currency")) {
                            balance = data.get("currency").getAsInt();
                        }
                        callback.onSuccess(balance);
                    } catch (Exception e) {
                        logger.error("Ошибка при парсинге баланса", e);
                        callback.onError("Ошибка при загрузке баланса: " + e.getMessage());
                    }
                } else {
                    callback.onError(message != null ? message : "Не удалось получить баланс");
                }
            }

            @Override
            public void onError(Throwable error) {
                logger.error("Ошибка при запросе баланса пользователя {}", username, error);
                callback.onError("Ошибка сети: " + error.getMessage());
            }
        });
    }

    /**
     * Отправляет запрос на покупку предмета
     * @param username Имя пользователя
     * @param cosmeticId ID предмета
     * @param price Цена предмета
     * @param callback Колбэк для результата
     */
    public void purchaseItem(String username, String cosmeticId, int price, PurchaseCallback callback) {
        Map<String, Object> requestBody = Map.of(
                "username", username,
                "cosmeticId", cosmeticId,
                "price", price
        );

        apiClient.post("/api/user/currency/purchase", requestBody, JsonObject.class, new ApiClient.Callback<JsonObject>() {
            @Override
            public void onSuccess(boolean success, String message, JsonObject data) {
                if (success && data != null && data.has("balance")) {
                    int newBalance = data.get("balance").getAsInt();
                    callback.onSuccess(newBalance);
                } else {
                    callback.onError(message != null ? message : "Ошибка при покупке товара");
                }
            }

            @Override
            public void onError(Throwable error) {
                logger.error("Ошибка при покупке предмета {} для пользователя {}", cosmeticId, username, error);
                callback.onError("Ошибка сети: " + error.getMessage());
            }
        });
    }

    /**
     * Генерирует ссылку для пополнения баланса через Telegram бота
     * @param username Имя пользователя
     * @param amount Сумма пополнения
     * @param callback Колбэк для результата
     */
    public void generatePaymentLink(String username, int amount, PaymentLinkCallback callback) {
        Map<String, Object> requestBody = Map.of(
                "username", username,
                "amount", amount
        );

        apiClient.post("/api/payment/generate", requestBody, JsonObject.class, new ApiClient.Callback<JsonObject>() {
            @Override
            public void onSuccess(boolean success, String message, JsonObject data) {
                if (success && data != null && data.has("paymentUrl")) {
                    String paymentUrl = data.get("paymentUrl").getAsString();
                    callback.onSuccess(paymentUrl);
                } else {
                    callback.onError(message != null ? message : "Не удалось сгенерировать ссылку для оплаты");
                }
            }

            @Override
            public void onError(Throwable error) {
                logger.error("Ошибка при генерации ссылки для оплаты", error);
                callback.onError("Ошибка сети: " + error.getMessage());
            }
        });
    }
}
