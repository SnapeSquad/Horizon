package com.horizon.launcher.api;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Сервис для работы с двухфакторной аутентификацией
 */
public class TwoFactorService {
    private static final Logger logger = LoggerFactory.getLogger(TwoFactorService.class);
    private final ApiClient apiClient;

    public TwoFactorService() {
        this.apiClient = ApiClient.getInstance();
    }

    /**
     * Получает статус 2FA
     */
    public TwoFactorStatus getStatus(String username) {
        logger.info("Получение статуса 2FA для: {}", username);
        
        ApiClient.ApiResponse response = apiClient.get("/api/user/2fa/status?username=" + username);
        
        if (response.getStatusCode() == 200 && response.getBody().has("success")) {
            JsonObject body = response.getBody();
            boolean enabled = body.has("enabled") && body.get("enabled").getAsBoolean();
            boolean hasTelegram = body.has("telegram_enabled") && body.get("telegram_enabled").getAsBoolean();
            
            return new TwoFactorStatus(enabled, hasTelegram);
        }
        
        return new TwoFactorStatus(false, false);
    }

    /**
     * Настраивает Telegram 2FA
     */
    public SetupResult setupTelegram(String username) {
        logger.info("Настройка Telegram 2FA для: {}", username);
        
        JsonObject data = new JsonObject();
        data.addProperty("username", username);

        ApiClient.ApiResponse response = apiClient.post("/api/user/2fa/telegram/setup", data);
        
        if (response.isSuccess()) {
            String linkCode = response.getBody().get("linkCode").getAsString();
            String message = response.getBody().get("message").getAsString();
            return new SetupResult(true, message, linkCode);
        } else {
            return new SetupResult(false, response.getMessage(), null);
        }
    }

    /**
     * Отключает 2FA
     */
    public boolean disable(String username, String password) {
        logger.info("Отключение 2FA для: {}", username);
        
        JsonObject data = new JsonObject();
        data.addProperty("username", username);
        data.addProperty("password", password);

        ApiClient.ApiResponse response = apiClient.post("/api/user/2fa/disable", data);
        return response.isSuccess();
    }

    public static class TwoFactorStatus {
        private final boolean enabled;
        private final boolean hasTelegram;

        public TwoFactorStatus(boolean enabled, boolean hasTelegram) {
            this.enabled = enabled;
            this.hasTelegram = hasTelegram;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public boolean hasTelegram() {
            return hasTelegram;
        }
    }

    public static class SetupResult {
        private final boolean success;
        private final String message;
        private final String linkCode;

        public SetupResult(boolean success, String message, String linkCode) {
            this.success = success;
            this.message = message;
            this.linkCode = linkCode;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getLinkCode() {
            return linkCode;
        }
    }
}









