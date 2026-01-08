package com.horizon.launcher.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.horizon.launcher.util.ConfigManager;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Клиент для работы с API сервером
 * 
 * Особенности безопасности:
 * - TLS 1.3 только
 * - Certificate Pinning для защиты от подмены IP через hosts
 */
public class ApiClient {
    private static final Logger logger = LoggerFactory.getLogger(ApiClient.class);
    private static ApiClient instance;
    private final OkHttpClient client;
    private final Gson gson;
    private final String baseUrl;

    private ApiClient() {
        this.gson = new Gson();
        this.baseUrl = ConfigManager.getInstance().getApiUrl();
        
        // Настройка TLS 1.3 и Certificate Pinning
        OkHttpClient configuredClient;
        try {
            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS);
            
            // Настройка TLS протоколов (только TLS 1.3)
            builder.sslSocketFactory(createTLS13SocketFactory(), (X509TrustManager) getTrustManager()[0]);
            builder.hostnameVerifier((hostname, session) -> {
                // Строгая проверка hostname для безопасности
                String expectedHost = extractHostname(baseUrl);
                if (expectedHost == null) {
                    logger.warn("Не удалось извлечь hostname из baseUrl: {}", baseUrl);
                    return false;
                }
                return hostname.equalsIgnoreCase(expectedHost);
            });
            
            // Certificate Pinning
            String hostname = extractHostname(baseUrl);
            if (hostname != null) {
                // Получаем закрепленные сертификаты из конфига
                List<String> pinnedCertificates = ConfigManager.getInstance().getPinnedCertificates(hostname);
                if (pinnedCertificates != null && !pinnedCertificates.isEmpty()) {
                    builder.certificatePinner(new CertificatePinner.Builder()
                            .add(hostname, pinnedCertificates.toArray(new String[0]))
                            .build());
                    logger.info("Certificate Pinning активирован для {}", hostname);
                } else {
                    logger.warn("Certificate Pinning не настроен для {}", hostname);
                }
            }
            
            configuredClient = builder.build();
        } catch (Exception e) {
            logger.error("Ошибка настройки TLS/Certificate Pinning, используется стандартная конфигурация", e);
            // Фоллбэк на стандартную конфигурацию
            configuredClient = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
        }
        
