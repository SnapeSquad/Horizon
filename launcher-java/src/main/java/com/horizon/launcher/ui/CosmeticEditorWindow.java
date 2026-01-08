package com.horizon.launcher.ui;

import com.horizon.launcher.api.ApiClient;
import com.horizon.launcher.api.CosmeticService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Редактор анимированной косметики
 */
public class CosmeticEditorWindow {
    private static final Logger logger = LoggerFactory.getLogger(CosmeticEditorWindow.class);
    private Stage stage;
    private String username;
    private List<AnimationFrame> frames;
    private TimelinePane timelinePane;
    private PreviewPane previewPane;
    private TextField idField;
    private TextField nameField;
    private ComboBox<String> typeCombo;
    private ComboBox<String> animTypeCombo;
    private Spinner<Integer> durationSpinner;
    private CheckBox loopCheck;
    private CosmeticService cosmeticService;
    
    public CosmeticEditorWindow(String username) {
        this.username = username;
        this.frames = new ArrayList<>();
        this.cosmeticService = new CosmeticService();
        createWindow();
    }

    private void createWindow() {
        stage = new Stage();
        stage.setTitle("Редактор косметики");
        stage.setWidth(1400);
        stage.setHeight(900);
        stage.initStyle(StageStyle.UNDECORATED);
        
        BorderPane root = new BorderPane();
        root.getStyleClass().add("animated-background");
        
        // Заголовок
        HBox header = createHeader();
        root.setTop(header);
        
        // Основной контент
        HBox mainContent = new HBox(20);
        mainContent.setPadding(new Insets(20));
        
        // Левая панель - свойства
        VBox propertiesPane = createPropertiesPane();
        propertiesPane.setPrefWidth(300);
        
        // Центр - превью
        previewPane = new PreviewPane();
        previewPane.setPrefWidth(500);
        
        // Правая панель - таймлайн
        timelinePane = new TimelinePane();
        timelinePane.setPrefWidth(500);
        
        mainContent.getChildren().addAll(propertiesPane, previewPane, timelinePane);
        root.setCenter(mainContent);
        
        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        stage.setScene(scene);
    }

    private HBox createHeader() {
        HBox header = new HBox(15);
        header.setPadding(new Insets(15, 20, 15, 20));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("glass-topbar");
        
        Label title = new Label("🎨 Редактор косметики");
        title.getStyleClass().add("title-text");
        title.setStyle("-fx-font-size: 24px;");
        
        Button saveBtn = new Button("💾 Сохранить");
        saveBtn.getStyleClass().add("ios-button");
        saveBtn.setOnAction(e -> saveCosmetic());
        
        Button closeBtn = new Button("×");
        closeBtn.getStyleClass().addAll("window-control", "window-control-close");
        closeBtn.setOnAction(e -> stage.close());
        
        header.getChildren().addAll(title, new Region(), saveBtn, closeBtn);
        return header;
    }

    private VBox createPropertiesPane() {
        VBox pane = new VBox(15);
        pane.setPadding(new Insets(20));
        pane.getStyleClass().add("glass-card");
        
        Label title = new Label("Свойства");
        title.getStyleClass().add("section-title");
        pane.getChildren().add(title);
        
        // ID косметики
        idField = new TextField();
        idField.setPromptText("ID косметики");
        idField.getStyleClass().add("glass-input");
        pane.getChildren().addAll(new Label("ID:"), idField);
        
        // Название
        nameField = new TextField();
        nameField.setPromptText("Название");
        nameField.getStyleClass().add("glass-input");
        pane.getChildren().addAll(new Label("Название:"), nameField);
        
        // Тип
        typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("hat", "cape", "badge", "trail", "weapon", "accessory");
        typeCombo.getStyleClass().add("glass-input");
        typeCombo.setValue("hat");
        pane.getChildren().addAll(new Label("Тип:"), typeCombo);
        
        // Тип анимации
        animTypeCombo = new ComboBox<>();
        animTypeCombo.getItems().addAll("rotate", "scale", "translate", "color", "custom");
        animTypeCombo.getStyleClass().add("glass-input");
        animTypeCombo.setValue("rotate");
        pane.getChildren().addAll(new Label("Тип анимации:"), animTypeCombo);
        
        // Длительность
        durationSpinner = new Spinner<>(100, 10000, 1000, 100);
        durationSpinner.getStyleClass().add("glass-input");
        pane.getChildren().addAll(new Label("Длительность (мс):"), durationSpinner);
        
        // Зацикливание
        loopCheck = new CheckBox("Зациклить");
        loopCheck.setSelected(true);
        pane.getChildren().add(loopCheck);
        
        // Кнопка добавления кадра
        Button addFrameBtn = new Button("+ Добавить кадр");
        addFrameBtn.getStyleClass().add("ios-button");
        addFrameBtn.setOnAction(e -> addFrame());
        pane.getChildren().add(addFrameBtn);
        
        return pane;
    }

    private void addFrame() {
        AnimationFrame frame = new AnimationFrame();
        frames.add(frame);
        timelinePane.addFrame(frame);
    }

