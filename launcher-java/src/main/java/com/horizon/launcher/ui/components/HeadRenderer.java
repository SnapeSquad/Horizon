package com.horizon.launcher.ui.components;

import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import javafx.scene.image.Image;

/**
 * Компонент для отрисовки 3D головы игрока 8x8 пикселей
 * Используется в профилях сообщений форума
 */
public class HeadRenderer extends StackPane {
    private static final Logger logger = LoggerFactory.getLogger(HeadRenderer.class);
    
    private static final double HEAD_SIZE = 8.0; // Размер головы в пикселях на экране
    
    private SubScene subScene;
    private PerspectiveCamera camera;
    private Group headGroup;
    private Box headBox;
    
    private String currentSkinUrl;
    private Image currentSkinImage;
    
    public HeadRenderer(double size) {
        super();
        this.setPrefWidth(size);
        this.setPrefHeight(size);
        
        createHeadModel();
    }
    
    /**
     * Создать 3D модель головы
     */
    private void createHeadModel() {
        headGroup = new Group();
        
        // Создаем куб для головы (стандартные размеры Minecraft головы: 8x8x8 единиц)
        headBox = new Box(HEAD_SIZE, HEAD_SIZE, HEAD_SIZE);
        headBox.setTranslateX(0);
        headBox.setTranslateY(0);
        headBox.setTranslateZ(0);
        
        // Материал для головы
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(Color.web("#8B7355")); // Цвет по умолчанию (Steve)
        material.setSpecularColor(Color.WHITE);
        material.setSpecularPower(32.0);
        headBox.setMaterial(material);
        
        // Поворачиваем голову для лучшего обзора
        Rotate rotateY = new Rotate(-25, Rotate.Y_AXIS);
        Rotate rotateX = new Rotate(-15, Rotate.X_AXIS);
        headBox.getTransforms().addAll(rotateY, rotateX);
        
        headGroup.getChildren().add(headBox);
        
        // Создаем SubScene для 3D рендеринга
        subScene = new SubScene(headGroup, HEAD_SIZE, HEAD_SIZE, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.TRANSPARENT);
        
        // Настройка камеры
        camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(1000.0);
        camera.setFieldOfView(45);
        camera.setTranslateZ(-30);
        camera.setTranslateY(0);
        camera.setTranslateX(0);
        subScene.setCamera(camera);
        
        this.getChildren().add(subScene);
    }
    
    /**
     * Загрузить скин по URL и применить к голове
     */
    public void setSkinUrl(String skinUrl) {
        if (skinUrl == null || skinUrl.equals(currentSkinUrl)) {
            return;
        }
        
        this.currentSkinUrl = skinUrl;
        
        // Загружаем скин асинхронно
        loadSkinAsync(skinUrl);
    }
    
    /**
     * Установить скин по имени пользователя (загружает из Mojang API)
     */
    public void setSkinByUsername(String username) {
        try {
            String skinUrl = "https://crafatar.com/avatars/" + username + "?size=64&overlay";
            setSkinUrl(skinUrl);
        } catch (Exception e) {
            logger.error("Ошибка при загрузке скина для пользователя: " + username, e);
        }
    }
    
    /**
     * Асинхронная загрузка скина
     */
    private void loadSkinAsync(String skinUrl) {
        javafx.concurrent.Task<Image> task = new javafx.concurrent.Task<Image>() {
            @Override
            protected Image call() throws Exception {
                try {
                    return new Image(skinUrl, true); // true = фоновая загрузка
                } catch (Exception e) {
                    logger.error("Ошибка при загрузке скина: " + skinUrl, e);
                    return null;
                }
            }
            
            @Override
            protected void succeeded() {
                Image image = getValue();
                if (image != null && !image.isError()) {
                    applySkinToHead(image);
                }
            }
        };
        
        new Thread(task).start();
    }
    
    /**
     * Применить текстуру скина к голове
     */
    private void applySkinToHead(Image skinImage) {
        if (skinImage == null || headBox == null) {
            return;
        }
        
        try {
            // Применяем текстуру напрямую к материалу
            PhongMaterial material = new PhongMaterial();
            material.setDiffuseMap(skinImage);
            material.setSpecularColor(Color.WHITE);
            material.setSpecularPower(32.0);
            headBox.setMaterial(material);
            
            this.currentSkinImage = skinImage;
        } catch (Exception e) {
            logger.error("Ошибка при применении текстуры к голове", e);
        }
    }
    
    /**
     * Получить размер головы в пикселях
     */
    public double getHeadSize() {
        return HEAD_SIZE;
    }
    
    /**
     * Установить размер головы (для масштабирования)
     */
    public void setHeadSize(double size) {
        this.setPrefWidth(size);
        this.setPrefHeight(size);
        if (subScene != null) {
            subScene.setWidth(size);
            subScene.setHeight(size);
        }
    }
}
