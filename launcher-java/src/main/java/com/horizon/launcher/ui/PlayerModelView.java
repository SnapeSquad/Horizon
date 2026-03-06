package com.horizon.launcher.ui;

import com.horizon.launcher.models.ModelData;
import com.horizon.launcher.models.BlockbenchModelParser;
import javafx.animation.RotateTransition;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.scene.AmbientLight;
import javafx.scene.PointLight;
import javafx.application.Platform;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JavaFX компонент для отображения 3D модели игрока Minecraft 1.21-1.21.10 (Steve/Alex)
 * Поддерживает крепление аксессуаров к различным костям
 */
public class PlayerModelView extends SubScene {
    private static final Logger logger = LoggerFactory.getLogger(PlayerModelView.class);
    
    private Group root3D;
    private Group playerGroup;
    private Map<String, Group> boneGroups;
    private Map<String, Group> accessories;
    private PerspectiveCamera camera;
    
    // Размеры модели Steve/Alex (в пикселях, масштабированы для 3D)
    private static final double HEAD_SIZE = 8.0;
    private static final double BODY_WIDTH = 8.0;
    private static final double BODY_HEIGHT = 12.0;
    private static final double BODY_DEPTH = 4.0;
    private static final double ARM_LENGTH = 12.0;
    private static final double ARM_WIDTH = 4.0;
    private static final double LEG_LENGTH = 12.0;
    private static final double LEG_WIDTH = 4.0;
    
    // Смещения для частей тела (относительно центра модели)
    private static final double HEAD_Y = 16.0;
    private static final double BODY_Y = 8.0;
    private static final double ARM_Y = 12.0;
    private static final double LEG_Y = 0.0;
    
    private PhongMaterial skinMaterial;
    private Image skinImage;
    private ModelData baseModel;
    private RotateTransition rotationAnimation;
    
    /**
     * Создать вид 3D модели игрока
     * @param width Ширина вьюпорта
     * @param height Высота вьюпорта
     * @param skinImage Изображение скина игрока (может быть null)
     */
    public PlayerModelView(double width, double height, Image skinImage) {
        super(new Group(), width, height, true, SceneAntialiasing.BALANCED);
        
        this.skinImage = skinImage;
        this.boneGroups = new HashMap<>();
        this.accessories = new HashMap<>();
        this.root3D = (Group) getRoot();
        
        // Создаем материал для кожи
        this.skinMaterial = new PhongMaterial();
        if (skinImage != null) {
            skinMaterial.setDiffuseMap(skinImage);
        } else {
            // Цвет по умолчанию (Steve: #8B7355, Alex: #F2C188)
            skinMaterial.setDiffuseColor(Color.web("#8B7355"));
        }
        skinMaterial.setSpecularColor(Color.WHITE);
        skinMaterial.setSpecularPower(32.0);
        
        // Настройка камеры
        setupCamera();
        
        // Настройка освещения
        setupLighting();
        
        // Создание модели игрока
        createPlayerModel();
        
        // Анимация вращения
        setupRotation();
        
        // Автоматически запускаем вращение
        Platform.runLater(() -> {
            startRotation();
        });
        
        logger.info("PlayerModelView создан: {}x{}", width, height);
    }
    
    /**
     * Создать вид 3D модели игрока без скина
     */
    public PlayerModelView(double width, double height) {
        this(width, height, null);
    }
    
    /**
     * Настройка камеры
     */
    private void setupCamera() {
        camera = new PerspectiveCamera(true);
        camera.setFieldOfView(45.0);
        camera.setNearClip(0.1);
        camera.setFarClip(1000.0);
        
        // Позиция камеры (вид сбоку и слегка сверху)
        camera.setTranslateX(0);
        camera.setTranslateY(-10);
        camera.setTranslateZ(-50);
        
        // Направление камеры на центр модели
        camera.setRotationAxis(new Point3D(1, 0, 0));
        camera.setRotate(10);
        
        setCamera(camera);
    }
    
    /**
     * Настройка освещения для 3D сцены
     */
    private void setupLighting() {
        // Ambient light для общего освещения
        AmbientLight ambientLight = new AmbientLight();
        ambientLight.setColor(Color.WHITE);
        ambientLight.getColor().deriveColor(0, 1, 0.7, 1); // Немного приглушенный
        root3D.getChildren().add(ambientLight);
        
        // Point light для направленного освещения
        PointLight pointLight = new PointLight();
        pointLight.setColor(Color.WHITE);
        pointLight.setTranslateX(30);
        pointLight.setTranslateY(-20);
        pointLight.setTranslateZ(-30);
        root3D.getChildren().add(pointLight);
        
        // Дополнительный свет с другой стороны для лучшей видимости
        PointLight pointLight2 = new PointLight();
        pointLight2.setColor(Color.WHITE.deriveColor(0, 1, 0.8, 1));
        pointLight2.setTranslateX(-30);
        pointLight2.setTranslateY(-20);
        pointLight2.setTranslateZ(-30);
        root3D.getChildren().add(pointLight2);
    }
    
