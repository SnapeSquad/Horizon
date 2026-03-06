package com.horizon.launcher.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Менеджер для получения Hardware ID (HWID) устройства
 * Генерирует SHA-256 хеш от идентификаторов железа
 */
public class HWIDManager {
    private static final Logger logger = LoggerFactory.getLogger(HWIDManager.class);
    private static HWIDManager instance;
    private String hwid;
    
    private HWIDManager() {
        this.hwid = generateHWID();
    }
    
    public static synchronized HWIDManager getInstance() {
        if (instance == null) {
            instance = new HWIDManager();
        }
        return instance;
    }
    
    /**
     * Сгенерировать HWID на основе идентификаторов железа
     */
    private String generateHWID() {
        StringBuilder hardwareInfo = new StringBuilder();
        
        // Получаем информацию о железе
        String osName = System.getProperty("os.name", "");
        String osVersion = System.getProperty("os.version", "");
        String osArch = System.getProperty("os.arch", "");
        String userHome = System.getProperty("user.home", "");
        String javaVersion = System.getProperty("java.version", "");
        String userName = System.getProperty("user.name", "");
        
        // Добавляем информацию о железе
        hardwareInfo.append(osName).append("|");
        hardwareInfo.append(osVersion).append("|");
        hardwareInfo.append(osArch).append("|");
        hardwareInfo.append(userHome).append("|");
        hardwareInfo.append(javaVersion).append("|");
        hardwareInfo.append(userName).append("|");
        
        // Пытаемся получить MAC адрес (если доступен)
        try {
            java.net.NetworkInterface network = java.net.NetworkInterface.getNetworkInterfaces().nextElement();
            byte[] mac = network.getHardwareAddress();
            if (mac != null) {
                StringBuilder macStr = new StringBuilder();
                for (byte b : mac) {
                    macStr.append(String.format("%02X", b));
                }
                hardwareInfo.append(macStr.toString());
            }
        } catch (Exception e) {
            logger.debug("Не удалось получить MAC адрес", e);
        }
        
        try {
            // Генерируем SHA-256 хеш
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(hardwareInfo.toString().getBytes());
            
            // Преобразуем в hex строку
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            logger.debug("HWID сгенерирован: {}", hexString.toString().substring(0, 16) + "...");
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("Ошибка при генерации HWID", e);
            // Fallback на простой хеш
            return String.valueOf(hardwareInfo.toString().hashCode());
        }
    }
    
    /**
     * Получить HWID
     */
    public String getHWID() {
        return hwid;
    }
    
    /**
     * Получить короткий HWID (первые 16 символов)
     */
    public String getShortHWID() {
        if (hwid != null && hwid.length() >= 16) {
            return hwid.substring(0, 16);
        }
        return hwid;
    }
}
