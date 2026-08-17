package net.xxxjk.TYPE_MOON_WORLD.chain.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.chain.entity.EnumaChainCrownEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public final class GoldenCrownFlowLayer extends GeoRenderLayer<EnumaChainCrownEntity> {
    private static final ResourceLocation MASK = TYPE_MOON_WORLD.id("textures/entity/heaven_chain_head_glint.png");

    public GoldenCrownFlowLayer(GeoRenderer<EnumaChainCrownEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(
        PoseStack poseStack,
        EnumaChainCrownEntity entity,
        BakedGeoModel bakedModel,
        RenderType renderType,
        MultiBufferSource buffer,
        VertexConsumer consumer,
        float partialTick,
        int packedLight,
        int packedOverlay
    ) {
        if (!entity.shouldRenderCrown()) {
            return;
        }
        float time = entity.tickCount + partialTick;
        RenderType flow = RenderType.energySwirl(MASK, time * 0.012F % 1.0F, time * 0.006F % 1.0F);
        getRenderer().reRender(
            bakedModel,
            poseStack,
            buffer,
            entity,
            flow,
            buffer.getBuffer(flow),
            partialTick,
            LightTexture.FULL_BRIGHT,
            packedOverlay,
            0xFFFFD84A
        );
    }
}

