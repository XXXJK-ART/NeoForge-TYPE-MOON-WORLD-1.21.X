package com.example.typemoonaddon.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.example.typemoonaddon.client.model.ShadowPiercingRhoAiasModel;
import com.example.typemoonaddon.entity.SakuraShadowPiercingRhoAiasEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.example.typemoonaddon.TypeMoonAddon;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public final class ShadowPiercingRhoAiasRenderer extends GeoEntityRenderer<SakuraShadowPiercingRhoAiasEntity> {
    private static final double MODEL_Y_OFFSET = -2.1D;
    private static final Set<UUID> LOGGED_RENDERS = ConcurrentHashMap.newKeySet();

    public ShadowPiercingRhoAiasRenderer(EntityRendererProvider.Context context) {
        super(context, new ShadowPiercingRhoAiasModel());
        withScale(1.8F);
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public void render(
        SakuraShadowPiercingRhoAiasEntity entity,
        float entityYaw,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight
    ) {
        if (LOGGED_RENDERS.add(entity.getUUID())) {
            TypeMoonAddon.LOGGER.info(
                "Rendering Rho Aias display entity id={} uuid={} at [{}, {}, {}]",
                entity.getId(),
                entity.getUUID(),
                entity.getX(),
                entity.getY(),
                entity.getZ()
            );
        }
        poseStack.pushPose();
        Vec3 budOffset = entity.getBudRenderOffset(partialTick);
        poseStack.translate(budOffset.x, MODEL_Y_OFFSET, budOffset.z);
        super.render(entity, 0.0F, partialTick, poseStack, bufferSource, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    @Override
    public RenderType getRenderType(
        SakuraShadowPiercingRhoAiasEntity entity,
        ResourceLocation texture,
        MultiBufferSource bufferSource,
        float partialTick
    ) {
        return RenderType.entityTranslucent(texture);
    }
}
