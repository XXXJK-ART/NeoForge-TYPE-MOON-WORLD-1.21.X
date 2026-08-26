package io.github.typemoonaddon.client.renderer;

import io.github.typemoonaddon.TypeMoonAddon;
import io.github.typemoonaddon.client.model.BlackShadowModel;
import io.github.typemoonaddon.entity.BlackShadowEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

final class BlackShadowOutlineLayer extends RenderLayer<BlackShadowEntity, BlackShadowModel> {
    private static final ResourceLocation TEXTURE = TypeMoonAddon.id("textures/entity/black_shadow_outline.png");
    private final BlackShadowModel model;

    BlackShadowOutlineLayer(RenderLayerParent<BlackShadowEntity, BlackShadowModel> parent, EntityModelSet modelSet) {
        super(parent);
        this.model = new BlackShadowModel(modelSet.bakeLayer(BlackShadowModel.LAYER_LOCATION));
    }

    @Override
    public void render(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        BlackShadowEntity entity,
        float limbSwing,
        float limbSwingAmount,
        float partialTick,
        float ageInTicks,
        float netHeadYaw,
        float headPitch
    ) {
        this.model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(TEXTURE));
        this.model.renderOutlineToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, -1);
    }
}
