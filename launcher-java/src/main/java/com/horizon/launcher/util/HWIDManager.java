package com.horizon.launcher.util;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.OperatingSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Менеджер для получения уникального идентификатора железа (HWID)
 * 
 * Собирает: CPU ID + Motherboard Serial + Disk UUID
 * Фоллбэк: Если не удалось получить серийник материнки, использует MAC-адрес первой сетевой карты
 */
public class HWIDManager {
    private static final Logger logger = LoggerFactory.getLogger(HWIDManager.class);
    private static HWIDManager instance;
    private String cachedHWID;
    
    private HWIDManager() {
    }
    
    public static HWIDManager getInstance() {
        if (instance == null) {
            instance = new HWIDManager();
        }
        return instance;
    }
    
    /**
     * Получает уникальный HWID системы
     * Результат кэшируется после первого вызова
     */
    public String getHWID() {
        if (cachedHWID != null) {
            return cachedHWID;
        }
        
        try {
            SystemInfo systemInfo = new SystemInfo();
            HardwareAbstractionLayer hal = systemInfo.getHardware();
            OperatingSystem os = systemInfo.getOperatingSystem();
            
            StringBuilder hwidBuilder = new StringBuilder();
            
            // 1. CPU ID (Processor ID)
            String cpuId = getProcessorId(hal);
            if (cpuId != null && !cpuId.isEmpty()) {
                hwidBuilder.append(cpuId);
                logger.debug("CPU ID получен: {}", cpuId);
            } else {
                logger.warn("Не удалось получить CPU ID");
            }
            
            // 2. Motherboard Serial Number
            String motherboardSerial = getMotherboardSerial(hal);
            if (motherboardSerial != null && !motherboardSerial.isEmpty()) {
                hwidBuilder.append("|").append(motherboardSerial);
                logger.debug("Серийный номер материнской платы получен: {}", motherboardSerial);
            } else {
                // Фоллбэк: MAC-адрес первой сетевой карты
                String macAddress = getFirstMacAddress(hal);
                if (macAddress != null && !macAddress.isEmpty()) {
                    hwidBuilder.append("|").append(macAddress);
                    logger.debug("Использован MAC-адрес (фоллбэк): {}", macAddress);
                } else {
                    logger.warn("Не удалось получить ни серийник материнки, ни MAC-адрес");
                }
            }
            
            // 3. Disk UUID (серийный номер системного диска)
            String diskSerial = getDiskSerial(os);
            if (diskSerial != null && !diskSerial.isEmpty()) {
                hwidBuilder.append("|").append(diskSerial);
                logger.debug("UUID диска получен: {}", diskSerial);
            } else {
                logger.warn("Не удалось получить UUID диска");
            }
            
            // Если хотя бы один компонент получен, создаем хеш
            if (hwidBuilder.length() > 0) {
                String rawHWID = hwidBuilder.toString();
                cachedHWID = hashHWID(rawHWID);
                logger.info("HWID успешно сгенерирован (длина: {})", cachedHWID.length());
                return cachedHWID;
            } else {
                // Критический фоллбэк: используем имя пользователя и имя ПК
                logger.error("Не удалось получить ни один компонент HWID, используется фоллбэк");
                String fallback = System.getProperty("user.name") + "|" + 
                                 System.getProperty("user.home") + "|" +
                                 System.getenv("COMPUTERNAME");
                cachedHWID = hashHWID(fallback);
                return cachedHWID;
            }
            
        } catch (Exception e) {
            logger.error("Ошибка при получении HWID", e);
            // Критический фоллбэк
            String fallback = System.getProperty("user.name") + "|" + 
                             System.getProperty("user.home") + "|" +
                             System.getenv("COMPUTERNAME");
            cachedHWID = hashHWID(fallback);
            return cachedHWID;
        }
    }
    
    /**
     * Получает ID процессора
     */
    private String getProcessorId(HardwareAbstractionLayer hal) {
        try {
            CentralProcessor processor = hal.getProcessor();
            String processorId = processor.getProcessorIdentifier().getProcessorID();
            if (processorId != null && !processorId.trim().isEmpty() && 
                !processorId.equals("unknown") && !processorId.equals("null")) {
                return processorId.trim();
            }
        } catch (Exception e) {
            logger.debug("Ошибка при получении CPU ID: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Получает серийный номер материнской платы
     */
    private String getMotherboardSerial(HardwareAbstractionLayer hal) {
        try {
            String serial = hal.getComputerSystem().getBaseboard().getSerialNumber();
            if (serial != null && !serial.trim().isEmpty() && 
                !serial.equals("unknown") && !serial.equals("null") &&
                !serial.equals("Default string") && !serial.equals("To be filled by O.E.M.")) {
                return serial.trim();
            }
        } catch (Exception e) {
            logger.debug("Ошибка при получении серийника материнки: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Получает MAC-адрес первой активной сетевой карты (фоллбэк)
     */
    private String getFirstMacAddress(HardwareAbstractionLayer hal) {
        try {
            List<NetworkIF> networkIFs = hal.getNetworkIFs();
            for (NetworkIF netIF : networkIFs) {
                if (netIF != null && netIF.getMacaddr() != null) {
                    String mac = netIF.getMacaddr();
                    if (mac != null && !mac.trim().isEmpty() && 
                        !mac.equals("00:00:00:00:00:00")) {
                        return mac.trim();
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Ошибка при получении MAC-адреса: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Получает серийный номер системного диска
     */
    private String getDiskSerial(OperatingSystem os) {
        try {
            // Получаем системный диск (обычно C:)
            String systemDrive = System.getenv("SystemDrive");
            if (systemDrive == null) {
                systemDrive = "C:";
            }
            
            // OSHI может получить информацию о дисках через FileSystem
            var fileSystem = os.getFileSystem();
            var fileStores = fileSystem.getFileStores();
            
            for (var store : fileStores) {
                if (store.getMount().startsWith(systemDrive)) {
                    String uuid = store.getUUID();
                    if (uuid != null && !uuid.trim().isEmpty() && 
                        !uuid.equals("unknown") && !uuid.equals("null")) {
                        return uuid.trim();
                    }
                    
                    // Альтернатива: используем имя тома
                    String volume = store.getVolume();
                    if (volume != null && !volume.trim().isEmpty()) {
                        return volume.trim();
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Ошибка при получении UUID диска: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Хеширует сырой HWID в SHA-256 для безопасности
     */
    private String hashHWID(String rawHWID) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawHWID.getBytes(StandardCharsets.UTF_8));
            
            // Конвертируем в hex строку
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 не поддерживается", e);
            // Фоллбэк: просто возвращаем сырой HWID (небезопасно, но лучше чем ничего)
            return rawHWID;
        }
    }
    
    /**
     * Сбрасывает кэш (для тестирования)
     */
    public void clearCache() {
        cachedHWID = null;
    }
}

