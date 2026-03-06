package com.horizon.launcher.services;

import com.google.gson.JsonObject;
import com.horizon.launcher.models.ApiResponse;
import com.horizon.launcher.network.ApiClient;
import com.horizon.launcher.utils.HWIDManager;
import com.horizon.launcher.utils.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Сервис авторизации с поддержкой Telegram 2FA
 */
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static AuthService instance;
    
    private final ApiClient apiClient;
    private final HWIDManager hwidManager;
    private final SessionManager sessionManager;
    
    /**
     * Колбэк для результата логина
     */
    public interface LoginCallback {
        void onSuccess(String username, String token);
        void onNeed2FA();
        void onError(String error);
    }
    
    /**
     * Колбэк для результата проверки 2FA
     */
    public interface Verify2FACallback {
        void onSuccess(String username, String token);
        void onError(String error);
    }
    
    /**
     * Колбэк для результата регистрации
     */
    public interface RegisterCallback {
        void onSuccess(String username, String token);
        void onError(String error);
    }
    
    /**
     * Колбэк для запроса кода восстановления пароля
     */
    public interface RequestRecoveryCodeCallback {
        void onSuccess();
        void onError(String error);
    }
    
    /**
     * Колбэк для восстановления пароля
     */
    public interface ResetPasswordCallback {
        void onSuccess();
        void onError(String error);
    }
    
    /**
     * Колбэк для автоматического входа по токену
     */
    public interface AutoLoginCallback {
        void onSuccess(String username, String token);
        void onError(String error);
    }
    
    private AuthService() {
        this.apiClient = ApiClient.getInstance();
        this.hwidManager = HWIDManager.getInstance();
        this.sessionManager = SessionManager.getInstance();
    }
    
    public static synchronized AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }
    
    /**
     * Попытка входа
     * @param username Имя пользователя
     * @param password Пароль
     * @param callback Колбэк для результата
     */
    public void attemptLogin(String username, String password, LoginCallback callback) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            if (callback != null) {
                callback.onError("Введите имя пользователя и пароль");
            }
            return;
        }
        
        // Формируем тело запроса с HWID
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", username);
        requestBody.put("password", password);
        requestBody.put("hwid", hwidManager.getHWID());
        
        apiClient.post("/api/auth/login", requestBody, JsonObject.class, new ApiClient.Callback<JsonObject>() {
            @Override
            public void onSuccess(boolean success, String message, JsonObject data) {
                // Проверяем на 2FA в первую очередь (даже если success = false)
                if (data != null) {
                    if (data.has("status") && "NEED_2FA".equals(data.get("status").getAsString())) {
                        logger.info("Требуется 2FA для пользователя: {}", username);
                        if (callback != null) {
                            callback.onNeed2FA();
                        }
                        return;
                    }
                    
                    if (data.has("requires2FA") && data.get("requires2FA").getAsBoolean()) {
                        logger.info("Требуется 2FA для пользователя: {}", username);
                        if (callback != null) {
                            callback.onNeed2FA();
                        }
                        return;
                    }
                }
                
                // Проверяем успешную авторизацию
                if (success && data != null && data.has("token")) {
                    String token = data.get("token").getAsString();
                    String returnedUsername = data.has("username") ? data.get("username").getAsString() : username;
                    
                    if (token != null && !token.isEmpty()) {
                        sessionManager.setToken(token);
                        sessionManager.setUsername(returnedUsername);
                        logger.info("Успешная авторизация для пользователя: {}", returnedUsername);
                        if (callback != null) {
                            callback.onSuccess(returnedUsername, token);
                        }
                        return;
                    }
                }
                
                // Ошибка авторизации - извлекаем детальное сообщение
                String errorMessage = extractDetailedErrorMessage(data, message, "Ошибка авторизации. Проверьте введенные данные и попробуйте снова.");
                logger.warn("Ошибка авторизации: {}", errorMessage);
                if (callback != null) {
                    callback.onError(errorMessage);
                }
            }
            
            @Override
            public void onError(Throwable error) {
                String errorMessage = extractErrorFromException(error);
                logger.error("Ошибка при запросе авторизации", error);
                if (callback != null) {
                    callback.onError(errorMessage);
                }
            }
        });
    }
    
    /**
     * Проверка 2FA кода
     * @param username Имя пользователя
     * @param code Код 2FA (6 символов)
     * @param callback Колбэк для результата
     */
    public void verify2FA(String username, String code, Verify2FACallback callback) {
        if (username == null || username.isEmpty()) {
            if (callback != null) {
                callback.onError("Имя пользователя не указано");
            }
            return;
        }
        
        if (code == null || code.isEmpty() || code.length() != 6) {
            if (callback != null) {
                callback.onError("Код 2FA должен содержать 6 символов");
            }
            return;
        }
        
        // Формируем тело запроса
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", username);
        requestBody.put("code", code);
        requestBody.put("hwid", hwidManager.getHWID());
        
        apiClient.post("/api/auth/verify-2fa", requestBody, JsonObject.class, new ApiClient.Callback<JsonObject>() {
            @Override
            public void onSuccess(boolean success, String message, JsonObject data) {
                if (success && data != null && data.has("token")) {
                    String token = data.get("token").getAsString();
                    if (token != null && !token.isEmpty()) {
                        sessionManager.setToken(token);
                        sessionManager.setUsername(username);
                        logger.info("Успешная проверка 2FA для пользователя: {}", username);
                        if (callback != null) {
                            callback.onSuccess(username, token);
                        }
                        return;
                    }
                }
                
                // Ошибка проверки 2FA - извлекаем детальное сообщение
                String errorMessage = extractDetailedErrorMessage(data, message, "Неверный код 2FA");
                logger.warn("Ошибка проверки 2FA: {}", errorMessage);
                if (callback != null) {
                    callback.onError(errorMessage);
                }
            }
            
            @Override
            public void onError(Throwable error) {
                String errorMessage = extractErrorFromException(error);
                logger.error("Ошибка при проверке 2FA", error);
                if (callback != null) {
                    callback.onError(errorMessage);
                }
            }
        });
    }
    
    /**
     * Регистрация нового пользователя
     * @param username Имя пользователя
     * @param password Пароль
     * @param callback Колбэк для результата
     */
    public void register(String username, String password, RegisterCallback callback) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            if (callback != null) {
                callback.onError("Введите имя пользователя и пароль");
            }
            return;
        }
        
        if (password.length() < 6) {
            if (callback != null) {
                callback.onError("Пароль должен содержать минимум 6 символов");
            }
            return;
        }
        
        // Формируем тело запроса с HWID
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", username);
        requestBody.put("password", password);
        requestBody.put("hwid", hwidManager.getHWID());
        
        apiClient.post("/api/auth/register", requestBody, JsonObject.class, new ApiClient.Callback<JsonObject>() {
            @Override
            public void onSuccess(boolean success, String message, JsonObject data) {
                if (success && data != null && data.has("token")) {
                    // Успешная регистрация
                    String token = data.get("token").getAsString();
                    String returnedUsername = data.has("username") ? data.get("username").getAsString() : username;
                    
                    if (token != null && !token.isEmpty()) {
                        sessionManager.setToken(token);
                        sessionManager.setUsername(returnedUsername);
                        logger.info("Успешная регистрация для пользователя: {}", returnedUsername);
                        if (callback != null) {
                            callback.onSuccess(returnedUsername, token);
                        }
                        return;
                    }
                }
                
                // Ошибка регистрации - извлекаем детальное сообщение
                String errorMessage = extractDetailedErrorMessage(data, message, "Ошибка регистрации");
                logger.warn("Ошибка регистрации: {}", errorMessage);
                if (callback != null) {
                    callback.onError(errorMessage);
                }
            }
            
            @Override
            public void onError(Throwable error) {
                String errorMessage = extractErrorFromException(error);
                logger.error("Ошибка при запросе регистрации", error);
                if (callback != null) {
                    callback.onError(errorMessage);
                }
            }
        });
    }
    
    /**
     * Запросить код восстановления пароля через Telegram
     * @param username Имя пользователя
     * @param callback Колбэк для результата
     */
    public void requestRecoveryCode(String username, RequestRecoveryCodeCallback callback) {
        if (username == null || username.isEmpty()) {
            if (callback != null) {
                callback.onError("Введите имя пользователя");
            }
            return;
        }
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", username);
        requestBody.put("hwid", hwidManager.getHWID());
        
        apiClient.post("/api/auth/recovery/request", requestBody, JsonObject.class, new ApiClient.Callback<JsonObject>() {
            @Override
            public void onSuccess(boolean success, String message, JsonObject data) {
                if (success) {
                    logger.info("Код восстановления запрошен для пользователя: {}", username);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                } else {
                    String errorMessage = extractDetailedErrorMessage(data, message, "Ошибка при запросе кода восстановления");
                    logger.warn("Ошибка запроса кода восстановления: {}", errorMessage);
                    if (callback != null) {
                        callback.onError(errorMessage);
                    }
                }
            }
            
            @Override
            public void onError(Throwable error) {
                String errorMessage = extractErrorFromException(error);
                logger.error("Ошибка при запросе кода восстановления", error);
                if (callback != null) {
                    callback.onError(errorMessage);
                }
            }
        });
    }
    
    /**
     * Восстановить пароль с использованием кода из Telegram
     * @param username Имя пользователя
     * @param code Код подтверждения (6 цифр)
     * @param newPassword Новый пароль
     * @param callback Колбэк для результата
     */
    public void resetPassword(String username, String code, String newPassword, ResetPasswordCallback callback) {
        if (username == null || username.isEmpty() || code == null || code.isEmpty() || newPassword == null || newPassword.isEmpty()) {
            if (callback != null) {
                callback.onError("Заполните все поля");
            }
            return;
        }
        
        if (code.length() != 6) {
            if (callback != null) {
                callback.onError("Код должен содержать 6 цифр");
            }
            return;
        }
        
        if (newPassword.length() < 6) {
            if (callback != null) {
                callback.onError("Пароль должен содержать минимум 6 символов");
            }
            return;
        }
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", username);
        requestBody.put("code", code);
        requestBody.put("newPassword", newPassword);
        requestBody.put("hwid", hwidManager.getHWID());
        
        apiClient.post("/api/auth/recovery/reset", requestBody, JsonObject.class, new ApiClient.Callback<JsonObject>() {
            @Override
            public void onSuccess(boolean success, String message, JsonObject data) {
                if (success) {
                    logger.info("Пароль успешно восстановлен для пользователя: {}", username);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                } else {
                    String errorMessage = extractDetailedErrorMessage(data, message, "Ошибка при восстановлении пароля");
                    logger.warn("Ошибка восстановления пароля: {}", errorMessage);
                    if (callback != null) {
                        callback.onError(errorMessage);
                    }
                }
            }
            
            @Override
            public void onError(Throwable error) {
                String errorMessage = extractErrorFromException(error);
                logger.error("Ошибка при восстановлении пароля", error);
                if (callback != null) {
                    callback.onError(errorMessage);
                }
            }
        });
    }
    
    /**
     * Автоматический вход по сохраненному токену
     * @param callback Колбэк для результата
     */
    public void autoLogin(AutoLoginCallback callback) {
        String token = sessionManager.getToken();
        String username = sessionManager.getUsername();
        
        if (token == null || token.isEmpty() || username == null || username.isEmpty()) {
            if (callback != null) {
                callback.onError("Сессия не найдена");
            }
            return;
        }
        
        // Проверяем токен через API
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("token", token);
        requestBody.put("username", username);
        requestBody.put("hwid", hwidManager.getHWID());
        
        apiClient.post("/api/auth/verify", requestBody, JsonObject.class, new ApiClient.Callback<JsonObject>() {
            @Override
            public void onSuccess(boolean success, String message, JsonObject data) {
                if (success && data != null) {
                    logger.info("Автоматический вход успешен для пользователя: {}", username);
                    if (callback != null) {
                        callback.onSuccess(username, token);
                    }
                } else {
                    // Токен невалиден, очищаем сессию
                    sessionManager.clearSession();
                    logger.warn("Токен невалиден, сессия очищена");
                    if (callback != null) {
                        callback.onError("Сессия истекла");
                    }
                }
            }
            
            @Override
            public void onError(Throwable error) {
                // При ошибке сети считаем, что токен валиден (офлайн режим)
                logger.warn("Ошибка при проверке токена, считаем токен валидным: {}", error.getMessage());
                if (callback != null) {
                    callback.onSuccess(username, token);
                }
            }
        });
    }
    
    /**
     * Проверить, авторизован ли пользователь
     */
    public boolean isAuthenticated() {
        return sessionManager.isAuthenticated();
    }
    
    /**
     * Получить текущего пользователя
     */
    public String getCurrentUsername() {
        return sessionManager.getUsername();
    }
    
    /**
     * Выйти из системы
     */
    public void logout() {
        sessionManager.clearSession();
        logger.info("Пользователь вышел из системы");
    }
    
    /**
     * Извлечь детальное сообщение об ошибке из ответа API
     */
    private String extractDetailedErrorMessage(JsonObject data, String message, String defaultMessage) {
        // Сначала проверяем поле message в ответе
        if (message != null && !message.isEmpty() && !message.equals("null")) {
            return message;
        }
        
        // Пытаемся извлечь из data
        if (data != null) {
            if (data.has("message") && !data.get("message").isJsonNull()) {
                String msg = data.get("message").getAsString();
                if (msg != null && !msg.isEmpty()) {
                    return msg;
                }
            }
            if (data.has("error") && !data.get("error").isJsonNull()) {
                String err = data.get("error").getAsString();
                if (err != null && !err.isEmpty()) {
                    return err;
                }
            }
        }
        
        return defaultMessage;
    }
    
    /**
     * Извлечь понятное сообщение об ошибке из исключения
     */
    private String extractErrorFromException(Throwable error) {
        if (error == null) {
            return "Произошла неизвестная ошибка";
        }
        
        String errorMessage = error.getMessage();
        if (errorMessage == null || errorMessage.isEmpty()) {
            return "Ошибка соединения с сервером";
        }
        
        // Если это IOException с детальным сообщением, используем его
        if (error instanceof java.io.IOException && errorMessage.length() > 20) {
            return errorMessage;
        }
        
        // Иначе формируем понятное сообщение
        String lowerMessage = errorMessage.toLowerCase();
        
        if (lowerMessage.contains("timeout")) {
            return "Превышено время ожидания ответа сервера. Проверьте подключение к интернету.";
        } else if (lowerMessage.contains("connection refused") || lowerMessage.contains("connect")) {
            return "Не удалось подключиться к серверу. Убедитесь, что сервер запущен.";
        } else if (lowerMessage.contains("network") || lowerMessage.contains("unreachable")) {
            return "Сеть недоступна. Проверьте подключение к интернету.";
        } else if (lowerMessage.contains("host not found") || lowerMessage.contains("unknown host")) {
            return "Сервер не найден. Проверьте настройки подключения.";
        }
        
        return "Ошибка сети: " + errorMessage;
    }
}
