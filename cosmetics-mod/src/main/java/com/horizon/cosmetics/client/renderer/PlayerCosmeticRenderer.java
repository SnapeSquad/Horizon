package com.horizon.cosmetics.client.renderer;

import com.horizon.cosmetics.client.CosmeticManager;
import com.horizon.cosmetics.common.ModelData;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Рендерер косметики для игроков
 * Использует MatrixStack (PoseStack в 1.21+) для трансформации контекста рисования
 * Отрисовывает модели косметики через VertexConsumer с использованием стандартного шейдера
 */
public class PlayerCosmeticRenderer {
    private static final Logger logger = LoggerFactory.getLogger(PlayerCosmeticRenderer.class);
    private static PlayerCosmeticRenderer instance;
    
    private final Map<UUID, Map<String, ModelData>> playerModelsCache;
    private final Map<UUID, Map<String, Identifier>> playerTexturesCache;
    
    private PlayerCosmeticRenderer() {
        this.playerModelsCache = new ConcurrentHashMap<>();
        this.playerTexturesCache = new ConcurrentHashMap<>();
    }
    
    /**
     * Получить единственный экземпляр PlayerCosmeticRenderer
     */
    public static synchronized PlayerCosmeticRenderer getInstance() {
        if (instance == null) {
            instance = new PlayerCosmeticRenderer();
        }
        return instance;
    }
    
    /**
     * Рендеринг косметики игрока
     * @param player Игрок
     * @param matrices MatrixStack для трансформации
     * @param vertexConsumers Провайдер вершинных консьюмеров
     * @param light Уровень освещения
     * @param model Модель игрока
     * @param cosmeticManager Менеджер косметики
     */
    public void renderCosmetics(
        PlayerEntity player,
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int light,
        PlayerEntityModel<PlayerEntity> model,
        CosmeticManager cosmeticManager
    ) {
        if (player == null || cosmeticManager == null) {
            return;
        }
        
        UUID playerUuid = player.getUuid();
        
        try {
            // Получаем косметику игрока (асинхронно, если еще не загружена)
            Map<String, String> cosmetics = getPlayerCosmetics(playerUuid, cosmeticManager);
            
            if (cosmetics == null || cosmetics.isEmpty()) {
                return;
            }
            
            // Рендерим косметику для каждой кости
            for (Map.Entry<String, String> entry : cosmetics.entrySet()) {
                String boneName = entry.getKey();
                String cosmeticId = entry.getValue();
                
                renderCosmeticForBone(
                    player,
                    boneName,
                    cosmeticId,
                    matrices,
                    vertexConsumers,
                    light,
                    model,
                    cosmeticManager
                );
            }
        } catch (Exception e) {
            logger.error("Ошибка при рендеринге косметики для игрока: {}", player.getName().getString(), e);
        }
    }
    
    /**
     * Рендеринг косметики для конкретной кости
     */
    private void renderCosmeticForBone(
        PlayerEntity player,
        String boneName,
        String cosmeticId,
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int light,
        PlayerEntityModel<PlayerEntity> model,
        CosmeticManager cosmeticManager
    ) {
        try {
            // Получаем модель косметики
            ModelData cosmeticModel = getCachedModel(player.getUuid(), boneName, cosmeticId, cosmeticManager);
            if (cosmeticModel == null) {
                return;
            }
            
            // Получаем текстуру косметики
            Identifier texture = getCachedTexture(player.getUuid(), boneName, cosmeticId, cosmeticManager);
            if (texture == null) {
                return;
            }
            
            // Сохраняем текущее состояние матрицы
            matrices.push();
            
            // Перемещаем контекст рисования к нужной части тела
            transformToBone(matrices, boneName, model, player);
            
            // Получаем VertexConsumer с использованием стандартного шейдера
            VertexConsumer vertexConsumer = vertexConsumers.getBuffer(
                RenderLayer.getEntityCutout(texture)
            );
            
            // Рендерим модель косметики
            renderCosmeticModel(cosmeticModel, matrices, vertexConsumer, light);
            
            // Восстанавливаем состояние матрицы
            matrices.pop();
            
        } catch (Exception e) {
            logger.error("Ошибка при рендеринге косметики '{}' на кости '{}' для игрока: {}", 
                cosmeticId, boneName, player.getName().getString(), e);
        }
    }
    
