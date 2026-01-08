package com.horizon.launcher.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для работы с форумом
 */
public class ForumService {
    private static final Logger logger = LoggerFactory.getLogger(ForumService.class);
    private final ApiClient apiClient;

    public ForumService() {
        this.apiClient = ApiClient.getInstance();
    }

    public List<Category> getCategories() {
        try {
            ApiClient.ApiResponse response = apiClient.get("/api/forum/categories");
            if (response.isSuccess() && response.getBody().has("categories")) {
                JsonArray categoriesArray = response.getBody().getAsJsonArray("categories");
                List<Category> categories = new ArrayList<>();
                for (var element : categoriesArray) {
                    JsonObject catJson = element.getAsJsonObject();
                    categories.add(new Category(
                        catJson.get("id").getAsInt(),
                        catJson.get("name").getAsString(),
                        catJson.has("description") ? catJson.get("description").getAsString() : null,
                        catJson.has("icon") ? catJson.get("icon").getAsString() : null,
                        catJson.has("topics_count") ? catJson.get("topics_count").getAsInt() : 0
                    ));
                }
                return categories;
            }
        } catch (Exception e) {
            logger.error("Ошибка получения категорий", e);
        }
        return new ArrayList<>();
    }

    public List<Topic> getTopics(int categoryId) {
        try {
            ApiClient.ApiResponse response = apiClient.get("/api/forum/topics?category_id=" + categoryId);
            if (response.isSuccess() && response.getBody().has("topics")) {
                JsonArray topicsArray = response.getBody().getAsJsonArray("topics");
                List<Topic> topics = new ArrayList<>();
                for (var element : topicsArray) {
                    JsonObject topicJson = element.getAsJsonObject();
                    topics.add(new Topic(
                        topicJson.get("id").getAsInt(),
                        topicJson.get("category_id").getAsInt(),
                        topicJson.get("author_username").getAsString(),
                        topicJson.get("title").getAsString(),
                        topicJson.has("content") ? topicJson.get("content").getAsString() : null,
                        topicJson.has("is_pinned") && topicJson.get("is_pinned").getAsInt() == 1,
                        topicJson.has("is_locked") && topicJson.get("is_locked").getAsInt() == 1,
                        topicJson.has("views") ? topicJson.get("views").getAsInt() : 0,
                        topicJson.has("replies_count") ? topicJson.get("replies_count").getAsInt() : 0,
                        topicJson.has("created_at") ? topicJson.get("created_at").getAsString() : null
                    ));
                }
                return topics;
            }
        } catch (Exception e) {
            logger.error("Ошибка получения тем", e);
        }
        return new ArrayList<>();
    }

    public List<Post> getPosts(int topicId) {
        try {
            ApiClient.ApiResponse response = apiClient.get("/api/forum/posts?topic_id=" + topicId);
            if (response.isSuccess() && response.getBody().has("posts")) {
                JsonArray postsArray = response.getBody().getAsJsonArray("posts");
                List<Post> posts = new ArrayList<>();
                for (var element : postsArray) {
                    JsonObject postJson = element.getAsJsonObject();
                    posts.add(new Post(
                        postJson.get("id").getAsInt(),
                        postJson.get("topic_id").getAsInt(),
                        postJson.get("author_username").getAsString(),
                        postJson.get("content").getAsString(),
                        postJson.has("is_edited") && postJson.get("is_edited").getAsInt() == 1,
                        postJson.has("created_at") ? postJson.get("created_at").getAsString() : null,
                        postJson.has("likes_count") ? postJson.get("likes_count").getAsInt() : 0,
                        postJson.has("is_liked") && postJson.get("is_liked").getAsBoolean()
                    ));
                }
                return posts;
            }
        } catch (Exception e) {
            logger.error("Ошибка получения сообщений", e);
        }
        return new ArrayList<>();
    }

    public boolean createTopic(int categoryId, String title, String content, String username) {
        try {
            JsonObject data = new JsonObject();
            data.addProperty("category_id", categoryId);
            data.addProperty("title", title);
            data.addProperty("content", content);
            data.addProperty("author_username", username);
            
            ApiClient.ApiResponse response = apiClient.post("/api/forum/topics", data);
            return response.isSuccess();
        } catch (Exception e) {
            logger.error("Ошибка создания темы", e);
            return false;
        }
    }

    public boolean createPost(int topicId, String content, String username) {
        try {
            JsonObject data = new JsonObject();
            data.addProperty("topic_id", topicId);
            data.addProperty("content", content);
            data.addProperty("author_username", username);
            
            ApiClient.ApiResponse response = apiClient.post("/api/forum/posts", data);
            return response.isSuccess();
        } catch (Exception e) {
            logger.error("Ошибка создания сообщения", e);
            return false;
        }
    }

    public boolean likePost(int postId, String username) {
        try {
            JsonObject data = new JsonObject();
            data.addProperty("post_id", postId);
            data.addProperty("username", username);
            
            ApiClient.ApiResponse response = apiClient.post("/api/forum/posts/like", data);
            return response.isSuccess();
        } catch (Exception e) {
            logger.error("Ошибка лайка", e);
            return false;
        }
    }

    public static class Category {
        private final int id;
        private final String name;
        private final String description;
        private final String icon;
        private final int topicsCount;

        public Category(int id, String name, String description, String icon, int topicsCount) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.icon = icon;
            this.topicsCount = topicsCount;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getIcon() { return icon; }
        public int getTopicsCount() { return topicsCount; }
    }

    public static class Topic {
        private final int id;
        private final int categoryId;
        private final String authorUsername;
        private final String title;
        private final String content;
        private final boolean isPinned;
        private final boolean isLocked;
        private final int views;
        private final int repliesCount;
        private final String createdAt;

        public Topic(int id, int categoryId, String authorUsername, String title, String content,
                    boolean isPinned, boolean isLocked, int views, int repliesCount, String createdAt) {
            this.id = id;
            this.categoryId = categoryId;
            this.authorUsername = authorUsername;
            this.title = title;
            this.content = content;
            this.isPinned = isPinned;
            this.isLocked = isLocked;
            this.views = views;
            this.repliesCount = repliesCount;
            this.createdAt = createdAt;
        }

        public int getId() { return id; }
        public int getCategoryId() { return categoryId; }
        public String getAuthorUsername() { return authorUsername; }
        public String getTitle() { return title; }
        public String getContent() { return content; }
        public boolean isPinned() { return isPinned; }
        public boolean isLocked() { return isLocked; }
        public int getViews() { return views; }
        public int getRepliesCount() { return repliesCount; }
        public String getCreatedAt() { return createdAt; }
    }

    public static class Post {
        private final int id;
        private final int topicId;
        private final String authorUsername;
        private final String content;
        private final boolean isEdited;
        private final String createdAt;
        private final int likesCount;
        private final boolean isLiked;

        public Post(int id, int topicId, String authorUsername, String content, boolean isEdited,
                   String createdAt, int likesCount, boolean isLiked) {
            this.id = id;
            this.topicId = topicId;
            this.authorUsername = authorUsername;
            this.content = content;
            this.isEdited = isEdited;
            this.createdAt = createdAt;
            this.likesCount = likesCount;
            this.isLiked = isLiked;
        }

        public int getId() { return id; }
        public int getTopicId() { return topicId; }
        public String getAuthorUsername() { return authorUsername; }
        public String getContent() { return content; }
        public boolean isEdited() { return isEdited; }
        public String getCreatedAt() { return createdAt; }
        public int getLikesCount() { return likesCount; }
        public boolean isLiked() { return isLiked; }
    }
}





