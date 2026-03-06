package com.horizon.launcher.minecraft;

import com.horizon.launcher.runtime.JavaRuntimeManager;
import com.horizon.launcher.util.ConfigManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.GZIPInputStream;
import java.util.jar.JarInputStream;
import java.util.ArrayList;
import java.util.List;

// Apache Commons Compress для распаковки TAR.GZ
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

/**
 * Загрузчик и распаковщик JRE 21 Runtime
 * Скачивает JRE с Temurin, проверяет целостность и распаковывает
 */
public class RuntimeDownloader {
    private static final Logger logger = LoggerFactory.getLogger(RuntimeDownloader.class);
    private static RuntimeDownloader instance;
    
    private final Path runtimeDirectory;
    private final OkHttpClient httpClient;
    private final ExecutorService executorService;
    
    private static final String JAVA_VERSION = "21";
    private static final String TEMURIN_BASE_URL = "https://api.adoptium.net/v3/binary/latest/21/ga/";
    
    /**
     * Результат загрузки и установки Runtime
     */
    public static class RuntimeInstallResult {
        private final boolean success;
        private final String message;
        private final Path javaExecutable;
        private final long downloadedBytes;
        
        public RuntimeInstallResult(boolean success, String message, Path javaExecutable, long downloadedBytes) {
            this.success = success;
            this.message = message;
            this.javaExecutable = javaExecutable;
            this.downloadedBytes = downloadedBytes;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public Path getJavaExecutable() {
            return javaExecutable;
        }
        
        public long getDownloadedBytes() {
            return downloadedBytes;
        }
    }
    