    /**
     * Трансформация MatrixStack к нужной кости модели игрока
     */
    private void transformToBone(
        MatrixStack matrices,
        String boneName,
        PlayerEntityModel<PlayerEntity> model,
        PlayerEntity player
    ) {
        // Используем позицию и поворот соответствующей части модели игрока
        switch (boneName) {
            case "head":
                // Трансформация к голове
                matrices.translate(0, -0.25, 0);
                model.head.rotate(matrices);
                break;
                
            case "body":
                // Трансформация к телу
                model.body.rotate(matrices);
                break;
                
            case "left_arm":
                // Трансформация к левой руке
                matrices.translate(0.0625, 0.375, 0);
                model.leftArm.rotate(matrices);
                break;
                
            case "right_arm":
                // Трансформация к правой руке
                matrices.translate(-0.0625, 0.375, 0);
                model.rightArm.rotate(matrices);
                break;
                
            case "left_leg":
                // Трансформация к левой ноге
                matrices.translate(0.0625, 0.375, 0);
                model.leftLeg.rotate(matrices);
                break;
                
            case "right_leg":
                // Трансформация к правой ноге
                matrices.translate(-0.0625, 0.375, 0);
                model.rightLeg.rotate(matrices);
                break;
                
            default:
                logger.warn("Неизвестная кость: {}", boneName);
                break;
        }
    }
    
    /**
     * Рендеринг модели косметики через VertexConsumer
     */
    private void renderCosmeticModel(
        ModelData modelData,
        MatrixStack matrices,
        VertexConsumer vertexConsumer,
        int light
    ) {
        if (modelData == null || modelData.getBones() == null) {
            return;
        }
        
        // Создаем карту костей для быстрого доступа
        java.util.Map<String, ModelData.Bone> boneMap = new java.util.HashMap<>();
        for (ModelData.Bone bone : modelData.getBones()) {
            if (bone.getName() != null) {
                boneMap.put(bone.getName(), bone);
            }
        }
        
        // Рендерим кости в правильном порядке (сначала родительские, потом дочерние)
        for (ModelData.Bone bone : modelData.getBones()) {
            if (bone.getParent() == null || bone.getParent().isEmpty()) {
                // Рендерим корневые кости
                renderBone(bone, modelData, boneMap, matrices, vertexConsumer, light);
            }
        }
    }
    
