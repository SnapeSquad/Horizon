package com.horizon.launcher.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.horizon.launcher.models.ForumPost;
import com.horizon.launcher.models.ForumTopic;
import com.horizon.launcher.models.UserRole;
import com.horizon.launcher.network.ApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Сервис для работы с форумом
 */
public class ForumService {
    private static final Logger logger = LoggerFactory.getLogger(ForumService.class);
    private static ForumService instance;
    private final ApiClient apiClient;
    private final Gson gson;
    private final SimpleDateFormat dateFormat;
    
    private ForumService() {
        this.apiClient = ApiClient.getInstance();
        this.gson = new Gson();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    }
    
    public static synchronized ForumService getInstance() {
        if (instance == null) {
            instance = new ForumService();
        }
        return instance;
    }
    
    /**
     * Колбэк для получения категорий
     */
    public interface CategoriesCallback {
        void onSuccess(List<Category> categories);
        void onError(String error);
    }
    
    /**
     * Колбэк для получения тем
     */
    public interface TopicsCallback {
        void onSuccess(List<ForumTopic> topics);
        void onError(String error);
    }
    
    /**
     * Колбэк для получения сообщений
     */
    public interface PostsCallback {
        void onSuccess(List<ForumPost> posts);
        void onError(String error);
    }
    
    /**
     * Колбэк для создания темы
     */
    public interface CreateTopicCallback {
        void onSuccess(int topicId);
        void onError(String error);
    }
    
    /**
     * Колбэк для создания сообщения
     */
    public interface CreatePostCallback {
        void onSuccess(int postId);
        void onError(String error);
    }
    
    /**
     * Колбэк для лайка
     */
    public interface LikeCallback {
        void onSuccess(boolean liked);
        void onError(String error);
    }
    
    /**
     * Модель категории форума
     */
    public static class Category {
        private int id;
        private String name;
        private String description;
        private String icon;
        private int topicsCount;
        
        public Category() {
        }
        
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public int getTopicsCount() { return topicsCount; }
        public void setTopicsCount(int topicsCount) { this.topicsCount = topicsCount; }
    }
    
    /**
     * Получить список категорий
     */
    public void getCategories(CategoriesCallback callback) {
        apiClient.get("/api/forum/categories", JsonObject.class, new ApiClient.Callback<JsonObject>() {
            @Override
            public void onSuccess(boolean success, String message, JsonObject data) {
                if (success && data != null && data.has("categories")) {
                    try {
                        JsonArray categoriesArray = data.getAsJsonArray("categories");
                        List<Category> categories = new ArrayList<>();
                        
                        for (JsonElement element : categoriesArray) {
                            JsonObject catObj = element.getAsJsonObject();
                            Category category = parseCategory(catObj);
                            if (category != null) {
                                categories.add(category);
                            }
                        }
                        
                        callback.onSuccess(categories);
                    } catch (Exception e) {
                        logger.error("Ошибка при парсинге категорий", e);
                        callback.onError("Ошибка при загрузке категорий: " + e.getMessage());
                    }
                } else {
                    callback.onError(message != null ? message : "Не удалось загрузить категории");
                }
            }
            
            @Override
            public void onError(Throwable error) {
                logger.error("Ошибка при получении категорий", error);
                callback.onError("Ошибка соединения: " + error.getMessage());
            }
        });
    }
    
    /**
     * Получить список тем в категории
     */
    public void getTopics(int categoryId, TopicsCallback callback) {
        apiClient.get("/api/forum/topics?category_id=" + categoryId, JsonObject.class, 
            new ApiClient.Callback<JsonObject>() {
                @Override
                public void onSuccess(boolean success, String message, JsonObject data) {
                    if (success && data != null && data.has("topics")) {
                        try {
                            JsonArray topicsArray = data.getAsJsonArray("topics");
                            List<ForumTopic> topics = new ArrayList<>();
                            
                            for (JsonElement element : topicsArray) {
                                JsonObject topicObj = element.getAsJsonObject();
                                ForumTopic topic = parseTopic(topicObj);
                                if (topic != null) {
                                    topics.add(topic);
                                }
                            }
                            
                            callback.onSuccess(topics);
                        } catch (Exception e) {
                            logger.error("Ошибка при парсинге тем", e);
                            callback.onError("Ошибка при загрузке тем: " + e.getMessage());
                        }
                    } else {
                        callback.onError(message != null ? message : "Не удалось загрузить темы");
                    }
                }
                
                @Override
                public void onError(Throwable error) {
                    logger.error("Ошибка при получении тем", error);
                    callback.onError("Ошибка соединения: " + error.getMessage());
                }
            });
    }
    
