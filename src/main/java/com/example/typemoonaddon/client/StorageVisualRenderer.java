package com.example.typemoonaddon.client;

import com.example.typemoonaddon.entity.StorageVisualEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Self-contained storage prism renderer used during creature transfers. */
public final class StorageVisualRenderer extends EntityRenderer<StorageVisualEntity> {
    private static final float MIN_CONTAINMENT_SCALE = 1.35F;
    private static final float MAX_CONTAINMENT_SCALE = 8.0F;
    private static final float CONTAINMENT_MARGIN = 1.24F;
    private static final ResourceLocation BEACON_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");
    private static final RenderType STORAGE_RENDER_TYPE =
            RenderType.entityTranslucentEmissive(BEACON_TEXTURE);

    public StorageVisualRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(
            StorageVisualEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight
    ) {
        float formation = entity.formationScale(partialTick);
        float scale = entity.isCollapsing() ? entity.collapseScale(partialTick) : formation;
        if (scale <= 0.001F) {
            return;
        }

        scale *= containmentScale(entity);

        float time = entity.tickCount + partialTick;
        float pulse = 0.5F + 0.5F * Mth.sin(time * 0.22F);
        float alpha = entity.isCollapsing()
                ? 0.22F * entity.collapseScale(partialTick)
                : (entity.tickCount < 12 ? 0.16F : 0.21F) + pulse * 0.045F;
        try (AddonRenderBuffers renderBuffers = AddonRenderBuffers.fixed(STORAGE_RENDER_TYPE, RenderType.lines())) {
            MultiBufferSource.BufferSource localBuffers = renderBuffers.source();
            VertexConsumer beam = localBuffers.getBuffer(STORAGE_RENDER_TYPE);
            VertexConsumer lines = localBuffers.getBuffer(RenderType.lines());

            poseStack.pushPose();
            try {
                poseStack.scale(scale, scale, scale);
                poseStack.mulPose(Axis.YP.rotationDegrees(entity.rotationDegrees(partialTick)));

                PoseStack.Pose pose = poseStack.last();
                drawBox(pose, beam, 1.5F, 1.5F, 1.5F, 0.18F, 0.82F, 1.0F, alpha);
                drawBox(pose, beam, 1.445F, 1.445F, 1.445F, 0.62F, 0.24F, 1.0F, alpha * 0.56F);
                drawBox(pose, beam, 0.15F + pulse * 0.035F, 1.72F, 0.15F,
                        0.42F, 0.72F, 1.0F, 0.075F + pulse * 0.035F);

                LevelRenderer.renderLineBox(poseStack, lines, box(1.515D),
                        0.58F + pulse * 0.34F, 1.0F, 0.98F, alpha);
                LevelRenderer.renderLineBox(poseStack, lines, box(1.475D),
                        0.28F, 0.72F + pulse * 0.25F, 1.0F, alpha * 0.68F);
                LevelRenderer.renderLineBox(poseStack, lines, box(1.415D),
                        0.78F, 0.34F + pulse * 0.34F, 1.0F, alpha * 0.76F);
                drawFaceLattice(pose, lines, pulse);
                drawCornerStars(pose, lines, pulse);
            } finally {
                poseStack.popPose();
            }

            poseStack.pushPose();
            try {
                poseStack.scale(scale, scale, scale);
                drawOrbitSystem(poseStack, lines, time, pulse);
                localBuffers.endBatch(STORAGE_RENDER_TYPE);
                localBuffers.endBatch(RenderType.lines());
            } finally {
                poseStack.popPose();
            }
        }
    }

    /** Sizes the prism from the synchronized target bounds, with bounded safety margins. */
    private static float containmentScale(StorageVisualEntity entity) {
        float result = MIN_CONTAINMENT_SCALE;
        if (entity.targetId().isEmpty()) {
            return result;
        }
        List<LivingEntity> nearby = entity.level().getEntitiesOfClass(
                LivingEntity.class,
                entity.getBoundingBox().inflate(16.0D),
                candidate -> candidate.getUUID().equals(entity.targetId().get()));
        if (!nearby.isEmpty()) {
            LivingEntity living = nearby.get(0);
            float largestDimension = Math.max(living.getBbWidth(), living.getBbHeight());
            result = Math.max(result, largestDimension * CONTAINMENT_MARGIN / 3.0F);
        }
        return Mth.clamp(result, MIN_CONTAINMENT_SCALE, MAX_CONTAINMENT_SCALE);
    }

    private static AABB box(double halfSize) {
        return new AABB(-halfSize, -halfSize, -halfSize, halfSize, halfSize, halfSize);
    }

    private static void drawFaceLattice(PoseStack.Pose pose, VertexConsumer lines, float pulse) {
        float offset = 0.58F + pulse * 0.3F;
        float[] values = {-0.72F, 0.0F, 0.72F};
        for (float value : values) {
            line(pose, lines, -1.49F, value, -1.505F, 1.49F, value, -1.505F,
                    0.8F, 1.0F, 0.34F, offset);
            line(pose, lines, -1.49F, value, 1.505F, 1.49F, value, 1.505F,
                    0.8F, 1.0F, 0.34F, offset);
            line(pose, lines, -1.505F, value, -1.49F, -1.505F, value, 1.49F,
                    0.72F, 0.42F, 1.0F, 0.3F);
            line(pose, lines, 1.505F, value, -1.49F, 1.505F, value, 1.49F,
                    0.72F, 0.42F, 1.0F, 0.3F);
        }
    }

