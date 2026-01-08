package com.horizon.launcher.api;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Сервис для работы с авторизацией
 */
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final ApiClient apiClient;
    private String currentUsername;

    public AuthService() {
        this.apiClient = ApiClient.getInstance();
    }

    /**
     * Регистрация нового пользователя
     */
    public AuthResult register(String username, String password) {
        logger.info("Регистрация пользователя: {}", username);
        
        // Валидация входных данных (AAA-уровень)
        com.horizon.launcher.util.ValidationUtils.ValidationResult usernameValidation = 
                com.horizon.launcher.util.ValidationUtils.validateUsername(username);
        if (!usernameValidation.isValid()) {
            logger.warn("Невалидное имя пользователя: {}", usernameValidation.getMessage());
            return new AuthResult(false, usernameValidation.getMessage(), null);
        }
        
        com.horizon.launcher.util.ValidationUtils.ValidationResult passwordValidation = 
                com.horizon.launcher.util.ValidationUtils.validatePassword(password);
        if (!passwordValidation.isValid()) {
            logger.warn("Невалидный пароль: {}", passwordValidation.getMessage());
            return new AuthResult(false, passwordValidation.getMessage(), null);
        }
        
        // Санитизация данных
        username = com.horizon.launcher.util.ValidationUtils.sanitize(username);
        
        JsonObject data = new JsonObject();
        data.addProperty("username", username);
        data.addProperty("password", password);

        ApiClient.ApiResponse response = apiClient.post("/api/auth/register", data);
        
        if (response.isSuccess()) {
            return new AuthResult(true, "Регистрация успешна!", null);
        } else {
            return new AuthResult(false, response.getMessage(), null);
        }
    }

    /**
     * Вход в систему
     */
    public AuthResult login(String username, String password, String twoFactorCode) {
        logger.info("Вход пользователя: {}", username);
        
        // Валидация входных данных (AAA-уровень)
        com.horizon.launcher.util.ValidationUtils.ValidationResult usernameValidation = 
                com.horizon.launcher.util.ValidationUtils.validateUsername(username);
        if (!usernameValidation.isValid()) {
            logger.warn("Невалидное имя пользователя: {}", usernameValidation.getMessage());
            return new AuthResult(false, usernameValidation.getMessage(), null);
        }
        
        if (password == null || password.isEmpty()) {
            return new AuthResult(false, "Пароль не может быть пустым", null);
        }
        
        // Валидация кода 2FA, если указан
        if (twoFactorCode != null && !twoFactorCode.isEmpty()) {
            com.horizon.launcher.util.ValidationUtils.ValidationResult codeValidation = 
                    com.horizon.launcher.util.ValidationUtils.validate2FACode(twoFactorCode);
            if (!codeValidation.isValid()) {
                logger.warn("Невалидный код 2FA: {}", codeValidation.getMessage());
                return new AuthResult(false, codeValidation.getMessage(), null);
            }
        }
        
        // Санитизация данных
        username = com.horizon.launcher.util.ValidationUtils.sanitize(username);
        
        JsonObject data = new JsonObject();
        data.addProperty("username", username);
        data.addProperty("password", password);
        if (twoFactorCode != null && !twoFactorCode.isEmpty()) {
            data.addProperty("twoFactorCode", twoFactorCode.trim().replaceAll("\\D", ""));
        }
        
        // Добавляем HWID для защиты от обхода бана
        try {
            com.horizon.launcher.util.HWIDManager hwidManager = com.horizon.launcher.util.HWIDManager.getInstance();
            String hwid = hwidManager.getHWID();
            data.addProperty("hwid", hwid);
            logger.debug("HWID отправлен при входе");
        } catch (Exception e) {
            logger.warn("Не удалось получить HWID для отправки", e);
        }

        ApiClient.ApiResponse response = apiClient.post("/api/auth/login", data);
        
        if (response.isSuccess()) {
            this.currentUsername = username;
            return new AuthResult(true, "Вход выполнен успешно!", username);
        } else {
            // Проверяем, требуется ли 2FA
            if (response.getBody().has("requires2FA") && 
                response.getBody().get("requires2FA").getAsBoolean()) {
                return new AuthResult(false, "Требуется код 2FA", null, true);
            }
            return new AuthResult(false, response.getMessage(), null);
        }
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public void logout() {
        this.currentUsername = null;
    }
    
    /**
     * Вход через Microsoft OAuth2 (лицензия)
     */
    public AuthResult loginMicrosoft() {
        logger.info("Начало Microsoft OAuth2 авторизации");
        
        try {
            com.horizon.launcher.auth.MicrosoftAuthService microsoftAuth = 
                    com.horizon.launcher.auth.MicrosoftAuthService.getInstance();
            
            // Выполняем авторизацию (асинхронно)
            java.util.concurrent.CompletableFuture<com.horizon.launcher.auth.MicrosoftAuthService.MicrosoftAuthResult> future = 
                    microsoftAuth.authenticate();
            
            // Ждем результат (в реальности это должно быть в отдельном потоке)
            com.horizon.launcher.auth.MicrosoftAuthService.MicrosoftAuthResult result = 
                    future.get(5, java.util.concurrent.TimeUnit.MINUTES);
            
            if (result.isSuccess()) {
                this.currentUsername = result.getMinecraftUsername();
                return new AuthResult(true, "Microsoft авторизация успешна!", 
                        result.getMinecraftUsername());
            } else {
                return new AuthResult(false, result.getMessage(), null);
            }
        } catch (Exception e) {
            logger.error("Ошибка Microsoft авторизации", e);
            return new AuthResult(false, "Ошибка Microsoft авторизации: " + e.getMessage(), null);
        }
    }

    /**
     * Результат операции авторизации
     */
    public static class AuthResult {
        private final boolean success;
        private final String message;
        private final String username;
        private final boolean requires2FA;

        public AuthResult(boolean success, String message, String username) {
            this(success, message, username, false);
        }

        public AuthResult(boolean success, String message, String username, boolean requires2FA) {
            this.success = success;
            this.message = message;
            this.username = username;
            this.requires2FA = requires2FA;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getUsername() {
            return username;
        }

        public boolean requires2FA() {
            return requires2FA;
        }
    }
}






