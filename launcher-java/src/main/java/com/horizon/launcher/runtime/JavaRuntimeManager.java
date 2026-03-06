package com.horizon.launcher.runtime;

import com.horizon.launcher.util.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Менеджер изолированной Java 21 Runtime
 * 
 * Лаунчер не должен зависеть от того, что установлено у пользователя в системе.
 * Проверяет папку runtime/java21. Если пусто — качает архив с официального зеркала
 * (Temurin), распаковывает и использует путь к этому java.exe для запуска игры.
 */
public class JavaRuntimeManager {
    private static final Logger logger = LoggerFactory.getLogger(JavaRuntimeManager.class);
    private static JavaRuntimeManager instance;
    
    private final Path runtimeDirectory;
    private final Path java21Directory;
    private static final String JAVA_VERSION = "21";
    private static final String TEMURIN_BASE_URL = "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse";
    
    private JavaRuntimeManager() {
        this.runtimeDirectory = ConfigManager.getInstance().getLauncherDir().resolve("runtime");
        this.java21Directory = runtimeDirectory.resolve("java21");
        
        try {
            Files.createDirectories(runtimeDirectory);
        } catch (IOException e) {
            logger.error("Не удалось создать директорию runtime", e);
        }
    }
    
    public static JavaRuntimeManager getInstance() {
        if (instance == null) {
            instance = new JavaRuntimeManager();
        }
        return instance;
    }
    
    /**
     * Получает путь к java.exe
     * Если Java не установлена, автоматически загружает и распаковывает её
     */
    public Path getJavaExecutable() throws IOException {
        Path javaExe = findJavaExecutable();
        
        if (javaExe != null && Files.exists(javaExe)) {
            logger.info("Используется Java из: {}", javaExe);
            return javaExe;
        }
        
        logger.info("Java 21 не найдена, начинаем загрузку...");
        downloadAndExtractJava();
        
        javaExe = findJavaExecutable();
        if (javaExe == null || !Files.exists(javaExe)) {
            throw new IOException("Не удалось найти java.exe после загрузки");
        }
        
        return javaExe;
    }
    
    /**
     * Ищет java.exe в директории java21
     */
    private Path findJavaExecutable() {
        // Стандартный путь: java21/bin/java.exe
        Path javaExe = java21Directory.resolve("bin").resolve("java.exe");
        if (Files.exists(javaExe)) {
            return javaExe;
        }
        
        // Альтернативный путь (может быть jdk-21.x.x или подобное)
        try {
            if (Files.exists(java21Directory)) {
                try (java.util.stream.Stream<Path> stream = Files.walk(java21Directory)) {
                    return stream
                            .filter(path -> path.getFileName().toString().equals("java.exe"))
                            .findFirst()
                            .orElse(null);
                }
            }
        } catch (IOException e) {
            logger.debug("Ошибка при поиске java.exe", e);
        }
        
        return null;
    }
    
    /**
     * Загружает и распаковывает Java 21 из Temurin
     */
    private void downloadAndExtractJava() throws IOException {
        logger.info("Загрузка Java 21 из Temurin...");
        
        // Получаем URL для загрузки
        String downloadUrl = getDownloadUrl();
        logger.info("URL загрузки: {}", downloadUrl);
        
        // Загружаем архив
        Path zipFile = runtimeDirectory.resolve("java21.zip");
        downloadFile(downloadUrl, zipFile);
        
        // Распаковываем
        logger.info("Распаковка Java 21...");
        extractZip(zipFile, java21Directory);
        
        // Удаляем архив
        Files.deleteIfExists(zipFile);
        
        logger.info("Java 21 успешно установлена в {}", java21Directory);
    }
    
    /**
     * Получает URL для загрузки Java 21
     * Использует Adoptium API для получения последней версии
     */
    private String getDownloadUrl() {
        // Для упрощения используем прямой URL к последней версии
        // В production можно использовать Adoptium API для получения актуальной версии
        return TEMURIN_BASE_URL;
    }
    
    /**
     * Загружает файл по URL
     */
    private void downloadFile(String urlString, Path destination) throws IOException {
        logger.info("Загрузка {} в {}", urlString, destination);
        
        // Валидация URL
        java.net.URI uri;
        try {
            uri = new java.net.URI(urlString);
        } catch (java.net.URISyntaxException e) {
            throw new IOException("Некорректный синтаксис URL: " + urlString, e);
        }
        
        // Проверка, что это HTTP/HTTPS URL
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new IOException("URL должен использовать протокол HTTP или HTTPS: " + urlString);
        }
        
        try (InputStream in = uri.toURL().openStream();
             FileOutputStream out = new FileOutputStream(destination.toFile())) {
            
            byte[] buffer = new byte[8192];
            long totalBytes = 0;
            int bytesRead;
            
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
                
                // Логируем прогресс каждые 10MB
                if (totalBytes % (10 * 1024 * 1024) == 0) {
                    logger.info("Загружено: {} MB", totalBytes / (1024 * 1024));
                }
            }
            
            logger.info("Загрузка завершена. Размер: {} MB", totalBytes / (1024 * 1024));
        }
    }
    
    /**
     * Распаковывает ZIP архив
     */
    private void extractZip(Path zipFile, Path destination) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path filePath = destination.resolve(entry.getName());
                
                // Пропускаем корневую директорию (обычно jdk-21.x.x)
                String entryName = entry.getName();
                if (entryName.contains("/")) {
                    String[] parts = entryName.split("/", 2);
                    if (parts.length > 1) {
                        filePath = destination.resolve(parts[1]);
                    }
                }
                
                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    Files.createDirectories(filePath.getParent());
                    try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }
    
    /**
     * Проверяет, установлена ли Java 21
     */
    public boolean isJavaInstalled() {
        Path javaExe = findJavaExecutable();
        return javaExe != null && Files.exists(javaExe);
    }
    
    /**
     * Получает версию установленной Java
     */
    public String getJavaVersion() {
        Path javaExe = findJavaExecutable();
        if (javaExe == null || !Files.exists(javaExe)) {
            return null;
        }
        
        try {
            Process process = new ProcessBuilder(javaExe.toString(), "-version")
                    .redirectErrorStream(true)
                    .start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null) {
                    return line;
                }
            }
        } catch (IOException e) {
            logger.debug("Ошибка при получении версии Java", e);
        }
        
        return null;
    }
}

