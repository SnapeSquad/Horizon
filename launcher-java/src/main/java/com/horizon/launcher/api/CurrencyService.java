package com.horizon.launcher.api;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Сервис для работы с донат-валютой
 */
public class CurrencyService {
    private static final Logger logger = LoggerFactory.getLogger(CurrencyService.class);
    private final ApiClient apiClient;

    public CurrencyService() {
        this.apiClient = ApiClient.getInstance();
    }

    /**
     * Получает баланс донат-валюты пользователя
     */
    public int getBalance(String username) {
        try {
            ApiClient.ApiResponse response = apiClient.get("/api/user/currency?username=" + username);
            if (response.isSuccess()) {
                JsonObject body = response.getBody();
                if (body.has("balance")) {
                    return body.get("balance").getAsInt();
                }
            }
        } catch (Exception e) {
            logger.error("Ошибка получения баланса", e);
        }
        return 0;
    }

    /**
     * Выдает донат-валюту пользователю (админ команда)
     */
    public boolean giveCurrency(String username, int amount, String adminToken) {
        try {
            JsonObject data = new JsonObject();
            data.addProperty("username", username);
            data.addProperty("amount", amount);
            data.addProperty("adminToken", adminToken);

            ApiClient.ApiResponse response = apiClient.post("/api/admin/currency/give", data);
            return response.isSuccess();
        } catch (Exception e) {
            logger.error("Ошибка выдачи валюты", e);
            return false;
        }
    }

    /**
     * Покупает косметику за донат-валюту
     */
    public boolean purchaseCosmetic(String username, String cosmeticId, int price) {
        try {
            JsonObject data = new JsonObject();
            data.addProperty("username", username);
            data.addProperty("cosmeticId", cosmeticId);
            data.addProperty("price", price);

            ApiClient.ApiResponse response = apiClient.post("/api/user/currency/purchase", data);
            return response.isSuccess();
        } catch (Exception e) {
            logger.error("Ошибка покупки косметики", e);
            return false;
        }
    }
    
    /**
     * Списывает валюту с баланса пользователя
     */
    public boolean deductBalance(String username, int amount) {
        logger.info("Списание {} валюты у пользователя: {}", amount, username);
        
        try {
            JsonObject data = new JsonObject();
            data.addProperty("username", username);
            data.addProperty("amount", amount);
            
            ApiClient.ApiResponse response = apiClient.post("/api/currency/deduct", data);
            
            if (response.isSuccess()) {
                logger.info("Валюта успешно списана");
                return true;
            } else {
                logger.warn("Ошибка списания валюты: {}", response.getMessage());
                return false;
            }
        } catch (Exception e) {
            logger.error("Ошибка списания валюты", e);
            return false;
        }
    }
}