    /**
     * Рендеринг кости модели с учетом иерархии
     */
    private void renderBone(
        ModelData.Bone bone,
        ModelData modelData,
        java.util.Map<String, ModelData.Bone> boneMap,
        MatrixStack matrices,
        VertexConsumer vertexConsumer,
        int light
    ) {
        if (bone == null) {
            return;
        }
        
        // Сохраняем состояние матрицы для кости
        matrices.push();
        
        // Применяем pivot (точку поворота) - это точка, вокруг которой происходит поворот
        // В Minecraft моделях pivot определяет точку, относительно которой позиционируются кубы
        // Origin кубов задается относительно начала координат кости, но при рендеринге
        // мы применяем трансформации относительно pivot
        if (bone.getPivot() != null && bone.getPivot().length >= 3) {
            double[] pivot = bone.getPivot();
            // Перемещаем к точке поворота
            matrices.translate(pivot[0] / 16.0, pivot[1] / 16.0, pivot[2] / 16.0);
        }
        
        // Применяем поворот кости относительно pivot
        if (bone.getRotation() != null && bone.getRotation().length >= 3) {
            double[] rotation = bone.getRotation();
            // Поворот в градусах, конвертируем в радианы через Quaternion
            matrices.multiply(
                net.minecraft.util.math.Quaternion.fromEulerXyzDegrees(
                    (float) Math.toRadians(rotation[0]),
                    (float) Math.toRadians(rotation[1]),
                    (float) Math.toRadians(rotation[2])
                )
            );
        }
        
        // Возвращаемся обратно от pivot, так как origin кубов задается относительно начала координат кости
        if (bone.getPivot() != null && bone.getPivot().length >= 3) {
            double[] pivot = bone.getPivot();
            matrices.translate(-pivot[0] / 16.0, -pivot[1] / 16.0, -pivot[2] / 16.0);
        }
        
        // Получаем кубы кости
        @SuppressWarnings("unchecked")
        java.util.List<ModelData.Cube> cubes = (java.util.List<ModelData.Cube>) bone.getProperties().get("cubes");
        
        if (cubes != null && !cubes.isEmpty()) {
            for (ModelData.Cube cube : cubes) {
                // Передаем родительскую кость для правильного позиционирования
                renderCube(cube, bone, matrices, vertexConsumer, light);
            }
        }
        
        // Рендерим дочерние кости
        List<ModelData.Bone> children = modelData.getChildBones(bone.getName());
        for (ModelData.Bone child : children) {
            renderBone(child, modelData, boneMap, matrices, vertexConsumer, light);
        }
        
        // Восстанавливаем состояние матрицы
        matrices.pop();
    }
    
    
    /**
     * Рендеринг куба модели
     */
    private void renderCube(
        ModelData.Cube cube,
        ModelData.Bone parentBone,
        MatrixStack matrices,
        VertexConsumer vertexConsumer,
        int light
    ) {
        if (cube == null || cube.getOrigin() == null || cube.getSize() == null) {
            return;
        }
        
        double[] origin = cube.getOrigin();
        double[] size = cube.getSize();
        
        // Сохраняем состояние матрицы
        matrices.push();
        
        // Origin - это позиция левого нижнего заднего угла куба в пикселях (16 пикселей = 1 блок)
        // Конвертируем из пикселей в блоки (1 пиксель = 1/16 блока)
        double originX = origin[0] / 16.0;
        double originY = origin[1] / 16.0;
        double originZ = origin[2] / 16.0;
        
        // Размеры куба в блоках
        double sizeX = size[0] / 16.0;
        double sizeY = size[1] / 16.0;
        double sizeZ = size[2] / 16.0;
        
        // Origin кубов задается относительно начала координат кости (после применения поворота кости)
        // Перемещаемся к позиции origin (левый нижний задний угол) и затем к центру куба
        matrices.translate(originX + sizeX / 2.0, originY + sizeY / 2.0, originZ + sizeZ / 2.0);
        
        // Применяем поворот самого куба, если есть (поворот вокруг центра куба)
        if (cube.getRotation() != null && cube.getRotation().length >= 3) {
            double[] rotation = cube.getRotation();
            matrices.multiply(
                net.minecraft.util.math.Quaternion.fromEulerXyzDegrees(
                    (float) Math.toRadians(rotation[0]),
                    (float) Math.toRadians(rotation[1]),
                    (float) Math.toRadians(rotation[2])
                )
            );
        }
        
        // Смещаем обратно, чтобы куб был отрисован вокруг (0,0,0) с центром в начале координат
        // Это нужно для правильной работы renderBox, который ожидает куб с центром в (0,0,0)
        matrices.translate(-sizeX / 2.0, -sizeY / 2.0, -sizeZ / 2.0);
        
        // Отрисовываем куб с учетом UV координат и зеркалирования
        renderBox(size, cube.getUv(), matrices, vertexConsumer, light, cube.isMirror());
        
        // Восстанавливаем состояние матрицы
        matrices.pop();
    }
    
