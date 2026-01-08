package com.horizon.launcher.updater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Главный класс для Updater.jar
 * 
 * Этот класс запускается отдельным процессом для замены Launcher.exe
 * Аргументы командной строки:
 * args[0] - путь к текущему Launcher.exe
 * args[1] - путь к новому Launcher.exe
 */
public class UpdaterMain {
    private static final Logger logger = LoggerFactory.getLogger(UpdaterMain.class);
    
    public static void main(String[] args) {
        if (args.length < 2) {
            logger.error("Недостаточно аргументов. Ожидается: <текущий_лаунчер> <новый_лаунчер>");
            System.exit(1);
        }
        
        String currentLauncher = args[0];
        String newLauncher = args[1];
        
        logger.info("Updater запущен");
        logger.info("Текущий лаунчер: {}", currentLauncher);
        logger.info("Новый лаунчер: {}", newLauncher);
        
        try {
            // 1. Ждем, пока текущий лаунчер закроется
            waitForFileUnlock(currentLauncher, 30); // Ждем до 30 секунд
            
            // 2. Создаем резервную копию текущего лаунчера
            String backupPath = currentLauncher + ".backup";
            logger.info("Создание резервной копии: {}", backupPath);
            Files.copy(Paths.get(currentLauncher), Paths.get(backupPath), 
                    StandardCopyOption.REPLACE_EXISTING);
            
            // 3. Заменяем текущий лаунчер новым
            logger.info("Замена лаунчера...");
            Files.copy(Paths.get(newLauncher), Paths.get(currentLauncher), 
                    StandardCopyOption.REPLACE_EXISTING);
            
            // 4. Удаляем временный файл нового лаунчера
            logger.info("Удаление временного файла...");
            Files.deleteIfExists(Paths.get(newLauncher));
            
            // 5. Запускаем новый лаунчер
            logger.info("Запуск нового лаунчера...");
            ProcessBuilder processBuilder = new ProcessBuilder(currentLauncher);
            processBuilder.start();
            
            // 6. Удаляем резервную копию через 5 секунд (после успешного запуска)
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    Files.deleteIfExists(Paths.get(backupPath));
                    logger.info("Резервная копия удалена");
                } catch (Exception e) {
                    logger.warn("Не удалось удалить резервную копию", e);
                }
            }).start();
            
            logger.info("Обновление завершено успешно");
            System.exit(0);
            
        } catch (Exception e) {
            logger.error("Ошибка при обновлении", e);
            System.exit(1);
        }
    }
    
    /**
     * Ждет, пока файл будет разблокирован (процесс закроется)
     */
    private static void waitForFileUnlock(String filePath, int maxSeconds) throws InterruptedException {
        int waited = 0;
        
        while (waited < maxSeconds) {
            try {
                // Пытаемся открыть файл на запись - если получится, значит он разблокирован
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(
                        new File(filePath), true)) {
                    logger.info("Файл разблокирован");
                    return;
                }
            } catch (IOException e) {
                // Файл все еще заблокирован
                logger.debug("Файл все еще заблокирован, ждем... ({}/{})", waited, maxSeconds);
                Thread.sleep(1000);
                waited++;
            }
        }
        
        logger.warn("Файл не разблокирован за {} секунд, продолжаем обновление", maxSeconds);
    }
}

