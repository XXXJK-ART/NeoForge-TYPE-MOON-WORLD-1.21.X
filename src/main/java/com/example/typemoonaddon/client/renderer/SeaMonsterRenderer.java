package com.example.typemoonaddon.client.renderer;

import com.example.typemoonaddon.client.model.SeaMonsterModel;
import com.example.typemoonaddon.entity.SeaMonsterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

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

    @Override
    protected float getDeathMaxRotation(SeaMonsterEntity animatable) {
        return animatable.isDissolving() ? 0.0F : super.getDeathMaxRotation(animatable);
    }

    @Override
    public Color getRenderColor(SeaMonsterEntity animatable, float partialTick, int packedLight) {
        if (!animatable.isDissolving()) {
            return super.getRenderColor(animatable, partialTick, packedLight);
        }
        int alpha = Math.round((1.0F - Mth.clamp(animatable.getDissolveProgress(partialTick), 0.0F, 1.0F)) * 255.0F);
        return Color.ofARGB(alpha, 255, 255, 255);
    }

    @Override
    public RenderType getRenderType(
            SeaMonsterEntity animatable,
            ResourceLocation texture,
            @Nullable MultiBufferSource bufferSource,
            float partialTick
    ) {
        return animatable.isDissolving() ? RenderType.entityTranslucent(texture) : super.getRenderType(animatable, texture, bufferSource, partialTick);
    }
}