    /**
     * Создание базовой модели игрока Steve/Alex
     */
    private void createPlayerModel() {
        playerGroup = new Group();
        root3D.getChildren().add(playerGroup);
        
        // Голова (head) - 8x8x8
        Group headGroup = createHeadBone();
        boneGroups.put("head", headGroup);
        playerGroup.getChildren().add(headGroup);
        
        // Тело (body) - 8x12x4
        Group bodyGroup = createBodyBone();
        boneGroups.put("body", bodyGroup);
        playerGroup.getChildren().add(bodyGroup);
        
        // Правая рука (right_arm) - 4x12x4
        Group rightArmGroup = createArmBone("right_arm", 8.0);
        boneGroups.put("right_arm", rightArmGroup);
        playerGroup.getChildren().add(rightArmGroup);
        
        // Левая рука (left_arm) - 4x12x4
        Group leftArmGroup = createArmBone("left_arm", -8.0);
        boneGroups.put("left_arm", leftArmGroup);
        playerGroup.getChildren().add(leftArmGroup);
        
        // Правая нога (right_leg) - 4x12x4
        Group rightLegGroup = createLegBone("right_leg", 2.0);
        boneGroups.put("right_leg", rightLegGroup);
        playerGroup.getChildren().add(rightLegGroup);
        
        // Левая нога (left_leg) - 4x12x4
        Group leftLegGroup = createLegBone("left_leg", -2.0);
        boneGroups.put("left_leg", leftLegGroup);
        playerGroup.getChildren().add(leftLegGroup);
        
        logger.debug("Базовая модель игрока создана с {} костями", boneGroups.size());
    }
    
    /**
     * Создать кость головы
     */
    private Group createHeadBone() {
        Group group = new Group();
        
        // Голова - куб 8x8x8
        Box headBox = new Box(HEAD_SIZE, HEAD_SIZE, HEAD_SIZE);
        headBox.setMaterial(skinMaterial);
        
        // Позиционирование головы
        headBox.setTranslateY(HEAD_Y);
        headBox.setTranslateX(0);
        headBox.setTranslateZ(0);
        
        group.getChildren().add(headBox);
        group.getTransforms().add(new Translate(0, 0, 0));
        
        return group;
    }
    
    /**
     * Создать кость тела
     */
    private Group createBodyBone() {
        Group group = new Group();
        
        // Тело - куб 8x12x4
        Box bodyBox = new Box(BODY_WIDTH, BODY_HEIGHT, BODY_DEPTH);
        bodyBox.setMaterial(skinMaterial);
        
        // Позиционирование тела
        bodyBox.setTranslateY(BODY_Y);
        bodyBox.setTranslateX(0);
        bodyBox.setTranslateZ(0);
        
        group.getChildren().add(bodyBox);
        group.getTransforms().add(new Translate(0, 0, 0));
        
        return group;
    }
    
    /**
     * Создать кость руки
     * @param boneName Имя кости (left_arm или right_arm)
     * @param xOffset Смещение по X (для левой/правой руки)
     */
    private Group createArmBone(String boneName, double xOffset) {
        Group group = new Group();
        
        // Рука - куб 4x12x4
        Box armBox = new Box(ARM_WIDTH, ARM_LENGTH, ARM_WIDTH);
        armBox.setMaterial(skinMaterial);
        
        // Позиционирование руки
        armBox.setTranslateY(ARM_Y);
        armBox.setTranslateX(xOffset);
        armBox.setTranslateZ(0);
        
        group.getChildren().add(armBox);
        group.getTransforms().add(new Translate(0, 0, 0));
        
        return group;
    }
    
    /**
     * Создать кость ноги
     * @param boneName Имя кости (left_leg или right_leg)
     * @param xOffset Смещение по X (для левой/правой ноги)
     */
    private Group createLegBone(String boneName, double xOffset) {
        Group group = new Group();
        
        // Нога - куб 4x12x4
        Box legBox = new Box(LEG_WIDTH, LEG_LENGTH, LEG_WIDTH);
        legBox.setMaterial(skinMaterial);
        
        // Позиционирование ноги
        legBox.setTranslateY(LEG_Y);
        legBox.setTranslateX(xOffset);
        legBox.setTranslateZ(0);
        
        group.getChildren().add(legBox);
        group.getTransforms().add(new Translate(0, 0, 0));
        
        return group;
    }
    
