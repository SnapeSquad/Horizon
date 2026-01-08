package com.horizon.launcher.ui.mvc;

import com.horizon.launcher.ui.i18n.LocalizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Базовый контроллер для MVC архитектуры
 */
public abstract class BaseController {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected LocalizationManager i18n;
    
    public BaseController() {
        this.i18n = LocalizationManager.getInstance();
        // Подписываемся на изменения локализации
        i18n.addListener(this::onLanguageChanged);
    }
    
    /**
     * Вызывается при изменении языка
     * Переопределяется в дочерних классах для обновления UI
     */
    protected void onLanguageChanged() {
        updateLocalizedStrings();
    }
    
    /**
     * Обновляет локализованные строки в UI
     * Должен быть переопределен в дочерних классах
     */
    protected abstract void updateLocalizedStrings();
    
    /**
     * Получает локализованную строку
     */
    protected String t(String key) {
        return i18n.get(key);
    }
    
    /**
     * Получает локализованную строку с параметрами
     */
    protected String t(String key, Object... args) {
        return i18n.get(key, args);
    }
    
    /**
     * Инициализация контроллера
     */
    public void initialize() {
        updateLocalizedStrings();
    }
}

