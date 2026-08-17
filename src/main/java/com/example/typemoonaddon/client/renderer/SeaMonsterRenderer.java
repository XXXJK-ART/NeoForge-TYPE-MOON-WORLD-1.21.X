package com.example.typemoonaddon.client.renderer;

import com.example.typemoonaddon.client.model.SeaMonsterModel;
import com.example.typemoonaddon.entity.SeaMonsterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class SeaMonsterRenderer extends GeoEntityRenderer<SeaMonsterEntity> {
    public SeaMonsterRenderer(Context context) {
        super(context, new SeaMonsterModel());
    }

    @Override
    public void preRender(PoseStack poseStack, SeaMonsterEntity animatable, BakedGeoModel model,
                          @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        float scale = animatable.isLarge() ? 2.0F : 1.0F;
        poseStack.scale(scale, scale, scale);
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }
}
