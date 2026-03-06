package com.horizon.launcher.ui;

import com.horizon.launcher.models.ForumPost;
import com.horizon.launcher.models.ForumTopic;
import com.horizon.launcher.models.UserRole;
import com.horizon.launcher.services.ForumService;
import com.horizon.launcher.ui.components.HeadRenderer;
import com.horizon.launcher.ui.components.NotificationBell;
import com.horizon.launcher.ui.components.VirtualizedForumList;
import com.horizon.launcher.utils.AnimationHelper;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Контроллер форума в стиле XenForo
 */
public class ForumController extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(ForumController.class);
    
    private final String username;
    private final ForumService forumService;
    
    // UI компоненты
    private ScrollPane topicsScrollPane;
    private VBox topicsList; // Оставляем для обратной совместимости
    private VirtualizedForumList virtualizedTopicsList; // Виртуализированный список
    private VBox categoriesList;
    private ScrollPane postsScrollPane;
    private VBox postsList;
    private TextField topicTitleField;
    private TextArea topicContentArea;
    private TextArea postContentArea;
    private Button createTopicButton;
    private Button createPostButton;
    private NotificationBell notificationBell;
    
    // Данные
    private List<ForumService.Category> categories;
    private List<ForumTopic> topics;
    private List<ForumPost> posts;
    private int selectedCategoryId;
    private int selectedTopicId;
    private ForumService.Category selectedCategory;
    
    public ForumController(String username) {
        this.username = username;
        this.forumService = ForumService.getInstance();
        this.selectedCategoryId = -1;
        this.selectedTopicId = -1;
        
        createUI();
        loadCategories();
    }
    
    /**
     * Создать UI
     */
    private void createUI() {
        this.setStyle("-fx-background-color: transparent;");
        
        // Верхняя панель с уведомлениями
        HBox topPanel = createTopPanel();
        this.setTop(topPanel);
        
        // Левая панель с категориями
        VBox categoriesPanel = createCategoriesPanel();
        this.setLeft(categoriesPanel);
        
        // Центральная область с темами
        BorderPane topicsArea = createTopicsArea();
        this.setCenter(topicsArea);
        
        // Правая панель с сообщениями
        BorderPane postsArea = createPostsArea();
        this.setRight(postsArea);
        
        // Применяем стили
        this.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
    }
    
    /**
     * Создать верхнюю панель
     */
    private HBox createTopPanel() {
        HBox topPanel = new HBox(15);
        topPanel.setPadding(new Insets(15));
        topPanel.setAlignment(Pos.CENTER_RIGHT);
        topPanel.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 10px;");
        
        Label titleLabel = new Label("Форум Horizon");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        // Колокольчик уведомлений
        notificationBell = new NotificationBell();
        notificationBell.setNotificationCount(0); // Пока без уведомлений
        
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        topPanel.getChildren().addAll(titleLabel, notificationBell);
        
        return topPanel;
    }
    
    /**
     * Создать панель категорий
     */
    private VBox createCategoriesPanel() {
        VBox categoriesPanel = new VBox(10);
        categoriesPanel.setPadding(new Insets(15));
        categoriesPanel.setPrefWidth(250);
        categoriesPanel.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 10px;");
        
        Label categoriesLabel = new Label("Категории");
        categoriesLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        categoriesList = new VBox(5);
        categoriesList.setSpacing(5);
        
        ScrollPane scrollPane = new ScrollPane(categoriesList);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        scrollPane.getStyleClass().add("forum-scroll-pane");
        
        categoriesPanel.getChildren().addAll(categoriesLabel, scrollPane);
        
        return categoriesPanel;
    }
    
    /**
     * Создать область с темами
     */
    private BorderPane createTopicsArea() {
        BorderPane topicsArea = new BorderPane();
        topicsArea.setPadding(new Insets(15));
        topicsArea.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 10px;");
        
        // Заголовок
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        
        Label topicsLabel = new Label("Темы");
        topicsLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        createTopicButton = new Button("Создать тему");
        createTopicButton.getStyleClass().add("button");
        createTopicButton.setOnAction(e -> showCreateTopicDialog());
        
        headerBox.getChildren().addAll(topicsLabel, createTopicButton);
        topicsArea.setTop(headerBox);
        
        // Виртуализированный список тем (для больших списков)
        virtualizedTopicsList = new VirtualizedForumList(this);
        virtualizedTopicsList.setPrefHeight(600);
        
        // Также создаем VBox для обратной совместимости
        topicsList = new VBox(10);
        topicsList.getStyleClass().add("forum-topics-list");
        
        // Используем виртуализированный список
        topicsScrollPane = new ScrollPane(virtualizedTopicsList);
        topicsScrollPane.setFitToWidth(true);
        topicsScrollPane.getStyleClass().add("forum-scroll-pane");
        topicsArea.setCenter(topicsScrollPane);
        
        return topicsArea;
    }
    
    /**
     * Создать область с сообщениями
     */
    private BorderPane createPostsArea() {
        BorderPane postsArea = new BorderPane();
        postsArea.setPadding(new Insets(15));
        postsArea.setPrefWidth(400);
        postsArea.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 10px;");
        
        // Заголовок
        Label postsLabel = new Label("Сообщения");
        postsLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        postsArea.setTop(postsLabel);
        
        // Список сообщений
        postsList = new VBox(15);
        postsList.setPadding(new Insets(10));
        
        postsScrollPane = new ScrollPane(postsList);
        postsScrollPane.setFitToWidth(true);
        postsScrollPane.getStyleClass().add("forum-scroll-pane");
        postsArea.setCenter(postsScrollPane);
        
        // Форма создания сообщения
        VBox createPostBox = new VBox(10);
        createPostBox.setPadding(new Insets(15));
        createPostBox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 10px;");
        
        Label createPostLabel = new Label("Ответить");
        createPostLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        postContentArea = new TextArea();
        postContentArea.setPrefRowCount(4);
        postContentArea.setWrapText(true);
        postContentArea.getStyleClass().add("text-field");
        
        createPostButton = new Button("Отправить");
        createPostButton.getStyleClass().add("button");
        createPostButton.setOnAction(e -> handleCreatePost());
        createPostButton.setDisable(true);
        
        createPostBox.getChildren().addAll(createPostLabel, postContentArea, createPostButton);
        postsArea.setBottom(createPostBox);
        
        return postsArea;
    }
    
    /**
     * Загрузить категории
     */
    private void loadCategories() {
        forumService.getCategories(new ForumService.CategoriesCallback() {
            @Override
            public void onSuccess(List<ForumService.Category> categories) {
                Platform.runLater(() -> {
                    ForumController.this.categories = categories;
                    renderCategories();
                });
            }
            
            @Override
            public void onError(String error) {
                logger.error("Ошибка при загрузке категорий: " + error);
            }
        });
    }
    
    /**
     * Отрисовать категории
     */
    private void renderCategories() {
        categoriesList.getChildren().clear();
        
        for (ForumService.Category category : categories) {
            VBox categoryCard = createCategoryCard(category);
            categoriesList.getChildren().add(categoryCard);
        }
    }
    
    /**
     * Создать карточку категории
     */
    private VBox createCategoryCard(ForumService.Category category) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: rgba(255, 255, 255, 0.08); -fx-background-radius: 8px; -fx-cursor: hand;");
        
        card.setOnMouseClicked(e -> {
            selectedCategoryId = category.getId();
            selectedCategory = category;
            loadTopics(category.getId());
        });
        
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        
        Label iconLabel = new Label(category.getIcon() != null ? category.getIcon() : "📁");
        iconLabel.setStyle("-fx-font-size: 20px;");
        
        Label nameLabel = new Label(category.getName());
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        headerBox.getChildren().addAll(iconLabel, nameLabel);
        
        if (category.getDescription() != null && !category.getDescription().isEmpty()) {
            Label descLabel = new Label(category.getDescription());
            descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255, 255, 255, 0.7); -fx-wrap-text: true;");
            card.getChildren().addAll(headerBox, descLabel);
        } else {
            card.getChildren().add(headerBox);
        }
        
        Label countLabel = new Label(category.getTopicsCount() + " тем");
        countLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255, 255, 255, 0.5);");
        card.getChildren().add(countLabel);
        
        return card;
    }
    
    /**
     * Загрузить темы
     */
    private void loadTopics(int categoryId) {
        forumService.getTopics(categoryId, new ForumService.TopicsCallback() {
            @Override
            public void onSuccess(List<ForumTopic> topics) {
                Platform.runLater(() -> {
                    ForumController.this.topics = topics;
                    renderTopics();
                });
            }
            
            @Override
            public void onError(String error) {
                logger.error("Ошибка при загрузке тем: " + error);
            }
        });
    }
    
    /**
     * Отрисовать темы
     */
    private void renderTopics() {
        // Используем виртуализированный список для больших списков
        if (virtualizedTopicsList != null) {
            if (topics == null || topics.isEmpty()) {
                virtualizedTopicsList.getItems().clear();
                logger.debug("Список тем пуст");
                return;
            }
            
            // Используем виртуализированный список
            virtualizedTopicsList.setTopics(topics);
            logger.debug("Отрисовано {} тем через виртуализированный список", topics.size());
        } else {
            // Fallback на старый метод для обратной совместимости
            topicsList.getChildren().clear();
            
            if (topics == null || topics.isEmpty()) {
                Label emptyLabel = new Label("Нет тем в этой категории");
                emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(255, 255, 255, 0.5);");
                topicsList.getChildren().add(emptyLabel);
                return;
            }
            
            for (ForumTopic topic : topics) {
                VBox topicCard = createTopicCard(topic);
                topicsList.getChildren().add(topicCard);
                AnimationHelper.fadeIn(topicCard);
            }
        }
    }
    
    /**
     * Создать карточку темы
     */
    private VBox createTopicCard(ForumTopic topic) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.getStyleClass().add("forum-topic-card");
        
        if (topic.isPinned()) {
            card.getStyleClass().add("pinned");
        }
        if (topic.isLocked()) {
            card.getStyleClass().add("locked");
        }
        
        card.setOnMouseClicked(e -> {
            selectedTopicId = topic.getId();
            loadPosts(topic.getId());
            createPostButton.setDisable(false);
        });
        
        // Заголовок темы
        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        
        if (topic.isPinned()) {
            Label pinnedIcon = new Label("📌");
            pinnedIcon.getStyleClass().add("forum-icon-pinned");
            titleBox.getChildren().add(pinnedIcon);
        }
        
        if (topic.isLocked()) {
            Label lockedIcon = new Label("🔒");
            lockedIcon.getStyleClass().add("forum-icon-locked");
            titleBox.getChildren().add(lockedIcon);
        }
        
        Label titleLabel = new Label(topic.getTitle());
        titleLabel.getStyleClass().add("forum-topic-title");
        
        titleBox.getChildren().add(titleLabel);
        
        // Автор и метаданные
        HBox metaBox = new HBox(15);
        metaBox.setAlignment(Pos.CENTER_LEFT);
        
        Text authorText = new Text(topic.getAuthorUsername());
        UserRole.applyRoleStyle(authorText, topic.getAuthorRole());
        
        // Применяем shimmer эффект для Owner и Curator
        if (topic.getAuthorRole() == UserRole.OWNER || topic.getAuthorRole() == UserRole.CURATOR) {
            AnimationHelper.shimmer(authorText);
        }
        
        Label authorLabel = new Label();
        authorLabel.setGraphic(authorText);
        authorLabel.getStyleClass().add("forum-topic-author");
        
        Label statsLabel = new Label(
            topic.getViews() + " просмотров • " + topic.getRepliesCount() + " ответов"
        );
        statsLabel.getStyleClass().add("forum-topic-stats");
        
        metaBox.getChildren().addAll(authorLabel, statsLabel);
        
        card.getChildren().addAll(titleBox, metaBox);
        
        return card;
    }
    
    /**
     * Загрузить сообщения темы
     */
    private void loadPosts(int topicId) {
        forumService.getPosts(topicId, username, new ForumService.PostsCallback() {
            @Override
            public void onSuccess(List<ForumPost> posts) {
                Platform.runLater(() -> {
                    ForumController.this.posts = posts;
                    renderPosts();
                });
            }
            
            @Override
            public void onError(String error) {
                logger.error("Ошибка при загрузке сообщений: " + error);
            }
        });
    }
    
    /**
     * Отрисовать сообщения
     */
    private void renderPosts() {
        postsList.getChildren().clear();
        
        if (posts == null || posts.isEmpty()) {
            Label emptyLabel = new Label("Нет сообщений в этой теме");
            emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(255, 255, 255, 0.5);");
            postsList.getChildren().add(emptyLabel);
            return;
        }
        
        for (ForumPost post : posts) {
            HBox postCard = createPostCard(post);
            postsList.getChildren().add(postCard);
            AnimationHelper.fadeIn(postCard);
        }
    }
    
    /**
     * Создать карточку сообщения
     */
    private HBox createPostCard(ForumPost post) {
        HBox card = new HBox(15);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 10px;");
        
        // Профиль пользователя
        VBox profileBox = new VBox(10);
        profileBox.getStyleClass().add("forum-post-profile");
        profileBox.setAlignment(Pos.TOP_CENTER);
        
        // Аватар (3D голова)
        HeadRenderer avatar = new HeadRenderer(64);
        avatar.setSkinByUsername(post.getAuthorUsername());
        avatar.getStyleClass().add("forum-user-avatar");
        
        // Имя пользователя с ролью
        UserRole authorRole = post.getAuthorRole() != null ? post.getAuthorRole() : UserRole.DEFAULT;
        Text usernameText = new Text(post.getAuthorUsername());
        UserRole.applyRoleStyle(usernameText, authorRole);
        
        // Применяем shimmer эффект для Owner и Curator
        if (authorRole == UserRole.OWNER || authorRole == UserRole.CURATOR) {
            AnimationHelper.shimmer(usernameText);
        }
        
        Label usernameLabel = new Label();
        usernameLabel.setGraphic(usernameText);
        usernameLabel.getStyleClass().add("forum-username");
        
        // Роль
        Label roleLabel = new Label(authorRole.getPrefix());
        roleLabel.getStyleClass().add("forum-user-role");
        
        profileBox.getChildren().addAll(avatar, usernameLabel, roleLabel);
        
        // Содержимое сообщения
        VBox contentBox = new VBox(10);
        contentBox.getStyleClass().add("forum-post-content");
        
        TextFlow contentFlow = new TextFlow();
        Label contentLabel = new Label(post.getContent());
        contentLabel.getStyleClass().add("forum-post-text");
        contentLabel.setWrapText(true);
        contentFlow.getChildren().add(contentLabel);
        
        // Метаданные
        HBox metaBox = new HBox(15);
        metaBox.getStyleClass().add("forum-post-meta");
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        String dateStr = post.getCreatedAt() != null ? dateFormat.format(post.getCreatedAt()) : "N/A";
        Label dateLabel = new Label(dateStr);
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255, 255, 255, 0.5);");
        
        Button likeButton = new Button("❤ " + post.getLikesCount());
        likeButton.getStyleClass().add("forum-like-button");
        if (post.isLiked()) {
            likeButton.getStyleClass().add("liked");
        }
        likeButton.setOnAction(e -> handleLikePost(post.getId()));
        
        metaBox.getChildren().addAll(dateLabel, likeButton);
        
        contentBox.getChildren().addAll(contentFlow, metaBox);
        
        card.getChildren().addAll(profileBox, contentBox);
        HBox.setHgrow(contentBox, Priority.ALWAYS);
        
        return card;
    }
    
    /**
     * Показать диалог создания темы
     */
    private void showCreateTopicDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Создать тему");
        dialog.setHeaderText(null);
        
        VBox dialogContent = new VBox(10);
        dialogContent.setPadding(new Insets(20));
        
        Label titleLabel = new Label("Заголовок:");
        titleLabel.setStyle("-fx-text-fill: white;");
        
        topicTitleField = new TextField();
        topicTitleField.getStyleClass().add("text-field");
        topicTitleField.setPromptText("Введите заголовок темы");
        
        Label contentLabel = new Label("Содержание:");
        contentLabel.setStyle("-fx-text-fill: white;");
        
        topicContentArea = new TextArea();
        topicContentArea.setPrefRowCount(8);
        topicContentArea.setWrapText(true);
        topicContentArea.getStyleClass().add("text-field");
        topicContentArea.setPromptText("Введите содержание темы");
        
        dialogContent.getChildren().addAll(titleLabel, topicTitleField, contentLabel, topicContentArea);
        
        dialog.getDialogPane().setContent(dialogContent);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setStyle("-fx-background-color: rgba(26, 26, 46, 1);");
        
        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                handleCreateTopic();
            }
        });
    }
    
    /**
     * Обработка создания темы
     */
    private void handleCreateTopic() {
        if (selectedCategoryId == -1) {
            showAlert("Ошибка", "Выберите категорию", Alert.AlertType.WARNING);
            return;
        }
        
        String title = topicTitleField.getText();
        String content = topicContentArea.getText();
        
        if (title == null || title.trim().isEmpty()) {
            showAlert("Ошибка", "Введите заголовок темы", Alert.AlertType.WARNING);
            return;
        }
        
        if (content == null || content.trim().isEmpty()) {
            showAlert("Ошибка", "Введите содержание темы", Alert.AlertType.WARNING);
            return;
        }
        
        forumService.createTopic(selectedCategoryId, title, content, username, 
            new ForumService.CreateTopicCallback() {
                @Override
                public void onSuccess(int topicId) {
                    Platform.runLater(() -> {
                        showAlert("Успешно", "Тема создана!", Alert.AlertType.INFORMATION);
                        loadTopics(selectedCategoryId);
                        selectedTopicId = topicId;
                        loadPosts(topicId);
                    });
                }
                
                @Override
                public void onError(String error) {
                    Platform.runLater(() -> {
                        showAlert("Ошибка", "Не удалось создать тему: " + error, 
                                 Alert.AlertType.ERROR);
                    });
                }
            });
    }
    
    /**
     * Обработка создания сообщения
     */
    private void handleCreatePost() {
        if (selectedTopicId == -1) {
            showAlert("Ошибка", "Выберите тему", Alert.AlertType.WARNING);
            return;
        }
        
        String content = postContentArea.getText();
        if (content == null || content.trim().isEmpty()) {
            showAlert("Ошибка", "Введите текст сообщения", Alert.AlertType.WARNING);
            return;
        }
        
        forumService.createPost(selectedTopicId, content, username, 
            new ForumService.CreatePostCallback() {
                @Override
                public void onSuccess(int postId) {
                    Platform.runLater(() -> {
                        postContentArea.clear();
                        loadPosts(selectedTopicId);
                    });
                }
                
                @Override
                public void onError(String error) {
                    Platform.runLater(() -> {
                        showAlert("Ошибка", "Не удалось создать сообщение: " + error, 
                                 Alert.AlertType.ERROR);
                    });
                }
            });
    }
    
    /**
     * Обработка лайка сообщения
     */
    private void handleLikePost(int postId) {
        forumService.likePost(postId, username, new ForumService.LikeCallback() {
            @Override
            public void onSuccess(boolean liked) {
                Platform.runLater(() -> {
                    // Перезагружаем сообщения для обновления счетчика лайков
                    if (selectedTopicId != -1) {
                        loadPosts(selectedTopicId);
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                logger.error("Ошибка при лайке сообщения: " + error);
            }
        });
    }
    
    /**
     * Показать диалоговое окно
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Обновить данные форума
     */
    public void refresh() {
        logger.info("Обновление форума...");
        loadCategories();
        if (selectedCategoryId > 0) {
            loadTopics(selectedCategoryId);
        }
    }
}
