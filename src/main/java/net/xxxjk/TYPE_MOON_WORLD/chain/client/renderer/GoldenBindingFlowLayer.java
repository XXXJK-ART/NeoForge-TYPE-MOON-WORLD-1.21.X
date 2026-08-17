package net.xxxjk.TYPE_MOON_WORLD.chain.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.chain.entity.HeavenChainBindingEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public final class GoldenBindingFlowLayer extends GeoRenderLayer<HeavenChainBindingEntity> {
    private static final ResourceLocation TEXTURE = TYPE_MOON_WORLD.id("textures/entity/chains_of_heaven_silver.png");

    public GoldenBindingFlowLayer(GeoRenderer<HeavenChainBindingEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(
        PoseStack poseStack,
        HeavenChainBindingEntity entity,
        BakedGeoModel bakedModel,
        RenderType renderType,
        MultiBufferSource buffer,
        VertexConsumer consumer,
        float partialTick,
        int packedLight,
        int packedOverlay
    ) {
        if (!entity.hasEnumaFlow()) {
            return;
        }
        float time = entity.tickCount + partialTick;
        RenderType flow = RenderType.energySwirl(TEXTURE, time * 0.014F % 1.0F, time * 0.007F % 1.0F);
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

