package com.horizon.launcher.minecraft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Построитель команды запуска для Minecraft
 * Формирует правильные аргументы JVM и команды для запуска игры
 */
public class LaunchBuilder {
    private static final Logger logger = LoggerFactory.getLogger(LaunchBuilder.class);
    
    /**
     * Тип сервера для запуска
     */
    public enum ServerType {
        ANARCHY("Анархия", "1.21"),
        SURVIVAL("Выживание", "1.21.10");
        
        private final String displayName;
        private final String minecraftVersion;
        
        ServerType(String displayName, String minecraftVersion) {
            this.displayName = displayName;
            this.minecraftVersion = minecraftVersion;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getMinecraftVersion() {
            return minecraftVersion;
        }
        
        public static ServerType fromVersion(String version) {
            if (version == null || version.isEmpty()) {
                return ANARCHY; // По умолчанию
            }
            
            // Сравниваем версии в порядке от наиболее специфичной к общей
            // Сначала проверяем точное совпадение для версии выживания
            if (version.equals("1.21.10")) {
                return SURVIVAL;
            }
            
            // Для версии 1.21 или других версий 1.21.x - анархия
            if (version.equals("1.21") || version.startsWith("1.21.")) {
                return ANARCHY;
            }
            
            // По умолчанию - анархия
            return ANARCHY;
        }
    }
    
    private String minecraftVersion;
    private String username;
    private String accessToken;
    private String uuid;
    private String clientId;
    private int maxMemoryMB;
    private int minMemoryMB;
    private Path javaExecutable;
    private Path gameDirectory;
    private Path assetsDirectory;
    private Path versionsDirectory;
    private Path librariesDirectory;
    private Path nativesDirectory;
    private String versionType;
    private ServerType serverType;
    private Map<String, String> customProperties;
    private List<String> additionalJvmArgs;
    private List<String> additionalGameArgs;
    
    private LaunchBuilder() {
        this.customProperties = new HashMap<>();
        this.additionalJvmArgs = new ArrayList<>();
        this.additionalGameArgs = new ArrayList<>();
        
        // Устанавливаем стандартные пути
        String userHome = System.getProperty("user.home");
        this.gameDirectory = Paths.get(userHome, ".minecraft");
        this.assetsDirectory = gameDirectory.resolve("assets");
        this.versionsDirectory = gameDirectory.resolve("versions");
        this.librariesDirectory = gameDirectory.resolve("libraries");
        this.nativesDirectory = gameDirectory.resolve("natives");
        
        // Значения по умолчанию
        this.minMemoryMB = 1024;
        this.maxMemoryMB = 4096;
        this.versionType = "horizon-launcher";
        this.clientId = UUID.randomUUID().toString();
    }
    
    public static LaunchBuilder create() {
        return new LaunchBuilder();
    }
    
    public LaunchBuilder version(String version) {
        this.minecraftVersion = version;
        // Автоматически определяем тип сервера по версии
        this.serverType = ServerType.fromVersion(version);
        return this;
    }
    
    /**
     * Устанавливает тип сервера и версию Minecraft
     * @param serverType Тип сервера (Анархия или Выживание)
     * @return LaunchBuilder для цепочки вызовов
     */
    public LaunchBuilder serverType(ServerType serverType) {
        this.serverType = serverType;
        this.minecraftVersion = serverType.getMinecraftVersion();
        return this;
    }
    
    /**
     * Получает тип сервера
     */
    public ServerType getServerType() {
        return serverType != null ? serverType : ServerType.fromVersion(minecraftVersion);
    }
    
    public LaunchBuilder username(String username) {
        this.username = username;
        return this;
    }
    
    public LaunchBuilder accessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }
    
    public LaunchBuilder uuid(String uuid) {
        this.uuid = uuid;
        return this;
    }
    
