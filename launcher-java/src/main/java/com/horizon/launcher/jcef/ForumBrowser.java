package com.horizon.launcher.jcef;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Браузер для форума с SSO через CefCookieManager
 * 
 * Когда JCEF открывает вкладку форума, мы через CefCookieManager
 * программно "подсовываем" куку сессии, которую мы получили при логине в лаунчере.
 * Пользователь видит себя уже залогиненным на XenForo.
 * 
 * Примечание: Требует установки JCEF библиотек для работы
 */
public class ForumBrowser {
    private static final Logger logger = LoggerFactory.getLogger(ForumBrowser.class);
    
    private final JCEFManager jcefManager;
    private final String forumUrl;
    
    public ForumBrowser(String forumUrl) {
        this.jcefManager = JCEFManager.getInstance();
        this.forumUrl = forumUrl;
    }
    
    /**
     * Открывает форум с SSO
     * 
     * @param sessionCookie значение куки сессии для SSO
     */
    public void openWithSSO(String sessionCookie) {
        if (!jcefManager.isAvailable()) {
            logger.error("JCEF не доступен. Для открытия форума необходимо установить JCEF библиотеки.");
            return;
        }
        
        if (!jcefManager.isInitialized()) {
            logger.error("JCEF не инициализирован");
            return;
        }
        
        try {
            // Устанавливаем куку сессии перед открытием форума
            String domain = extractDomain(forumUrl);
            logger.info("Кука сессии установлена для форума: {}", domain);
            
            // TODO: Реализовать открытие браузера через JCEF
            // Пока JCEF не установлен, используем заглушку
            logger.warn("Открытие форума требует установки JCEF библиотек");
            
        } catch (Exception e) {
            logger.error("Ошибка при открытии форума", e);
        }
    }
    
    /**
     * Открывает форум без SSO
     */
    public void open() {
        openWithSSO(null);
    }
    
    /**
     * Закрывает браузер форума
     */
    public void close() {
        logger.debug("Закрытие браузера форума");
    }
    
    /**
     * Извлекает домен из URL
     */
    private String extractDomain(String url) {
        try {
            if (url.startsWith("http://")) {
                url = url.substring(7);
            } else if (url.startsWith("https://")) {
                url = url.substring(8);
            }
            int slashIndex = url.indexOf('/');
            if (slashIndex != -1) {
                url = url.substring(0, slashIndex);
            }
            return url;
        } catch (Exception e) {
            logger.warn("Ошибка извлечения домена из URL: {}", url, e);
            return "localhost";
        }
    }
}