    private static void drawCornerStars(PoseStack.Pose pose, VertexConsumer lines, float pulse) {
        float offset = 0.1F + pulse * 0.075F;
        for (int x : new int[]{-1, 1}) {
            for (int y : new int[]{-1, 1}) {
                for (int z : new int[]{-1, 1}) {
                    float px = x * 1.515F;
                    float py = y * 1.515F;
                    float pz = z * 1.515F;
                    line(pose, lines, px - offset, py, pz, px + offset, py, pz,
                            0.88F, 0.96F, 1.0F, 0.92F);
                    line(pose, lines, px, py - offset, pz, px, py + offset, pz,
                            0.88F, 0.96F, 1.0F, 0.92F);
                    line(pose, lines, px, py, pz - offset, px, py, pz + offset,
                            0.72F, 0.48F, 1.0F, 0.84F);
                }
            }
        }
    }

    private static void drawOrbitSystem(PoseStack poseStack, VertexConsumer lines, float time, float pulse) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-time * 1.45F));
        poseStack.mulPose(Axis.XP.rotationDegrees(13.0F));
        drawThickRing(poseStack.last(), lines, 2.05F, 0.0F, 0.3F, 0.84F, 1.0F, 0.66F + pulse * 0.24F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 1.1F + 65.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-22.0F));
        drawThickRing(poseStack.last(), lines, 1.88F, 0.0F, 0.72F, 0.3F, 1.0F, 0.6F + pulse * 0.24F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(76.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(time * 0.85F));
        drawThickRing(poseStack.last(), lines, 1.76F, 0.0F, 0.35F, 0.66F, 1.0F, 0.52F + pulse * 0.22F);
        poseStack.popPose();
    }

    private static void drawThickRing(
            PoseStack.Pose pose,
            VertexConsumer lines,
            float radius,
            float y,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        drawRing(pose, lines, radius, y, red, green, blue, alpha);
        for (int index = 1; index <= 3; index++) {
            float offset = index * 0.018F;
            float adjustedAlpha = alpha * (1.0F - index * 0.14F);
            drawRing(pose, lines, radius + offset, y, red, green, blue, adjustedAlpha);
            drawRing(pose, lines, radius - offset, y, red, green, blue, adjustedAlpha);
            drawRing(pose, lines, radius, y + offset * 0.7F, red, green, blue, adjustedAlpha * 0.78F);
            drawRing(pose, lines, radius, y - offset * 0.7F, red, green, blue, adjustedAlpha * 0.78F);
        }
    }

    private static void drawRing(
            PoseStack.Pose pose,
            VertexConsumer lines,
            float radius,
            float y,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        int segments = 72;
        for (int index = 0; index < segments; index++) {
            double a0 = Math.PI * 2.0D * index / segments;
            double a1 = Math.PI * 2.0D * (index + 1) / segments;
            line(pose, lines,
                    (float)Math.cos(a0) * radius, y, (float)Math.sin(a0) * radius,
                    (float)Math.cos(a1) * radius, y, (float)Math.sin(a1) * radius,
                    red, green, blue, alpha);
        }
    }

    private static void drawBox(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float x,
            float y,
            float z,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        quad(pose, consumer, -x, -y, -z, x, -y, -z, x, y, -z, -x, y, -z, red, green, blue, alpha, 0.0F, 0.0F, -1.0F);
        quad(pose, consumer, -x, -y, z, -x, y, z, x, y, z, x, -y, z, red, green, blue, alpha, 0.0F, 0.0F, 1.0F);
        quad(pose, consumer, -x, -y, -z, -x, y, -z, -x, y, z, -x, -y, z, red, green, blue, alpha, -1.0F, 0.0F, 0.0F);
        quad(pose, consumer, x, -y, -z, x, -y, z, x, y, z, x, y, -z, red, green, blue, alpha, 1.0F, 0.0F, 0.0F);
        quad(pose, consumer, -x, y, -z, x, y, -z, x, y, z, -x, y, z, red, green, blue, alpha, 0.0F, 1.0F, 0.0F);
        quad(pose, consumer, -x, -y, -z, -x, -y, z, x, -y, z, x, -y, -z, red, green, blue, alpha, 0.0F, -1.0F, 0.0F);
    }

    private static void quad(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float red, float green, float blue, float alpha,
            float nx, float ny, float nz
    ) {
        vertex(pose, consumer, x0, y0, z0, red, green, blue, alpha, 0.0F, 0.0F, nx, ny, nz);
        vertex(pose, consumer, x1, y1, z1, red, green, blue, alpha, 1.0F, 0.0F, nx, ny, nz);
        vertex(pose, consumer, x2, y2, z2, red, green, blue, alpha, 1.0F, 1.0F, nx, ny, nz);
        vertex(pose, consumer, x3, y3, z3, red, green, blue, alpha, 0.0F, 1.0F, nx, ny, nz);
    }

    private static void vertex(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float x, float y, float z,
            float red, float green, float blue, float alpha,
            float u, float v,
            float nx, float ny, float nz
    ) {
        consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0)
                .setNormal(pose, nx, ny, nz);
    }

    private static void line(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float red, float green, float blue, float alpha
    ) {
        consumer.addVertex(pose, x0, y0, z0)
                .setColor(red, green, blue, alpha)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
        consumer.addVertex(pose, x1, y1, z1)
                .setColor(red, green, blue, alpha)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(StorageVisualEntity entity) {
        return BEACON_TEXTURE;
    }
}
