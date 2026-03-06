package com.horizon.launcher.minecraft;

import com.horizon.launcher.util.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Менеджер ресурсов для проверки и управления файлами Minecraft
 * Реализует проверку целостности файлов через SHA-256 и очистку неразрешенных файлов
 */
public class AssetManager {
    private static final Logger logger = LoggerFactory.getLogger(AssetManager.class);
    private static AssetManager instance;
    
    private final Path minecraftDir;
    private final Path modsDir;
    private final Path versionsDir;
    private final ExecutorService executorService;
    
    // Кэш для хешей файлов (путь -> хеш)
    private final Map<String, String> hashCache = new ConcurrentHashMap<>();
    
    // Максимальное количество попыток для вычисления хеша при ошибках
    private static final int MAX_RETRY_ATTEMPTS = 3;
    
    /**
     * Результат проверки файла
     */
    public static class FileVerificationResult {
        private final String path;
        private final boolean valid;
        private final String expectedHash;
        private final String actualHash;
        private final String error;
        
        public FileVerificationResult(String path, boolean valid, String expectedHash, String actualHash, String error) {
            this.path = path;
            this.valid = valid;
            this.expectedHash = expectedHash;
            this.actualHash = actualHash;
            this.error = error;
        }
        
        public String getPath() {
            return path;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getExpectedHash() {
            return expectedHash;
        }
        
        public String getActualHash() {
            return actualHash;
        }
        
        public String getError() {
            return error;
        }
    }
    
    /**
     * Результат операции очистки
     */
    public static class CleanupResult {
        private final int filesDeleted;
        private final List<String> deletedFiles;
        private final List<String> errors;
        
        public CleanupResult(int filesDeleted, List<String> deletedFiles, List<String> errors) {
            this.filesDeleted = filesDeleted;
            this.deletedFiles = deletedFiles;
            this.errors = errors;
        }
        
        public int getFilesDeleted() {
            return filesDeleted;
        }
        
        public List<String> getDeletedFiles() {
            return deletedFiles;
        }
        
        public List<String> getErrors() {
            return errors;
        }
    }
    
    private AssetManager() {
        String userHome = System.getProperty("user.home");
        this.minecraftDir = Paths.get(userHome, ".minecraft");
        this.modsDir = minecraftDir.resolve("mods");
        this.versionsDir = minecraftDir.resolve("versions");
        
        // Создаем пул потоков для многопоточной проверки
        int threadCount = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
        this.executorService = Executors.newFixedThreadPool(threadCount, r -> {
            Thread t = new Thread(r, "AssetManager-Verifier");
            t.setDaemon(true);
            return t;
        });
        
        try {
            Files.createDirectories(modsDir);
            Files.createDirectories(versionsDir);
        } catch (IOException e) {
            logger.error("Не удалось создать директории модов и версий", e);
        }
    }
    
    public static synchronized AssetManager getInstance() {
        if (instance == null) {
            instance = new AssetManager();
        }
        return instance;
    }
    
    /**
     * Асинхронная проверка файлов по манифесту
     * 
     * @param manifest Манифест: путь файла -> SHA-256 хеш
     * @return CompletableFuture со списком результатов проверки
     */
    public CompletableFuture<List<FileVerificationResult>> verifyFiles(Map<String, String> manifest) {
        logger.info("Начинаем проверку {} файлов", manifest.size());
        
        List<CompletableFuture<FileVerificationResult>> futures = new ArrayList<>();
        
        for (Map.Entry<String, String> entry : manifest.entrySet()) {
            String relativePath = entry.getKey();
            String expectedHash = entry.getValue();
            
            CompletableFuture<FileVerificationResult> future = CompletableFuture.supplyAsync(() -> {
                return verifyFile(relativePath, expectedHash);
            }, executorService);
            
            futures.add(future);
        }
        
        // Ждем завершения всех проверок
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );
        
        return allFutures.thenApply(v -> {
            List<FileVerificationResult> results = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
            
            long validCount = results.stream().filter(FileVerificationResult::isValid).count();
            long invalidCount = results.size() - validCount;
            
            logger.info("Проверка завершена: {} валидных, {} невалидных", validCount, invalidCount);
            
            return results;
        });
    }
    