    /**
     * Прикрепить аксессуар к указанной кости
     * @param boneName Имя кости (head, body, left_arm, right_arm, left_leg, right_leg)
     * @param modelData Модель аксессуара (может быть null для простых аксессуаров)
     * @param textureImage Текстура аксессуара
     * @return Группа аксессуара или null при ошибке
     */
    public Group attachAccessory(String boneName, ModelData modelData, Image textureImage) {
        if (boneName == null || boneName.isEmpty()) {
            logger.error("Имя кости не может быть пустым");
            return null;
        }
        
        Group boneGroup = boneGroups.get(boneName);
        if (boneGroup == null) {
            logger.warn("Кость '{}' не найдена в модели игрока", boneName);
            return null;
        }
        
        // Удаляем предыдущий аксессуар для этой кости, если есть
        Group previousAccessory = accessories.remove(boneName);
        if (previousAccessory != null && boneGroup.getChildren().contains(previousAccessory)) {
            boneGroup.getChildren().remove(previousAccessory);
        }
        
        // Создаем группу для аксессуара
        Group accessoryGroup = new Group();
        
        if (modelData != null) {
            // Создаем аксессуар на основе модели
            createAccessoryFromModel(accessoryGroup, modelData, textureImage);
        } else {
            // Создаем простой аксессуар (по умолчанию - оболочка вокруг кости)
            createSimpleAccessory(accessoryGroup, boneName, textureImage);
        }
        
        // Добавляем аксессуар к кости
        boneGroup.getChildren().add(accessoryGroup);
        accessories.put(boneName, accessoryGroup);
        
        logger.info("Аксессуар прикреплен к кости '{}'", boneName);
        return accessoryGroup;
    }
    
    /**
     * Создать аксессуар из модели Blockbench
     */
    private void createAccessoryFromModel(Group accessoryGroup, ModelData modelData, Image textureImage) {
        if (modelData == null || modelData.getBones() == null) {
            logger.warn("Модель аксессуара пуста или не содержит костей");
            return;
        }
        
        // Создаем материал для аксессуара
        PhongMaterial accessoryMaterial = new PhongMaterial();
        if (textureImage != null) {
            accessoryMaterial.setDiffuseMap(textureImage);
        } else {
            accessoryMaterial.setDiffuseColor(Color.web("#667eea")); // Цвет по умолчанию
        }
        accessoryMaterial.setSpecularColor(Color.WHITE);
        accessoryMaterial.setSpecularPower(32.0);
        
        // Создаем карту костей для быстрого доступа
        Map<String, ModelData.Bone> boneMap = new HashMap<>();
        for (ModelData.Bone bone : modelData.getBones()) {
            if (bone.getName() != null) {
                boneMap.put(bone.getName(), bone);
            }
        }
        
        // Обрабатываем кости в правильном порядке (сначала корневые, затем дочерние)
        for (ModelData.Bone bone : modelData.getBones()) {
            if (bone.getParent() == null || bone.getParent().isEmpty()) {
                // Создаем группу для кости с правильными трансформациями
                createBoneGroupWithTransform(accessoryGroup, bone, modelData, boneMap, accessoryMaterial);
            }
        }
    }
    
    /**
     * Создать группу для кости с правильными трансформациями (pivot, rotation)
     */
    private void createBoneGroupWithTransform(
        Group parentGroup,
        ModelData.Bone bone,
        ModelData modelData,
        Map<String, ModelData.Bone> boneMap,
        PhongMaterial material
    ) {
        if (bone == null) {
            return;
        }
        
        // Создаем группу для кости
        Group boneGroup = new Group();
        
        // Применяем pivot кости (точка поворота)
        if (bone.getPivot() != null && bone.getPivot().length >= 3) {
            double[] pivot = bone.getPivot();
            double scaleFactor = 1.0 / 16.0;
            
            // Перемещаемся к точке поворота
            boneGroup.setTranslateX(pivot[0] * scaleFactor);
            boneGroup.setTranslateY(-pivot[1] * scaleFactor); // Инвертируем Y для JavaFX
            boneGroup.setTranslateZ(pivot[2] * scaleFactor);
        }
        
        // Применяем поворот кости относительно pivot
        if (bone.getRotation() != null && bone.getRotation().length >= 3) {
            double[] rotation = bone.getRotation();
            // Поворот относительно начала координат группы (которое уже на pivot)
            Rotate rotateX = new Rotate(rotation[0], 0, 0, 0, Rotate.X_AXIS);
            Rotate rotateY = new Rotate(rotation[1], 0, 0, 0, Rotate.Y_AXIS);
            Rotate rotateZ = new Rotate(rotation[2], 0, 0, 0, Rotate.Z_AXIS);
            boneGroup.getTransforms().addAll(rotateX, rotateY, rotateZ);
        }
        
        // Возвращаемся обратно от pivot к началу координат кости
        if (bone.getPivot() != null && bone.getPivot().length >= 3) {
            double[] pivot = bone.getPivot();
            double scaleFactor = 1.0 / 16.0;
            Translate translateBack = new Translate(-pivot[0] * scaleFactor, pivot[1] * scaleFactor, -pivot[2] * scaleFactor);
            boneGroup.getTransforms().add(translateBack);
        }
        
        // Создаем меш для кубов кости
        createBoneMesh(boneGroup, bone, material);
        
        // Рекурсивно обрабатываем дочерние кости
        List<ModelData.Bone> children = modelData.getChildBones(bone.getName());
        for (ModelData.Bone child : children) {
            createBoneGroupWithTransform(boneGroup, child, modelData, boneMap, material);
        }
        
        // Добавляем группу кости в родительскую группу
        parentGroup.getChildren().add(boneGroup);
    }
    
