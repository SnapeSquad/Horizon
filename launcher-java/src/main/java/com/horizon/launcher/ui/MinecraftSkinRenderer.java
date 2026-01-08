package com.horizon.launcher.ui;

import javafx.animation.AnimationTimer;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Рендерер 3D модели скина Minecraft персонажа
 * Поддерживает классические и slim модели
 */
public class MinecraftSkinRenderer {
    private static final Logger logger = LoggerFactory.getLogger(MinecraftSkinRenderer.class);
    
    private Group root;
    private Group playerModel;
    private SubScene subScene;
    private PerspectiveCamera camera;
    
    private double anchorX, anchorY;
    private double anchorAngleX = 0;
    private double anchorAngleY = 0;
    private double angleX = -20;
    private double angleY = -30;
    
    private Rotate rotateX;
    private Rotate rotateY;
    
    private AnimationTimer breathingAnimation;
    private boolean isSlimModel = false;
    
    private Map<String, Box> bodyParts = new HashMap<>();
    
    public MinecraftSkinRenderer(double width, double height) {
        root = new Group();
        playerModel = new Group();
        
        // Настройка камеры
        camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(10000.0);
        camera.setFieldOfView(30);
        camera.getTransforms().addAll(
            new Translate(0, -8, -100)
        );
        
        // Вращение модели
        rotateX = new Rotate(angleX, Rotate.X_AXIS);
        rotateY = new Rotate(angleY, Rotate.Y_AXIS);
        playerModel.getTransforms().addAll(rotateY, rotateX);
        
        root.getChildren().add(playerModel);
        
        // Создаем SubScene
        subScene = new SubScene(root, width, height, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.TRANSPARENT);
        subScene.setCamera(camera);
        
        // Обработка мыши для вращения
        setupMouseControl();
        
        // Создаем модель персонажа
        createPlayerModel();
        
        // Добавляем освещение
        addLighting();
        
        // Запускаем анимацию дыхания
        startBreathingAnimation();
    }
    
    /**
     * Создает базовую модель персонажа
     */
    private void createPlayerModel() {
        // Голова (8x8x8)
        Box head = createBox(8, 8, 8, 0, 0, 0);
        head.getTransforms().add(new Translate(0, -10, 0));
        bodyParts.put("head", head);
        
        // Тело (8x12x4)
        Box body = createBox(8, 12, 4, 0, 6, 0);
        bodyParts.put("body", body);
        
        // Правая рука (4x12x4 для classic, 3x12x4 для slim)
        Box rightArm = createBox(4, 12, 4, -6, 4, 0);
        bodyParts.put("rightArm", rightArm);
        
        // Левая рука (4x12x4 для classic, 3x12x4 для slim)
        Box leftArm = createBox(4, 12, 4, 6, 4, 0);
        bodyParts.put("leftArm", leftArm);
        
        // Правая нога (4x12x4)
        Box rightLeg = createBox(4, 12, 4, -2, 18, 0);
        bodyParts.put("rightLeg", rightLeg);
        
        // Левая нога (4x12x4)
        Box leftLeg = createBox(4, 12, 4, 2, 18, 0);
        bodyParts.put("leftLeg", leftLeg);
        
        playerModel.getChildren().addAll(head, body, rightArm, leftArm, rightLeg, leftLeg);
    }
    
    /**
     * Создает Box с заданными параметрами
     */
    private Box createBox(double width, double height, double depth, double x, double y, double z) {
        Box box = new Box(width, height, depth);
        box.setTranslateX(x);
        box.setTranslateY(y);
        box.setTranslateZ(z);
        
        // Дефолтный материал
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(Color.web("#C6A080")); // Цвет кожи Steve
        box.setMaterial(material);
        
        return box;
    }
    
    /**
     * Добавляет освещение в сцену
     */
    private void addLighting() {
        // Ambient light - общее освещение
        AmbientLight ambientLight = new AmbientLight(Color.web("#404040"));
        
        // Point light - точечный свет спереди
        PointLight frontLight = new PointLight(Color.web("#FFFFFF"));
        frontLight.setTranslateZ(-50);
        frontLight.setTranslateY(-20);
        
        // Point light - точечный свет сзади для объема
        PointLight backLight = new PointLight(Color.web("#808080"));
        backLight.setTranslateZ(50);
        
        root.getChildren().addAll(ambientLight, frontLight, backLight);
    }
    
    /**
     * Настройка управления мышью
     */
    private void setupMouseControl() {
        subScene.setOnMousePressed(event -> {
            anchorX = event.getSceneX();
            anchorY = event.getSceneY();
            anchorAngleX = angleX;
            anchorAngleY = angleY;
        });
        
        subScene.setOnMouseDragged(event -> {
            angleY = anchorAngleY + (anchorX - event.getSceneX()) * 0.5;
            angleX = anchorAngleX + (event.getSceneY() - anchorY) * 0.5;
            
            // Ограничиваем вращение по X
            if (angleX > 90) angleX = 90;
            if (angleX < -90) angleX = -90;
            
            rotateX.setAngle(angleX);
            rotateY.setAngle(angleY);
        });
        
        // Zoom с помощью колеса мыши
        subScene.setOnScroll(event -> {
            double delta = event.getDeltaY();
            double scale = playerModel.getScaleX();
            
            if (delta > 0) {
                scale *= 1.1;
            } else {
                scale *= 0.9;
            }
            
            // Ограничиваем масштаб
            if (scale < 0.5) scale = 0.5;
            if (scale > 3.0) scale = 3.0;
            
            playerModel.setScaleX(scale);
            playerModel.setScaleY(scale);
            playerModel.setScaleZ(scale);
        });
    }
    