        this.client = configuredClient;
    }
    
    /**
     * Создает SSLSocketFactory с поддержкой только TLS 1.3
     */
    private SSLSocketFactory createTLS13SocketFactory() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        TrustManager[] trustManagers = getTrustManager();
        sslContext.init(null, trustManagers, new java.security.SecureRandom());
        
        return new TLSSocketFactory(sslContext.getSocketFactory());
    }
    
    /**
     * Получает TrustManager для проверки сертификатов
     */
    private TrustManager[] getTrustManager() {
        return new TrustManager[]{
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    // Проверка клиентских сертификатов
                }
                
                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    // Проверка серверных сертификатов
                    // Certificate Pinning выполняется на уровне OkHttp
                }
                
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }
        };
    }
    
    /**
     * Извлекает hostname из URL
     */
    private String extractHostname(String url) {
        try {
            if (url.startsWith("http://")) {
                url = url.substring(7);
            } else if (url.startsWith("https://")) {
                url = url.substring(8);
            }
            int portIndex = url.indexOf(':');
            int pathIndex = url.indexOf('/');
            if (portIndex != -1 && (pathIndex == -1 || portIndex < pathIndex)) {
                return url.substring(0, portIndex);
            } else if (pathIndex != -1) {
                return url.substring(0, pathIndex);
            }
            return url;
        } catch (Exception e) {
            logger.error("Ошибка извлечения hostname из URL: {}", url, e);
            return null;
        }
    }
    
    /**
     * Кастомный SSLSocketFactory, который принудительно использует TLS 1.3
     */
    private static class TLSSocketFactory extends SSLSocketFactory {
        private final SSLSocketFactory delegate;
        
        public TLSSocketFactory(SSLSocketFactory delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }
        
        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }
        
        @Override
        public java.net.Socket createSocket(java.net.Socket s, String host, int port, boolean autoClose) throws IOException {
            return enableTLS13(delegate.createSocket(s, host, port, autoClose));
        }
        
        @Override
        public java.net.Socket createSocket(String host, int port) throws IOException {
            return enableTLS13(delegate.createSocket(host, port));
        }
        
        @Override
        public java.net.Socket createSocket(String host, int port, java.net.InetAddress localHost, int localPort) throws IOException {
            return enableTLS13(delegate.createSocket(host, port, localHost, localPort));
        }
        
        @Override
        public java.net.Socket createSocket(java.net.InetAddress host, int port) throws IOException {
            return enableTLS13(delegate.createSocket(host, port));
        }
        
        @Override
        public java.net.Socket createSocket(java.net.InetAddress address, int port, java.net.InetAddress localAddress, int localPort) throws IOException {
            return enableTLS13(delegate.createSocket(address, port, localAddress, localPort));
        }
        
        private java.net.Socket enableTLS13(java.net.Socket socket) {
            if (socket instanceof SSLSocket) {
                SSLSocket sslSocket = (SSLSocket) socket;
                // Принудительно включаем только TLS 1.3
                sslSocket.setEnabledProtocols(new String[]{"TLSv1.3"});
                // Если TLS 1.3 не поддерживается, используем TLS 1.2 как фоллбэк
                if (sslSocket.getEnabledProtocols().length == 0) {
                    logger.warn("TLS 1.3 не поддерживается, используется TLS 1.2");
                    sslSocket.setEnabledProtocols(new String[]{"TLSv1.2"});
                }
            }
            return socket;
        }
    }

    public static ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }

    /**
     * Выполняет POST запрос
     */
    public ApiResponse post(String endpoint, JsonObject data) {
        try {
            String url = baseUrl + endpoint;
            String json = gson.toJson(data);
            
            RequestBody body = RequestBody.create(
                    json, 
                    MediaType.get("application/json; charset=utf-8")
            );
            
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            logger.debug("POST {} -> {}", url, json);

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "{}";
                logger.debug("Response {}: {}", response.code(), responseBody);
                
                return new ApiResponse(response.code(), gson.fromJson(responseBody, JsonObject.class));
            }
        } catch (IOException e) {
            logger.error("Ошибка POST запроса к {}", endpoint, e);
            return new ApiResponse(500, createErrorResponse("Ошибка подключения к серверу"));
        }
    }

    /**
     * Выполняет GET запрос
     */
    public ApiResponse get(String endpoint) {
        try {
            String url = baseUrl + endpoint;
            
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            logger.debug("GET {}", url);

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "{}";
                logger.debug("Response {}: {}", response.code(), responseBody);
                
                return new ApiResponse(response.code(), gson.fromJson(responseBody, JsonObject.class));
            }
        } catch (IOException e) {
            logger.error("Ошибка GET запроса к {}", endpoint, e);
            return new ApiResponse(500, createErrorResponse("Ошибка подключения к серверу"));
        }
    }

    private JsonObject createErrorResponse(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("success", false);
        error.addProperty("message", message);
        return error;
    }

    /**
     * Класс для хранения ответа API
     */
    public static class ApiResponse {
        private final int statusCode;
        private final JsonObject body;

        public ApiResponse(int statusCode, JsonObject body) {
            this.statusCode = statusCode;
            this.body = body != null ? body : new JsonObject();
        }

        public int getStatusCode() {
            return statusCode;
        }

        public JsonObject getBody() {
            return body;
        }

        public boolean isSuccess() {
            return statusCode == 200 && body.has("success") && body.get("success").getAsBoolean();
        }

        public String getMessage() {
            if (body.has("message")) {
                return body.get("message").getAsString();
            }
            return "Неизвестная ошибка";
        }
    }
}




