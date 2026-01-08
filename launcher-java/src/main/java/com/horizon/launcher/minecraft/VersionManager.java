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

/**
 * Менеджер версий Minecraft - загружает и управляет версиями игры
 */
public class VersionManager {
    private static final Logger logger = LoggerFactory.getLogger(VersionManager.class);
    private static final String VERSION_MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    private final ConfigManager configManager;

    public VersionManager() {
        this.configManager = ConfigManager.getInstance();
    }

    /**
     * Проверяет и загружает версию Minecraft, если её нет
     */
    public boolean ensureVersion(String version) throws IOException {
        Path versionsDir = configManager.getGameDir().resolve("versions").resolve(version);
        Path versionJson = versionsDir.resolve(version + ".json");
        Path versionJar = versionsDir.resolve(version + ".jar");

        // Если версия уже загружена
        if (Files.exists(versionJson) && Files.exists(versionJar)) {
            logger.info("Версия {} уже загружена", version);
            return true;
        }

        logger.info("Загрузка версии {}...", version);
        Files.createDirectories(versionsDir);

        // Получаем информацию о версии из манифеста
        String versionUrl = getVersionUrl(version);
        if (versionUrl == null) {
            throw new IOException("Версия " + version + " не найдена в манифесте");
        }

        // Загружаем JSON версии
        downloadFile(versionUrl, versionJson);
        logger.info("JSON версии загружен: {}", versionJson);

        // Парсим JSON и загружаем клиент
        JsonObject versionData = JsonParser.parseReader(Files.newBufferedReader(versionJson)).getAsJsonObject();
        JsonObject downloads = versionData.getAsJsonObject("downloads");
        JsonObject client = downloads.getAsJsonObject("client");
        String clientUrl = client.get("url").getAsString();

        downloadFile(clientUrl, versionJar);
        logger.info("Клиент загружен: {}", versionJar);

        // Загружаем библиотеки
        downloadLibraries(versionData);

        return true;
    }

    private String getVersionUrl(String version) throws IOException {
        URI uri = URI.create(VERSION_MANIFEST_URL);
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setRequestMethod("GET");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            JsonObject manifest = JsonParser.parseReader(reader).getAsJsonObject();
            var versions = manifest.getAsJsonArray("versions");

            for (var element : versions) {
                JsonObject versionObj = element.getAsJsonObject();
                if (versionObj.get("id").getAsString().equals(version)) {
                    return versionObj.get("url").getAsString();
                }
            }
        }

        return null;
    }

    private void downloadFile(String urlString, Path destination) throws IOException {
        logger.info("Загрузка {} -> {}", urlString, destination);
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

    private void downloadLibraries(JsonObject versionData) throws IOException {
        Path librariesDir = configManager.getGameDir().resolve("libraries");
        Files.createDirectories(librariesDir);

        var libraries = versionData.getAsJsonArray("libraries");
        logger.info("Загрузка {} библиотек...", libraries.size());

        for (var element : libraries) {
            JsonObject library = element.getAsJsonObject();
            JsonObject downloads = library.getAsJsonObject("downloads");
            
            if (downloads.has("artifact")) {
                JsonObject artifact = downloads.getAsJsonObject("artifact");
                String path = artifact.get("path").getAsString();
                String url = artifact.get("url").getAsString();

                Path libraryFile = librariesDir.resolve(path);
                if (!Files.exists(libraryFile)) {
                    Files.createDirectories(libraryFile.getParent());
                    downloadFile(url, libraryFile);
                }
            }
        }
    }

    public Path getVersionJar(String version) {
        return configManager.getGameDir()
                .resolve("versions")
                .resolve(version)
                .resolve(version + ".json");
    }
}

