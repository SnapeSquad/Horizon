package com.horizon.launcher.util;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Менеджер конфигурации лаунчера
 */
public class ConfigManager {
    private static ConfigManager instance;
    private Properties config;
    private File configFile;
    private Path launcherDir;

    private ConfigManager() {
        config = new Properties();
        launcherDir = Paths.get(System.getProperty("user.home"), ".horizon-launcher");
        
        try {
            Files.createDirectories(launcherDir);
        } catch (IOException e) {
            System.err.println("Не удалось создать директорию лаунчера: " + e.getMessage());
        }
        
        configFile = new File(launcherDir.toFile(), "config.properties");
    }

    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    public void initialize() {
        loadConfig();
    }

    public void loadConfig() {
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                config.load(reader);
            } catch (IOException e) {
                System.err.println("Ошибка загрузки конфигурации: " + e.getMessage());
            }
        }
    }

    public void saveConfig() {
        try (FileWriter writer = new FileWriter(configFile)) {
            config.store(writer, "Horizon Launcher Configuration");
        } catch (IOException e) {
            System.err.println("Ошибка сохранения конфигурации: " + e.getMessage());
        }
    }

    public String get(String key, String defaultValue) {
        return config.getProperty(key, defaultValue);
    }

    public void set(String key, String value) {
        config.setProperty(key, value);
        saveConfig();
    }

    public Path getLauncherDir() {
        return launcherDir;
    }

    public Path getGameDir() {
        return launcherDir.resolve("game");
    }

    public String getApiUrl() {
        return get("api.url", "http://localhost:3000");
    }

    public void setApiUrl(String url) {
        set("api.url", url);
    }

    public int getRamInGB() {
        return Integer.parseInt(get("ram.gb", "4"));
    }

    public void setRamInGB(int ram) {
        set("ram.gb", String.valueOf(ram));
    }
    
    /**
     * Получает список закрепленных сертификатов для hostname (Certificate Pinning)
     * Формат: sha256/XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
     */
    public List<String> getPinnedCertificates(String hostname) {
        String pinned = get("cert.pin." + hostname, "");
        if (pinned.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(pinned.split(","));
    }
    
    /**
     * Устанавливает закрепленные сертификаты для hostname
     */
    public void setPinnedCertificates(String hostname, List<String> certificates) {
        set("cert.pin." + hostname, String.join(",", certificates));
    }
}






