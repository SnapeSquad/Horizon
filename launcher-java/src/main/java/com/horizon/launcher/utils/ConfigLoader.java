package com.horizon.launcher.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Загрузчик конфигурации из properties файлов
 */
public class ConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    private static final String CONFIG_FILE = "/config.properties";
    private static ConfigLoader instance;
    private Properties properties;
    
    private ConfigLoader() {
        this.properties = new Properties();
        loadConfig();
    }
    
    public static synchronized ConfigLoader getInstance() {
        if (instance == null) {
            instance = new ConfigLoader();
        }
        return instance;
    }
    
    private void loadConfig() {
        try (InputStream is = getClass().getResourceAsStream(CONFIG_FILE)) {
            if (is != null) {
                properties.load(is);
                logger.info("Конфигурация загружена из {}", CONFIG_FILE);
            } else {
                logger.warn("Файл конфигурации {} не найден, используются значения по умолчанию", CONFIG_FILE);
                loadDefaults();
            }
        } catch (IOException e) {
            logger.error("Ошибка при загрузке конфигурации", e);
            loadDefaults();
        }
    }
    
    private void loadDefaults() {
        properties.setProperty("api.server.url", "http://localhost:3000");
        properties.setProperty("telegram.bot.url", "https://t.me/your_bot_username");
        properties.setProperty("api.timeout.connect", "10");
        properties.setProperty("api.timeout.read", "10");
        properties.setProperty("api.timeout.write", "10");
        properties.setProperty("minecraft.default.memory", "4096");
        properties.setProperty("minecraft.default.version", "1.21");
    }
    
    public String get(String key) {
        // Сначала проверяем system properties, затем файл конфигурации
        // Преобразуем точки в нижние подчеркивания для system properties
        String systemKey = "horizon." + key.replace(".", "_");
        String systemProp = System.getProperty(systemKey);
        if (systemProp != null && !systemProp.isEmpty()) {
            return systemProp;
        }
        return properties.getProperty(key);
    }
    
    public String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }
    
    public int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                logger.warn("Не удалось распарсить значение {} для ключа {}", value, key);
            }
        }
        return defaultValue;
    }
}