    /**
     * Создать меш (mesh) для кости аксессуара
     */
    private void createBoneMesh(Group parentGroup, ModelData.Bone bone, PhongMaterial material) {
        if (bone == null) {
            return;
        }
        
        // Получаем кубы кости
        @SuppressWarnings("unchecked")
        List<ModelData.Cube> cubes = (List<ModelData.Cube>) bone.getProperties().get("cubes");
        
        if (cubes != null && !cubes.isEmpty()) {
            for (ModelData.Cube cube : cubes) {
                if (cube.getOrigin() != null && cube.getSize() != null) {
                    double[] origin = cube.getOrigin();
                    double[] size = cube.getSize();
                    
                    // Конвертируем из пикселей в JavaFX координаты (Minecraft использует 16 пикселей на блок)
                    double scaleFactor = 1.0 / 16.0;
                    
                    // Размеры куба в JavaFX координатах
                    double width = size[0] * scaleFactor;
                    double height = size[1] * scaleFactor;
                    double depth = size[2] * scaleFactor;
                    
                    // Создаем Box с правильными размерами
                    Box box = new Box(width, height, depth);
                    box.setMaterial(material);
                    
                    // Origin - это позиция левого нижнего заднего угла куба в пикселях
                    // Origin задается относительно начала координат кости (после применения поворота кости)
                    // Pivot кости уже применен в родительской группе (boneGroup через createBoneGroupWithTransform)
                    double originX = origin[0] * scaleFactor;
                    double originY = origin[1] * scaleFactor;
                    double originZ = origin[2] * scaleFactor;
                    
                    // Позиция центра куба (origin - это левый нижний задний угол)
                    double centerX = originX + (width / 2.0);
                    double centerY = -(originY + (height / 2.0)); // Инвертируем Y для JavaFX (инверсия координат)
                    double centerZ = originZ + (depth / 2.0);
                    
                    // Позиционируем куб по центру (в системе координат после применения трансформаций кости)
                    box.setTranslateX(centerX);
                    box.setTranslateY(centerY);
                    box.setTranslateZ(centerZ);
                    
                    // Применяем поворот куба относительно его центра (если есть)
                    if (cube.getRotation() != null && cube.getRotation().length >= 3) {
                        double[] rotation = cube.getRotation();
                        Rotate cubeRotateX = new Rotate(rotation[0], centerX, centerY, centerZ, Rotate.X_AXIS);
                        Rotate cubeRotateY = new Rotate(rotation[1], centerX, centerY, centerZ, Rotate.Y_AXIS);
                        Rotate cubeRotateZ = new Rotate(rotation[2], centerX, centerY, centerZ, Rotate.Z_AXIS);
                        box.getTransforms().addAll(cubeRotateX, cubeRotateY, cubeRotateZ);
                    }
                    
                    // Зеркалирование куба, если указано
                    if (cube.isMirror()) {
                        box.setScaleX(-1.0);
                    }
                    
                    // Добавляем куб в родительскую группу кости
                    parentGroup.getChildren().add(box);
                }
            }
        }
    }
    