    /**
     * Рендеринг бокса (куба) через VertexConsumer
     * Полная реализация с отрисовкой всех 6 граней
     */
    private void renderBox(
        double[] size,
        double[] uv,
        MatrixStack matrices,
        VertexConsumer vertexConsumer,
        int light,
        boolean mirror
    ) {
        if (size == null || size.length < 3) {
            logger.warn("Некорректный размер куба");
            return;
        }
        
        // Размеры куба в блоках (Minecraft использует 16 пикселей на блок)
        // size задается в пикселях модели (16 пикселей = 1 блок)
        float width = (float) (size[0] / 16.0);
        float height = (float) (size[1] / 16.0);
        float depth = (float) (size[2] / 16.0);
        
        // UV координаты в пикселях на текстуре (если не указаны, используем базовые)
        // В Minecraft моделях UV координаты задаются в пикселях относительно текстуры
        // Стандартный размер текстуры: 64x64 пикселей для большинства моделей
        // Нормализуем к диапазону 0.0-1.0 для использования в VertexConsumer
        float textureSize = 64.0f; // Стандартный размер текстуры Minecraft
        
        // Базовые UV координаты (начало текстуры для этого куба) в нормализованном виде
        float u = uv != null && uv.length >= 1 ? (float) (uv[0] / textureSize) : 0.0f;
        float v = uv != null && uv.length >= 2 ? (float) (uv[1] / textureSize) : 0.0f;
        
        // Размеры куба в UV координатах (нормализованные к размеру текстуры)
        // size задается в пикселях модели, которые соответствуют пикселям на текстуре
        // Например, если куб имеет размер 8x8x8 пикселей на текстуре 64x64,
        // то в нормализованных UV координатах это будет 8/64 = 0.125
        float uWidth = (float) (size[0] / textureSize);   // Ширина куба в UV координатах (0.0-1.0)
        float vHeight = (float) (size[1] / textureSize);  // Высота куба в UV координатах (0.0-1.0)
        float uDepth = (float) (size[2] / textureSize);   // Глубина куба в UV координатах (0.0-1.0)
        
        // Получаем матрицу трансформации
        net.minecraft.util.math.Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        net.minecraft.util.math.Matrix3f normalMatrix = matrices.peek().getNormalMatrix();
        
        int overlay = net.minecraft.client.render.OverlayTexture.DEFAULT_UV;
        int packedLight = light;
        float normalX = 0.0f;
        float normalY = 0.0f;
        float normalZ = 0.0f;
        
        // Половины размеров для позиционирования
        float halfWidth = width / 2.0f;
        float halfHeight = height / 2.0f;
        float halfDepth = depth / 2.0f;
        
        // Фронтальная грань (North/Z-)
        normalX = 0.0f;
        normalY = 0.0f;
        normalZ = -1.0f;
        if (mirror) {
            normalZ = 1.0f;
        }
        // Фронтальная грань (North/Z-) - передняя грань куба
        // Вершины в порядке: нижний левый, нижний правый, верхний правый, верхний левый
        // UV координаты: нижний левый=(u, v+vHeight), нижний правый=(u+uWidth, v+vHeight),
        //                верхний правый=(u+uWidth, v), верхний левый=(u, v)
        vertexConsumer.vertex(matrix4f, -halfWidth, -halfHeight, -halfDepth)
            .color(255, 255, 255, 255)
            .texture(u, v + vHeight)
            .overlay(overlay)
            .light(packedLight)
            .normal(normalMatrix, normalX, normalY, normalZ)
            .next();
        vertexConsumer.vertex(matrix4f, halfWidth, -halfHeight, -halfDepth)
            .color(255, 255, 255, 255)
            .texture(u + uWidth, v + vHeight)
            .overlay(overlay)
            .light(packedLight)
            .normal(normalMatrix, normalX, normalY, normalZ)
            .next();
        vertexConsumer.vertex(matrix4f, halfWidth, halfHeight, -halfDepth)
            .color(255, 255, 255, 255)
            .texture(u + uWidth, v)
            .overlay(overlay)
            .light(packedLight)
            .normal(normalMatrix, normalX, normalY, normalZ)
            .next();
        vertexConsumer.vertex(matrix4f, -halfWidth, halfHeight, -halfDepth)
            .color(255, 255, 255, 255)
            .texture(u, v)
            .overlay(overlay)
            .light(packedLight)
            .normal(normalMatrix, normalX, normalY, normalZ)
            .next();
        
        // Задняя грань (South/Z+) - задняя грань куба
        normalX = 0.0f;
        normalY = 0.0f;
        normalZ = 1.0f;
        if (mirror) {
            normalZ = -1.0f;
        }
        // Для задней грани порядок вершин обратный для правильной ориентации
        vertexConsumer.vertex(matrix4f, halfWidth, -halfHeight, halfDepth)
            .color(255, 255, 255, 255)
            .texture(u + uWidth, v + vHeight)
            .overlay(overlay)
            .light(packedLight)
            .normal(normalMatrix, normalX, normalY, normalZ)
            .next();
        vertexConsumer.vertex(matrix4f, -halfWidth, -halfHeight, halfDepth)
            .color(255, 255, 255, 255)
            .texture(u, v + vHeight)
            .overlay(overlay)
            .light(packedLight)
            .normal(normalMatrix, normalX, normalY, normalZ)
            .next();
        vertexConsumer.vertex(matrix4f, -halfWidth, halfHeight, halfDepth)
            .color(255, 255, 255, 255)
            .texture(u, v)
            .overlay(overlay)
            .light(packedLight)
            .normal(normalMatrix, normalX, normalY, normalZ)
            .next();
        vertexConsumer.vertex(matrix4f, halfWidth, halfHeight, halfDepth)
            .color(255, 255, 255, 255)
            .texture(u + uWidth, v)
            .overlay(overlay)
            .light(packedLight)
            .normal(normalMatrix, normalX, normalY, normalZ)
            .next();
        
        // Верхняя грань (Top/Y+) - верхняя грань куба
        normalX = 0.0f;
        normalY = 1.0f;
        normalZ = 0.0f;
        vertexConsumer.vertex(matrix4f, -halfWidth, halfHeight, -halfDepth)
            .color(255, 255, 255, 255)
            .texture(u, v)
            .overlay(overlay)
            .light(packedLight)
            .normal(normalMatrix, normalX, normalY, normalZ)
            .next();
        vertexConsumer.vertex(matrix4f, halfWidth, halfHeight, -halfDepth)
            .color(255, 255, 255, 255)
            .texture(u + uWidth, v)
            .overlay(overlay)
            .light(packedLight)
            .normal(normalMatrix, normalX, normalY, normalZ)
            .next();
        vertexConsumer.vertex(matrix4f, halfWidth, halfHeight, halfDepth)
            .color(255, 255, 255, 255)
            .texture(u + uWidth, v + uDepth)
            .overlay(overlay)
            .light(packedLight)
            .normal(normalMatrix, normalX, normalY, normalZ)
            .next();
        vertexConsumer.vertex(matrix4f, -halfWidth, halfHeight, halfDepth)
            .color(255, 255, 255, 255)
            .texture(u, v + uDepth)
            .overlay(overlay)
            .light(packedLight)
            .normal(normalMatrix, normalX, normalY, normalZ)
            .next();
        
        // Нижняя грань (Bottom/Y-) - нижняя грань куба
        normalX = 0.0f;
        normalY = -1.0f;
        normalZ = 0.0f;
        // Для нижней грани порядок вершин обратный для правильной ориентации
        vertexConsumer.vertex(matrix4f, -halfWidth, -halfHeight, halfDepth)
            .color(255, 255, 255, 255)
            .texture(u, v + uDepth)
            .overlay(overlay)
            .light(packedLight)
            .normal(normalMatrix, normalX, normalY, normalZ)
            .next();
        vertexConsumer.vertex(matrix4f, halfWidth, -halfHeight, halfDepth)
            .color(255, 255, 255, 255)
            .texture(u + uWidth, v + uDepth)
            .overlay(overlay)
            .light(packedLight)
            .normal(normalMatrix, normalX, normalY, normalZ)
            .next();
        vertexConsumer.vertex(matrix4f, halfWidth, -halfHeight, -halfDepth)
            .color(255, 255, 255, 255)
            .texture(u + uWidth, v)
            .overlay(overlay)
            .light(packedLight)
            .normal(normalMatrix, normalX, normalY, normalZ)
            .next();
        vertexConsumer.vertex(matrix4f, -halfWidth, -halfHeight, -halfDepth)
            .color(255, 255, 255, 255)
            .texture(u, v)
            .overlay(overlay)
            .light(packedLight)
            .normal(normalMatrix, normalX, normalY, normalZ)
            .next();
        
        // Правая грань (East/X+) - правая боковая грань куба
        normalX = 1.0f;
        normalY = 0.0f;
        normalZ = 0.0f;
        // UV координаты для боковой грани смещены на ширину
        float sideU = u + uWidth;
        vertexConsumer.vertex(matrix4f, halfWidth, -halfHeight, -halfDepth)
            .color(255, 255, 255, 255)
            .texture(sideU, v + vHeight)
            .overlay(overlay)
            .light(packedLight)
            .normal(normalMatrix, normalX, normalY, normalZ)
            .next();
        vertexConsumer.vertex(matrix4f, halfWidth, -halfHeight, halfDepth)
            .color(255, 255, 255, 255)
            .texture(sideU + uDepth, v + vHeight)
            .overlay(overlay)
            .light(packedLight)
            .normal(normalMatrix, normalX, normalY, normalZ)
            .next();
        vertexConsumer.vertex(matrix4f, halfWidth, halfHeight, halfDepth)
            .color(255, 255, 255, 255)
            .texture(sideU + uDepth, v)
            .overlay(overlay)
            .light(packedLight)
            .normal(normalMatrix, normalX, normalY, normalZ)
            .next();
        vertexConsumer.vertex(matrix4f, halfWidth, halfHeight, -halfDepth)
            .color(255, 255, 255, 255)
            .texture(sideU, v)
            .overlay(overlay)
            .light(packedLight)
            .normal(normalMatrix, normalX, normalY, normalZ)
            .next();
        
        // Левая грань (West/X-) - левая боковая грань куба
        normalX = -1.0f;
        normalY = 0.0f;
        normalZ = 0.0f;
        // UV координаты для левой боковой грани смещены на ширину + глубину
        float leftSideU = u + uWidth + uDepth;
        vertexConsumer.vertex(matrix4f, -halfWidth, -halfHeight, halfDepth)
            .color(255, 255, 255, 255)
            .texture(leftSideU, v + vHeight)
            .overlay(overlay)
            .light(packedLight)
            .normal(normalMatrix, normalX, normalY, normalZ)
            .next();
        vertexConsumer.vertex(matrix4f, -halfWidth, -halfHeight, -halfDepth)
            .color(255, 255, 255, 255)
            .texture(leftSideU + uDepth, v + vHeight)
            .overlay(overlay)
            .light(packedLight)
            .normal(normalMatrix, normalX, normalY, normalZ)
            .next();
        vertexConsumer.vertex(matrix4f, -halfWidth, halfHeight, -halfDepth)
            .color(255, 255, 255, 255)
            .texture(leftSideU + uDepth, v)
            .overlay(overlay)
            .light(packedLight)
            .normal(normalMatrix, normalX, normalY, normalZ)
            .next();
        vertexConsumer.vertex(matrix4f, -halfWidth, halfHeight, halfDepth)
            .color(255, 255, 255, 255)
            .texture(leftSideU, v)
            .overlay(overlay)
            .light(packedLight)
            .normal(normalMatrix, normalX, normalY, normalZ)
            .next();
        
        logger.debug("Куб отрисован: {}x{}x{}", width, height, depth);
    }
    