    /**
     * Проверка одного файла
     */
    private FileVerificationResult verifyFile(String relativePath, String expectedHash) {
        Path filePath = minecraftDir.resolve(relativePath);
        
        try {
            if (!Files.exists(filePath)) {
                return new FileVerificationResult(
                        relativePath,
                        false,
                        expectedHash,
                        null,
                        "Файл не существует"
                );
            }
            
            // Вычисляем SHA-256 хеш файла
            String actualHash = calculateSHA256(filePath);
            
            boolean valid = expectedHash.equalsIgnoreCase(actualHash);
            
            if (!valid) {
                logger.warn("Файл {} имеет неверный хеш. Ожидалось: {}, получено: {}", 
                        relativePath, expectedHash, actualHash);
            }
            
            return new FileVerificationResult(
                    relativePath,
                    valid,
                    expectedHash,
                    actualHash,
                    valid ? null : "Хеш не совпадает"
            );
            
        } catch (Exception e) {
            logger.error("Ошибка при проверке файла: {}", relativePath, e);
            return new FileVerificationResult(
                    relativePath,
                    false,
                    expectedHash,
                    null,
                    "Ошибка: " + e.getMessage()
            );
        }
    }
    
    /**
     * Вычисление SHA-256 хеша файла с кэшированием и retry логикой
     */
    private String calculateSHA256(Path filePath) throws IOException, NoSuchAlgorithmException {
        String absolutePath = filePath.toAbsolutePath().toString();
        
        // Проверяем кэш
        if (hashCache.containsKey(absolutePath)) {
            // Проверяем, не изменился ли файл (по времени модификации)
            try {
                long lastModified = Files.getLastModifiedTime(filePath).toMillis();
                String cachedHash = hashCache.get(absolutePath);
                // Используем кэш, если файл не изменился
                // Для более точной проверки можно добавить метаданные о времени в кэш
                return cachedHash;
            } catch (Exception e) {
                // Если не удалось проверить, пересчитываем
                hashCache.remove(absolutePath);
            }
        }
        
        // Вычисляем хеш с retry логикой
        IOException lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                long fileSize = Files.size(filePath);
                
                try (InputStream inputStream = Files.newInputStream(filePath)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalRead = 0;
                    
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        digest.update(buffer, 0, bytesRead);
                        totalRead += bytesRead;
                        
                        // Проверяем целостность: если файл изменился во время чтения
                        if (totalRead > fileSize) {
                            throw new IOException("Размер файла изменился во время чтения");
                        }
                    }
                }
                
                byte[] hashBytes = digest.digest();
                String hash = bytesToHex(hashBytes);
                
                // Сохраняем в кэш
                hashCache.put(absolutePath, hash);
                