    private RuntimeDownloader() {
        this.runtimeDirectory = ConfigManager.getInstance().getLauncherDir().resolve("runtime");
        this.httpClient = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build();
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "RuntimeDownloader");
            t.setDaemon(true);
            return t;
        });
        
        try {
            Files.createDirectories(runtimeDirectory);
        } catch (IOException e) {
            logger.error("Не удалось создать директорию runtime", e);
        }
    }
    
    public static synchronized RuntimeDownloader getInstance() {
        if (instance == null) {
            instance = new RuntimeDownloader();
        }
        return instance;
    }
    
    /**
     * Проверяет наличие JRE и загружает его при необходимости
     * 
     * @param platform Платформа (windows, linux, mac)
     * @param architecture Архитектура (x64, aarch64)
     * @return CompletableFuture с результатом установки
     */
    public CompletableFuture<RuntimeInstallResult> ensureRuntimeInstalled(String platform, String architecture) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Проверяем, установлен ли уже JRE
                Path javaExe = findJavaExecutable();
                if (javaExe != null && Files.exists(javaExe)) {
                    logger.info("JRE уже установлен: {}", javaExe);
                    return new RuntimeInstallResult(true, "JRE уже установлен", javaExe, 0);
                }
                
                // Загружаем JRE
                logger.info("Начинаем загрузку JRE {} для {} {}", JAVA_VERSION, platform, architecture);
                return downloadAndExtractRuntime(platform, architecture);
                
            } catch (Exception e) {
                logger.error("Ошибка при установке JRE", e);
                return new RuntimeInstallResult(false, "Ошибка: " + e.getMessage(), null, 0);
            }
        }, executorService);
    }
    
    /**
     * Загрузка и распаковка JRE
     */
    private RuntimeInstallResult downloadAndExtractRuntime(String platform, String architecture) throws Exception {
        // Формируем URL для загрузки
        String url = buildDownloadUrl(platform, architecture);
        logger.info("URL загрузки: {}", url);
        
        // Путь для сохранения архива
        String archiveExtension = platform.equals("windows") ? ".zip" : ".tar.gz";
        Path archivePath = runtimeDirectory.resolve("jre-" + JAVA_VERSION + "-" + platform + "-" + architecture + archiveExtension);
        
        // Скачиваем архив
        long downloadedBytes = downloadArchive(url, archivePath);
        
        // Проверяем целостность архива
        if (!verifyArchiveIntegrity(archivePath)) {
            throw new IOException("Архив поврежден или неполный");
        }
        
        // Распаковываем архив
        Path java21Dir = runtimeDirectory.resolve("java21");
        extractArchive(archivePath, java21Dir, platform);
        
        // Удаляем архив после распаковки
        Files.deleteIfExists(archivePath);
        
        // Находим java.exe
        Path javaExe = findJavaExecutable();
        if (javaExe == null || !Files.exists(javaExe)) {
            throw new IOException("Не удалось найти java.exe после распаковки");
        }
        
        // Проверяем версию Java
        if (!verifyJavaVersion(javaExe)) {
            logger.warn("Установленная Java не соответствует требуемой версии 21, но будет использована");
        }
        
        logger.info("JRE успешно установлен: {}", javaExe);
        return new RuntimeInstallResult(true, "JRE успешно установлен", javaExe, downloadedBytes);
    }
    
    /**
     * Формирование URL для загрузки
     */
    private String buildDownloadUrl(String platform, String architecture) {
        String os = platform.equals("windows") ? "windows" : platform.equals("mac") ? "mac" : "linux";
        String arch = architecture.equals("aarch64") ? "aarch64" : "x64";
        String extension = platform.equals("windows") ? "zip" : "tar.gz";
        
        // Используем Temurin (Eclipse Adoptium)
        return String.format(
                "https://api.adoptium.net/v3/binary/latest/%s/ga/%s/%s/jdk/hotspot/normal/eclipse?project=jdk&archive_type=%s",
                JAVA_VERSION, os, arch, extension
        );
    }
    
    /**
     * Скачивание архива
     */
    private long downloadArchive(String url, Path targetPath) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Неуспешный ответ при загрузке: " + response.code());
            }
            
            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Пустое тело ответа");
            }
            
            long totalBytes = body.contentLength();
            logger.info("Размер архива: {} байт ({} МБ)", totalBytes, totalBytes / (1024 * 1024));
            
            try (InputStream inputStream = body.byteStream();
                 OutputStream outputStream = Files.newOutputStream(targetPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                
                byte[] buffer = new byte[8192];
                long downloaded = 0;
                int bytesRead;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    downloaded += bytesRead;
                    
                    // Логируем прогресс каждые 10 МБ
                    if (downloaded % (10 * 1024 * 1024) == 0) {
                        double percent = totalBytes > 0 ? (downloaded * 100.0 / totalBytes) : 0;
                        logger.info("Загружено: {} МБ / {} МБ ({}%)", 
                                downloaded / (1024 * 1024), 
                                totalBytes / (1024 * 1024), 
                                String.format("%.1f", percent));
                    }
                }
                
                logger.info("Архив успешно загружен: {}", targetPath);
                return downloaded;
            }
        }
    }
    
    /**
     * Проверка целостности архива
     */
    private boolean verifyArchiveIntegrity(Path archivePath) throws IOException {
        if (!Files.exists(archivePath)) {
            return false;
        }
        
        long fileSize = Files.size(archivePath);
        if (fileSize == 0) {
            logger.error("Архив пуст");
            return false;
        }
        
        // Минимальный размер JRE архива (примерно 100 МБ)
        if (fileSize < 100 * 1024 * 1024) {
            logger.warn("Архив слишком мал для JRE: {} байт", fileSize);
            return false;
        }
        
        // Проверяем, что архив можно открыть
        try {
            if (archivePath.toString().endsWith(".zip")) {
                try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archivePath))) {
                    ZipEntry entry = zip.getNextEntry();
                    if (entry == null) {
                        logger.error("ZIP архив не содержит записей");
                        return false;
                    }
                }
            } else if (archivePath.toString().endsWith(".tar.gz")) {
                try (GZIPInputStream gzip = new GZIPInputStream(Files.newInputStream(archivePath))) {
                    // Проверяем, что можно прочитать начало архива
                    byte[] buffer = new byte[1024];
                    int bytesRead = gzip.read(buffer);
                    if (bytesRead <= 0) {
                        logger.error("TAR.GZ архив поврежден");
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Ошибка при проверке целостности архива", e);
            return false;
        }
        
        logger.info("Целостность архива проверена успешно");
        return true;
    }
    
    /**
     * Распаковка архива
     */
    private void extractArchive(Path archivePath, Path targetDir, String platform) throws IOException {
        logger.info("Начинаем распаковку архива в {}", targetDir);
        
        // Удаляем старую директорию, если существует
        if (Files.exists(targetDir)) {
            deleteDirectory(targetDir);
        }
        Files.createDirectories(targetDir);
        
        if (archivePath.toString().endsWith(".zip")) {
            extractZip(archivePath, targetDir);
        } else if (archivePath.toString().endsWith(".tar.gz")) {
            extractTarGz(archivePath, targetDir);
        } else {
            throw new IOException("Неподдерживаемый формат архива: " + archivePath);
        }
        
        logger.info("Архив успешно распакован");
    }
    
    /**
     * Распаковка ZIP архива
     */
    private void extractZip(Path zipPath, Path targetDir) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName());
                
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zipInputStream, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
                
                zipInputStream.closeEntry();
            }
        }
    }
    
    /**
     * Распаковка TAR.GZ архива используя Apache Commons Compress
     */
    private void extractTarGz(Path tarGzPath, Path targetDir) throws IOException {
        logger.info("Распаковка TAR.GZ архива: {}", tarGzPath);
        
        int filesExtracted = 0;
        int directoriesCreated = 0;
        long totalBytesExtracted = 0;
        
        try (FileInputStream fis = new FileInputStream(tarGzPath.toFile());
             GzipCompressorInputStream gzis = new GzipCompressorInputStream(fis);
             TarArchiveInputStream tarInput = new TarArchiveInputStream(gzis)) {
            
            TarArchiveEntry entry;
            while ((entry = tarInput.getNextTarEntry()) != null) {
                String entryName = normalizePath(entry.getName());
                
                if (entry.isDirectory()) {
                    Path dirPath = targetDir.resolve(entryName);
                    // Проверка на path traversal
                    if (!dirPath.normalize().startsWith(targetDir.normalize())) {
                        logger.warn("Обнаружена попытка path traversal в директории: {}", entry.getName());
                        continue;
                    }
                    try {
                        Files.createDirectories(dirPath);
                        directoriesCreated++;
                    } catch (FileAlreadyExistsException e) {
                        // Игнорируем, если директория уже существует
                    }
                    continue;
                }
                
                if (entry.isSymbolicLink()) {
                    logger.debug("Пропущена символическая ссылка: {}", entry.getName());
                    continue;
                }
                
                Path filePath = targetDir.resolve(entryName);
                
                // Проверка на path traversal атаки
                if (!filePath.normalize().startsWith(targetDir.normalize())) {
                    logger.warn("Обнаружена попытка path traversal: {}", entry.getName());
                    continue;
                }
                
                try {
                    Files.createDirectories(filePath.getParent());
                } catch (FileAlreadyExistsException e) {
                    // Игнорируем, если директория уже существует
                }
                
                try (OutputStream outputStream = Files.newOutputStream(filePath, 
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    long entrySize = entry.getSize();
                    if (entrySize > 0) {
                        long copied = IOUtils.copy(tarInput, outputStream);
                        totalBytesExtracted += copied;
                        
                        if (copied != entrySize) {
                            logger.warn("Размер распакованного файла не совпадает: {} (ожидалось: {}, получено: {})", 
                                    entry.getName(), entrySize, copied);
                        }
                    }
                    
                    // Устанавливаем права доступа (если поддерживается)
                    try {
                        java.util.Set<java.nio.file.attribute.PosixFilePermission> perms = 
                                java.nio.file.attribute.PosixFilePermissions.fromString("rw-r--r--");
                        Files.setPosixFilePermissions(filePath, perms);
                    } catch (UnsupportedOperationException e) {
                        // Игнорируем на Windows, где PosixFilePermissions не поддерживается
                    } catch (IOException e) {
                        logger.debug("Не удалось установить права доступа для файла: {}", filePath, e);
                    }
                }
                
                filesExtracted++;
                
                // Логируем прогресс каждые 100 файлов
                if (filesExtracted % 100 == 0) {
                    logger.debug("Распаковано файлов: {}, директорий: {}, байт: {}", 
                            filesExtracted, directoriesCreated, totalBytesExtracted);
                }
            }
        }
        
        logger.info("TAR.GZ успешно распакован: {} файлов, {} директорий, {} МБ", 
                filesExtracted, directoriesCreated, totalBytesExtracted / (1024 * 1024));
    }
    
    /**
     * Нормализация пути (защита от path traversal)
     */
    private String normalizePath(String path) {
        // Заменяем обратные слеши на прямые
        path = path.replace("\\", "/");
        // Удаляем начальные слеши
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        // Удаляем ".." из пути
        String[] parts = path.split("/");
        List<String> normalizedParts = new ArrayList<>();
        for (String part : parts) {
            if (part.equals("..")) {
                if (!normalizedParts.isEmpty()) {
                    normalizedParts.remove(normalizedParts.size() - 1);
                }
            } else if (!part.equals(".") && !part.isEmpty()) {
                normalizedParts.add(part);
            }
        }
        return String.join("/", normalizedParts);
    }
    
    /**
     * Рекурсивное удаление директории
     */
    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
    
    /**
     * Поиск java.exe после распаковки
     */
    private Path findJavaExecutable() {
        Path java21Dir = runtimeDirectory.resolve("java21");
        
        // Ищем java.exe в стандартном месте
        String exeName = System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java";
        
        // Проверяем стандартные пути: java21/bin/java, jdk-21.x.x/bin/java
        Path[] standardPaths = {
            java21Dir.resolve("bin").resolve(exeName),
            java21Dir.resolve("jdk-" + JAVA_VERSION).resolve("bin").resolve(exeName)
        };
        
        for (Path standardPath : standardPaths) {
            if (Files.exists(standardPath)) {
                return standardPath;
            }
        }
        
        // Ищем рекурсивно в java21Dir
        try {
            try (java.util.stream.Stream<Path> stream = Files.walk(java21Dir, 5)) {
                return stream
                        .filter(path -> {
                            String fileName = path.getFileName().toString();
                            return fileName.equals(exeName) && Files.isRegularFile(path);
                        })
                        .findFirst()
                        .orElse(null);
            }
        } catch (IOException e) {
            logger.debug("Ошибка при поиске java.exe", e);
            return null;
        }
    }
    
    /**
     * Проверка версии Java
     */
    private boolean verifyJavaVersion(Path javaExe) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    javaExe.toAbsolutePath().toString(), "-version"
            );
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.warn("Не удалось проверить версию Java (код выхода: {})", exitCode);
                return false;
            }
            
            String versionOutput = output.toString().toLowerCase();
            if (versionOutput.contains("version \"" + JAVA_VERSION) || 
                versionOutput.contains("openjdk version \"" + JAVA_VERSION) ||
                versionOutput.contains("java version \"" + JAVA_VERSION)) {
                logger.info("Версия Java подтверждена: {}", versionOutput.trim());
                return true;
            } else {
                logger.warn("Версия Java не соответствует ожидаемой {}. Вывод: {}", JAVA_VERSION, versionOutput.trim());
                return false;
            }
            
        } catch (Exception e) {
            logger.warn("Ошибка при проверке версии Java", e);
            return false;
        }
    }
    
    /**
     * Остановка executor service
     */
    public void shutdown() {
        executorService.shutdown();
        httpClient.dispatcher().executorService().shutdown();
    }
}