    /**
     * Анимация дыхания (небольшое покачивание)
     */
    private void startBreathingAnimation() {
        breathingAnimation = new AnimationTimer() {
            private long lastUpdate = 0;
            private double breathPhase = 0;
            
            @Override
            public void handle(long now) {
                if (lastUpdate == 0) {
                    lastUpdate = now;
                    return;
                }
                
                double deltaTime = (now - lastUpdate) / 1_000_000_000.0;
                lastUpdate = now;
                
                breathPhase += deltaTime * 2; // Скорость дыхания
                
                // Небольшое движение вверх-вниз
                double breathOffset = Math.sin(breathPhase) * 0.3;
                playerModel.setTranslateY(breathOffset);
            }
        };
        
        breathingAnimation.start();
    }
    
    /**
     * Загружает скин из URL
     */
    public void loadSkin(String skinUrl) {
        try {
            logger.info("Загрузка скина: {}", skinUrl);
            URL url = new URL(skinUrl);
            Image skinImage = new Image(url.openStream());
            applySkinTexture(skinImage);
        } catch (Exception e) {
            logger.error("Ошибка загрузки скина", e);
            loadDefaultSkin();
        }
    }
    
    /**
     * Загружает скин из массива байтов
     */
    public void loadSkin(byte[] skinData) {
        try {
            Image skinImage = new Image(new ByteArrayInputStream(skinData));
            applySkinTexture(skinImage);
        } catch (Exception e) {
            logger.error("Ошибка загрузки скина из данных", e);
            loadDefaultSkin();
        }
    }
    
    /**
     * Загружает скин из InputStream
     */
    public void loadSkin(InputStream skinStream) {
        try {
            Image skinImage = new Image(skinStream);
            applySkinTexture(skinImage);
        } catch (Exception e) {
            logger.error("Ошибка загрузки скина из потока", e);
            loadDefaultSkin();
        }
    }
    
    /**
     * Загружает дефолтный скин Steve
     */
    public void loadDefaultSkin() {
        try {
            InputStream defaultSkin = getClass().getResourceAsStream("/images/steve.png");
            if (defaultSkin != null) {
                Image skinImage = new Image(defaultSkin);
                applySkinTexture(skinImage);
            } else {
                logger.warn("Дефолтный скин не найден, используем цвета");
            }
        } catch (Exception e) {
            logger.error("Ошибка загрузки дефолтного скина", e);
        }
    }
    
    /**
     * Применяет текстуру скина к модели
     */
    private void applySkinTexture(Image skinImage) {
        // Здесь должна быть логика наложения текстуры на каждую часть тела
        // Для упрощения используем общий цвет
        // В полноценной реализации нужно вырезать нужные части из текстуры
        
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(skinImage);
        
        // Применяем материал ко всем частям тела
        for (Box part : bodyParts.values()) {
            part.setMaterial(material);
        }
        
        logger.info("Текстура скина применена");
    }
    
    /**
     * Устанавливает модель как slim (Alex)
     */
    public void setSlimModel(boolean slim) {
        if (this.isSlimModel == slim) return;
        
        this.isSlimModel = slim;
        
        // Пересоздаем руки с правильной шириной
        playerModel.getChildren().removeAll(bodyParts.get("rightArm"), bodyParts.get("leftArm"));
        
        double armWidth = slim ? 3 : 4;
        double armOffset = slim ? -5.5 : -6;
        
        Box rightArm = createBox(armWidth, 12, 4, armOffset, 4, 0);
        Box leftArm = createBox(armWidth, 12, 4, -armOffset, 4, 0);
        
        bodyParts.put("rightArm", rightArm);
        bodyParts.put("leftArm", leftArm);
        
        playerModel.getChildren().addAll(rightArm, leftArm);
        
        logger.info("Модель изменена на: {}", slim ? "slim (Alex)" : "classic (Steve)");
    }
    
    /**
     * Возвращает SubScene для встраивания в интерфейс
     */
    public SubScene getSubScene() {
        return subScene;
    }
    
    /**
     * Останавливает анимацию
     */
    public void stop() {
        if (breathingAnimation != null) {
            breathingAnimation.stop();
        }
    }
    
    /**
     * Сбрасывает вращение модели
     */
    public void resetRotation() {
        angleX = -20;
        angleY = -30;
        rotateX.setAngle(angleX);
        rotateY.setAngle(angleY);
        playerModel.setScaleX(1.0);
        playerModel.setScaleY(1.0);
        playerModel.setScaleZ(1.0);
    }
}

