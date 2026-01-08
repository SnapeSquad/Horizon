package com.horizon.launcher.ui;

import com.horizon.launcher.api.SkinService;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Компонент для 3D превью скина Minecraft
 */
public class SkinPreview3D extends StackPane {
    private static final Logger logger = LoggerFactory.getLogger(SkinPreview3D.class);
    private Group root3D;
    private SubScene subScene;
    private Rotate rotateX;
    private Rotate rotateY;
    private double mousePosX, mousePosY;
    private double mouseOldX, mouseOldY;
    private Box head, body, leftArm, rightArm, leftLeg, rightLeg;
    private javafx.animation.AnimationTimer autoRotateTimer;
    private boolean isSlim = false;
    private Image fullSkinImage;

    public SkinPreview3D() {
        create3DPreview();
    }

    public void setSlimModel(boolean slim) {
        this.isSlim = slim;
        recreateModel();
    }

    private void create3DPreview() {
        // Создаем 3D сцену
        root3D = new Group();
        
        // Камера
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setFieldOfView(45);
        camera.setNearClip(0.1);
        camera.setFarClip(1000);
        camera.getTransforms().add(new Translate(0, 0, -200));
        
        // Создаем SubScene для 3D
        subScene = new SubScene(root3D, 400, 400, true, SceneAntialiasing.BALANCED);
        subScene.setCamera(camera);
        subScene.setFill(Color.TRANSPARENT);
        
        // Вращение
        rotateX = new Rotate(20, Rotate.X_AXIS);
        rotateY = new Rotate(0, Rotate.Y_AXIS);
        root3D.getTransforms().addAll(rotateX, rotateY);
        
        // Создаем модель игрока
        createPlayerModel();
        
        // Обработка мыши для вращения
        setOnMousePressed(e -> {
            if (autoRotateTimer != null) {
                autoRotateTimer.stop();
            }
            mouseOldX = e.getSceneX();
            mouseOldY = e.getSceneY();
        });
        
        setOnMouseDragged(e -> {
            mousePosX = e.getSceneX();
            mousePosY = e.getSceneY();
            
            double deltaX = mousePosX - mouseOldX;
            double deltaY = mousePosY - mouseOldY;
            
            rotateY.setAngle(rotateY.getAngle() + deltaX * 0.5);
            rotateX.setAngle(Math.max(-90, Math.min(90, rotateX.getAngle() - deltaY * 0.5)));
            
            mouseOldX = mousePosX;
            mouseOldY = mousePosY;
        });
        
        setOnMouseReleased(e -> {
            // Возобновляем автовращение через 2 секунды
            javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    Thread.sleep(2000);
                    return null;
                }
            };
            task.setOnSucceeded(event -> startAutoRotation());
            new Thread(task).start();
        });
        
        getChildren().add(subScene);
    }

    private void recreateModel() {
        // Удаляем старую модель
        root3D.getChildren().clear();
        // Создаем новую модель
        createPlayerModel();
        // Применяем текстуру если она была загружена
        if (fullSkinImage != null) {
            applySkinTexture(fullSkinImage);
        }
    }

    private void createPlayerModel() {
        // Создаем части тела как отдельные объекты для правильного применения текстур
        // Голова (8x8x8 блоков)
        head = new Box(16, 16, 16);
        head.setTranslateY(-30);
        head.setMaterial(createDefaultMaterial());
        root3D.getChildren().add(head);
        
        // Тело (8x12x4 блока)
        body = new Box(16, 24, 8);
        body.setTranslateY(0);
        body.setMaterial(createDefaultMaterial());
        root3D.getChildren().add(body);
        
        // Левая рука (4x12x4 блока для classic, 3x12x3 для slim)
        double armWidth = isSlim ? 6 : 8;
        double armDepth = isSlim ? 6 : 8;
        leftArm = new Box(armWidth, 24, armDepth);
        leftArm.setTranslateX(-12);
        leftArm.setTranslateY(-2);
        leftArm.setMaterial(createDefaultMaterial());
        root3D.getChildren().add(leftArm);
        
        // Правая рука (4x12x4 блока для classic, 3x12x3 для slim)
        rightArm = new Box(armWidth, 24, armDepth);
        rightArm.setTranslateX(12);
        rightArm.setTranslateY(-2);
        rightArm.setMaterial(createDefaultMaterial());
        root3D.getChildren().add(rightArm);
        
        // Левая нога (4x12x4 блока)
        leftLeg = new Box(8, 24, 8);
        leftLeg.setTranslateX(-4);
        leftLeg.setTranslateY(18);
        leftLeg.setMaterial(createDefaultMaterial());
        root3D.getChildren().add(leftLeg);
        
        // Правая нога (4x12x4 блока)
        rightLeg = new Box(8, 24, 8);
        rightLeg.setTranslateX(4);
        rightLeg.setTranslateY(18);
        rightLeg.setMaterial(createDefaultMaterial());
        root3D.getChildren().add(rightLeg);
    }

    private PhongMaterial createDefaultMaterial() {
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(Color.LIGHTGRAY);
        material.setSpecularColor(Color.WHITE);
        return material;
    }

    /**
     * Загружает скин для пользователя
     */
    public void loadSkin(String username) {
        new Thread(() -> {
            try {
                SkinService skinService = new SkinService();
                String skinUrl = skinService.getSkinUrlByUsername(username);
                
                logger.info("Загрузка скина из URL: {}", skinUrl);
                
                if (skinUrl != null && !skinUrl.isEmpty()) {
                    Image skinImage = new Image(skinUrl, true);
                    
                    // Ждем загрузки изображения
                    skinImage.progressProperty().addListener((obs, oldVal, newVal) -> {
                        if (newVal.doubleValue() == 1.0 && !skinImage.isError()) {
                            Platform.runLater(() -> {
                                fullSkinImage = skinImage;
                                applySkinTexture(skinImage);
                            });
                        }
                    });
                    
                    // Если изображение уже загружено
                    if (skinImage.getProgress() == 1.0 && !skinImage.isError()) {
                        Platform.runLater(() -> {
                            fullSkinImage = skinImage;
                            applySkinTexture(skinImage);
                        });
                    }
                } else {
                    logger.warn("URL скина пустой, используем дефолтный материал");
                }
            } catch (Exception e) {
                logger.error("Ошибка загрузки скина", e);
            }
        }).start();
    }

    private void applySkinTexture(Image skinImage) {
        if (skinImage == null || skinImage.isError()) {
            logger.warn("Изображение скина не загружено или содержит ошибки");
            return;
        }
        
        try {
            double skinWidth = skinImage.getWidth();
            double skinHeight = skinImage.getHeight();
            
            // Определяем формат скина (64x64 или 64x32)
            boolean is64x64 = skinHeight >= 64;
            
            // Координаты для разных частей тела в текстуре скина
            // Формат: x, y, width, height (в пикселях текстуры)
            int headX = 8, headY = 8, headSize = 8;
            int bodyX = 20, bodyY = 20, bodyW = 8, bodyH = 12;
            int armX = 44, armY = 20, armW = 4, armH = 12;
            int legX = 4, legY = 20, legW = 4, legH = 12;
            
            // Если скин 64x32 (старый формат), корректируем координаты
            if (!is64x64) {
                // В старом формате координаты немного другие
                headX = 8; headY = 8; headSize = 8;
                bodyX = 20; bodyY = 20; bodyW = 8; bodyH = 12;
                armX = 44; armY = 20; armW = 4; armH = 12;
                legX = 4; legY = 20; legW = 4; legH = 12;
            }
            
            PixelReader reader = skinImage.getPixelReader();
            
            // Создаем материалы для каждой части тела из соответствующих частей скина
            if (head != null) {
                WritableImage headTexture = extractTexturePart(reader, headX, headY, headSize, headSize, skinWidth, skinHeight);
                PhongMaterial headMaterial = new PhongMaterial();
                headMaterial.setDiffuseMap(headTexture);
                headMaterial.setSpecularColor(Color.WHITE);
                headMaterial.setSpecularPower(32);
                head.setMaterial(headMaterial);
            }
            
            if (body != null) {
                WritableImage bodyTexture = extractTexturePart(reader, bodyX, bodyY, bodyW, bodyH, skinWidth, skinHeight);
                PhongMaterial bodyMaterial = new PhongMaterial();
                bodyMaterial.setDiffuseMap(bodyTexture);
                bodyMaterial.setSpecularColor(Color.WHITE);
                bodyMaterial.setSpecularPower(32);
                body.setMaterial(bodyMaterial);
            }
            
            if (leftArm != null) {
                WritableImage armTexture = extractTexturePart(reader, armX, armY, armW, armH, skinWidth, skinHeight);
                PhongMaterial armMaterial = new PhongMaterial();
                armMaterial.setDiffuseMap(armTexture);
                armMaterial.setSpecularColor(Color.WHITE);
                armMaterial.setSpecularPower(32);
                leftArm.setMaterial(armMaterial);
            }
            
            if (rightArm != null) {
                // Правая рука - зеркально от левой
                WritableImage armTexture = extractTexturePart(reader, armX, armY, armW, armH, skinWidth, skinHeight);
                PhongMaterial armMaterial = new PhongMaterial();
                armMaterial.setDiffuseMap(armTexture);
                armMaterial.setSpecularColor(Color.WHITE);
                armMaterial.setSpecularPower(32);
                rightArm.setMaterial(armMaterial);
            }
            
            if (leftLeg != null) {
                WritableImage legTexture = extractTexturePart(reader, legX, legY, legW, legH, skinWidth, skinHeight);
                PhongMaterial legMaterial = new PhongMaterial();
                legMaterial.setDiffuseMap(legTexture);
                legMaterial.setSpecularColor(Color.WHITE);
                legMaterial.setSpecularPower(32);
                leftLeg.setMaterial(legMaterial);
            }
            
            if (rightLeg != null) {
                WritableImage legTexture = extractTexturePart(reader, legX, legY, legW, legH, skinWidth, skinHeight);
                PhongMaterial legMaterial = new PhongMaterial();
                legMaterial.setDiffuseMap(legTexture);
                legMaterial.setSpecularColor(Color.WHITE);
                legMaterial.setSpecularPower(32);
                rightLeg.setMaterial(legMaterial);
            }
            
            logger.info("Текстура скина успешно применена к модели");
        } catch (Exception e) {
            logger.error("Ошибка применения текстуры скина", e);
            // В случае ошибки применяем полную текстуру ко всем частям
            PhongMaterial fallbackMaterial = new PhongMaterial();
            fallbackMaterial.setDiffuseMap(skinImage);
            fallbackMaterial.setSpecularColor(Color.WHITE);
            if (head != null) head.setMaterial(fallbackMaterial);
            if (body != null) body.setMaterial(fallbackMaterial);
            if (leftArm != null) leftArm.setMaterial(fallbackMaterial);
            if (rightArm != null) rightArm.setMaterial(fallbackMaterial);
            if (leftLeg != null) leftLeg.setMaterial(fallbackMaterial);
            if (rightLeg != null) rightLeg.setMaterial(fallbackMaterial);
        }
    }

    /**
     * Извлекает часть текстуры из скина
     */
    private WritableImage extractTexturePart(PixelReader reader, int x, int y, int width, int height, 
                                            double skinWidth, double skinHeight) {
        // Масштабируем координаты если скин больше стандартного размера
        double scaleX = skinWidth / 64.0;
        double scaleY = skinHeight / (skinHeight >= 64 ? 64.0 : 32.0);
        
        int scaledX = (int)(x * scaleX);
        int scaledY = (int)(y * scaleY);
        int scaledW = (int)(width * scaleX);
        int scaledH = (int)(height * scaleY);
        
        // Проверяем границы
        scaledX = Math.max(0, Math.min(scaledX, (int)skinWidth - 1));
        scaledY = Math.max(0, Math.min(scaledY, (int)skinHeight - 1));
        scaledW = Math.min(scaledW, (int)skinWidth - scaledX);
        scaledH = Math.min(scaledH, (int)skinHeight - scaledY);
        
        if (scaledW <= 0 || scaledH <= 0) {
            // Возвращаем дефолтное изображение
            WritableImage defaultImg = new WritableImage(8, 8);
            return defaultImg;
        }
        
        WritableImage part = new WritableImage(reader, scaledX, scaledY, scaledW, scaledH);
        return part;
    }

    /**
     * Автоматическое вращение
     */
    public void startAutoRotation() {
        if (autoRotateTimer != null) {
            autoRotateTimer.stop();
        }
        
        autoRotateTimer = new javafx.animation.AnimationTimer() {
            @Override
            public void handle(long now) {
                rotateY.setAngle(rotateY.getAngle() + 0.3);
            }
        };
        autoRotateTimer.start();
    }
    
    public void stopAutoRotation() {
        if (autoRotateTimer != null) {
            autoRotateTimer.stop();
        }
    }
}