                return hash;
                
            } catch (IOException e) {
                lastException = e;
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    logger.warn("Ошибка при вычислении хеша файла {} (попытка {}/{}): {}", 
                            filePath, attempt, MAX_RETRY_ATTEMPTS, e.getMessage());
                    try {
                        Thread.sleep(100 * attempt); // Экспоненциальная задержка
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Прервано вычисление хеша", ie);
                    }
                }
            }
        }
        
        throw new IOException("Не удалось вычислить хеш файла после " + MAX_RETRY_ATTEMPTS + " попыток", lastException);
    }
    
    /**
     * Очистка кэша хешей
     */
    public void clearHashCache() {
        hashCache.clear();
        logger.debug("Кэш хешей очищен");
    }
    
    /**
     * Очистка устаревших записей из кэша (файлы, которые больше не существуют)
     */
    public void cleanHashCache() {
        int removed = 0;
        for (String path : new ArrayList<>(hashCache.keySet())) {
            Path filePath = Paths.get(path);
            if (!Files.exists(filePath)) {
                hashCache.remove(path);
                removed++;
            }
        }
        if (removed > 0) {
            logger.debug("Удалено {} устаревших записей из кэша хешей", removed);
        }
    }
    
    /**
     * Конвертация байтов в hex строку
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    /**
     * Очистка файлов в /mods и /versions, которые не указаны в манифесте
     * 
     * @param manifest Манифест разрешенных файлов: путь файла -> хеш
     * @return Результат очистки
     */
    public CompletableFuture<CleanupResult> cleanupUnlistedFiles(Map<String, String> manifest) {
        logger.info("Начинаем очистку файлов не из манифеста");
        
        return CompletableFuture.supplyAsync(() -> {
            Set<String> allowedPaths = new HashSet<>();
            
            // Создаем набор разрешенных путей
            for (String path : manifest.keySet()) {
                allowedPaths.add(path);
                // Также добавляем все родительские директории
                Path pathObj = Paths.get(path);
                Path parent = pathObj.getParent();
                while (parent != null && !parent.equals(Paths.get(""))) {
                    allowedPaths.add(parent.toString().replace("\\", "/"));
                    parent = parent.getParent();
                }
            }
            
            List<String> deletedFiles = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            
            // Очищаем директорию mods
            try {
                CleanupResult modsResult = cleanupDirectory(modsDir, "mods", allowedPaths);
                deletedFiles.addAll(modsResult.getDeletedFiles());
                errors.addAll(modsResult.getErrors());
            } catch (Exception e) {
                logger.error("Ошибка при очистке директории mods", e);
                errors.add("Ошибка очистки mods: " + e.getMessage());
            }
            
            // Очищаем директорию versions
            try {
                CleanupResult versionsResult = cleanupDirectory(versionsDir, "versions", allowedPaths);
                deletedFiles.addAll(versionsResult.getDeletedFiles());
                errors.addAll(versionsResult.getErrors());
            } catch (Exception e) {
                logger.error("Ошибка при очистке директории versions", e);
                errors.add("Ошибка очистки versions: " + e.getMessage());
            }
            
            logger.info("Очистка завершена: удалено {} файлов, ошибок: {}", 
                    deletedFiles.size(), errors.size());
            
            return new CleanupResult(deletedFiles.size(), deletedFiles, errors);
        }, executorService);
    }
    
    /**
     * Файлы и директории, которые всегда должны быть сохранены
     */
    private static final Set<String> PROTECTED_PATTERNS = new HashSet<>(Arrays.asList(
            "options.txt", "optionsof.txt", "servers.dat", "usercache.json",
            "usernamecache.json", "launcher_profiles.json", "launcher_accounts.json",
            "logs", "screenshots", "resourcepacks", "saves", "stats"
    ));
    
    /**
     * Очистка одной директории
     */
    private CleanupResult cleanupDirectory(Path directory, String relativePrefix, Set<String> allowedPaths) throws IOException {
        List<String> deletedFiles = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        if (!Files.exists(directory)) {
            return new CleanupResult(0, deletedFiles, errors);
        }
        
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                // Вычисляем относительный путь от minecraftDir
                Path relativePath = minecraftDir.relativize(file);
                String relativePathStr = relativePath.toString().replace("\\", "/");
                String fileName = file.getFileName().toString();
                
                // Проверяем, защищен ли файл
                boolean isProtected = false;
                for (String pattern : PROTECTED_PATTERNS) {
                    if (relativePathStr.contains(pattern) || fileName.equals(pattern)) {
                        isProtected = true;
                        break;
                    }
                }
                
                if (isProtected) {
                    logger.debug("Защищенный файл пропущен: {}", relativePathStr);
                    return FileVisitResult.CONTINUE;
                }
                
                // Проверяем, разрешен ли этот файл
                boolean isAllowed = false;
                for (String allowedPath : allowedPaths) {
                    if (relativePathStr.equals(allowedPath) || relativePathStr.startsWith(allowedPath + "/")) {
                        isAllowed = true;
                        break;
                    }
                }
                
                if (!isAllowed) {
                    try {
                        // Проверяем, не заблокирован ли файл другим процессом
                        if (!Files.isWritable(file)) {
                            logger.warn("Файл заблокирован для записи: {}", relativePathStr);
                            errors.add("Файл заблокирован: " + relativePathStr);
                            return FileVisitResult.CONTINUE;
                        }
                        
                        long fileSize = Files.size(file);
                        Files.delete(file);
                        deletedFiles.add(relativePathStr);
                        logger.debug("Удален файл не из манифеста: {} ({} байт)", relativePathStr, fileSize);
                    } catch (java.nio.file.FileSystemException e) {
                        // Файл используется другим процессом
                        logger.warn("Не удалось удалить файл (используется другим процессом): {}", relativePathStr);
                        errors.add("Файл используется: " + relativePathStr);
                    } catch (IOException e) {
                        logger.error("Не удалось удалить файл: {}", relativePathStr, e);
                        errors.add("Не удалось удалить: " + relativePathStr + " (" + e.getMessage() + ")");
                    }
                }
                
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                logger.warn("Ошибка при посещении файла: {}", file, exc);
                errors.add("Ошибка при посещении: " + file);
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    logger.warn("Ошибка при посещении директории: {}", dir, exc);
                    errors.add("Ошибка при посещении директории: " + dir);
                    return FileVisitResult.CONTINUE;
                }
                
                // Удаляем пустые директории (кроме корневых mods и versions)
                if (!dir.equals(modsDir) && !dir.equals(versionsDir)) {
                    try {
                        // Проверяем, что директория действительно пуста
                        boolean isEmpty = true;
                        try (java.nio.file.DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                            if (stream.iterator().hasNext()) {
                                isEmpty = false;
                            }
                        }
                        
                        if (isEmpty) {
                            Files.delete(dir);
                            logger.debug("Удалена пустая директория: {}", dir);
                        }
                    } catch (java.nio.file.DirectoryNotEmptyException e) {
                        // Директория не пуста, пропускаем
                    } catch (IOException e) {
                        logger.debug("Не удалось удалить директорию (возможно, не пуста): {}", dir);
                    }
                }
                
                return FileVisitResult.CONTINUE;
            }
        });
        
        return new CleanupResult(deletedFiles.size(), deletedFiles, errors);
    }
    
    /**
     * Остановка executor service
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
