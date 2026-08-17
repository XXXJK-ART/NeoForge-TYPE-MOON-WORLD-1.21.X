package com.example.typemoonaddon.client.renderer;

import com.example.typemoonaddon.entity.GillesDeRaisEntity;
import com.example.typemoonaddon.entity.HugeSeaMonsterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.HumanoidServantRenderer;

public final class GillesDeRaisRenderer extends HumanoidServantRenderer<GillesDeRaisEntity> {
    public GillesDeRaisRenderer(Context renderManager) {
        super(renderManager, "gilles_de_rais_caster");
        this.addLayer(new ItemInHandLayer<>(this, renderManager.getItemInHandRenderer()));
        this.addLayer(new GillesDeRaisArmorLayer(this));
    }

    @Override
    public void render(GillesDeRaisEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        if (entity.getVehicle() instanceof HugeSeaMonsterEntity) {
            return;
        }
        this.model.rightArmPose = HumanoidModel.ArmPose.ITEM;
        this.model.leftArmPose = HumanoidModel.ArmPose.ITEM;
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        this.model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        this.model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
    }
}
