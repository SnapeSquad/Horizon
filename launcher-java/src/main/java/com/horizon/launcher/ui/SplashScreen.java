package com.horizon.launcher.ui;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Экран загрузки (Splash) для автоматического входа
 */
public class SplashScreen extends Stage {
    private static final Logger logger = LoggerFactory.getLogger(SplashScreen.class);
    
    private ProgressIndicator progressIndicator;
    private Label statusLabel;
    
    public SplashScreen() {
        initStyle(StageStyle.TRANSPARENT);
        setTitle("Horizon Launcher - Загрузка");
        setWidth(400);
        setHeight(300);
        
        createUI();
    }
    
    private void createUI() {
        StackPane root = new StackPane();
        root.setStyle(
            "-fx-background-color: linear-gradient(" +
            "from 0% 0% to 100% 100%, " +
            "rgba(102, 126, 234, 0.3) 0%, " +
            "rgba(118, 75, 162, 0.3) 100%" +
            ");"
        );
        
        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPrefSize(400, 300);
        
        Label titleLabel = new Label("Horizon Launcher");
        titleLabel.setStyle(
            "-fx-text-fill: white; " +
            "-fx-font-size: 32px; " +
            "-fx-font-weight: bold; " +
            "-fx-font-family: 'Minecraft Unicode', -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Segoe UI', Roboto, sans-serif;"
        );
        
        progressIndicator = new ProgressIndicator();
        progressIndicator.setStyle("-fx-progress-color: white;");
        
        statusLabel = new Label("Проверка сессии...");
        statusLabel.setStyle(
            "-fx-text-fill: rgba(255, 255, 255, 0.8); " +
            "-fx-font-size: 14px; " +
            "-fx-font-family: 'Minecraft Unicode', -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Segoe UI', Roboto, sans-serif;"
        );
        
        content.getChildren().addAll(titleLabel, progressIndicator, statusLabel);
        root.getChildren().add(content);
        
        Scene scene = new Scene(root, 400, 300);
        scene.setFill(Color.TRANSPARENT);
        setScene(scene);
        
        centerOnScreen();
    }
    
    public void setStatus(String status) {
        statusLabel.setText(status);
    }
}
