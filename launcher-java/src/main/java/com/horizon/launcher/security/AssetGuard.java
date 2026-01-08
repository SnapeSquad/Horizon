package com.horizon.launcher.security;

import com.horizon.launcher.util.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Asset Guard - система защиты файлов через хеширование
 * 
 * При выборе сервера лаунчер получает от API список файлов и их SHA-256 хешей.
 * Лаунчер сканирует папку /mods/. Если файла нет в списке — удаляет.
 * Если хеш не совпадает — перекачивает.
 * 
 * Использует многопоточность для быстрой проверки хешей больших файлов.
 */
public class AssetGuard {
    private static final Logger logger = LoggerFactory.getLogger(AssetGuard.class);
    private static AssetGuard instance;
    
    private final Path modsDirectory;
    private final ExecutorService executorService;
    
    private AssetGuard() {
        this.modsDirectory = ConfigManager.getInstance().getGameDir().resolve("mods");
        // Создаем пул потоков для многопоточной проверки хешей
        // Количество потоков = количество ядер процессора
        int threadCount = Runtime.getRuntime().availableProcessors();
        this.executorService = Executors.newFixedThreadPool(threadCount);
        
        // Создаем директорию mods, если её нет
        try {
            Files.createDirectories(modsDirectory);
        } catch (IOException e) {
            logger.error("Не удалось создать директорию mods", e);
        }
    }
    
    public static AssetGuard getInstance() {
        if (instance == null) {
            instance = new AssetGuard();
        }
        return instance;
    }
    
    /**
     * Проверяет и синхронизирует файлы с сервером
     * 
     * @param serverFileList Map<имя_файла, SHA-256_хеш> - список файлов с сервера
     * @return AssetGuardResult - результат проверки
     */
    public AssetGuardResult verifyAndSync(Map<String, String> serverFileList) {
        logger.info("Начало проверки файлов в {}", modsDirectory);
        
        List<String> filesToDownload = new ArrayList<>();
        List<String> filesToDelete = new ArrayList<>();
        List<String> validFiles = new ArrayList<>();
        
        // Получаем список локальных файлов
        Map<String, File> localFiles = getLocalFiles();
        
        // Проверяем каждый файл из списка сервера
        List<Future<FileCheckResult>> futures = new ArrayList<>();
        
        for (Map.Entry<String, String> serverFile : serverFileList.entrySet()) {
            String fileName = serverFile.getKey();
            String expectedHash = serverFile.getValue();
            
            Future<FileCheckResult> future = executorService.submit(() -> {
                return checkFile(fileName, expectedHash, localFiles.get(fileName));
            });
            futures.add(future);
        }
        
        // Собираем результаты
        for (Future<FileCheckResult> future : futures) {
            try {
                FileCheckResult result = future.get(30, TimeUnit.SECONDS);
                if (result.needsDownload) {
                    filesToDownload.add(result.fileName);
                } else if (result.isValid) {
                    validFiles.add(result.fileName);
                }
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                logger.error("Ошибка при проверке файла", e);
            }
        }
        
        // Проверяем файлы, которых нет в списке сервера (удаляем)
        for (String localFileName : localFiles.keySet()) {
            if (!serverFileList.containsKey(localFileName)) {
                filesToDelete.add(localFileName);
            }
        }
        
        // Удаляем лишние файлы
        for (String fileName : filesToDelete) {
            deleteFile(fileName);
        }
        
        logger.info("Проверка завершена. Валидных: {}, к загрузке: {}, удалено: {}", 
                validFiles.size(), filesToDownload.size(), filesToDelete.size());
        
        return new AssetGuardResult(validFiles, filesToDownload, filesToDelete);
    }
    
    /**
     * Проверяет один файл
     */
    private FileCheckResult checkFile(String fileName, String expectedHash, File localFile) {
        if (localFile == null || !localFile.exists()) {
            logger.debug("Файл {} отсутствует, требуется загрузка", fileName);
            return new FileCheckResult(fileName, false, true);
        }
        
        try {
            String actualHash = calculateSHA256(localFile);
            if (actualHash.equalsIgnoreCase(expectedHash)) {
                logger.debug("Файл {} валиден", fileName);
                return new FileCheckResult(fileName, true, false);
            } else {
                logger.warn("Хеш файла {} не совпадает. Ожидается: {}, получено: {}", 
                        fileName, expectedHash, actualHash);
                return new FileCheckResult(fileName, false, true);
            }
        } catch (Exception e) {
            logger.error("Ошибка при проверке файла {}", fileName, e);
            return new FileCheckResult(fileName, false, true);
        }
    }
    
    /**
     * Вычисляет SHA-256 хеш файла
     */
    private String calculateSHA256(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        
        byte[] hash = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    /**
     * Получает список локальных файлов в директории mods
     */
    private Map<String, File> getLocalFiles() {
        Map<String, File> files = new HashMap<>();
        
        if (!Files.exists(modsDirectory)) {
            return files;
        }
        
        File[] fileArray = modsDirectory.toFile().listFiles();
        if (fileArray != null) {
            for (File file : fileArray) {
                if (file.isFile()) {
                    files.put(file.getName(), file);
                }
            }
        }
        
        return files;
    }
    
    /**
     * Удаляет файл
     */
    private void deleteFile(String fileName) {
        try {
            Path filePath = modsDirectory.resolve(fileName);
            Files.deleteIfExists(filePath);
            logger.info("Удален файл: {}", fileName);
        } catch (IOException e) {
            logger.error("Ошибка при удалении файла {}", fileName, e);
        }
    }
    
    /**
     * Результат проверки одного файла
     */
    private static class FileCheckResult {
        final String fileName;
        final boolean isValid;
        final boolean needsDownload;
        
        FileCheckResult(String fileName, boolean isValid, boolean needsDownload) {
            this.fileName = fileName;
            this.isValid = isValid;
            this.needsDownload = needsDownload;
        }
    }
    
    /**
     * Результат проверки всех файлов
     */
    public static class AssetGuardResult {
        private final List<String> validFiles;
        private final List<String> filesToDownload;
        private final List<String> deletedFiles;
        
        public AssetGuardResult(List<String> validFiles, List<String> filesToDownload, List<String> deletedFiles) {
            this.validFiles = validFiles;
            this.filesToDownload = filesToDownload;
            this.deletedFiles = deletedFiles;
        }
        
        public List<String> getValidFiles() {
            return validFiles;
        }
        
        public List<String> getFilesToDownload() {
            return filesToDownload;
        }
        
        public List<String> getDeletedFiles() {
            return deletedFiles;
        }
        
        public boolean hasChanges() {
            return !filesToDownload.isEmpty() || !deletedFiles.isEmpty();
        }
    }
    
    /**
     * Закрывает пул потоков (вызывать при завершении работы лаунчера)
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

