package com.horizon.launcher.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Менеджер конфигурации лаунчера
 */
public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static ConfigManager instance;
    
    private final Path launcherDir;

    private ConfigManager() {
        // Используем домашнюю директорию пользователя для хранения данных лаунчера
        String userHome = System.getProperty("user.home");
        this.launcherDir = Paths.get(userHome, ".horizon");
        
        // Создаем директорию, если её нет
        try {
            java.nio.file.Files.createDirectories(launcherDir);
            logger.info("Директория лаунчера: {}", launcherDir);
        } catch (java.io.IOException e) {
            logger.error("Не удалось создать директорию лаунчера", e);
        }
    }

    /**
     * Получить единственный экземпляр ConfigManager
     */
    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    /**
     * Получить директорию лаунчера
     */
    public Path getLauncherDir() {
        return launcherDir;
    }

    /**
     * Получить директорию для Minecraft файлов
     */
    public Path getMinecraftDir() {
        return launcherDir.resolve("minecraft");
    }

    /**
     * Получить директорию для модов
     */
    public Path getModsDir() {
        return launcherDir.resolve("mods");
    }

    /**
     * Получить директорию для скинов
     */
    public Path getSkinsDir() {
        return launcherDir.resolve("skins");
    }
}
