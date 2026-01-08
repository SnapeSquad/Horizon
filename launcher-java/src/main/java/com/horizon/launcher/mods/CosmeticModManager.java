package com.horizon.launcher.mods;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Менеджер модов для отображения косметики в игре
 */
public class CosmeticModManager {
    private static final Logger logger = LoggerFactory.getLogger(CosmeticModManager.class);
    private final Path modsDirectory;
    
    public CosmeticModManager() {
        this.modsDirectory = Paths.get(System.getProperty("user.home"), ".horizon", "mods");
        try {
            Files.createDirectories(modsDirectory);
        } catch (IOException e) {
            logger.error("Ошибка создания директории модов", e);
        }
    }

    /**
     * Устанавливает мод для косметики
     */
    public boolean installMod(String modName, String version, byte[] modData) {
        try {
            Path modFile = modsDirectory.resolve(modName + "-" + version + ".jar");
            Files.write(modFile, modData);
            logger.info("Мод {} установлен", modName);
            return true;
        } catch (IOException e) {
            logger.error("Ошибка установки мода", e);
            return false;
        }
    }

    /**
     * Получает список установленных модов
     */
    public List<ModInfo> getInstalledMods() {
        List<ModInfo> mods = new ArrayList<>();
        try {
            File[] files = modsDirectory.toFile().listFiles((dir, name) -> name.endsWith(".jar"));
            if (files != null) {
                for (File file : files) {
                    ModInfo info = new ModInfo();
                    info.name = file.getName().replace(".jar", "");
                    info.path = file.getAbsolutePath();
                    info.size = file.length();
                    mods.add(info);
                }
            }
        } catch (Exception e) {
            logger.error("Ошибка получения списка модов", e);
        }
        return mods;
    }

    /**
     * Генерирует мод для косметики пользователя
     */
    public boolean generateCosmeticMod(String username, List<String> cosmeticIds) {
        try {
            // Создаем структуру мода
            JsonObject modJson = new JsonObject();
            modJson.addProperty("modid", "horizon_cosmetics");
            modJson.addProperty("name", "Horizon Cosmetics");
            modJson.addProperty("version", "1.0.0");
            
            JsonArray cosmeticsArray = new JsonArray();
            for (String cosmeticId : cosmeticIds) {
                cosmeticsArray.add(cosmeticId);
            }
            modJson.add("cosmetics", cosmeticsArray);
            
            // Сохраняем конфигурацию мода
            Path configPath = modsDirectory.resolve("cosmetics.json");
            Files.write(configPath, modJson.toString().getBytes());
            
            logger.info("Мод косметики сгенерирован для {}", username);
            return true;
        } catch (Exception e) {
            logger.error("Ошибка генерации мода", e);
            return false;
        }
    }

    public static class ModInfo {
        public String name;
        public String path;
        public long size;
    }
}

