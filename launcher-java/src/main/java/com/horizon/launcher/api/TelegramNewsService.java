package com.horizon.launcher.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Сервис для получения новостей из Telegram канала
 */
public class TelegramNewsService {
    private static final Logger logger = LoggerFactory.getLogger(TelegramNewsService.class);
    private static final String TELEGRAM_API_BASE = "https://api.telegram.org/bot";
    private static final String CHANNEL_USERNAME = "Hor1zonNews";
    private final OkHttpClient client;
    private String botToken; // Можно использовать публичный бот или токен из конфига

    public TelegramNewsService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        // Для публичных каналов можно использовать публичный бот
        // Или получить токен из конфига
        this.botToken = System.getProperty("telegram.bot.token", "");
    }

    /**
     * Получает последние новости из канала
     */
    public List<NewsItem> getLatestNews(int count) {
        List<NewsItem> news = new ArrayList<>();
        
        try {
            // Используем публичный RSS или парсинг через Telegram API
            // Для публичных каналов можно использовать getUpdates или getChannelHistory
            
            // Альтернативный способ: парсинг через веб-интерфейс Telegram
            // Или использование RSS фида канала (если доступен)
            
            // Временная заглушка - в реальности нужно использовать Telegram Bot API
            // или парсить публичную страницу канала
            news = fetchNewsFromChannel(count);
            
        } catch (Exception e) {
            logger.error("Ошибка получения новостей", e);
            // Возвращаем заглушку
            news.add(new NewsItem(
                "Добро пожаловать в Horizon!",
                "Лаунчер обновлен с новым дизайном и функциями.",
                "",
                LocalDateTime.now().minusDays(1)
            ));
        }
        
        return news;
    }

    private List<NewsItem> fetchNewsFromChannel(int count) {
        List<NewsItem> news = new ArrayList<>();
        
        // Если есть токен бота, используем Telegram Bot API
        if (botToken != null && !botToken.isEmpty()) {
            try {
                // Получаем обновления через getUpdates
                String url = TELEGRAM_API_BASE + botToken + "/getUpdates?offset=-100";
                Request request = new Request.Builder().url(url).get().build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String body = response.body().string();
                        JsonObject json = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                        
                        if (json.has("result")) {
                            JsonArray updates = json.getAsJsonArray("result");
                            int added = 0;
                            
                            // Обрабатываем обновления в обратном порядке (новые первыми)
                            for (int i = updates.size() - 1; i >= 0 && added < count; i--) {
                                JsonObject update = updates.get(i).getAsJsonObject();
                                if (update.has("channel_post")) {
                                    JsonObject post = update.getAsJsonObject("channel_post");
                                    if (post.has("chat") && 
                                        post.getAsJsonObject("chat").has("username") &&
                                        post.getAsJsonObject("chat").get("username").getAsString().equals(CHANNEL_USERNAME)) {
                                        
                                        String text = post.has("text") ? post.get("text").getAsString() : "";
                                        String caption = post.has("caption") ? post.get("caption").getAsString() : "";
                                        String content = !text.isEmpty() ? text : caption;
                                        
                                        // Получаем изображение
                                        String imageUrl = "";
                                        if (post.has("photo")) {
                                            JsonArray photos = post.getAsJsonArray("photo");
                                            if (photos.size() > 0) {
                                                JsonObject photo = photos.get(photos.size() - 1).getAsJsonObject();
                                                String fileId = photo.get("file_id").getAsString();
                                                imageUrl = getFileUrl(fileId);
                                            }
                                        }
                                        
                                        long date = post.has("date") ? post.get("date").getAsLong() : System.currentTimeMillis() / 1000;
                                        LocalDateTime dateTime = LocalDateTime.ofInstant(
                                            Instant.ofEpochSecond(date), 
                                            ZoneId.systemDefault()
                                        );
                                        
                                        if (!content.isEmpty()) {
                                            String title = content.length() > 50 ? content.substring(0, 50) + "..." : content;
                                            String description = content.length() > 150 ? content.substring(0, 150) + "..." : content;
                                            
                                            news.add(new NewsItem(title, description, imageUrl, dateTime));
                                            added++;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Не удалось получить новости через Bot API, используем заглушку", e);
            }
        }
        
        // Если новостей нет, добавляем заглушку
        if (news.isEmpty()) {
            news.add(new NewsItem(
                "Добро пожаловать в Horizon!",
                "Лаунчер обновлен с новым дизайном и функциями.",
                "",
                LocalDateTime.now().minusDays(1)
            ));
            news.add(new NewsItem(
                "Новая система косметики",
                "Теперь вы можете настраивать свой внешний вид в игре.",
                "",
                LocalDateTime.now().minusDays(2)
            ));
            news.add(new NewsItem(
                "Обновление серверов",
                "Серверы обновлены до версии 1.21.10.",
                "",
                LocalDateTime.now().minusDays(3)
            ));
            news.add(new NewsItem(
                "Донат валюта",
                "Новая система донат-валюты для покупки косметики.",
                "",
                LocalDateTime.now().minusDays(4)
            ));
        }
        
        return news;
    }

    private String getFileUrl(String fileId) {
        if (botToken == null || botToken.isEmpty()) {
            return "";
        }
        
        try {
            String url = TELEGRAM_API_BASE + botToken + "/getFile?file_id=" + fileId;
            Request request = new Request.Builder().url(url).get().build();
            
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    JsonObject json = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                    
                    if (json.has("result") && json.getAsJsonObject("result").has("file_path")) {
                        String filePath = json.getAsJsonObject("result").get("file_path").getAsString();
                        return "https://api.telegram.org/file/bot" + botToken + "/" + filePath;
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Не удалось получить URL файла", e);
        }
        
        return "";
    }

    /**
     * Класс для представления новости
     */
    public static class NewsItem {
        private final String title;
        private final String description;
        private final String imageUrl;
        private final LocalDateTime date;

        public NewsItem(String title, String description, String imageUrl, LocalDateTime date) {
            this.title = title;
            this.description = description;
            this.imageUrl = imageUrl;
            this.date = date;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public LocalDateTime getDate() {
            return date;
        }

        public String getFormattedDate() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            return date.format(formatter);
        }
    }
}

