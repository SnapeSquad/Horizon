package com.horizon.launcher.minecraft;

import com.horizon.launcher.security.AntiInject;
import com.horizon.launcher.util.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Класс для запуска Minecraft с автоматическим инжектом мода
 * Автоматически собирает мод (если нужно) и копирует его в .minecraft/mods/
 */
public class GameLauncher {
    private static final Logger logger = LoggerFactory.getLogger(GameLauncher.class);
    private static GameLauncher instance;
    
    private final Path launcherDir;
    private final Path modSourceDir;
    private final Path modBuildDir;
    private final Path minecraftModsDir;
    
    private GameLauncher() {
        this.launcherDir = ConfigManager.getInstance().getLauncherDir();
        // Путь к исходникам мода относительно корня проекта
        this.modSourceDir = launcherDir.getParent().resolve("cosmetics-mod");
        this.modBuildDir = modSourceDir.resolve("build").resolve("libs");
        
        // Путь к директории модов Minecraft (стандартная директория)
        String userHome = System.getProperty("user.home");
        this.minecraftModsDir = Paths.get(userHome, ".minecraft", "mods");
        
        try {
            Files.createDirectories(minecraftModsDir);
        } catch (IOException e) {
            logger.error("Не удалось создать директорию модов Minecraft", e);
        }
    }
    
    public static synchronized GameLauncher getInstance() {
        if (instance == null) {
            instance = new GameLauncher();
        }
        return instance;
    }
    
    /**
     * Запускает Minecraft с автоматическим инжектом мода
     * 
     * @param minecraftVersion Версия Minecraft (например, "1.21")
     * @param username Имя пользователя
     * @param accessToken Токен доступа
     * @param uuid UUID игрока
     * @param maxMemory Максимальная память в МБ
     * @return CompletableFuture завершается при завершении процесса Minecraft
     */
    public CompletableFuture<Process> launchMinecraft(
            String minecraftVersion,
            String username,
            String accessToken,
            String uuid,
            int maxMemory
    ) {
        return launchMinecraft(minecraftVersion, username, accessToken, uuid, maxMemory, false, false);
    }
    
