package com.horizon.launcher.util;

import com.google.gson.JsonObject;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.software.os.OperatingSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Логгер с интеграцией Discord Webhooks
 * 
 * При возникновении Exception собирает:
 * - Версию ОС
 * - Текущий RAM
 * - HWID
 * - Последние 50 строк лога
 * 
 * Отправляет всё в Discord через Webhook в красивом Embed сообщении
 */
public class DiscordLogger {
    private static final Logger logger = LoggerFactory.getLogger(DiscordLogger.class);
    private static DiscordLogger instance;
    private final String webhookUrl;
    private final SystemInfo systemInfo;
    private final Path logFile;
    
    private DiscordLogger() {
        this.webhookUrl = ConfigManager.getInstance().get("discord.webhook.url", "");
        this.systemInfo = new SystemInfo();
        this.logFile = ConfigManager.getInstance().getLauncherDir().resolve("launcher.log");
    }
    
    public static DiscordLogger getInstance() {
        if (instance == null) {
            instance = new DiscordLogger();
        }
        return instance;
    }
    
    /**
     * Отправляет Exception в Discord
     */
    public void logException(Throwable exception, String context) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            logger.debug("Discord Webhook URL не настроен, пропуск отправки");
            return;
        }
        
        try {
            JsonObject embed = buildExceptionEmbed(exception, context);
            sendToDiscord(embed);
        } catch (Exception e) {
            logger.error("Ошибка при отправке лога в Discord", e);
        }
    }
    
    /**
     * Отправляет обычное сообщение в Discord
     */
    public void logMessage(String title, String message, int color) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            return;
        }
        
        try {
            JsonObject embed = new JsonObject();
            embed.addProperty("title", title);
            embed.addProperty("description", message);
            embed.addProperty("color", color);
            embed.addProperty("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            
            sendToDiscord(embed);
        } catch (Exception e) {
            logger.error("Ошибка при отправке сообщения в Discord", e);
        }
    }
    
    /**
     * Строит Embed для Exception
     */
    private JsonObject buildExceptionEmbed(Throwable exception, String context) {
        JsonObject embed = new JsonObject();
        embed.addProperty("title", "🚨 Exception в Horizon Launcher");
        embed.addProperty("color", 15158332); // Красный цвет
        
        StringBuilder description = new StringBuilder();
        description.append("**Контекст:** ").append(context != null ? context : "Не указан").append("\n\n");
        description.append("**Исключение:** `").append(exception.getClass().getName()).append("`\n");
        description.append("**Сообщение:** ").append(exception.getMessage() != null ? exception.getMessage() : "Нет сообщения").append("\n\n");
        
        // Stack trace (первые 10 строк)
        StackTraceElement[] stackTrace = exception.getStackTrace();
        if (stackTrace.length > 0) {
            description.append("**Stack Trace:**\n```\n");
            int maxLines = Math.min(10, stackTrace.length);
            for (int i = 0; i < maxLines; i++) {
                description.append(stackTrace[i].toString()).append("\n");
            }
            if (stackTrace.length > 10) {
                description.append("... и еще ").append(stackTrace.length - 10).append(" строк\n");
            }
            description.append("```\n\n");
        }
        
        // Системная информация в fields (Discord формат)
        com.google.gson.JsonArray fieldsArray = new com.google.gson.JsonArray();
        
        // Версия ОС
        try {
            OperatingSystem os = systemInfo.getOperatingSystem();
            String osInfo = os.getFamily() + " " + os.getVersionInfo().getVersion();
            addFieldToArray(fieldsArray, "OS", osInfo, false);
        } catch (Exception e) {
            addFieldToArray(fieldsArray, "OS", "Не удалось определить", false);
        }
        
        // RAM
        try {
            GlobalMemory memory = systemInfo.getHardware().getMemory();
            long totalRAM = memory.getTotal();
            long availableRAM = memory.getAvailable();
            String ramInfo = String.format("%.2f GB / %.2f GB", 
                (totalRAM - availableRAM) / (1024.0 * 1024.0 * 1024.0),
                totalRAM / (1024.0 * 1024.0 * 1024.0));
            addFieldToArray(fieldsArray, "RAM", ramInfo, false);
        } catch (Exception e) {
            addFieldToArray(fieldsArray, "RAM", "Не удалось определить", false);
        }
        
        // HWID
        try {
            String hwid = HWIDManager.getInstance().getHWID();
            addFieldToArray(fieldsArray, "HWID", hwid.substring(0, Math.min(32, hwid.length())) + "...", false);
        } catch (Exception e) {
            addFieldToArray(fieldsArray, "HWID", "Не удалось определить", false);
        }
        
        // Последние строки лога
        try {
            List<String> lastLogLines = getLastLogLines(20); // Уменьшено до 20 для экономии места
            if (!lastLogLines.isEmpty()) {
                StringBuilder logText = new StringBuilder();
                for (String line : lastLogLines) {
                    logText.append(line).append("\n");
                }
                // Discord ограничение на длину поля - 1024 символа
                String logContent = logText.toString();
                if (logContent.length() > 1000) {
                    logContent = logContent.substring(0, 997) + "...";
                }
                addFieldToArray(fieldsArray, "Последние строки лога", "```\n" + logContent + "```", true);
            }
        } catch (Exception e) {
            logger.debug("Не удалось прочитать лог файл", e);
        }
        
        embed.addProperty("description", description.toString());
        embed.add("fields", fieldsArray);
        embed.addProperty("timestamp", LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_DATE_TIME));
        
        JsonObject footer = new JsonObject();
        footer.addProperty("text", "Horizon Launcher v1.0.0");
        embed.add("footer", footer);
        
        return embed;
    }
    
    /**
     * Добавляет поле в массив fields для Discord Embed
     */
    private void addFieldToArray(com.google.gson.JsonArray fieldsArray, String name, String value, boolean inline) {
        JsonObject field = new JsonObject();
        field.addProperty("name", name);
        field.addProperty("value", value);
        field.addProperty("inline", inline);
        fieldsArray.add(field);
    }
    
    /**
     * Получает последние N строк из лог файла
     */
    private List<String> getLastLogLines(int count) {
        List<String> lines = new ArrayList<>();
        
        if (!Files.exists(logFile)) {
            return lines;
        }
        
        try {
            List<String> allLines = Files.readAllLines(logFile);
            int startIndex = Math.max(0, allLines.size() - count);
            lines = allLines.subList(startIndex, allLines.size());
        } catch (IOException e) {
            logger.debug("Ошибка чтения лог файла", e);
        }
        
        return lines;
    }
    
    /**
     * Отправляет Embed в Discord через Webhook
     */
    private void sendToDiscord(JsonObject embed) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            return;
        }
        
        try {
            // Формируем JSON для Discord Webhook
            JsonObject payload = new JsonObject();
            
            // Discord ожидает массив embeds
            com.google.gson.JsonArray embedsArray = new com.google.gson.JsonArray();
            embedsArray.add(embed);
            payload.add("embeds", embedsArray);
            
            // Используем Java 21 HttpClient для отправки
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                logger.debug("Лог успешно отправлен в Discord");
            } else {
                logger.warn("Discord Webhook вернул код: {}", response.statusCode());
            }
        } catch (Exception e) {
            logger.error("Ошибка отправки в Discord Webhook", e);
        }
    }
}

