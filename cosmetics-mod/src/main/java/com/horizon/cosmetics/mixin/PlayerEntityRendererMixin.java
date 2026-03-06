package com.horizon.cosmetics.mixin;

import com.horizon.cosmetics.client.CosmeticManager;
import com.horizon.cosmetics.client.renderer.PlayerCosmeticRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mixin для PlayerEntityRenderer для добавления отрисовки косметики
 * Использует PoseStack (MatrixStack в 1.21+) для трансформации контекста рисования
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin extends LivingEntityRenderer<PlayerEntity, PlayerEntityModel<PlayerEntity>> {
    private static final Logger logger = LoggerFactory.getLogger(PlayerEntityRendererMixin.class);
    
    public PlayerEntityRendererMixin(EntityRendererFactory.Context ctx, PlayerEntityModel<PlayerEntity> model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }
    
    /**
     * Инжект в метод render для добавления отрисовки косметики
     * В версии 1.21 используется MatrixStack (PoseStack)
     */
    @Inject(
        method = "render(Lnet/minecraft/entity/player/PlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            shift = At.Shift.AFTER
        )
    )
    private void onRenderPlayer(
        PlayerEntity player,
        float f,
        float g,
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int light,
        CallbackInfo ci
    ) {
        try {
            // Получаем менеджер косметики
            CosmeticManager cosmeticManager = CosmeticManager.getInstance();
            
            // Получаем рендерер косметики
            PlayerCosmeticRenderer cosmeticRenderer = PlayerCosmeticRenderer.getInstance();
            
            // Рендерим косметику игрока
            cosmeticRenderer.renderCosmetics(
                player,
                matrices,
                vertexConsumers,
                light,
                this.getModel(),
                cosmeticManager
            );
        } catch (Exception e) {
            logger.error("Ошибка при отрисовке косметики для игрока: {}", player.getName().getString(), e);
        }
    }
    
    /**
     * Дополнительный инжект для отрисовки косметики после отрисовки слоев
     */
    @Inject(
        method = "render",
        at = @At("RETURN")
    )
    private void onRenderPlayerReturn(
        PlayerEntity player,
        float f,
        float g,
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int light,
        CallbackInfo ci
    ) {
        // Дополнительная обработка после рендеринга
        // Здесь можно добавить отрисовку специальных эффектов
    }
}