    /**
     * Создать простой аксессуар (оболочка вокруг кости)
     */
    private void createSimpleAccessory(Group accessoryGroup, String boneName, Image textureImage) {
        PhongMaterial accessoryMaterial = new PhongMaterial();
        if (textureImage != null) {
            accessoryMaterial.setDiffuseMap(textureImage);
        } else {
            accessoryMaterial.setDiffuseColor(Color.web("#667eea"));
        }
        accessoryMaterial.setSpecularColor(Color.WHITE);
        accessoryMaterial.setSpecularPower(32.0);
        
        // Создаем оболочку в зависимости от типа кости
        Box accessoryBox = null;
        switch (boneName) {
            case "head":
                accessoryBox = new Box(HEAD_SIZE + 0.5, HEAD_SIZE + 0.5, HEAD_SIZE + 0.5);
                accessoryBox.setTranslateY(HEAD_Y);
                break;
            case "body":
                accessoryBox = new Box(BODY_WIDTH + 0.5, BODY_HEIGHT + 0.5, BODY_DEPTH + 0.5);
                accessoryBox.setTranslateY(BODY_Y);
                break;
            case "left_arm":
            case "right_arm":
                accessoryBox = new Box(ARM_WIDTH + 0.5, ARM_LENGTH + 0.5, ARM_WIDTH + 0.5);
                accessoryBox.setTranslateY(ARM_Y);
                accessoryBox.setTranslateX(boneName.equals("right_arm") ? 8.0 : -8.0);
                break;
            case "left_leg":
            case "right_leg":
                accessoryBox = new Box(LEG_WIDTH + 0.5, LEG_LENGTH + 0.5, LEG_WIDTH + 0.5);
                accessoryBox.setTranslateY(LEG_Y);
                accessoryBox.setTranslateX(boneName.equals("right_leg") ? 2.0 : -2.0);
                break;
        }
        
        if (accessoryBox != null) {
            accessoryBox.setMaterial(accessoryMaterial);
            accessoryBox.setOpacity(0.8); // Полупрозрачность для видимости скина под ним
            accessoryGroup.getChildren().add(accessoryBox);
        }
    }
    
    /**
     * Удалить аксессуар с указанной кости
     */
    public void removeAccessory(String boneName) {
        Group boneGroup = boneGroups.get(boneName);
        if (boneGroup == null) {
            return;
        }
        
        Group accessory = accessories.remove(boneName);
        if (accessory != null && boneGroup.getChildren().contains(accessory)) {
            boneGroup.getChildren().remove(accessory);
            logger.info("Аксессуар удален с кости '{}'", boneName);
        }
    }
    
    /**
     * Установить скин игрока
     */
    public void setSkin(Image skinImage) {
        this.skinImage = skinImage;
        if (skinImage != null) {
            this.skinMaterial.setDiffuseMap(skinImage);
        } else {
            this.skinMaterial.setDiffuseColor(Color.web("#8B7355"));
        }
        
        // Обновляем материал для всех частей тела
        updateMaterials();
    }
    
    /**
     * Обновить материалы для всех частей тела
     */
    private void updateMaterials() {
        for (Group boneGroup : boneGroups.values()) {
            for (Node node : boneGroup.getChildren()) {
                if (node instanceof Box && node != accessories.values().stream()
                    .flatMap(g -> g.getChildren().stream())
                    .filter(n -> n == node)
                    .findFirst()
                    .orElse(null)) {
                    ((Box) node).setMaterial(skinMaterial);
                }
            }
        }
    }
    
    /**
     * Настройка автоматического вращения модели
     */
    private void setupRotation() {
        rotationAnimation = new RotateTransition(Duration.seconds(10), playerGroup);
        rotationAnimation.setAxis(Rotate.Y_AXIS);
        rotationAnimation.setByAngle(360);
        rotationAnimation.setCycleCount(RotateTransition.INDEFINITE);
        rotationAnimation.setInterpolator(javafx.animation.Interpolator.LINEAR);
    }
    
    /**
     * Запустить автоматическое вращение
     */
    public void startRotation() {
        if (rotationAnimation != null) {
            rotationAnimation.play();
        }
    }
    
    /**
     * Остановить автоматическое вращение
     */
    public void stopRotation() {
        if (rotationAnimation != null) {
            rotationAnimation.stop();
        }
    }
    
    /**
     * Установить угол поворота модели вручную
     */
    public void setRotation(double angle) {
        if (rotationAnimation != null) {
            rotationAnimation.stop();
        }
        playerGroup.setRotationAxis(Rotate.Y_AXIS);
        playerGroup.setRotate(angle);
    }
    
    /**
     * Получить группу кости по имени
     */
    public Group getBoneGroup(String boneName) {
        return boneGroups.get(boneName);
    }
    
    /**
     * Получить аксессуар на кости
     */
    public Group getAccessory(String boneName) {
        return accessories.get(boneName);
    }
}
