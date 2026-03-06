package com.horizon.launcher.network;

import com.google.gson.Gson;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * HTTP клиент для работы с API сервером
 * Использует паттерн Singleton
 */
public class ApiClient {
    private static final Logger logger = LoggerFactory.getLogger(ApiClient.class);
    
    private static final String DEFAULT_BASE_URL = "http://localhost:3000";
    private static final String CONTENT_TYPE_JSON = "application/json";
    
    private static ApiClient instance;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private String baseUrl;

    /**
     * Приватный конструктор для Singleton паттерна
     */
    private ApiClient() {
        // Загружаем конфигурацию из properties файла
        com.horizon.launcher.utils.ConfigLoader config = com.horizon.launcher.utils.ConfigLoader.getInstance();
        this.baseUrl = config.get("api.server.url", DEFAULT_BASE_URL);
        this.gson = new Gson();
        
        // Загружаем таймауты из конфигурации
        int connectTimeout = config.getInt("api.timeout.connect", 10);
        int readTimeout = config.getInt("api.timeout.read", 10);
        int writeTimeout = config.getInt("api.timeout.write", 10);
        
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .writeTimeout(writeTimeout, TimeUnit.SECONDS)
                .build();
        
        logger.info("ApiClient инициализирован с базовым URL: {} (таймауты: connect={}s, read={}s, write={}s)", 
                baseUrl, connectTimeout, readTimeout, writeTimeout);
    }

