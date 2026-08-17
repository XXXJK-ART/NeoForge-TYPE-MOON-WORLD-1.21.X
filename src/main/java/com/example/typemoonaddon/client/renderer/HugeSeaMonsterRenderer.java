package com.example.typemoonaddon.client.renderer;

import com.example.typemoonaddon.client.model.HugeSeaMonsterModel;
import com.example.typemoonaddon.entity.HugeSeaMonsterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class HugeSeaMonsterRenderer extends GeoEntityRenderer<HugeSeaMonsterEntity> {
    public HugeSeaMonsterRenderer(Context context) {
        super(context, new HugeSeaMonsterModel());
        this.shadowRadius = 4.0F;
    }

    @Override
    public void preRender(PoseStack poseStack, HugeSeaMonsterEntity animatable, BakedGeoModel model,
                          @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        poseStack.scale(3.2F, 3.2F, 3.2F);
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }
}