    /**
     * Получить сообщения темы
     */
    public void getPosts(int topicId, String username, PostsCallback callback) {
        String endpoint = "/api/forum/posts?topic_id=" + topicId;
        if (username != null && !username.isEmpty()) {
            endpoint += "&username=" + username;
        }
        
        apiClient.get(endpoint, JsonObject.class, new ApiClient.Callback<JsonObject>() {
            @Override
            public void onSuccess(boolean success, String message, JsonObject data) {
                if (success && data != null && data.has("posts")) {
                    try {
                        JsonArray postsArray = data.getAsJsonArray("posts");
                        List<ForumPost> posts = new ArrayList<>();
                        
                        for (JsonElement element : postsArray) {
                            JsonObject postObj = element.getAsJsonObject();
                            ForumPost post = parsePost(postObj);
                            if (post != null) {
                                posts.add(post);
                            }
                        }
                        
                        callback.onSuccess(posts);
                    } catch (Exception e) {
                        logger.error("Ошибка при парсинге сообщений", e);
                        callback.onError("Ошибка при загрузке сообщений: " + e.getMessage());
                    }
                } else {
                    callback.onError(message != null ? message : "Не удалось загрузить сообщения");
                }
            }
            
            @Override
            public void onError(Throwable error) {
                logger.error("Ошибка при получении сообщений", error);
                callback.onError("Ошибка соединения: " + error.getMessage());
            }
        });
    }
    
    /**
     * Создать новую тему
     */
    public void createTopic(int categoryId, String title, String content, String authorUsername, 
                           CreateTopicCallback callback) {
        Map<String, Object> request = new HashMap<>();
        request.put("category_id", categoryId);
        request.put("title", title);
        request.put("content", content);
        request.put("author_username", authorUsername);
        
        apiClient.post("/api/forum/topics", request, JsonObject.class, new ApiClient.Callback<JsonObject>() {
            @Override
            public void onSuccess(boolean success, String message, JsonObject data) {
                if (success && data != null && data.has("topic_id")) {
                    int topicId = data.get("topic_id").getAsInt();
                    callback.onSuccess(topicId);
                } else {
                    callback.onError(message != null ? message : "Ошибка при создании темы");
                }
            }
            
            @Override
            public void onError(Throwable error) {
                logger.error("Ошибка при создании темы", error);
                callback.onError("Ошибка соединения: " + error.getMessage());
            }
        });
    }
    
    /**
     * Создать новое сообщение
     */
    public void createPost(int topicId, String content, String authorUsername, CreatePostCallback callback) {
        Map<String, Object> request = new HashMap<>();
        request.put("topic_id", topicId);
        request.put("content", content);
        request.put("author_username", authorUsername);
        
        apiClient.post("/api/forum/posts", request, JsonObject.class, new ApiClient.Callback<JsonObject>() {
            @Override
            public void onSuccess(boolean success, String message, JsonObject data) {
                if (success && data != null && data.has("post_id")) {
                    int postId = data.get("post_id").getAsInt();
                    callback.onSuccess(postId);
                } else {
                    callback.onError(message != null ? message : "Ошибка при создании сообщения");
                }
            }
            
            @Override
            public void onError(Throwable error) {
                logger.error("Ошибка при создании сообщения", error);
                callback.onError("Ошибка соединения: " + error.getMessage());
            }
        });
    }
    
    /**
     * Лайкнуть сообщение
     */
    public void likePost(int postId, String username, LikeCallback callback) {
        Map<String, Object> request = new HashMap<>();
        request.put("post_id", postId);
        request.put("username", username);
        
        apiClient.post("/api/forum/posts/like", request, JsonObject.class, new ApiClient.Callback<JsonObject>() {
            @Override
            public void onSuccess(boolean success, String message, JsonObject data) {
                if (success && data != null && data.has("liked")) {
                    boolean liked = data.get("liked").getAsBoolean();
                    callback.onSuccess(liked);
                } else {
                    callback.onError(message != null ? message : "Ошибка при лайке");
                }
            }
            
            @Override
            public void onError(Throwable error) {
                logger.error("Ошибка при лайке сообщения", error);
                callback.onError("Ошибка соединения: " + error.getMessage());
            }
        });
    }
    