    /**
     * Получить единственный экземпляр ApiClient
     */
    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }

    /**
     * Установить базовый URL API сервера
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        logger.info("Базовый URL изменен на: {}", baseUrl);
    }

    /**
     * Получить базовый URL API сервера
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Отправить HTTP запрос к API серверу
     * 
     * @param endpoint Путь к эндпоинту (например, "/api/auth/login")
     * @param method HTTP метод (GET, POST, PUT, DELETE)
     * @param body Тело запроса (объект, который будет сериализован в JSON). Может быть null для GET запросов
     * @param responseClass Класс для десериализации данных ответа
     * @param callback Колбэк для обработки результата
     * @param <T> Тип данных в ответе
     */
    @SuppressWarnings("unchecked")
    public <T> void sendRequest(String endpoint, String method, Object body, 
                                Class<T> responseClass, Callback<T> callback) {
        String url = baseUrl + endpoint;
        
        // Создаем тело запроса, если оно есть
        RequestBody requestBody = null;
        if (body != null) {
            String json = gson.toJson(body);
            requestBody = RequestBody.create(json, MediaType.parse(CONTENT_TYPE_JSON));
            logger.debug("Отправка запроса {} {} с телом: {}", method, url, json);
        } else {
            logger.debug("Отправка запроса {} {}", method, url);
        }

        // Создаем запрос
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", CONTENT_TYPE_JSON);

        // Устанавливаем метод и тело запроса
        switch (method.toUpperCase()) {
            case "GET":
                requestBuilder.get();
                break;
            case "POST":
                if (requestBody != null) {
                    requestBuilder.post(requestBody);
                } else {
                    requestBuilder.post(RequestBody.create("", MediaType.parse(CONTENT_TYPE_JSON)));
                }
                break;
            case "PUT":
                if (requestBody != null) {
                    requestBuilder.put(requestBody);
                } else {
                    requestBuilder.put(RequestBody.create("", MediaType.parse(CONTENT_TYPE_JSON)));
                }
                break;
            case "DELETE":
                if (requestBody != null) {
                    requestBuilder.delete(requestBody);
                } else {
                    requestBuilder.delete();
                }
                break;
            default:
                logger.error("Неподдерживаемый HTTP метод: {}", method);
                callback.onError(new IllegalArgumentException("Unsupported HTTP method: " + method));
                return;
        }

        Request request = requestBuilder.build();

        // Отправляем запрос асинхронно
        httpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                try {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    logger.debug("Ответ от {} {}: status={}, body={}", method, url, response.code(), responseBody);

                    // Пытаемся распарсить ответ как JSON, даже если HTTP код неуспешен
                    // Сервер может возвращать валидный JSON с success: false даже при 401, 403 и т.д.
                    com.google.gson.JsonObject jsonObject = null;
                    try {
                        if (!responseBody.isEmpty()) {
                            jsonObject = gson.fromJson(responseBody, com.google.gson.JsonObject.class);
                        }
                    } catch (com.google.gson.JsonSyntaxException e) {
                        logger.debug("Ответ не является валидным JSON: {}", responseBody);
                    }
                    
                    // Если ответ успешен (200-299) или это валидный JSON, обрабатываем его
                    if (response.isSuccessful() || jsonObject != null) {
                        if (jsonObject == null) {
                            logger.error("Не удалось распарсить ответ: {}", responseBody);
                            callback.onError(new IOException("Failed to parse response"));
                            return;
                        }

                        // Извлекаем поля из ответа
                        boolean success = jsonObject.has("success") && jsonObject.get("success").getAsBoolean();
                        String message = jsonObject.has("message") && !jsonObject.get("message").isJsonNull() 
                            ? jsonObject.get("message").getAsString() : null;
                        
                        // Десериализуем данные, если они есть и указан класс
                        T data = null;
                        if (jsonObject.has("data") && !jsonObject.get("data").isJsonNull()) {
                            // Если есть поле data, используем его
                            com.google.gson.JsonElement dataElement = jsonObject.get("data");
                            if (responseClass != null) {
                                try {
                                    // Десериализуем в указанный класс
                                    data = gson.fromJson(dataElement, responseClass);
                                } catch (Exception e) {
                                    logger.debug("Не удалось десериализовать data в класс {}: {}", responseClass, e.getMessage());
                                }
                            } else {
                                // Если класс не указан, пробуем вернуть как строку
                                try {
                                    @SuppressWarnings("unchecked")
                                    T stringData = (T) dataElement.getAsString();
                                    data = stringData;
                                } catch (Exception e) {
                                    // Если не строка, возвращаем как JSON строку
                                    @SuppressWarnings("unchecked")
                                    T jsonData = (T) dataElement.toString();
                                    data = jsonData;
                                }
                            }
                        } else if (responseClass != null) {
                            // Если поля data нет, но указан класс, десериализуем весь JSON
                            try {
                                data = gson.fromJson(jsonObject, responseClass);
                            } catch (Exception e) {
                                logger.debug("Не удалось десериализовать весь JSON в класс {}: {}", responseClass, e.getMessage());
                            }
                        }

                        callback.onSuccess(success, message, data);
                    } else {
                        // HTTP код неуспешен и ответ не является валидным JSON
                        logger.warn("Неуспешный HTTP ответ без валидного JSON: status={}, body={}", response.code(), responseBody);
                        callback.onError(new IOException("HTTP error: " + response.code() + " - " + responseBody));
                    }
                } catch (Exception e) {
                    logger.error("Ошибка при обработке ответа", e);
                    callback.onError(e);
                } finally {
                    response.close();
                }
            }

            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                logger.error("Ошибка при отправке запроса {} {}", method, url, e);
                
                // Определяем тип ошибки и формируем понятное сообщение
                String errorMessage = getNetworkErrorMessage(e);
                callback.onError(new IOException(errorMessage, e));
            }
        });
    }

    /**
     * Упрощенный метод для GET запросов
     */
    public <T> void get(String endpoint, Class<T> responseClass, Callback<T> callback) {
        sendRequest(endpoint, "GET", null, responseClass, callback);
    }

    /**
     * Упрощенный метод для POST запросов
     */
    public <T> void post(String endpoint, Object body, Class<T> responseClass, Callback<T> callback) {
        sendRequest(endpoint, "POST", body, responseClass, callback);
    }

    /**
     * Упрощенный метод для PUT запросов
     */
    public <T> void put(String endpoint, Object body, Class<T> responseClass, Callback<T> callback) {
        sendRequest(endpoint, "PUT", body, responseClass, callback);
    }

    /**
     * Упрощенный метод для DELETE запросов
     */
    public <T> void delete(String endpoint, Class<T> responseClass, Callback<T> callback) {
        sendRequest(endpoint, "DELETE", null, responseClass, callback);
    }

    /**
     * Извлечь сообщение об ошибке из ответа сервера
     */
    private String extractErrorMessage(String responseBody, int statusCode) {
        if (responseBody == null || responseBody.isEmpty()) {
            return getDefaultHttpErrorMessage(statusCode);
        }
        
        // Пытаемся распарсить как JSON
        try {
            com.google.gson.JsonObject jsonObject = gson.fromJson(responseBody, com.google.gson.JsonObject.class);
            if (jsonObject.has("message") && !jsonObject.get("message").isJsonNull()) {
                return jsonObject.get("message").getAsString();
            }
            if (jsonObject.has("error") && !jsonObject.get("error").isJsonNull()) {
                return jsonObject.get("error").getAsString();
            }
        } catch (Exception e) {
            // Не JSON, возвращаем как есть или дефолтное сообщение
        }
        
        // Если не удалось извлечь из JSON, возвращаем дефолтное сообщение
        return getDefaultHttpErrorMessage(statusCode);
    }
    
    /**
     * Получить дефолтное сообщение об ошибке по HTTP коду
     */
    private String getDefaultHttpErrorMessage(int statusCode) {
        switch (statusCode) {
            case 400:
                return "Неверный запрос. Проверьте введенные данные.";
            case 401:
                return "Неверный логин или пароль.";
            case 403:
                return "Доступ запрещен. Возможно, ваш аккаунт заблокирован.";
            case 404:
                return "Сервер не найден. Проверьте подключение к интернету.";
            case 409:
                return "Конфликт данных. Возможно, логин уже занят.";
            case 429:
                return "Слишком много запросов. Попробуйте позже.";
            case 500:
            case 502:
            case 503:
                return "Ошибка сервера. Попробуйте позже.";
            default:
                return "Ошибка соединения с сервером (код " + statusCode + ").";
        }
    }
    
    /**
     * Получить понятное сообщение об ошибке сети
     */
    private String getNetworkErrorMessage(IOException e) {
        String message = e.getMessage();
        if (message == null) {
            return "Ошибка соединения с сервером.";
        }
        
        String lowerMessage = message.toLowerCase();
        
        if (lowerMessage.contains("timeout") || lowerMessage.contains("timed out")) {
            return "Превышено время ожидания ответа сервера. Проверьте подключение к интернету.";
        } else if (lowerMessage.contains("connection refused") || lowerMessage.contains("connectexception")) {
            return "Не удалось подключиться к серверу. Убедитесь, что сервер запущен.";
        } else if (lowerMessage.contains("network is unreachable") || lowerMessage.contains("no route to host")) {
            return "Сеть недоступна. Проверьте подключение к интернету.";
        } else if (lowerMessage.contains("unknown host") || lowerMessage.contains("host not found")) {
            return "Сервер не найден. Проверьте настройки подключения.";
        } else if (lowerMessage.contains("ssl") || lowerMessage.contains("certificate")) {
            return "Ошибка SSL соединения. Проверьте сертификат сервера.";
        }
        
        return "Ошибка сети: " + message;
    }
    
    /**
     * Интерфейс колбэка для обработки результатов запроса
     * @param <T> Тип данных в ответе
     */
    public interface Callback<T> {
        /**
         * Вызывается при успешном ответе
         * @param success Успешность операции
         * @param message Сообщение от сервера
         * @param data Данные из ответа
         */
        void onSuccess(boolean success, String message, T data);

        /**
         * Вызывается при ошибке
         * @param error Исключение с описанием ошибки
         */
        void onError(Throwable error);
    }
}
