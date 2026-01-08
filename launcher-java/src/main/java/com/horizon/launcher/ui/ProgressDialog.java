package com.horizon.launcher.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Диалог прогресса с процентами и временем
 */
public class ProgressDialog {
    private Stage stage;
    private ProgressBar progressBar;
    private Label statusLabel;
    private Label percentLabel;
    private Label timeLabel;
    private long startTime;
    private double currentProgress = 0.0;

    public ProgressDialog(String title) {
        stage = new Stage();
        stage.setTitle(title);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setWidth(500);
        stage.setHeight(200);
        stage.setResizable(false);

        VBox root = new VBox(15);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("glass-card");
        root.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8); -fx-background-radius: 20;");

        // Заголовок
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        root.getChildren().add(titleLabel);

        // Статус
        statusLabel = new Label("Инициализация...");
        statusLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.9); -fx-font-size: 14px;");
        root.getChildren().add(statusLabel);

        // Прогресс-бар
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(440);
        progressBar.setPrefHeight(25);
        progressBar.getStyleClass().add("glass-progress");
        root.getChildren().add(progressBar);

        // Проценты и время
        HBox infoBox = new HBox(20);
        infoBox.setAlignment(Pos.CENTER);
        
        percentLabel = new Label("0%");
        percentLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 600;");
        
        timeLabel = new Label("Осталось: ~0 сек");
        timeLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 12px;");
        
        infoBox.getChildren().addAll(percentLabel, timeLabel);
        root.getChildren().add(infoBox);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        stage.setScene(scene);
        
        startTime = System.currentTimeMillis();
    }

    public void updateProgress(double progress, String status) {
        Platform.runLater(() -> {
            currentProgress = Math.max(0, Math.min(1, progress));
            progressBar.setProgress(currentProgress);
            
            int percent = (int) (currentProgress * 100);
            percentLabel.setText(percent + "%");
            
            if (status != null && !status.isEmpty()) {
                statusLabel.setText(status);
            }
            
            // Вычисляем оставшееся время
            long elapsed = System.currentTimeMillis() - startTime;
            if (currentProgress > 0.01) {
                long estimatedTotal = (long) (elapsed / currentProgress);
                long remaining = estimatedTotal - elapsed;
                
                if (remaining < 1000) {
                    timeLabel.setText("Завершение...");
                } else if (remaining < 60000) {
                    timeLabel.setText("Осталось: ~" + (remaining / 1000) + " сек");
                } else {
                    timeLabel.setText("Осталось: ~" + (remaining / 60000) + " мин");
                }
            } else {
                timeLabel.setText("Вычисление...");
            }
        });
    }

    public void show() {
        Platform.runLater(() -> {
            stage.show();
            stage.centerOnScreen();
        });
    }

    public void close() {
        Platform.runLater(() -> {
            stage.close();
        });
    }

    public boolean isShowing() {
        return stage.isShowing();
    }
}