    /**
     * Парсить категорию из JSON
     */
    private Category parseCategory(JsonObject catObj) {
        try {
            Category category = new Category();
            if (catObj.has("id")) category.setId(catObj.get("id").getAsInt());
            if (catObj.has("name")) category.setName(catObj.get("name").getAsString());
            if (catObj.has("description")) category.setDescription(catObj.get("description").getAsString());
            if (catObj.has("icon")) category.setIcon(catObj.get("icon").getAsString());
            if (catObj.has("topics_count")) category.setTopicsCount(catObj.get("topics_count").getAsInt());
            return category;
        } catch (Exception e) {
            logger.error("Ошибка при парсинге категории", e);
            return null;
        }
    }
    
    /**
     * Парсить тему из JSON
     */
    private ForumTopic parseTopic(JsonObject topicObj) {
        try {
            ForumTopic topic = new ForumTopic();
            if (topicObj.has("id")) topic.setId(topicObj.get("id").getAsInt());
            if (topicObj.has("category_id")) topic.setCategoryId(topicObj.get("category_id").getAsInt());
            if (topicObj.has("author_username")) topic.setAuthorUsername(topicObj.get("author_username").getAsString());
            if (topicObj.has("title")) topic.setTitle(topicObj.get("title").getAsString());
            if (topicObj.has("content")) topic.setContent(topicObj.get("content").getAsString());
            if (topicObj.has("is_pinned")) topic.setPinned(topicObj.get("is_pinned").getAsInt() == 1);
            if (topicObj.has("is_locked")) topic.setLocked(topicObj.get("is_locked").getAsInt() == 1);
            if (topicObj.has("views")) topic.setViews(topicObj.get("views").getAsInt());
            if (topicObj.has("replies_count")) topic.setRepliesCount(topicObj.get("replies_count").getAsInt());
            if (topicObj.has("created_at")) topic.setCreatedAt(parseDate(topicObj.get("created_at").getAsString()));
            if (topicObj.has("last_reply_at")) {
                String lastReplyStr = topicObj.get("last_reply_at").getAsString();
                if (lastReplyStr != null && !lastReplyStr.isEmpty()) {
                    topic.setLastReplyAt(parseDate(lastReplyStr));
                }
            }
            
            // Определяем роль автора (по умолчанию DEFAULT)
            topic.setAuthorRole(UserRole.DEFAULT);
            
            return topic;
        } catch (Exception e) {
            logger.error("Ошибка при парсинге темы", e);
            return null;
        }
    }
    
    /**
     * Парсить сообщение из JSON
     */
    private ForumPost parsePost(JsonObject postObj) {
        try {
            ForumPost post = new ForumPost();
            if (postObj.has("id")) post.setId(postObj.get("id").getAsInt());
            if (postObj.has("topic_id")) post.setTopicId(postObj.get("topic_id").getAsInt());
            if (postObj.has("author_username")) post.setAuthorUsername(postObj.get("author_username").getAsString());
            if (postObj.has("content")) post.setContent(postObj.get("content").getAsString());
            if (postObj.has("is_edited")) post.setEdited(postObj.get("is_edited").getAsInt() == 1);
            if (postObj.has("created_at")) post.setCreatedAt(parseDate(postObj.get("created_at").getAsString()));
            if (postObj.has("edited_at")) {
                String editedStr = postObj.get("edited_at").getAsString();
                if (editedStr != null && !editedStr.isEmpty()) {
                    post.setEditedAt(parseDate(editedStr));
                }
            }
            if (postObj.has("likes_count")) post.setLikesCount(postObj.get("likes_count").getAsInt());
            if (postObj.has("is_liked")) post.setLiked(postObj.get("is_liked").getAsInt() == 1);
            
            // Определяем роль автора (по умолчанию DEFAULT)
            post.setAuthorRole(UserRole.DEFAULT);
            
            return post;
        } catch (Exception e) {
            logger.error("Ошибка при парсинге сообщения", e);
            return null;
        }
    }
    
    /**
     * Парсить дату из строки
     */
    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        
        try {
            // Пробуем несколько форматов
            String[] formats = {
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd"
            };
            
            for (String format : formats) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(format);
                    return sdf.parse(dateStr);
                } catch (ParseException e) {
                    // Пробуем следующий формат
                }
            }
            
            // Если ничего не подошло, возвращаем текущую дату
            logger.warn("Не удалось распарсить дату: {}", dateStr);
            return new Date();
        } catch (Exception e) {
            logger.error("Ошибка при парсинге даты: " + dateStr, e);
            return new Date();
        }
    }
}
