package com.horizon.launcher.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

/**
 * Система защиты от инжекции читов (Anti-Inject)
 * Проверяет запущенные процессы на наличие известных чит-инжекторов
 */
public class AntiInject {
    private static final Logger logger = LoggerFactory.getLogger(AntiInject.class);
    private static AntiInject instance;
    
    private final ExecutorService executorService;
    
    /**
     * Известные имена процессов чит-инжекторов
     * Список должен регулярно обновляться
     */
    private static final Set<String> KNOWN_INJECTOR_PROCESSES = new HashSet<>(Arrays.asList(
            // Популярные инжекторы
            "vape", "vape.gg", "vape_lite",
            "ghost_client", "ghostclient",
            "wurst", "wurstclient",
            "impact", "impactclient",
            "meteor", "meteorclient",
            "hacked", "hackclient",
            "cheatbreaker", "cheatbreakerplus",
            "sigma", "sigmaclient",
            "rusherhack", "rusherhackclient",
            "future", "futureclient",
            "kami", "kamiblue",
            "seppuku", "seppukuclient",
            
            // Инжекторы DLL
            "dllinject", "dll_injector",
            "winject", "xenos",
            "processhacker", "procmon",
            
            // Другие подозрительные процессы
            "cheatengine", "cheat_engine",
            "artmoney", "tsearch",
            "gameguardian", "gameguard",
            "bytecodeviewer", "jd-gui"
    ));
    
    /**
     * Подозрительные ключевые слова в путях процессов
     */
    private static final Set<String> SUSPICIOUS_KEYWORDS = new HashSet<>(Arrays.asList(
            "hack", "cheat", "crack", "inject", "bypass", "exploit",
            "modmenu", "mod_menu", "xray", "esp", "aimbot"
    ));
    
    /**
     * Системные процессы, которые нужно исключить из проверки
     */
    private static final Set<String> SYSTEM_PROCESSES = new HashSet<>(Arrays.asList(
            // Системные процессы Windows
            "system", "smss.exe", "csrss.exe", "wininit.exe", "winlogon.exe",
            "services.exe", "lsass.exe", "svchost.exe", "explorer.exe",
            "dwm.exe", "taskhostw.exe", "audiodg.exe", "spoolsv.exe",
            
            // Системные процессы Linux
            "init", "systemd", "kthreadd", "ksoftirqd", "migration",
            "rcu_sched", "rcu_bh", "watchdog", "migration",
            
            // Системные процессы macOS
            "kernel_task", "launchd", "UserEventAgent", "cfprefsd",
            "kernel_task", "syslogd", "distnoted", "fseventsd"
    ));
    
    /**
     * Системные пути, которые нужно исключить из проверки
     */
    private static final Set<String> SYSTEM_PATHS = new HashSet<>(Arrays.asList(
            "C:\\Windows\\System32", "C:\\Windows\\SysWOW64",
            "/usr/bin", "/usr/sbin", "/bin", "/sbin",
            "/System/Library", "/usr/libexec"
    ));
    
    /**
     * Результат проверки на инжекторы
     */
    public static class InjectorScanResult {
        private final boolean clean;
        private final List<String> detectedProcesses;
        private final List<String> suspiciousProcesses;
        private final String message;
        
        public InjectorScanResult(boolean clean, List<String> detectedProcesses, 
                                 List<String> suspiciousProcesses, String message) {
            this.clean = clean;
            this.detectedProcesses = detectedProcesses;
            this.suspiciousProcesses = suspiciousProcesses;
            this.message = message;
        }
        
        public boolean isClean() {
            return clean;
        }
        
        public List<String> getDetectedProcesses() {
            return detectedProcesses;
        }
        
