package com.horizon.launcher.ui;

import com.horizon.launcher.api.ForumService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Полноценное окно форума
 */
public class ForumWindow {
    private static final Logger logger = LoggerFactory.getLogger(ForumWindow.class);
    private final ForumService forumService;
    private final String username;
    private VBox root;
    private StackPane mainStack;
    
    // Текущее состояние
    private ForumService.Category currentCategory;
    private ForumService.Topic currentTopic;
    private VBox categoriesView;
    private VBox topicsView;
    private VBox postsView;

    public ForumWindow(String username) {
        this.forumService = new ForumService();
        this.username = username;
        createWindow();
    }

    private void createWindow() {
        root = new VBox(0);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("animated-background");
        
        // Заголовок
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 20, 0));
        
        Label title = new Label("💬 Форум");
        title.getStyleClass().add("title-text");
        title.setStyle("-fx-font-size: 32px;");
        
        Button backBtn = new Button("← Назад");
        backBtn.getStyleClass().add("ios-button");
        backBtn.setPrefWidth(120);
        backBtn.setOnAction(e -> goBack());
        
        header.getChildren().addAll(backBtn, title);
        root.getChildren().add(header);
        
        // Основной контент
        mainStack = new StackPane();
        mainStack.setPrefHeight(600);
        
        categoriesView = createCategoriesView();
        topicsView = createTopicsView();
        postsView = createPostsView();
        
        mainStack.getChildren().addAll(categoriesView, topicsView, postsView);
        categoriesView.setVisible(true);
        topicsView.setVisible(false);
        postsView.setVisible(false);
        
        root.getChildren().add(mainStack);
        
        loadCategories();
    }

    private VBox createCategoriesView() {
        VBox view = new VBox(15);
        view.setPadding(new Insets(20));
        
        Label title = new Label("Категории форума");
        title.getStyleClass().add("section-title");
        title.setStyle("-fx-font-size: 24px;");
        view.getChildren().add(title);
        
        FlowPane categoriesPane = new FlowPane(15, 15);
        categoriesPane.setAlignment(Pos.TOP_LEFT);
        categoriesPane.setPrefWrapLength(900);
        
        view.getChildren().add(categoriesPane);
        
        return view;
    }

    private VBox createTopicsView() {
        VBox view = new VBox(15);
        view.setPadding(new Insets(20));
        
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label();
        title.getStyleClass().add("section-title");
        title.setStyle("-fx-font-size: 24px;");
        
        Button newTopicBtn = new Button("+ Новая тема");
        newTopicBtn.getStyleClass().add("ios-button");
        newTopicBtn.setPrefWidth(150);
        newTopicBtn.setOnAction(e -> showNewTopicDialog());
        
        header.getChildren().addAll(title, newTopicBtn);
        view.getChildren().add(header);
        
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        
        VBox topicsList = new VBox(10);
        topicsList.setPadding(new Insets(10));
        scrollPane.setContent(topicsList);
        
        view.getChildren().add(scrollPane);
        
        return view;
    }

    private VBox createPostsView() {
        VBox view = new VBox(15);
        view.setPadding(new Insets(20));
        
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label();
        title.getStyleClass().add("section-title");
        title.setStyle("-fx-font-size: 24px;");
        
        Button newPostBtn = new Button("+ Ответить");
        newPostBtn.getStyleClass().add("ios-button");
        newPostBtn.setPrefWidth(150);
        newPostBtn.setOnAction(e -> showNewPostDialog());
        
        header.getChildren().addAll(title, newPostBtn);
        view.getChildren().add(header);
        
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        
        VBox postsList = new VBox(15);
        postsList.setPadding(new Insets(10));
        scrollPane.setContent(postsList);
        
        view.getChildren().add(scrollPane);
        
        return view;
    }

    private void loadCategories() {
        new Thread(() -> {
            List<ForumService.Category> categories = forumService.getCategories();
            
            Platform.runLater(() -> {
                FlowPane pane = (FlowPane) ((VBox) categoriesView.getChildren().get(1)).getChildren().get(0);
                pane.getChildren().clear();
                
                for (ForumService.Category category : categories) {
                    VBox card = createCategoryCard(category);
                    pane.getChildren().add(card);
                }
            });
        }).start();
    }

    private VBox createCategoryCard(ForumService.Category category) {
        VBox card = new VBox(12);
        card.setPrefWidth(280);
        card.setPrefHeight(150);
        card.setPadding(new Insets(20));
        card.getStyleClass().add("forum-category-card");
        card.setAlignment(Pos.TOP_LEFT);
        card.setOnMouseClicked(e -> openCategory(category));
        
        HBox iconTitle = new HBox(10);
        iconTitle.setAlignment(Pos.CENTER_LEFT);
        
        Label icon = new Label(category.getIcon() != null ? category.getIcon() : "📁");
        icon.setStyle("-fx-font-size: 32px;");
        
        VBox titleBox = new VBox(5);
        Label title = new Label(category.getName());
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        
        Label desc = new Label(category.getDescription() != null ? category.getDescription() : "");
        desc.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 13px;");
        desc.setWrapText(true);
        
        titleBox.getChildren().addAll(title, desc);
        iconTitle.getChildren().addAll(icon, titleBox);
        
        Label topicsCount = new Label(category.getTopicsCount() + " тем");
        topicsCount.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.6); -fx-font-size: 12px;");
        
        card.getChildren().addAll(iconTitle, topicsCount);
        
        return card;
    }

    private void openCategory(ForumService.Category category) {
        currentCategory = category;
        
        new Thread(() -> {
            List<ForumService.Topic> topics = forumService.getTopics(category.getId());
            
            Platform.runLater(() -> {
                // Обновляем заголовок
                Label title = (Label) ((HBox) topicsView.getChildren().get(0)).getChildren().get(0);
                title.setText(category.getIcon() + " " + category.getName());
                
                // Загружаем темы
                ScrollPane scrollPane = (ScrollPane) topicsView.getChildren().get(1);
                VBox topicsList = (VBox) scrollPane.getContent();
                topicsList.getChildren().clear();
                
                for (ForumService.Topic topic : topics) {
                    HBox topicRow = createTopicRow(topic);
                    topicsList.getChildren().add(topicRow);
                }
                
                // Переключаемся на вид тем
                categoriesView.setVisible(false);
                topicsView.setVisible(true);
                postsView.setVisible(false);
            });
        }).start();
    }

    private HBox createTopicRow(ForumService.Topic topic) {
        HBox row = new HBox(15);
        row.setPadding(new Insets(15));
        row.getStyleClass().add("forum-topic-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setOnMouseClicked(e -> openTopic(topic));
        
        VBox content = new VBox(8);
        content.setPrefWidth(600);
        
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        
        if (topic.isPinned()) {
            Label pinIcon = new Label("📌");
            pinIcon.setStyle("-fx-font-size: 16px;");
            titleRow.getChildren().add(pinIcon);
        }
        
        Label title = new Label(topic.getTitle());
        title.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        title.setWrapText(true);
        titleRow.getChildren().add(title);
        
        Label author = new Label("Автор: " + topic.getAuthorUsername());
        author.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 13px;");
        
        HBox stats = new HBox(15);
        stats.setAlignment(Pos.CENTER_LEFT);
        
        Label views = new Label("👁 " + topic.getViews());
        views.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.6); -fx-font-size: 12px;");
        
        Label replies = new Label("💬 " + topic.getRepliesCount());
        replies.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.6); -fx-font-size: 12px;");
        
        stats.getChildren().addAll(views, replies);
        
        content.getChildren().addAll(titleRow, author, stats);
        row.getChildren().add(content);
        
        return row;
    }

    private void openTopic(ForumService.Topic topic) {
        currentTopic = topic;
        
        new Thread(() -> {
            List<ForumService.Post> posts = forumService.getPosts(topic.getId());
            
            Platform.runLater(() -> {
                // Обновляем заголовок
                Label title = (Label) ((HBox) postsView.getChildren().get(0)).getChildren().get(0);
                title.setText("📌 " + topic.getTitle());
                
                // Загружаем сообщения
                ScrollPane scrollPane = (ScrollPane) postsView.getChildren().get(1);
                VBox postsList = (VBox) scrollPane.getContent();
                postsList.getChildren().clear();
                
                for (ForumService.Post post : posts) {
                    VBox postCard = createPostCard(post);
                    postsList.getChildren().add(postCard);
                }
                
                // Переключаемся на вид сообщений
                categoriesView.setVisible(false);
                topicsView.setVisible(false);
                postsView.setVisible(true);
            });
        }).start();
    }

    private VBox createPostCard(ForumService.Post post) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.getStyleClass().add("forum-post-card");
        card.setPrefWidth(800);
        
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label author = new Label(post.getAuthorUsername());
        author.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        
        Label date = new Label(formatDate(post.getCreatedAt()));
        date.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.6); -fx-font-size: 12px;");
        
        if (post.isEdited()) {
            Label edited = new Label("(редактировано)");
            edited.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.5); -fx-font-size: 11px;");
            header.getChildren().add(edited);
        }
        
        header.getChildren().addAll(author, date);
        
        Label content = new Label(post.getContent());
        content.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.9); -fx-font-size: 14px;");
        content.setWrapText(true);
        
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);
        
        Button likeBtn = new Button((post.isLiked() ? "❤️" : "🤍") + " " + post.getLikesCount());
        likeBtn.getStyleClass().add("forum-action-button");
        likeBtn.setOnAction(e -> toggleLike(post));
        
        actions.getChildren().add(likeBtn);
        
        card.getChildren().addAll(header, content, actions);
        
        return card;
    }

    private void toggleLike(ForumService.Post post) {
        new Thread(() -> {
            boolean success = forumService.likePost(post.getId(), username);
            if (success) {
                // Перезагружаем сообщения
                openTopic(currentTopic);
            }
        }).start();
    }

    private void showNewTopicDialog() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Новая тема");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        TextField titleField = new TextField();
        titleField.setPromptText("Название темы");
        titleField.getStyleClass().add("glass-input");
        titleField.setPrefWidth(500);
        
        TextArea contentArea = new TextArea();
        contentArea.setPromptText("Содержание темы");
        contentArea.getStyleClass().add("glass-input");
        contentArea.setPrefWidth(500);
        contentArea.setPrefHeight(200);
        contentArea.setWrapText(true);
        
        content.getChildren().addAll(
            new Label("Название:"), titleField,
            new Label("Содержание:"), contentArea
        );
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return new Pair<>(titleField.getText(), contentArea.getText());
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(result -> {
            new Thread(() -> {
                boolean success = forumService.createTopic(
                    currentCategory.getId(),
                    result.getKey(),
                    result.getValue(),
                    username
                );
                Platform.runLater(() -> {
                    if (success) {
                        openCategory(currentCategory);
                    }
                });
            }).start();
        });
    }

    private void showNewPostDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Новый ответ");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        TextArea contentArea = new TextArea();
        contentArea.setPromptText("Ваш ответ");
        contentArea.getStyleClass().add("glass-input");
        contentArea.setPrefWidth(500);
        contentArea.setPrefHeight(200);
        contentArea.setWrapText(true);
        
        content.getChildren().add(contentArea);
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return contentArea.getText();
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(result -> {
            new Thread(() -> {
                boolean success = forumService.createPost(currentTopic.getId(), result, username);
                Platform.runLater(() -> {
                    if (success) {
                        openTopic(currentTopic);
                    }
                });
            }).start();
        });
    }

    private void goBack() {
        if (postsView.isVisible()) {
            postsView.setVisible(false);
            topicsView.setVisible(true);
        } else if (topicsView.isVisible()) {
            topicsView.setVisible(false);
            categoriesView.setVisible(true);
        }
    }

    private String formatDate(String dateStr) {
        if (dateStr == null) return "";
        try {
            LocalDateTime date = LocalDateTime.parse(dateStr.replace(" ", "T"));
            return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        } catch (Exception e) {
            return dateStr;
        }
    }

    public VBox getRoot() {
        return root;
    }
    
    // Вспомогательный класс Pair
    private static class Pair<K, V> {
        private final K key;
        private final V value;
        
        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }
        
        public K getKey() { return key; }
        public V getValue() { return value; }
    }
}

