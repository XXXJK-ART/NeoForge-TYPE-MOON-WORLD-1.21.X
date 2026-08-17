package net.xxxjk.TYPE_MOON_WORLD.chain.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.xxxjk.TYPE_MOON_WORLD.chain.client.model.HeavenChainBindingModel;
import net.xxxjk.TYPE_MOON_WORLD.chain.entity.HeavenChainBindingEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class HeavenChainBindingRenderer extends GeoEntityRenderer<HeavenChainBindingEntity> {
    public HeavenChainBindingRenderer(EntityRendererProvider.Context context) {
        super(context, new HeavenChainBindingModel());
        addRenderLayer(new GoldenBindingFlowLayer(this));
        shadowRadius = 0.0F;
    }

    @Override
    public void render(
        HeavenChainBindingEntity entity,
        float entityYaw,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight
    ) {
        poseStack.pushPose();
        poseStack.scale(entity.widthScale(), entity.heightScale(), entity.widthScale());
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    @Override
    public RenderType getRenderType(
        HeavenChainBindingEntity entity,
        ResourceLocation texture,
        MultiBufferSource bufferSource,
        float partialTick
    ) {
        return RenderType.entityTranslucentEmissive(texture);
    }
}

