package com.horizon.launcher.utils;

import com.horizon.launcher.util.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Менеджер сессий для хранения зашифрованных JWT токенов и данных пользователя
 * Использует HWID как часть ключа шифрования для защиты данных на конкретном ПК
 */
public class SessionManager {
    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);
    private static SessionManager instance;
    
    private static final String SESSION_FILE = "session.properties";
    
    private final Path sessionFilePath;
    private Properties sessionProperties;
    private final HWIDManager hwidManager;
    
    private SessionManager() {
        this.hwidManager = HWIDManager.getInstance();
        this.sessionProperties = new Properties();
        Path launcherDir = ConfigManager.getInstance().getLauncherDir();
        this.sessionFilePath = launcherDir.resolve(SESSION_FILE);
        loadSession();
    }
    
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    /**
     * Загрузить сессию из файла
     */
    private void loadSession() {
        try {
            if (Files.exists(sessionFilePath)) {
                try (FileInputStream fis = new FileInputStream(sessionFilePath.toFile())) {
                    sessionProperties.load(fis);
                }
                logger.info("Сессия загружена из {}", sessionFilePath);
            } else {
                logger.debug("Файл сессии не найден: {}", sessionFilePath);
            }
        } catch (IOException e) {
            logger.warn("Ошибка при загрузке сессии из файла {}", sessionFilePath, e);
        }
    }
    
    /**
     * Сохранить сессию в файл
     */
    private void saveSession() {
        try {
            Path sessionDir = sessionFilePath.getParent();
            if (sessionDir != null && !Files.exists(sessionDir)) {
                Files.createDirectories(sessionDir);
            }
            
            try (FileOutputStream fos = new FileOutputStream(sessionFilePath.toFile())) {
                sessionProperties.store(fos, "Horizon Launcher Session Data");
            }
            logger.debug("Сессия сохранена в {}", sessionFilePath);
        } catch (IOException e) {
            logger.error("Ошибка при сохранении сессии в файл {}", sessionFilePath, e);
        }
    }
    
    /**
     * Сохранить зашифрованный JWT токен
     */
    public void setToken(String token) {
        if (token != null && !token.isEmpty()) {
            try {
                // Шифруем токен с использованием HWID
                String encryptedToken = CryptoHelper.encrypt(token, hwidManager.getHWID());
                sessionProperties.setProperty("token", encryptedToken);
                saveSession();
                logger.debug("Токен зашифрован и сохранен");
            } catch (Exception e) {
                logger.error("Ошибка при шифровании токена", e);
                // Fallback: сохраняем без шифрования (для совместимости)
                sessionProperties.setProperty("token", token);
                saveSession();
            }
        }
    }
    
    /**
     * Получить расшифрованный JWT токен
     */
    public String getToken() {
        String encryptedToken = sessionProperties.getProperty("token", null);
        if (encryptedToken == null || encryptedToken.isEmpty()) {
            return null;
        }
        
        try {
            // Пытаемся расшифровать токен
            return CryptoHelper.decrypt(encryptedToken, hwidManager.getHWID());
        } catch (Exception e) {
            logger.warn("Ошибка при расшифровке токена, возможно старый формат", e);
            // Fallback: возвращаем как есть (для совместимости со старыми версиями)
            return encryptedToken;
        }
    }
    
    /**
     * Сохранить имя пользователя
     */
    public void setUsername(String username) {
        if (username != null && !username.isEmpty()) {
            sessionProperties.setProperty("username", username);
            saveSession();
        }
    }
    
    /**
     * Получить имя пользователя
     */
    public String getUsername() {
        return sessionProperties.getProperty("username", null);
    }
    
    /**
     * Проверить, авторизован ли пользователь
     */
    public boolean isAuthenticated() {
        String token = getToken();
        return token != null && !token.isEmpty();
    }
    
    /**
     * Очистить сессию (выход)
     */
    public void clearSession() {
        sessionProperties.clear();
        try {
            if (Files.exists(sessionFilePath)) {
                Files.delete(sessionFilePath);
            }
            logger.info("Сессия очищена");
        } catch (IOException e) {
            logger.warn("Ошибка при удалении файла сессии {}", sessionFilePath, e);
        }
    }
}