    private void saveCosmetic() {
        // Валидация полей
        String id = idField.getText().trim();
        String name = nameField.getText().trim();
        
        if (id.isEmpty()) {
            showAlert("Ошибка", "Введите ID косметики", Alert.AlertType.ERROR);
            return;
        }
        
        if (name.isEmpty()) {
            showAlert("Ошибка", "Введите название косметики", Alert.AlertType.ERROR);
            return;
        }
        
        if (frames.isEmpty()) {
            showAlert("Ошибка", "Добавьте хотя бы один кадр анимации", Alert.AlertType.ERROR);
            return;
        }
        
        // Создаем JSON объект косметики
        JsonObject cosmeticData = new JsonObject();
        cosmeticData.addProperty("id", id);
        cosmeticData.addProperty("name", name);
        cosmeticData.addProperty("type", typeCombo.getValue());
        cosmeticData.addProperty("animationType", animTypeCombo.getValue());
        cosmeticData.addProperty("duration", durationSpinner.getValue());
        cosmeticData.addProperty("loop", loopCheck.isSelected());
        
        // Добавляем кадры анимации
        JsonArray framesArray = new JsonArray();
        for (AnimationFrame frame : frames) {
            JsonObject frameObj = new JsonObject();
            frameObj.addProperty("rotation", frame.getRotation());
            frameObj.addProperty("scaleX", frame.getScaleX());
            frameObj.addProperty("scaleY", frame.getScaleY());
            frameObj.addProperty("translateX", frame.getTranslateX());
            frameObj.addProperty("translateY", frame.getTranslateY());
            frameObj.addProperty("duration", frame.getDuration());
            framesArray.add(frameObj);
        }
        cosmeticData.add("frames", framesArray);
        
        // Сохранение через API
        new Thread(() -> {
            try {
                ApiClient client = ApiClient.getInstance();
                ApiClient.ApiResponse response = client.post("/api/cosmetics/save", cosmeticData);
                
                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        showAlert("Успех", "Косметика успешно сохранена!", Alert.AlertType.INFORMATION);
                        logger.info("Косметика {} успешно сохранена для пользователя {}", id, username);
                    } else {
                        showAlert("Ошибка", "Не удалось сохранить косметику: " + response.getBody(), Alert.AlertType.ERROR);
                        logger.error("Ошибка сохранения косметики: {}", response.getBody());
                    }
                });
            } catch (Exception e) {
                logger.error("Ошибка при сохранении косметики", e);
                Platform.runLater(() -> {
                    showAlert("Ошибка", "Ошибка при сохранении: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        }).start();
    }
    
    private void showAlert(String title, String message, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public void show() {
        stage.show();
    }

    // Внутренние классы
    private class PreviewPane extends VBox {
        public PreviewPane() {
            setPadding(new Insets(20));
            getStyleClass().add("glass-card");
            setAlignment(Pos.CENTER);
            
            Label title = new Label("Превью");
            title.getStyleClass().add("section-title");
            getChildren().add(title);
            
            // Превью косметики будет здесь
            StackPane preview = new StackPane();
            preview.setPrefWidth(400);
            preview.setPrefHeight(400);
            preview.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3); -fx-background-radius: 12;");
            getChildren().add(preview);
        }
    }

    private class TimelinePane extends VBox {
        private HBox timeline;
        
        public TimelinePane() {
            setPadding(new Insets(20));
            getStyleClass().add("glass-card");
            
            Label title = new Label("Таймлайн анимации");
            title.getStyleClass().add("section-title");
            getChildren().add(title);
            
            timeline = new HBox(5);
            timeline.setPadding(new Insets(10));
            timeline.setAlignment(Pos.CENTER_LEFT);
            getChildren().add(timeline);
        }
        
        public void addFrame(AnimationFrame frame) {
            VBox frameBox = new VBox(5);
            frameBox.setPadding(new Insets(10));
            frameBox.getStyleClass().add("timeline-frame");
            frameBox.setPrefWidth(80);
            frameBox.setPrefHeight(100);
            
            Label frameLabel = new Label("Кадр " + (timeline.getChildren().size() + 1));
            frameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
            frameBox.getChildren().add(frameLabel);
            
            timeline.getChildren().add(frameBox);
        }
    }

    private class AnimationFrame {
        private double rotation = 0;
        private double scaleX = 1;
        private double scaleY = 1;
        private double translateX = 0;
        private double translateY = 0;
        private Color color = Color.WHITE;
        private int duration = 100;
        
        // Getters and setters
        public double getRotation() { return rotation; }
        public void setRotation(double rotation) { this.rotation = rotation; }
        public double getScaleX() { return scaleX; }
        public void setScaleX(double scaleX) { this.scaleX = scaleX; }
        public double getScaleY() { return scaleY; }
        public void setScaleY(double scaleY) { this.scaleY = scaleY; }
        public double getTranslateX() { return translateX; }
        public void setTranslateX(double translateX) { this.translateX = translateX; }
        public double getTranslateY() { return translateY; }
        public void setTranslateY(double translateY) { this.translateY = translateY; }
        public Color getColor() { return color; }
        public void setColor(Color color) { this.color = color; }
        public int getDuration() { return duration; }
        public void setDuration(int duration) { this.duration = duration; }
    }
}

