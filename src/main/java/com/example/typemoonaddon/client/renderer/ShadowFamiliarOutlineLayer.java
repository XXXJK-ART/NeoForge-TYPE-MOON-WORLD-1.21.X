package com.example.typemoonaddon.client.renderer;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.client.model.ShadowFamiliarModel;
import com.example.typemoonaddon.client.model.ShadowFamiliarOutlineModel;
import com.example.typemoonaddon.entity.SakuraShadowFamiliarEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

final class ShadowFamiliarOutlineLayer extends RenderLayer<SakuraShadowFamiliarEntity, ShadowFamiliarModel> {
    private static final ResourceLocation TEXTURE = TypeMoonAddon.id("textures/entity/shadow_familiar_outline.png");
    private final ShadowFamiliarOutlineModel model;

    ShadowFamiliarOutlineLayer(RenderLayerParent<SakuraShadowFamiliarEntity, ShadowFamiliarModel> parent, EntityModelSet modelSet) {
        super(parent);
        this.model = new ShadowFamiliarOutlineModel(modelSet.bakeLayer(ShadowFamiliarOutlineModel.LAYER_LOCATION));
    }

    @Override
    public void render(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        SakuraShadowFamiliarEntity entity,
        float limbSwing,
        float limbSwingAmount,
        float partialTick,
        float ageInTicks,
        float netHeadYaw,
        float headPitch
    ) {
        this.model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(TEXTURE));
        this.model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
    }
}