        public List<String> getSuspiciousProcesses() {
            return suspiciousProcesses;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    private AntiInject() {
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "AntiInject-Scanner");
            t.setDaemon(true);
            return t;
        });
    }
    
    public static synchronized AntiInject getInstance() {
        if (instance == null) {
            instance = new AntiInject();
        }
        return instance;
    }
    
    /**
     * Асинхронная проверка запущенных процессов на наличие инжекторов
     * 
     * @return CompletableFuture с результатом проверки
     */
    public CompletableFuture<InjectorScanResult> scanForInjectors() {
        logger.info("Начинаем сканирование процессов на наличие инжекторов");
        
        return CompletableFuture.supplyAsync(() -> {
            List<String> detected = new ArrayList<>();
            List<String> suspicious = new ArrayList<>();
            
            try {
                List<ProcessInfo> runningProcesses = getRunningProcesses();
                logger.debug("Найдено {} запущенных процессов", runningProcesses.size());
                
                for (ProcessInfo process : runningProcesses) {
                    String processName = process.getName().toLowerCase();
                    String processPath = process.getPath() != null ? process.getPath().toLowerCase() : "";
                    
                    // Пропускаем системные процессы
                    if (isSystemProcess(processName, processPath)) {
                        continue;
                    }
                    
                    // Проверка на известные инжекторы
                    boolean isKnownInjector = false;
                    for (String injector : KNOWN_INJECTOR_PROCESSES) {
                        String injectorLower = injector.toLowerCase();
                        // Точное совпадение имени или совпадение в пути
                        if (processName.equals(injectorLower) || 
                            processName.startsWith(injectorLower) ||
                            processPath.contains(injectorLower)) {
                            detected.add(process.getName() + " (" + process.getPath() + ")");
                            isKnownInjector = true;
                            logger.warn("Обнаружен известный инжектор: {} ({})", process.getName(), process.getPath());
                            break;
                        }
                    }
                    
                    if (!isKnownInjector) {
                        // Проверка на подозрительные ключевые слова (только в имени, не в пути, чтобы не было ложных срабатываний)
                        for (String keyword : SUSPICIOUS_KEYWORDS) {
                            String keywordLower = keyword.toLowerCase();
                            // Проверяем только имя процесса для подозрительных ключевых слов
                            if (processName.contains(keywordLower)) {
                                suspicious.add(process.getName() + " (" + process.getPath() + ")");
                                logger.debug("Обнаружен подозрительный процесс: {} ({})", process.getName(), process.getPath());
                                break;
                            }
                        }
                    }
                }
                
                boolean clean = detected.isEmpty();
                String message;
                
                if (clean && suspicious.isEmpty()) {
                    message = "Система чиста, инжекторы не обнаружены";
                } else if (clean) {
                    message = String.format("Обнаружено %d подозрительных процессов (не критично)", suspicious.size());
                } else {
                    message = String.format("ВНИМАНИЕ: Обнаружено %d инжекторов и %d подозрительных процессов!", 
                            detected.size(), suspicious.size());
                }
                
                logger.info("Сканирование завершено: {}", message);
                
                return new InjectorScanResult(clean, detected, suspicious, message);
                
            } catch (Exception e) {
                logger.error("Ошибка при сканировании процессов", e);
                return new InjectorScanResult(false, detected, suspicious, 
                        "Ошибка сканирования: " + e.getMessage());
            }
        }, executorService);
    }
    
    /**
     * Получение списка запущенных процессов
     */
    private List<ProcessInfo> getRunningProcesses() throws Exception {
        List<ProcessInfo> processes = new ArrayList<>();
        
        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder processBuilder;
        
        if (os.contains("win")) {
            // Windows: используем wmic или tasklist
            processBuilder = new ProcessBuilder("wmic", "process", "get", "name,executablepath", "/format:csv");
        } else if (os.contains("mac")) {
            // macOS: используем ps
            processBuilder = new ProcessBuilder("ps", "-eo", "comm=,args=");
        } else {
            // Linux: используем ps
            processBuilder = new ProcessBuilder("ps", "-eo", "comm=,args=");
        }
        
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            boolean isFirstLine = true;
            
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                // Пропускаем заголовок CSV (Windows)
                if (os.contains("win") && isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                
                ProcessInfo processInfo = parseProcessLine(line, os);
                if (processInfo != null) {
                    processes.add(processInfo);
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.warn("Процесс получения списка процессов завершился с кодом: {}", exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Exception("Прервано получение списка процессов", e);
        }
        
        return processes;
    }
    
    /**
     * Проверка, является ли процесс системным
     */
    private boolean isSystemProcess(String processName, String processPath) {
        // Проверка по имени
        for (String systemProc : SYSTEM_PROCESSES) {
            if (processName.equals(systemProc.toLowerCase()) || 
                processName.startsWith(systemProc.toLowerCase() + ".")) {
                return true;
            }
        }
        
        // Проверка по пути
        if (processPath != null && !processPath.isEmpty()) {
            for (String systemPath : SYSTEM_PATHS) {
                if (processPath.contains(systemPath.toLowerCase())) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Парсинг строки процесса в зависимости от ОС
     */
    private ProcessInfo parseProcessLine(String line, String os) {
        try {
            if (os.contains("win")) {
                // Windows CSV формат: Node,ExecutablePath,Name,...
                // Пример: "COMPUTERNAME,C:\\Windows\\System32\\notepad.exe,notepad.exe"
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String path = parts[1].trim();
                    String name = parts[2].trim();
                    
                    // Пропускаем пустые строки и заголовки
                    if (!path.isEmpty() && !name.isEmpty() && !name.equals("Name")) {
                        // Извлекаем только имя файла из пути, если имя не указано
                        if (name.isEmpty() && !path.isEmpty()) {
                            int lastSeparator = Math.max(path.lastIndexOf('\\'), path.lastIndexOf('/'));
                            name = lastSeparator >= 0 ? path.substring(lastSeparator + 1) : path;
                        }
                        return new ProcessInfo(name, path);
                    }
                }
            } else {
                // Linux/Mac: формат "comm args" или "pid comm args"
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    return null;
                }
                
                String[] parts = trimmed.split("\\s+", 3);
                if (parts.length >= 1) {
                    // Если первый элемент - число (PID), пропускаем его
                    String name;
                    String path;
                    
                    try {
                        Integer.parseInt(parts[0]);
                        // Первый элемент - PID, берем второй как имя
                        if (parts.length >= 2) {
                            name = parts[1];
                            path = parts.length >= 3 ? parts[2] : "";
                        } else {
                            return null;
                        }
                    } catch (NumberFormatException e) {
                        // Первый элемент - имя процесса
                        name = parts[0];
                        path = parts.length >= 2 ? parts[1] : "";
                    }
                    
                    // Фильтруем пустые имена
                    if (!name.isEmpty() && name.length() > 0 && name.length() < 256) {
                        return new ProcessInfo(name, path);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Ошибка при парсинге строки процесса: {}", line, e);
        }
        
        return null;
    }
    
    /**
     * Информация о процессе
     */
    private static class ProcessInfo {
        private final String name;
        private final String path;
        
        public ProcessInfo(String name, String path) {
            this.name = name;
            this.path = path;
        }
        
        public String getName() {
            return name;
        }
        
        public String getPath() {
            return path;
        }
    }
    
    /**
     * Добавление известного инжектора в список (для обновления)
     */
    public static void addKnownInjector(String injectorName) {
        KNOWN_INJECTOR_PROCESSES.add(injectorName.toLowerCase());
    }
    
    /**
     * Остановка executor service
     */
    public void shutdown() {
        executorService.shutdown();
    }
}
