package com.horizon.launcher.updater;

import com.horizon.launcher.util.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.util.Properties;

/**
 * Система автообновления лаунчера
 * 
 * Процесс:
 * 1. Launcher.exe запускает Updater.jar
 * 2. Launcher.exe закрывается (освобождает файл)
 * 3. Updater.jar заменяет Launcher.exe на новую версию
 * 4. Updater.jar запускает новый Launcher.exe и закрывается сам
 */
public class AutoUpdater {
    private static final Logger logger = LoggerFactory.getLogger(AutoUpdater.class);
    private static AutoUpdater instance;
    
    private final String currentVersion;
    private final String updateCheckUrl;
    private final HttpClient httpClient;
    
    private AutoUpdater() {
        this.currentVersion = getCurrentVersion();
        this.updateCheckUrl = ConfigManager.getInstance().get("updater.url", 
                "https://api.horizon.example.com/launcher/version");
        this.httpClient = HttpClient.newHttpClient();
    }
    
    public static AutoUpdater getInstance() {
        if (instance == null) {
            instance = new AutoUpdater();
        }
        return instance;
    }
    
    /**
     * Проверяет наличие обновлений
     */
    public UpdateInfo checkForUpdates() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(updateCheckUrl))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                // Парсим JSON ответ
                com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(
                        response.body()).getAsJsonObject();
                
                String latestVersion = json.get("version").getAsString();
                String downloadUrl = json.get("downloadUrl").getAsString();
                String changelog = json.has("changelog") ? 
                        json.get("changelog").getAsString() : "";
                
                boolean hasUpdate = !latestVersion.equals(currentVersion);
                
                return new UpdateInfo(hasUpdate, latestVersion, downloadUrl, changelog);
            }
        } catch (Exception e) {
            logger.error("Ошибка при проверке обновлений", e);
        }
        
        return new UpdateInfo(false, currentVersion, null, null);
    }
    
    /**
     * Запускает процесс обновления
     */
    public void startUpdate(UpdateInfo updateInfo) {
        if (!updateInfo.hasUpdate()) {
            logger.warn("Попытка обновления без доступных обновлений");
            return;
        }
        
        try {
            logger.info("Начинаем процесс обновления до версии {}", updateInfo.getLatestVersion());
            
            // 1. Загружаем новый лаунчер
            Path newLauncherPath = downloadNewVersion(updateInfo.getDownloadUrl());
            
            // 2. Запускаем Updater.jar
            launchUpdater(newLauncherPath);
            
            // 3. Закрываем текущий лаунчер
            logger.info("Закрываем текущий лаунчер для обновления...");
            System.exit(0);
            
        } catch (Exception e) {
            logger.error("Ошибка при запуске обновления", e);
        }
    }
    
    /**
     * Загружает новую версию лаунчера
     */
    private Path downloadNewVersion(String downloadUrl) throws IOException {
        logger.info("Загрузка новой версии из {}", downloadUrl);
        
        // Валидация URL
        com.horizon.launcher.util.ValidationUtils.ValidationResult urlValidation = 
                com.horizon.launcher.util.ValidationUtils.validateUrl(downloadUrl);
        if (!urlValidation.isValid()) {
            throw new IOException("Некорректный URL для загрузки: " + urlValidation.getMessage());
        }
        
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
        Path newLauncherPath = tempDir.resolve("horizon-launcher-new.exe");
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .GET()
                .build();
        
        try {
            HttpResponse<InputStream> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofInputStream());
            
            if (response.statusCode() == 200) {
                try (InputStream inputStream = response.body();
                     FileOutputStream outputStream = new FileOutputStream(newLauncherPath.toFile())) {
                    
                    byte[] buffer = new byte[8192];
                    long totalBytes = 0;
                    int bytesRead;
                    
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        totalBytes += bytesRead;
                        
                        // Логируем прогресс каждые 10MB
                        if (totalBytes % (10 * 1024 * 1024) == 0) {
                            logger.info("Загружено: {} MB", totalBytes / (1024 * 1024));
                        }
                    }
                    
                    logger.info("Загрузка завершена. Размер: {} MB", totalBytes / (1024 * 1024));
                }
            } else {
                throw new IOException("Ошибка загрузки: HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            throw new IOException("Ошибка при загрузке новой версии", e);
        }
        
        return newLauncherPath;
    }
    
    /**
     * Запускает Updater.jar для замены файла
     */
    private void launchUpdater(Path newLauncherPath) throws IOException {
        // Находим путь к текущему исполняемому файлу
        String currentExecutable = getCurrentExecutablePath();
        if (currentExecutable == null) {
            throw new IOException("Не удалось определить путь к текущему исполняемому файлу");
        }
        
        // Находим Updater.jar (должен быть рядом с лаунчером)
        Path launcherDir = Paths.get(currentExecutable).getParent();
        Path updaterJar = launcherDir.resolve("Updater.jar");
        
        if (!Files.exists(updaterJar)) {
            throw new IOException("Updater.jar не найден в " + launcherDir);
        }
        
        // Запускаем Updater.jar с параметрами:
        // java -jar Updater.jar <текущий_лаунчер> <новый_лаунчер>
        ProcessBuilder processBuilder = new ProcessBuilder(
                "java", "-jar", updaterJar.toString(),
                currentExecutable,
                newLauncherPath.toString()
        );
        
        processBuilder.start();
        logger.info("Updater.jar запущен");
    }
    
    /**
     * Получает путь к текущему исполняемому файлу
     */
    private String getCurrentExecutablePath() {
        // В Java сложно получить путь к .exe файлу, если мы запущены из JAR
        // Используем различные способы
        
        // Способ 1: Если запущены из JAR, используем путь к JAR
        String jarPath = getClass().getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath();
        
        if (jarPath != null && !jarPath.isEmpty()) {
            try {
                // Декодируем URL-encoded путь
                String decodedPath = java.net.URLDecoder.decode(jarPath, "UTF-8");
                // Убираем префикс file:/
                if (decodedPath.startsWith("file:/")) {
                    decodedPath = decodedPath.substring(6);
                }
                // Убираем ведущий слеш на Windows
                if (decodedPath.startsWith("/") && decodedPath.length() > 1 && 
                    decodedPath.charAt(2) == ':') {
                    decodedPath = decodedPath.substring(1);
                }
                return decodedPath;
            } catch (Exception e) {
                logger.debug("Ошибка декодирования пути JAR", e);
            }
        }
        
        // Способ 2: Используем системное свойство
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            // Это не идеально, но может помочь
        }
        
        // Способ 3: Используем рабочую директорию
        return System.getProperty("user.dir") + File.separator + "HorizonLauncher.exe";
    }
    
    /**
     * Получает текущую версию лаунчера
     */
    private String getCurrentVersion() {
        try {
            // Читаем версию из properties файла
            InputStream versionStream = getClass().getResourceAsStream("/version.properties");
            if (versionStream != null) {
                Properties props = new Properties();
                props.load(versionStream);
                return props.getProperty("version", "1.0.0");
            }
        } catch (Exception e) {
            logger.debug("Не удалось загрузить версию из properties", e);
        }
        
        // Фоллбэк: версия из конфига
        return ConfigManager.getInstance().get("launcher.version", "1.0.0");
    }
    
    /**
     * Класс для хранения информации об обновлении
     */
    public static class UpdateInfo {
        private final boolean hasUpdate;
        private final String latestVersion;
        private final String downloadUrl;
        private final String changelog;
        
        public UpdateInfo(boolean hasUpdate, String latestVersion, String downloadUrl, String changelog) {
            this.hasUpdate = hasUpdate;
            this.latestVersion = latestVersion;
            this.downloadUrl = downloadUrl;
            this.changelog = changelog;
        }
        
        public boolean hasUpdate() {
            return hasUpdate;
        }
        
        public String getLatestVersion() {
            return latestVersion;
        }
        
        public String getDownloadUrl() {
            return downloadUrl;
        }
        
        public String getChangelog() {
            return changelog;
        }
    }
}