    /**
     * Получить косметику игрока (с кешированием)
     */
    private Map<String, String> getPlayerCosmetics(UUID playerUuid, CosmeticManager cosmeticManager) {
        // В реальной реализации здесь должно быть кеширование
        // и асинхронная загрузка косметики
        return cosmeticManager.getPlayerCosmetics(playerUuid).join();
    }
    
    /**
     * Получить модель косметики (с кешированием)
     */
    private ModelData getCachedModel(UUID playerUuid, String boneName, String cosmeticId, CosmeticManager cosmeticManager) {
        Map<String, ModelData> playerModels = playerModelsCache.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>());
        
        return playerModels.computeIfAbsent(boneName, k -> 
            cosmeticManager.loadModelAsync(cosmeticId).join()
        );
    }
    
    /**
     * Получить текстуру косметики (с кешированием)
     */
    private Identifier getCachedTexture(UUID playerUuid, String boneName, String cosmeticId, CosmeticManager cosmeticManager) {
        Map<String, Identifier> playerTextures = playerTexturesCache.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>());
        
        return playerTextures.computeIfAbsent(boneName, k -> 
            cosmeticManager.loadTextureAsync(cosmeticId).join()
        );
    }
    
    /**
     * Очистить кеш для игрока
     */
    public void clearPlayerCache(UUID playerUuid) {
        playerModelsCache.remove(playerUuid);
        playerTexturesCache.remove(playerUuid);
    }
    
    /**
     * Очистить весь кеш
     */
    public void clearAllCache() {
        playerModelsCache.clear();
        playerTexturesCache.clear();
    }
}
