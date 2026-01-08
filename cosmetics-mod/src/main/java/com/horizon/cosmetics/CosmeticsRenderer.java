package com.horizon.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CosmeticsRenderer extends RenderLayer<AbstractClientPlayer, net.minecraft.client.model.PlayerModel<AbstractClientPlayer>> {
    
    public CosmeticsRenderer() {
        super(null);
    }

    @SubscribeEvent
    public void onRenderPlayer(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        CosmeticsManager.PlayerCosmetics cosmetics = CosmeticsManager.getPlayerCosmetics(player.getUUID());
        
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = event.getMultiBufferSource();
        int packedLight = event.getPackedLight();
        
        // Render wings
        if (cosmetics.hasWings) {
            renderWings(poseStack, bufferSource, packedLight, player, cosmetics.wingsType);
        }
        
        // Render cape
        if (cosmetics.hasCape) {
            renderCape(poseStack, bufferSource, packedLight, player, cosmetics.capeType);
        }
        
        // Spawn particles
        if (cosmetics.hasParticles && player.level().isClientSide) {
            spawnParticles(player, cosmetics.particlesType);
        }
    }

    private void renderWings(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, Player player, String wingsType) {
        poseStack.pushPose();
        
        // Позиционируем крылья за спиной игрока
        poseStack.translate(0.0, 1.5, 0.15);
        poseStack.scale(0.8F, 0.8F, 0.8F);
        
        // Получаем время для анимации
        float time = player.level().getGameTime() + Minecraft.getInstance().getPartialTick();
        boolean isFlying = !player.onGround() && player.getDeltaMovement().y < 0;
        
        // TODO: Рендерим модель крыльев с текстурой
        // В реальной имплементации здесь будет загрузка модели и текстуры из ресурсов
        
        poseStack.popPose();
    }

    private void renderCape(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, Player player, String capeType) {
        // TODO: Implement cape rendering
        // Similar to vanilla cape but with custom textures
    }

    private void spawnParticles(Player player, String particlesType) {
        if (player.level().getGameTime() % 5 == 0) { // Spawn every 5 ticks
            double px = player.getX() + (player.level().random.nextDouble() - 0.5) * 0.8;
            double py = player.getY() + player.level().random.nextDouble() * player.getBbHeight();
            double pz = player.getZ() + (player.level().random.nextDouble() - 0.5) * 0.8;
            
            switch (particlesType) {
                case "stars":
                    player.level().addParticle(ParticleTypes.END_ROD, px, py, pz, 0, 0.05, 0);
                    break;
                case "flames":
                    player.level().addParticle(ParticleTypes.FLAME, px, py, pz, 0, 0.05, 0);
                    break;
                case "sparkles":
                    player.level().addParticle(ParticleTypes.GLOW, px, py, pz, 0, 0, 0);
                    break;
            }
        }
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        // Required override
    }
}

