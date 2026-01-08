package com.horizon.launcher.ui.i18n;

import com.horizon.launcher.util.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Менеджер локализации
 * 
 * Поддерживает языки: RU (Русский), EN (English), TT (Татарский)
 * Все строки хранятся в .properties файлах
 * При смене языка интерфейс перерисовывается мгновенно без перезагрузки
 */
public class LocalizationManager {
    private static final Logger logger = LoggerFactory.getLogger(LocalizationManager.class);
    private static LocalizationManager instance;
    
    private Locale currentLocale;
    private ResourceBundle resourceBundle;
    private final List<LocalizationChangeListener> listeners = new ArrayList<>();
    
    public enum Language {
        RU("ru", "Русский"),
        EN("en", "English"),
        TT("tt", "Татарча");
        
        private final String code;
        private final String displayName;
        
        Language(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public static Language fromCode(String code) {
            for (Language lang : values()) {
                if (lang.code.equals(code)) {
                    return lang;
                }
            }
            return EN; // По умолчанию
        }
    }
    
    private LocalizationManager() {
        // Загружаем сохраненный язык из конфига
        String savedLanguage = ConfigManager.getInstance().get("ui.language", "ru");
        setLanguage(Language.fromCode(savedLanguage));
    }
    
    public static LocalizationManager getInstance() {
        if (instance == null) {
            instance = new LocalizationManager();
        }
        return instance;
    }
    
    /**
     * Устанавливает язык интерфейса
     */
    public void setLanguage(Language language) {
        try {
            this.currentLocale = Locale.forLanguageTag(language.getCode());
            this.resourceBundle = ResourceBundle.getBundle("i18n.messages", currentLocale, 
                    new UTF8Control());
            
            // Сохраняем выбор в конфиг
            ConfigManager.getInstance().set("ui.language", language.getCode());
            
            // Уведомляем всех слушателей об изменении языка
            notifyListeners();
            
            logger.info("Язык интерфейса изменен на: {}", language.getDisplayName());
        } catch (Exception e) {
            logger.error("Ошибка при загрузке локализации для языка: {}", language, e);
            // Фоллбэк на английский
            try {
                this.currentLocale = Locale.ENGLISH;
                this.resourceBundle = ResourceBundle.getBundle("i18n.messages", currentLocale, 
                        new UTF8Control());
            } catch (Exception ex) {
                logger.error("Критическая ошибка: не удалось загрузить даже английскую локализацию", ex);
            }
        }
    }
    
    /**
     * Получает локализованную строку по ключу
     */
    public String get(String key) {
        if (resourceBundle == null) {
            return key; // Возвращаем ключ, если локализация не загружена
        }
        
        try {
            return resourceBundle.getString(key);
        } catch (MissingResourceException e) {
            logger.warn("Отсутствует локализация для ключа: {}", key);
            return key; // Возвращаем ключ, если строка не найдена
        }
    }
    
    /**
     * Получает локализованную строку с параметрами
     */
    public String get(String key, Object... args) {
        String template = get(key);
        return String.format(template, args);
    }
    
    /**
     * Получает текущий язык
     */
    public Language getCurrentLanguage() {
        return Language.fromCode(currentLocale.getLanguage());
    }
    
    /**
     * Добавляет слушателя изменений локализации
     */
    public void addListener(LocalizationChangeListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Удаляет слушателя
     */
    public void removeListener(LocalizationChangeListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Уведомляет всех слушателей об изменении языка
     */
    private void notifyListeners() {
        for (LocalizationChangeListener listener : listeners) {
            try {
                listener.onLanguageChanged();
            } catch (Exception e) {
                logger.error("Ошибка при уведомлении слушателя об изменении языка", e);
            }
        }
    }
    
    /**
     * Интерфейс для слушателей изменений локализации
     */
    public interface LocalizationChangeListener {
        void onLanguageChanged();
    }
    
    /**
     * Кастомный ResourceBundle.Control для поддержки UTF-8 в .properties файлах
     */
    private static class UTF8Control extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format,
                                       ClassLoader loader, boolean reload)
                throws IllegalAccessException, InstantiationException, IOException {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            
            try (InputStream stream = loader.getResourceAsStream(resourceName)) {
                if (stream != null) {
                    try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                        return new PropertyResourceBundle(reader);
                    }
                }
            }
            return null;
        }
    }
}

