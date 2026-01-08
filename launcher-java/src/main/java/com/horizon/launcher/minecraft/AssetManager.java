package com.horizon.launcher.minecraft;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.horizon.launcher.util.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Менеджер для загрузки ассетов и нативов Minecraft
 */
public class AssetManager {
    private static final Logger logger = LoggerFactory.getLogger(AssetManager.class);
    private final ConfigManager configManager;

    public AssetManager() {
        this.configManager = ConfigManager.getInstance();
    }

    /**
     * Загружает ассеты для версии
     */
    public void downloadAssets(String version) throws IOException {
        Path assetsDir = configManager.getGameDir().resolve("assets");
        Path versionJson = configManager.getGameDir()
                .resolve("versions")
                .resolve(version)
                .resolve(version + ".json");

        if (!Files.exists(versionJson)) {
            throw new IOException("Version JSON not found: " + versionJson);
        }

        JsonObject versionData = JsonParser.parseReader(
                Files.newBufferedReader(versionJson)).getAsJsonObject();
        
        JsonObject assetIndex = versionData.getAsJsonObject("assetIndex");
        String assetIndexUrl = assetIndex.get("url").getAsString();
        String assetIndexId = assetIndex.get("id").getAsString();

        logger.info("Загрузка индекса ассетов: {}", assetIndexId);

        // Загружаем индекс ассетов
        Path assetIndexFile = assetsDir.resolve("indexes").resolve(assetIndexId + ".json");
        Files.createDirectories(assetIndexFile.getParent());
        downloadFile(assetIndexUrl, assetIndexFile);

        // Парсим индекс и загружаем ассеты
        JsonObject assets = JsonParser.parseReader(
                Files.newBufferedReader(assetIndexFile)).getAsJsonObject()
                .getAsJsonObject("objects");

        String baseUrl = "https://resources.download.minecraft.net/";
        int total = assets.size();
        int downloaded = 0;

        for (String key : assets.keySet()) {
            JsonObject asset = assets.getAsJsonObject(key);
            String hash = asset.get("hash").getAsString();
            String hashPrefix = hash.substring(0, 2);
            String assetUrl = baseUrl + hashPrefix + "/" + hash;
            
            Path assetFile = assetsDir.resolve("objects").resolve(hashPrefix).resolve(hash);
            
            if (!Files.exists(assetFile)) {
                Files.createDirectories(assetFile.getParent());
                downloadFile(assetUrl, assetFile);
            }
            
            downloaded++;
            if (downloaded % 100 == 0) {
                logger.info("Загружено ассетов: {}/{}", downloaded, total);
            }
        }

        logger.info("Все ассеты загружены");
    }

    /**
     * Распаковывает нативные библиотеки
     */
    public void extractNatives(String version) throws IOException {
        Path versionJson = configManager.getGameDir()
                .resolve("versions")
                .resolve(version)
                .resolve(version + ".json");

        if (!Files.exists(versionJson)) {
            throw new IOException("Version JSON not found: " + versionJson);
        }

        JsonObject versionData = JsonParser.parseReader(
                Files.newBufferedReader(versionJson)).getAsJsonObject();
        
        Path nativesDir = configManager.getGameDir().resolve("natives");
        Files.createDirectories(nativesDir);

        var libraries = versionData.getAsJsonArray("libraries");
        String osName = getOSName();
        String osArch = System.getProperty("os.arch");

        logger.info("Распаковка нативов для {} {}", osName, osArch);

        for (var element : libraries) {
            JsonObject library = element.getAsJsonObject();
            
            // Проверяем, есть ли нативы для этой ОС
            if (library.has("natives")) {
                JsonObject natives = library.getAsJsonObject("natives");
                if (natives.has(osName)) {
                    String nativeClassifier = natives.get(osName).getAsString()
                            .replace("${arch}", getArchClassifier(osArch));
                    
                    JsonObject downloads = library.getAsJsonObject("downloads");
                    if (downloads.has("classifiers")) {
                        JsonObject classifiers = downloads.getAsJsonObject("classifiers");
                        if (classifiers.has(nativeClassifier)) {
                            JsonObject nativeInfo = classifiers.getAsJsonObject(nativeClassifier);
                            String nativeUrl = nativeInfo.get("url").getAsString();
                            String nativePath = nativeInfo.get("path").getAsString();
                            
                            Path nativeJar = configManager.getGameDir()
                                    .resolve("libraries")
                                    .resolve(nativePath);
                            
                            if (Files.exists(nativeJar)) {
                                extractJar(nativeJar, nativesDir);
                            } else {
                                // Загружаем если нет
                                Files.createDirectories(nativeJar.getParent());
                                downloadFile(nativeUrl, nativeJar);
                                extractJar(nativeJar, nativesDir);
                            }
                        }
                    }
                }
            }
        }

        logger.info("Нативы распакованы в: {}", nativesDir);
    }

    private void extractJar(Path jarFile, Path targetDir) throws IOException {
        logger.debug("Распаковка JAR: {}", jarFile);
        
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(jarFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    Path targetFile = targetDir.resolve(entry.getName());
                    Files.createDirectories(targetFile.getParent());
                    
                    try (FileOutputStream fos = new FileOutputStream(targetFile.toFile())) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private String getOSName() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return "windows";
        if (os.contains("mac")) return "osx";
        if (os.contains("nix") || os.contains("nux")) return "linux";
        return "unknown";
    }

    private String getArchClassifier(String arch) {
        if (arch.contains("64")) return "64";
        if (arch.contains("32")) return "32";
        return "64"; // default
    }

    private void downloadFile(String urlString, Path destination) throws IOException {
        logger.debug("Загрузка {} -> {}", urlString, destination);
        URI uri = URI.create(urlString);
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();

        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(destination.toFile())) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
}

