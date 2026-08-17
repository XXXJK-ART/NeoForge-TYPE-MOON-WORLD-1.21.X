package net.xxxjk.TYPE_MOON_WORLD.chain.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.chain.client.model.EnumaChainCrownModel;
import net.xxxjk.TYPE_MOON_WORLD.chain.entity.EnumaChainCrownEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class EnumaChainCrownRenderer extends GeoEntityRenderer<EnumaChainCrownEntity> {
    private static final ResourceLocation TEXTURE = TYPE_MOON_WORLD.id("textures/entity/heaven_chain_head.png");

    public EnumaChainCrownRenderer(EntityRendererProvider.Context context) {
        super(context, new EnumaChainCrownModel());
        addRenderLayer(new GoldenCrownFlowLayer(this));
        shadowRadius = 0.0F;
    }

    @Override
    public void render(
        EnumaChainCrownEntity entity,
        float entityYaw,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight
    ) {
        if (!entity.shouldRenderCrown()) {
            return;
        }
        poseStack.pushPose();
        poseStack.scale(10.0F, 10.0F, 10.0F);
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    @Override
    protected void applyRotations(
        EnumaChainCrownEntity entity,
        PoseStack poseStack,
        float ageInTicks,
        float rotationYaw,
        float partialTick,
        float nativeScale
    ) {
        super.applyRotations(entity, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
    }

    @Override
    public ResourceLocation getTextureLocation(EnumaChainCrownEntity entity) {
        return TEXTURE;
    }

    @Override
    public RenderType getRenderType(
        EnumaChainCrownEntity entity,
        ResourceLocation texture,
        MultiBufferSource bufferSource,
        float partialTick
    ) {
        return RenderType.entityCutoutNoCull(texture);
    }
}

