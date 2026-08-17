package com.example.typemoonaddon.client.renderer;

import com.example.typemoonaddon.client.model.GillesDeRaisModel;
import com.example.typemoonaddon.entity.GillesDeRaisEntity;
import com.example.typemoonaddon.entity.HugeSeaMonsterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.xxxjk.TYPE_MOON_WORLD.client.renderer.BaseServantRenderer;

public final class GillesDeRaisRenderer extends BaseServantRenderer<GillesDeRaisEntity> {
    public GillesDeRaisRenderer(Context renderManager) {
        super(renderManager, new GillesDeRaisModel(), 0.9F);
    }

    @Override
    public void render(GillesDeRaisEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        if (entity.getVehicle() instanceof HugeSeaMonsterEntity) {
            return;
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}
