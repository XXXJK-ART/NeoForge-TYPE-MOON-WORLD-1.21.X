package io.github.typemoonaddon.client.renderer;

import io.github.typemoonaddon.TypeMoonAddon;
import io.github.typemoonaddon.client.model.BlackShadowModel;
import io.github.typemoonaddon.entity.BlackShadowEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public final class BlackShadowRenderer extends MobRenderer<BlackShadowEntity, BlackShadowModel> {
    private static final ResourceLocation TEXTURE = TypeMoonAddon.id("textures/entity/black_shadow.png");

    public BlackShadowRenderer(EntityRendererProvider.Context context) {
        super(context, new BlackShadowModel(context.bakeLayer(BlackShadowModel.LAYER_LOCATION)), 0.35F);
        this.addLayer(new BlackShadowOutlineLayer(this, context.getModelSet()));
    }

    @Override
    public ResourceLocation getTextureLocation(BlackShadowEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(
        BlackShadowEntity entity,
        float entityYaw,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight
    ) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        int chargeTicks = entity.getMagicOutputChargeTicks();
        if (chargeTicks <= 0) {
            return;
        }

        float elapsed = BlackShadowEntity.MAGIC_OUTPUT_CHARGE_TICKS - chargeTicks + partialTick;
        float pulse = 1.0F + 0.025F * (float)Math.sin(elapsed * 0.28F);
        float orbRadius = entity.magicOutputOrbRadius(partialTick);
        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() + orbRadius, 0.0D);
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        renderSphere(
            consumer,
            matrix,
            orbRadius * pulse,
            elapsed
        );
        renderSphere(
            consumer,
            matrix,
            orbRadius * (pulse + 0.018F),
            elapsed + 7.0F
        );
        poseStack.popPose();
    }

    private static void renderSphere(VertexConsumer consumer, Matrix4f matrix, float radius, float animation) {
        int latitudeSegments = 16;
        int longitudeSegments = 32;
        for (int latitude = 0; latitude < latitudeSegments; latitude++) {
            double latitudeA = -Math.PI * 0.5D + Math.PI * latitude / latitudeSegments;
            double latitudeB = -Math.PI * 0.5D + Math.PI * (latitude + 1) / latitudeSegments;
            for (int longitude = 0; longitude < longitudeSegments; longitude++) {
                double longitudeA = Math.PI * 2.0D * longitude / longitudeSegments;
                double longitudeB = Math.PI * 2.0D * (longitude + 1) / longitudeSegments;
                int red = 90 + (int)(85.0D * (0.5D + 0.5D * Math.sin(
                    longitudeA * 3.0D + latitudeA * 4.0D + animation * 0.08D
                )));
                int color = 0xE0000000 | red << 16 | 4 << 8 | 10;
                sphereVertex(consumer, matrix, radius, latitudeA, longitudeA, color);
                sphereVertex(consumer, matrix, radius, latitudeA, longitudeB, color);
                sphereVertex(consumer, matrix, radius, latitudeB, longitudeB, color);
                sphereVertex(consumer, matrix, radius, latitudeB, longitudeA, color);
            }
        }
    }

    private static void sphereVertex(
        VertexConsumer consumer,
        Matrix4f matrix,
        float radius,
        double latitude,
        double longitude,
        int color
    ) {
        float horizontal = radius * (float)Math.cos(latitude);
        consumer.addVertex(
            matrix,
            horizontal * (float)Math.cos(longitude),
            radius * (float)Math.sin(latitude),
            horizontal * (float)Math.sin(longitude)
        ).setColor(color);
    }

    @Override
    protected void setupRotations(
        BlackShadowEntity entity,
        PoseStack poseStack,
        float bob,
        float bodyRot,
        float partialTick,
        float scale
    ) {
        super.setupRotations(entity, poseStack, bob, bodyRot + 180.0F, partialTick, scale);
    }

    @Override
    protected float getFlipDegrees(BlackShadowEntity entity) {
        return 0.0F;
    }
}