    /**
     * Полная версия запуска с поддержкой безопасности и валидации
     */
    public CompletableFuture<Process> launchMinecraft(
            String minecraftVersion,
            String username,
            String accessToken,
            String uuid,
            int maxMemory,
            boolean verifyFiles,
            boolean cleanupUnlisted
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 0. Проверка безопасности (Anti-Inject)
                logger.info("Проверка системы на наличие инжекторов...");
                AntiInject antiInject = AntiInject.getInstance();
                AntiInject.InjectorScanResult scanResult = antiInject.scanForInjectors().join();
                
                if (!scanResult.isClean()) {
                    logger.warn("Обнаружены подозрительные процессы: {}", scanResult.getMessage());
                    // Можно добавить логику блокировки запуска
                    // throw new SecurityException("Обнаружены инжекторы: " + scanResult.getMessage());
                }
                
                // 1. Проверка и валидация файлов (если требуется)
                if (verifyFiles) {
                    logger.info("Проверка файлов по манифесту...");
                    Map<String, String> manifest = new HashMap<>();
                    // В реальности манифест должен загружаться с API
                    // manifest.put("mods/horizon-cosmetics-1.0.0.jar", "sha256-hash-here");
                    
                    AssetManager assetManager = AssetManager.getInstance();
                    List<AssetManager.FileVerificationResult> verificationResults = 
                            assetManager.verifyFiles(manifest).join();
                    
                    long invalidCount = verificationResults.stream()
                            .filter(r -> !r.isValid())
                            .count();
                    if (invalidCount > 0) {
                        logger.warn("Найдено {} файлов с неверными хешами", invalidCount);
                    }
                }
                
                // 2. Очистка неразрешенных файлов (если требуется)
                if (cleanupUnlisted) {
                    logger.info("Очистка файлов не из манифеста...");
                    Map<String, String> manifest = new HashMap<>();
                    AssetManager assetManager = AssetManager.getInstance();
                    AssetManager.CleanupResult cleanupResult = 
                            assetManager.cleanupUnlistedFiles(manifest).join();
                    
                    logger.info("Удалено {} файлов, ошибок: {}", 
                            cleanupResult.getFilesDeleted(), cleanupResult.getErrors().size());
                }
                
                // 3. Проверяем и собираем мод
                logger.info("Проверка мода...");
                ensureModBuilt();
                
                // 4. Копируем мод в .minecraft/mods/
                logger.info("Копирование мода в .minecraft/mods/...");
                copyModToMinecraft();
                
                // 5. Обеспечиваем наличие JRE
                logger.info("Проверка JRE...");
                RuntimeDownloader runtimeDownloader = RuntimeDownloader.getInstance();
                String os = System.getProperty("os.name").toLowerCase();
                String arch = System.getProperty("os.arch").toLowerCase();
                String platform = os.contains("win") ? "windows" : os.contains("mac") ? "mac" : "linux";
                String architecture = arch.contains("64") ? "x64" : "aarch64";
                
                RuntimeDownloader.RuntimeInstallResult runtimeResult = 
                        runtimeDownloader.ensureRuntimeInstalled(platform, architecture).join();
                
                if (!runtimeResult.isSuccess()) {
                    throw new IOException("Не удалось установить JRE: " + runtimeResult.getMessage());
                }
                
                Path javaExe = runtimeResult.getJavaExecutable();
                logger.info("Используется JRE: {}", javaExe);
                
                // 6. Запускаем Minecraft через LaunchBuilder
                logger.info("Формирование команды запуска Minecraft {}...", minecraftVersion);
                Process minecraftProcess = startMinecraftWithBuilder(
                        minecraftVersion, username, accessToken, uuid, maxMemory, javaExe
                );
                
                logger.info("Minecraft успешно запущен (PID: {})", minecraftProcess.pid());
                return minecraftProcess;
                
            } catch (Exception e) {
                logger.error("Ошибка при запуске Minecraft", e);
                throw new RuntimeException("Не удалось запустить Minecraft", e);
            }
        });
    }
    
    /**
     * Проверяет, собран ли мод, и собирает его при необходимости
     */
    private void ensureModBuilt() throws IOException, InterruptedException {
        // Проверяем, существует ли собранный мод
        Path modJar = findModJar();
        
        if (modJar != null && Files.exists(modJar)) {
            logger.info("Мод уже собран: {}", modJar);
            return;
        }
        
        logger.info("Мод не найден, начинаем сборку...");
        buildMod();
    }
    
    /**
     * Находит JAR файл мода в директории сборки
     */
    private Path findModJar() {
        if (!Files.exists(modBuildDir)) {
            return null;
        }
        
        try {
            try (java.util.stream.Stream<Path> stream = Files.walk(modBuildDir)) {
                return stream
                        .filter(path -> path.getFileName().toString().endsWith(".jar"))
                        .filter(path -> !path.getFileName().toString().contains("-sources"))
                        .filter(path -> !path.getFileName().toString().contains("-dev"))
                        .findFirst()
                        .orElse(null);
            }
        } catch (IOException e) {
            logger.error("Ошибка при поиске JAR файла мода", e);
            return null;
        }
    }
    
    /**
     * Собирает мод используя Gradle
     */
    private void buildMod() throws IOException, InterruptedException {
        if (!Files.exists(modSourceDir)) {
            throw new IOException("Директория исходников мода не найдена: " + modSourceDir);
        }
        
        // Проверяем наличие gradlew
        Path gradlew = modSourceDir.resolve(System.getProperty("os.name").toLowerCase().contains("win") 
                ? "gradlew.bat" 
                : "gradlew");
        
        if (!Files.exists(gradlew)) {
            logger.warn("Gradle Wrapper не найден, создаем...");
            // Можно создать gradle wrapper или использовать системный gradle
            logger.error("Gradle Wrapper требуется для сборки мода. Пожалуйста, инициализируйте Gradle в cosmetics-mod/");
            throw new IOException("Gradle Wrapper не найден в " + modSourceDir);
        }
        
        // Запускаем сборку
        List<String> command = new ArrayList<>();
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            command.add("cmd.exe");
            command.add("/c");
            command.add(gradlew.toAbsolutePath().toString());
        } else {
            command.add("bash");
            command.add(gradlew.toAbsolutePath().toString());
        }
        command.add("build");
        command.add("--no-daemon");
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(modSourceDir.toFile());
        processBuilder.redirectErrorStream(true);
        
        logger.info("Запуск сборки мода: {}", String.join(" ", command));
        
        Process buildProcess = processBuilder.start();
        
        // Читаем вывод сборки
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(buildProcess.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.debug("[Gradle] {}", line);
            }
        }
        
        int exitCode = buildProcess.waitFor();
        if (exitCode != 0) {
            throw new IOException("Сборка мода завершилась с ошибкой (код выхода: " + exitCode + ")");
        }
        
        logger.info("Мод успешно собран");
    }
    
    /**
     * Копирует собранный мод в .minecraft/mods/
     */
    private void copyModToMinecraft() throws IOException {
        Path modJar = findModJar();
        if (modJar == null || !Files.exists(modJar)) {
            throw new IOException("JAR файл мода не найден после сборки");
        }
        
        // Имя файла мода в директории модов
        String modFileName = modJar.getFileName().toString();
        Path targetMod = minecraftModsDir.resolve(modFileName);
        
        // Копируем мод (перезаписываем, если уже существует)
        Files.copy(modJar, targetMod, StandardCopyOption.REPLACE_EXISTING);
        
        logger.info("Мод скопирован: {} -> {}", modJar, targetMod);
    }
    
    /**
     * Запускает Minecraft через LaunchBuilder (новый метод)
     */
    private Process startMinecraftWithBuilder(
            String minecraftVersion,
            String username,
            String accessToken,
            String uuid,
            int maxMemory,
            Path javaExe
    ) throws IOException {
        // Используем LaunchBuilder для формирования команды
        LaunchBuilder builder = LaunchBuilder.create()
                .version(minecraftVersion)
                .username(username)
                .accessToken(accessToken)
                .uuid(uuid)
                .memory(maxMemory / 2, maxMemory)
                .javaExecutable(javaExe)
                .customProperty("horizon.token", accessToken);
        
        builder.validate();
        
        List<String> command = builder.build();
        logger.info("Команда запуска: {}", builder.buildCommandString());
        
        // Создаем процесс
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(Paths.get(System.getProperty("user.home"), ".minecraft").toFile());
        
        // Перенаправляем вывод в файл
        Path logFile = Paths.get(System.getProperty("user.home"), ".minecraft", "logs", "horizon-launcher.log");
        Files.createDirectories(logFile.getParent());
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(logFile.toFile());
        
        return processBuilder.start();
    }
    
    /**
     * Запускает процесс Minecraft (старый метод, оставлен для совместимости)
     * 
     * Использует Fabric Loader для запуска Minecraft с модом.
     * Мод автоматически загружается из .minecraft/mods/
     */
    private Process startMinecraft(
            String minecraftVersion,
            String username,
            String accessToken,
            String uuid,
            int maxMemory
    ) throws IOException {
        // Путь к Java (используем системную или изолированную)
        Path javaExe = findJavaExecutable();
        if (javaExe == null || !Files.exists(javaExe)) {
            throw new IOException("Java не найдена. Убедитесь, что Java установлена или используйте JavaRuntimeManager.");
        }
        
        // Путь к директории Minecraft
        String userHome = System.getProperty("user.home");
        Path minecraftDir = Paths.get(userHome, ".minecraft");
        Path versionsDir = minecraftDir.resolve("versions");
        Path librariesDir = minecraftDir.resolve("libraries");
        Path assetsDir = minecraftDir.resolve("assets");
        
        // Проверяем наличие Fabric Loader для указанной версии
        String fabricVersion = "fabric-loader-" + minecraftVersion + "-0.15.0";
        Path fabricVersionDir = versionsDir.resolve(fabricVersion);
        Path fabricJar = fabricVersionDir.resolve(fabricVersion + ".jar");
        Path fabricJson = fabricVersionDir.resolve(fabricVersion + ".json");
        
        if (!Files.exists(fabricJar) || !Files.exists(fabricJson)) {
            logger.warn("Fabric Loader не найден для версии {}. Попытка использовать стандартный запуск...", minecraftVersion);
            // Можно попытаться установить Fabric Loader автоматически
            // или использовать стандартный Minecraft Launcher с модом в classpath
            return startMinecraftStandard(minecraftVersion, username, accessToken, uuid, maxMemory, javaExe, minecraftDir);
        }
        
        // Запускаем Minecraft через Fabric Loader
        logger.info("Запуск Minecraft {} через Fabric Loader", minecraftVersion);
        return startMinecraftWithFabric(minecraftVersion, username, accessToken, uuid, maxMemory, 
                javaExe, minecraftDir, fabricJar, fabricJson);
    }
    
    /**
     * Запускает Minecraft через Fabric Loader
     */
    private Process startMinecraftWithFabric(
            String minecraftVersion,
            String username,
            String accessToken,
            String uuid,
            int maxMemory,
            Path javaExe,
            Path minecraftDir,
            Path fabricJar,
            Path fabricJson
    ) throws IOException {
        List<String> command = new ArrayList<>();
        
        // Java executable
        command.add(javaExe.toAbsolutePath().toString());
        
        // JVM аргументы
        command.add("-Xmx" + maxMemory + "M");
        command.add("-Xms" + (maxMemory / 2) + "M");
        command.add("-Djava.library.path=" + minecraftDir.resolve("natives").toAbsolutePath());
        command.add("-Dminecraft.launcher.brand=horizon-launcher");
        command.add("-Dminecraft.launcher.version=1.0.0");
        
        // Classpath: Fabric Loader и все библиотеки
        StringBuilder classpath = new StringBuilder();
        classpath.append(fabricJar.toAbsolutePath());
        classpath.append(System.getProperty("path.separator"));
        
        // Добавляем библиотеки Minecraft (упрощенная версия)
        Path librariesPath = minecraftDir.resolve("libraries");
        if (Files.exists(librariesPath)) {
            try {
                try (java.util.stream.Stream<Path> stream = Files.walk(librariesPath)) {
                    stream
                            .filter(path -> path.toString().endsWith(".jar"))
                            .limit(100) // Ограничение для производительности
                            .forEach(path -> {
                                classpath.append(System.getProperty("path.separator"));
                                classpath.append(path.toAbsolutePath());
                            });
                }
            } catch (IOException e) {
                logger.warn("Ошибка при добавлении библиотек в classpath", e);
            }
        }
        
        command.add("-cp");
        command.add(classpath.toString());
        
        // Главный класс Fabric Loader
        command.add("net.fabricmc.loader.launch.knot.KnotClient");
        
        // Аргументы Minecraft
        command.add("--username");
        command.add(username);
        command.add("--version");
        command.add("fabric-loader-" + minecraftVersion + "-0.15.0");
        command.add("--gameDir");
        command.add(minecraftDir.toAbsolutePath().toString());
        command.add("--assetsDir");
        command.add(minecraftDir.resolve("assets").toAbsolutePath().toString());
        command.add("--assetIndex");
        command.add(minecraftVersion); // Упрощенно
        command.add("--uuid");
        command.add(uuid);
        command.add("--accessToken");
        command.add(accessToken);
        command.add("--userType");
        command.add("mojang");
        command.add("--versionType");
        command.add("fabric-loader-" + minecraftVersion);
        
        logger.info("Запуск команды: {}", String.join(" ", command));
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(minecraftDir.toFile());
        
        // Перенаправляем вывод
        File logFile = minecraftDir.resolve("logs").resolve("horizon-launcher.log").toFile();
        logFile.getParentFile().mkdirs();
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(logFile);
        
        return processBuilder.start();
    }
    
    /**
     * Запускает Minecraft стандартным способом (без Fabric Loader)
     * Использует LaunchBuilder для формирования команды запуска
     */
    private Process startMinecraftStandard(
            String minecraftVersion,
            String username,
            String accessToken,
            String uuid,
            int maxMemory,
            Path javaExe,
            Path minecraftDir
    ) throws IOException {
        logger.info("Запуск Minecraft {} стандартным способом (без Fabric Loader)", minecraftVersion);
        logger.info("Мод скопирован в .minecraft/mods/ и будет загружен, если используется Fabric Loader");
        
        // Используем LaunchBuilder для формирования команды
        // Для стандартного запуска используем главный класс Minecraft напрямую
        return startMinecraftWithBuilder(minecraftVersion, username, accessToken, uuid, maxMemory, javaExe);
    }
    
    /**
     * Находит исполняемый файл Java
     */
    private Path findJavaExecutable() {
        // Сначала пытаемся найти системную Java
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            Path javaExe = Paths.get(javaHome, "bin", 
                    System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java");
            if (Files.exists(javaExe)) {
                return javaExe;
            }
        }
        
        // Пытаемся найти Java через PATH
        try {
            Process whichJava = new ProcessBuilder(
                    System.getProperty("os.name").toLowerCase().contains("win") ? "where" : "which",
                    "java"
            ).start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(whichJava.getInputStream()))) {
                String path = reader.readLine();
                if (path != null && !path.trim().isEmpty()) {
                    Path javaPath = Paths.get(path.trim());
                    if (Files.exists(javaPath)) {
                        return javaPath;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Не удалось найти Java через PATH", e);
        }
        
        // Пытаемся использовать JavaRuntimeManager
        try {
            com.horizon.launcher.runtime.JavaRuntimeManager runtimeManager = 
                    com.horizon.launcher.runtime.JavaRuntimeManager.getInstance();
            return runtimeManager.getJavaExecutable();
        } catch (Exception e) {
            logger.debug("Не удалось получить Java из JavaRuntimeManager", e);
        }
        
        return null;
    }
    
    /**
     * Проверяет, установлен ли Fabric Loader для указанной версии Minecraft
     */
    public boolean isFabricLoaderInstalled(String minecraftVersion) {
        String userHome = System.getProperty("user.home");
        Path versionsDir = Paths.get(userHome, ".minecraft", "versions");
        Path fabricVersionDir = versionsDir.resolve("fabric-loader-" + minecraftVersion);
        
        return Files.exists(fabricVersionDir);
    }
    
    /**
     * Получает путь к директории модов Minecraft
     */
    public Path getMinecraftModsDir() {
        return minecraftModsDir;
    }
    
    /**
     * Получает путь к исходникам мода
     */
    public Path getModSourceDir() {
        return modSourceDir;
    }
}
