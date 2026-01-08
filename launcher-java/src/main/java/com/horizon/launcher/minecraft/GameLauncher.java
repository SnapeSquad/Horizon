package com.horizon.launcher.minecraft;

import com.horizon.launcher.runtime.JavaRuntimeManager;
import com.horizon.launcher.util.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Класс для запуска Minecraft
 */
public class GameLauncher {
    private static final Logger logger = LoggerFactory.getLogger(GameLauncher.class);
    private final ConfigManager configManager;
    private Process gameProcess;
    private Consumer<String> progressCallback;

    public GameLauncher() {
        this.configManager = ConfigManager.getInstance();
    }

    public void setProgressCallback(Consumer<String> callback) {
        this.progressCallback = callback;
    }

    /**
     * Запускает игру
     */
    public void launch(String server, String username, int ramInGB) throws IOException {
        logger.info("Запуск Minecraft для сервера: {}, пользователь: {}, RAM: {}GB", 
                   server, username, ramInGB);

        String version = getVersionForServer(server);
        Path gameDir = configManager.getGameDir();
        Files.createDirectories(gameDir);

        // Загружаем версию, если её нет
        VersionManager versionManager = new VersionManager();
        AssetManager assetManager = new AssetManager();
        
        if (progressCallback != null) {
            progressCallback.accept("Проверка версии Minecraft...");
        }
        versionManager.ensureVersion(version);
        
        if (progressCallback != null) {
            progressCallback.accept("Загрузка ассетов...");
        }
        assetManager.downloadAssets(version);
        
        if (progressCallback != null) {
            progressCallback.accept("Распаковка нативов...");
        }
        assetManager.extractNatives(version);
        
        if (progressCallback != null) {
            progressCallback.accept("Версия готова к запуску");
        }

        // Генерируем UUID для оффлайн режима
        UUID playerUUID = generateOfflineUUID(username);
        
        // Используем изолированную Java 21
        Path javaExe;
        try {
            javaExe = JavaRuntimeManager.getInstance().getJavaExecutable();
            logger.info("Используется изолированная Java 21: {}", javaExe);
        } catch (IOException e) {
            logger.error("Не удалось получить Java 21, пробуем системную", e);
            // Фоллбэк на системную Java
            String javaPath = findJava();
            if (javaPath == null) {
                throw new IOException("Java не найдена! Установите Java 17 или выше.", e);
            }
            javaExe = Path.of(javaPath);
        }

        logger.info("Используется Java: {}", javaExe);
        logger.info("Версия Minecraft: {}", version);
        logger.info("UUID игрока: {}", playerUUID);

        // Создаем команду запуска
        List<String> command = buildLaunchCommand(javaExe.toString(), version, username, playerUUID, ramInGB, server, gameDir);
        
        logger.info("Команда запуска: {}", String.join(" ", command));

        // Запускаем процесс
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(gameDir.toFile());
        processBuilder.redirectErrorStream(true);
        
        gameProcess = processBuilder.start();
        
        // Отслеживаем вывод
        new Thread(() -> {
            try (var reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(gameProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.debug("[MINECRAFT] {}", line);
                    if (progressCallback != null) {
                        progressCallback.accept(line);
                    }
                }
            } catch (IOException e) {
                logger.error("Ошибка чтения вывода игры", e);
            }
        }).start();

        // Отслеживаем завершение процесса
        new Thread(() -> {
            try {
                int exitCode = gameProcess.waitFor();
                logger.info("Игра завершена с кодом: {}", exitCode);
            } catch (InterruptedException e) {
                logger.error("Ошибка ожидания завершения игры", e);
            }
        }).start();

        logger.info("Игра успешно запущена, PID: {}", gameProcess.pid());
    }

    private String getVersionForServer(String server) {
        return switch (server) {
            case "anarchy" -> "1.21";
            case "survival" -> "1.21.10";
            default -> "1.21";
        };
    }

    private UUID generateOfflineUUID(String username) {
        // Генерируем UUID на основе имени пользователя (оффлайн режим)
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes());
    }

    private String findJava() {
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            String javaPath = javaHome + File.separator + "bin" + File.separator + "java";
            if (new File(javaPath).exists() || new File(javaPath + ".exe").exists()) {
                return javaPath + (System.getProperty("os.name").toLowerCase().contains("win") ? ".exe" : "");
            }
        }

        // Пробуем найти в PATH
        String[] possiblePaths = {
            "java",
            "java.exe",
            System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
        };

        for (String path : possiblePaths) {
            try {
                Process process = new ProcessBuilder(path, "-version").start();
                if (process.waitFor() == 0) {
                    return path;
                }
            } catch (Exception e) {
                // Продолжаем поиск
            }
        }

        return null;
    }

    private List<String> buildLaunchCommand(String javaPath, String version, String username, 
                                           UUID uuid, int ramInGB, String server, Path gameDir) throws IOException {
        List<String> command = new ArrayList<>();
        
        command.add(javaPath);
        
        // Параметры JVM
        command.add("-Xmx" + ramInGB + "G");
        command.add("-Xms2G");
        
        // Classpath - добавляем все библиотеки
        StringBuilder classpath = new StringBuilder();
        Path librariesDir = gameDir.resolve("libraries");
        if (Files.exists(librariesDir)) {
            try (Stream<Path> paths = Files.walk(librariesDir)) {
                paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".jar"))
                    .forEach(p -> {
                        if (classpath.length() > 0) classpath.append(File.pathSeparator);
                        classpath.append(p.toString());
                    });
            }
        }
        
        // Добавляем клиент JAR
        Path versionJar = gameDir.resolve("versions").resolve(version).resolve(version + ".jar");
        if (Files.exists(versionJar)) {
            if (classpath.length() > 0) classpath.append(File.pathSeparator);
            classpath.append(versionJar.toString());
        }
        
        if (classpath.length() > 0) {
            command.add("-cp");
            command.add(classpath.toString());
        }
        
        // Системные свойства
        command.add("-Djava.library.path=" + gameDir.resolve("natives").toString());
        command.add("-Dminecraft.launcher.brand=horizon");
        command.add("-Dminecraft.launcher.version=1.0.0");
        if (Files.exists(versionJar)) {
            command.add("-Dminecraft.client.jar=" + versionJar.toString());
        }
        
        // Основной класс
        command.add("net.minecraft.client.main.Main");
        
        // Параметры игры
        command.add("--username");
        command.add(username);
        command.add("--version");
        command.add(version);
        command.add("--gameDir");
        command.add(gameDir.toString());
        command.add("--assetsDir");
        command.add(gameDir.resolve("assets").toString());
        command.add("--assetIndex");
        command.add(version);
        command.add("--uuid");
        command.add(uuid.toString());
        command.add("--accessToken");
        command.add("offline_access_token");
        command.add("--userType");
        command.add("legacy");
        command.add("--versionType");
        command.add("release");
        
        // Сервер
        String[] serverConfig = getServerConfig(server);
        command.add("--server");
        command.add(serverConfig[0]);
        command.add("--port");
        command.add(serverConfig[1]);
        
        return command;
    }

    private String[] getServerConfig(String server) {
        return switch (server) {
            case "anarchy" -> new String[]{"anarchy.example.com", "25565"};
            case "survival" -> new String[]{"survival.example.com", "25565"};
            default -> new String[]{"localhost", "25565"};
        };
    }

    public void stop() {
        if (gameProcess != null && gameProcess.isAlive()) {
            gameProcess.destroy();
            logger.info("Процесс игры остановлен");
        }
    }
}