    public LaunchBuilder clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }
    
    public LaunchBuilder memory(int minMB, int maxMB) {
        this.minMemoryMB = minMB;
        this.maxMemoryMB = maxMB;
        return this;
    }
    
    public LaunchBuilder javaExecutable(Path javaExe) {
        this.javaExecutable = javaExe;
        return this;
    }
    
    public LaunchBuilder gameDirectory(Path dir) {
        this.gameDirectory = dir;
        return this;
    }
    
    public LaunchBuilder versionType(String type) {
        this.versionType = type;
        return this;
    }
    
    public LaunchBuilder customProperty(String key, String value) {
        this.customProperties.put(key, value);
        return this;
    }
    
    public LaunchBuilder addJvmArg(String arg) {
        this.additionalJvmArgs.add(arg);
        return this;
    }
    
    public LaunchBuilder addGameArg(String arg) {
        this.additionalGameArgs.add(arg);
        return this;
    }
    
    /**
     * Формирует полную команду запуска
     * 
     * @return Список аргументов команды
     */
    public List<String> build() {
        if (javaExecutable == null) {
            throw new IllegalStateException("Java executable не установлен");
        }
        
        if (minecraftVersion == null) {
            throw new IllegalStateException("Версия Minecraft не установлена");
        }
        
        List<String> command = new ArrayList<>();
        
        // 1. Java executable
        command.add(javaExecutable.toAbsolutePath().toString());
        
        // 2. JVM аргументы (память)
        command.add("-Xmx" + maxMemoryMB + "M");
        command.add("-Xms" + minMemoryMB + "M");
        
        // 3. Системные свойства (-D)
        command.add("-Djava.library.path=" + nativesDirectory.toAbsolutePath().toString());
        command.add("-Dminecraft.launcher.brand=horizon-launcher");
        command.add("-Dminecraft.launcher.version=1.0.0");
        command.add("-Dminecraft.client.jar=" + getVersionJarPath().toAbsolutePath().toString());
        
        // Кастомные свойства (включая токен Horizon)
        if (accessToken != null) {
            command.add("-Dhorizon.token=" + accessToken);
        }
        for (Map.Entry<String, String> prop : customProperties.entrySet()) {
            command.add("-D" + prop.getKey() + "=" + prop.getValue());
        }
        
        // 4. Classpath для библиотек
        String classpath = buildClasspath();
        command.add("-cp");
        command.add(classpath);
        
        // 5. Дополнительные JVM аргументы
        command.addAll(additionalJvmArgs);
        
        // 6. Главный класс
        command.add("net.minecraft.client.main.Main");
        
        // 7. Аргументы игры
        command.addAll(buildGameArguments());
        command.addAll(additionalGameArgs);
        
        return command;
    }
    
    /**
     * Формирует classpath из библиотек
     */
    private String buildClasspath() {
        List<String> classpathEntries = new ArrayList<>();
        Set<String> addedJars = new HashSet<>(); // Для предотвращения дубликатов
        
        // Добавляем версионный JAR
        Path versionJar = getVersionJarPath();
        if (java.nio.file.Files.exists(versionJar)) {
            String jarPath = versionJar.toAbsolutePath().toString();
            classpathEntries.add(jarPath);
            addedJars.add(jarPath.toLowerCase());
            logger.debug("Добавлен версионный JAR в classpath: {}", versionJar);
        } else {
            logger.warn("Версионный JAR не найден: {}", versionJar);
        }
        
        // Добавляем все библиотеки из libraries/
        try {
            if (java.nio.file.Files.exists(librariesDirectory)) {
                try (java.util.stream.Stream<Path> stream = java.nio.file.Files.walk(librariesDirectory, 10)) {
                    stream
                            .filter(path -> {
                                String fileName = path.getFileName().toString();
                                return fileName.endsWith(".jar") && java.nio.file.Files.isRegularFile(path);
                            })
                            .forEach(path -> {
                                String jarPath = path.toAbsolutePath().toString();
                                String jarPathLower = jarPath.toLowerCase();
                                
                                // Избегаем дубликатов
                                if (!addedJars.contains(jarPathLower)) {
                                    classpathEntries.add(jarPath);
                                    addedJars.add(jarPathLower);
                                }
                            });
                }
                
                logger.debug("Добавлено {} библиотек в classpath", classpathEntries.size() - 1);
            } else {
                logger.warn("Директория библиотек не существует: {}", librariesDirectory);
            }
        } catch (Exception e) {
            logger.error("Ошибка при построении classpath библиотек", e);
            // Не прерываем выполнение, продолжаем с тем, что есть
        }
        
        if (classpathEntries.isEmpty()) {
            logger.error("Classpath пуст! Игра не сможет запуститься.");
            throw new IllegalStateException("Classpath пуст. Проверьте наличие версионного JAR и библиотек.");
        }
        
        // Используем правильный разделитель для пути
        String separator = System.getProperty("path.separator");
        String classpath = String.join(separator, classpathEntries);
        
        // Проверяем максимальную длину classpath (Windows ограничение ~8191 символов)
        if (classpath.length() > 8000) {
            logger.warn("Classpath очень длинный: {} символов (может вызвать проблемы на Windows)", classpath.length());
        }
        
        return classpath;
    }
    
    /**
     * Формирует аргументы игры
     */
    private List<String> buildGameArguments() {
        List<String> args = new ArrayList<>();
        
        // Стандартные аргументы Minecraft
        args.add("--username");
        args.add(username != null ? username : "Player");
        
        args.add("--version");
        args.add(minecraftVersion);
        
        args.add("--gameDir");
        args.add(gameDirectory.toAbsolutePath().toString());
        
        args.add("--assetsDir");
        args.add(assetsDirectory.toAbsolutePath().toString());
        
        args.add("--assetIndex");
        args.add(getAssetIndex());
        
        if (uuid != null) {
            args.add("--uuid");
            args.add(uuid);
        }
        
        if (accessToken != null) {
            args.add("--accessToken");
            args.add(accessToken);
        }
        
        args.add("--userType");
        args.add("mojang");
        
        // Добавляем информацию о типе сервера в versionType
        String finalVersionType = versionType;
        if (serverType != null) {
            String serverTypeName = serverType.getDisplayName().toLowerCase().replace(" ", "-");
            finalVersionType = versionType + "-" + serverTypeName;
        }
        args.add("--versionType");
        args.add(finalVersionType);
        
        if (clientId != null) {
            args.add("--clientId");
            args.add(clientId);
        }
        
        // Ширина и высота окна (можно сделать настраиваемыми)
        args.add("--width");
        args.add("854");
        args.add("--height");
        args.add("480");
        
        return args;
    }
    
    /**
     * Получает путь к версионному JAR файлу
     */
    private Path getVersionJarPath() {
        Path versionDir = versionsDirectory.resolve(minecraftVersion);
        Path jarPath = versionDir.resolve(minecraftVersion + ".jar");
        
        // Если стандартный путь не существует, пытаемся найти любой JAR в директории версии
        if (!java.nio.file.Files.exists(jarPath)) {
            try {
                Optional<Path> foundJar = java.nio.file.Files.list(versionDir)
                        .filter(path -> path.toString().endsWith(".jar"))
                        .findFirst();
                if (foundJar.isPresent()) {
                    return foundJar.get();
                }
            } catch (IOException e) {
                logger.warn("Ошибка при поиске JAR файла версии: {}", minecraftVersion, e);
            }
        }
        
        return jarPath;
    }
    
    /**
     * Получает индекс ассетов для версии
     * Для версий 1.21 и 1.21.10 используется индекс 5 (1.21 asset index)
     * В реальной реализации нужно читать из version.json
     */
    private String getAssetIndex() {
        if (minecraftVersion == null) {
            logger.warn("Версия Minecraft не установлена, используется индекс по умолчанию");
            return "5"; // По умолчанию для 1.21
        }
        
        // Для версий 1.21 и 1.21.10 используется индекс 5 (Minecraft 1.21 asset index)
        if (minecraftVersion.equals("1.21") || minecraftVersion.equals("1.21.10") || minecraftVersion.startsWith("1.21.")) {
            logger.debug("Используется asset index 5 для версии {}", minecraftVersion);
            return "5";
        }
        
        // Для других версий (если будут добавлены в будущем)
        if (minecraftVersion.startsWith("1.20")) {
            return "4"; // Minecraft 1.20
        } else if (minecraftVersion.startsWith("1.19")) {
            return "3"; // Minecraft 1.19
        } else if (minecraftVersion.startsWith("1.18")) {
            return "2"; // Minecraft 1.18
        } else if (minecraftVersion.startsWith("1.17")) {
            return "1"; // Minecraft 1.17
        }
        
        logger.warn("Не удалось определить индекс ассетов для версии: {}, используется значение по умолчанию 5", minecraftVersion);
        return "5"; // По умолчанию для 1.21
    }
    
    /**
     * Получает полную команду запуска как строку (для логирования)
     */
    public String buildCommandString() {
        List<String> command = build();
        return String.join(" ", command);
    }
    
    /**
     * Валидация параметров перед построением команды
     */
    public void validate() {
        if (javaExecutable == null) {
            throw new IllegalStateException("Java executable не установлен");
        }
        if (!java.nio.file.Files.exists(javaExecutable)) {
            throw new IllegalStateException("Java executable не существует: " + javaExecutable);
        }
        if (!java.nio.file.Files.isExecutable(javaExecutable)) {
            throw new IllegalStateException("Java executable не является исполняемым: " + javaExecutable);
        }
        
        if (minecraftVersion == null || minecraftVersion.isEmpty()) {
            throw new IllegalStateException("Версия Minecraft не указана");
        }
        
        // Валидация версий для серверов
        if (minecraftVersion != null) {
            if (!minecraftVersion.equals("1.21") && !minecraftVersion.equals("1.21.10")) {
                logger.warn("Версия Minecraft {} не является поддерживаемой для серверов Horizon (1.21 или 1.21.10)", minecraftVersion);
            }
            
            // Проверяем, что версия соответствует типу сервера
            ServerType detectedType = ServerType.fromVersion(minecraftVersion);
            if (serverType != null && serverType != detectedType) {
                logger.warn("Несоответствие версии {} и типа сервера {}", minecraftVersion, serverType);
            }
            
            // Валидация формата версии
            if (!minecraftVersion.matches("\\d+\\.\\d+(\\.\\d+)?(-.*)?")) {
                logger.warn("Версия Minecraft имеет нестандартный формат: {}", minecraftVersion);
            }
        }
        
        if (username == null || username.isEmpty()) {
            throw new IllegalStateException("Имя пользователя не указано");
        }
        if (username.length() > 16) {
            throw new IllegalStateException("Имя пользователя слишком длинное (максимум 16 символов)");
        }
        if (!username.matches("[a-zA-Z0-9_]+")) {
            logger.warn("Имя пользователя содержит нестандартные символы: {}", username);
        }
        
        if (accessToken == null || accessToken.isEmpty()) {
            logger.warn("Access token не указан, игра может не запуститься");
        }
        
        if (maxMemoryMB < minMemoryMB) {
            throw new IllegalStateException("Максимальная память меньше минимальной");
        }
        if (minMemoryMB < 512) {
            throw new IllegalStateException("Минимальная память слишком мала (минимум 512 МБ)");
        }
        if (maxMemoryMB < 512) {
            throw new IllegalStateException("Максимальная память слишком мала (минимум 512 МБ)");
        }
        if (maxMemoryMB > 16384) {
            logger.warn("Максимальная память очень большая: {} МБ (может вызвать проблемы)", maxMemoryMB);
        }
        
        // Валидация директорий
        if (gameDirectory != null && !java.nio.file.Files.exists(gameDirectory)) {
            try {
                java.nio.file.Files.createDirectories(gameDirectory);
                logger.info("Создана директория игры: {}", gameDirectory);
            } catch (IOException e) {
                throw new IllegalStateException("Не удалось создать директорию игры: " + gameDirectory, e);
            }
        }
        
        // Проверка версионного JAR
        Path versionJar = getVersionJarPath();
        if (!java.nio.file.Files.exists(versionJar)) {
            logger.warn("Версионный JAR не найден: {} (игра может не запуститься)", versionJar);
        }
    }
}
