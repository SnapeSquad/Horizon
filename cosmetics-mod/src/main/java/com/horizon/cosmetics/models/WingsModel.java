package com.horizon.cosmetics.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.RenderType;

public class WingsModel extends Model {
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    
    public WingsModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
    }
    
    public static LayerDefinition createDragonWings() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        
        // Левое крыло
        PartDefinition leftWing = partdefinition.addOrReplaceChild("left_wing",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(0.0F, -8.0F, 0.0F, 20.0F, 16.0F, 2.0F),
            PartPose.offset(2.0F, 0.0F, 2.0F));
        
        // Правое крыло
        PartDefinition rightWing = partdefinition.addOrReplaceChild("right_wing",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-20.0F, -8.0F, 0.0F, 20.0F, 16.0F, 2.0F),
            PartPose.offset(-2.0F, 0.0F, 2.0F));
        
        return LayerDefinition.create(meshdefinition, 64, 32);
    }
    
    public static LayerDefinition createAngelWings() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        
        // Левое крыло (более изящное)
        PartDefinition leftWing = partdefinition.addOrReplaceChild("left_wing",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(0.0F, -10.0F, 0.0F, 18.0F, 20.0F, 1.0F),
            PartPose.offset(2.0F, 0.0F, 2.0F));
        
        // Правое крыло
        PartDefinition rightWing = partdefinition.addOrReplaceChild("right_wing",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-18.0F, -10.0F, 0.0F, 18.0F, 20.0F, 1.0F),
            PartPose.offset(-2.0F, 0.0F, 2.0F));
        
        return LayerDefinition.create(meshdefinition, 64, 32);
    }
    
    public void animate(float time, boolean isFlying) {
        float angle = (float) Math.sin(time * 0.05) * 0.4F;
        
        if (isFlying) {
            // Анимация полета
            leftWing.yRot = -0.5F + angle;
            rightWing.yRot = 0.5F - angle;
        } else {
            // Анимация в покое
            leftWing.yRot = -0.2F + angle * 0.3F;
            rightWing.yRot = 0.2F - angle * 0.3F;
        }
    }
    
    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        leftWing.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        rightWing.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}

