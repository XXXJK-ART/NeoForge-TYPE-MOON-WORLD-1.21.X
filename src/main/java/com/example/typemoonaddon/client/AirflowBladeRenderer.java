package com.example.typemoonaddon.client;

import com.example.typemoonaddon.entity.AirflowBladeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Reference-driven translucent wind geometry; damage and collision stay server-side. */
public final class AirflowBladeRenderer extends EntityRenderer<AirflowBladeEntity> {
    private static final ResourceLocation BEACON_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");

    public AirflowBladeRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(
            AirflowBladeEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight
    ) {
        VertexConsumer translucent = buffers.getBuffer(RenderType.entityTranslucentEmissive(BEACON_TEXTURE));
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        float time = entity.tickCount + partialTick;
        float pulse = 0.75F + 0.25F * Mth.sin(time * 0.55F);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
        if (entity.mode() == AirflowBladeEntity.BLADE_MODE) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(time * 0.34F) * 3.5F));
            drawBlade(poseStack.last(), translucent, lines, time, pulse);
        } else {
            float charge = 1.05F + entity.charge() * 0.9F;
            poseStack.scale(charge, charge, charge);
            drawCannon(poseStack.last(), translucent, lines, time, pulse);
        }
        poseStack.popPose();
    }

    private static void drawBlade(
            PoseStack.Pose pose,
            VertexConsumer translucent,
            VertexConsumer lines,
            float time,
            float pulse
    ) {
        float shimmer = 0.84F + pulse * 0.16F;
        drawBladeSheet(pose, translucent, lines,
                0.0F, 0.0F, 2.55F, 0.96F, 0.14F, shimmer);
        drawBladeSheet(pose, translucent, lines,
                -0.72F, 0.22F, 2.2F, 0.72F, 0.075F, shimmer * 0.78F);
        drawBladeSheet(pose, translucent, lines,
                0.72F, -0.18F, 2.2F, 0.72F, 0.075F, shimmer * 0.78F);

        for (int index = 0; index < 7; index++) {
            float phase = time * 0.18F + index * 0.9F;
            float y = -1.8F + index * 0.6F + Mth.sin(phase) * 0.12F;
            float x = Mth.sin(phase * 0.73F) * 0.72F;
            line(pose, lines,
                    x, y, 0.22F,
                    x * 0.62F, y + Mth.cos(phase) * 0.22F, 2.25F,
                    0.72F, 0.94F, 1.0F, 0.34F);
        }
    }

    private static void drawBladeSheet(
            PoseStack.Pose pose,
            VertexConsumer translucent,
            VertexConsumer lines,
            float xOffset,
            float zOffset,
            float halfHeight,
            float halfWidth,
            float interiorAlpha,
            float brightness
    ) {
        int segments = 20;
        for (int index = 0; index < segments; index++) {
            float t0 = -1.0F + index * 2.0F / segments;
            float t1 = -1.0F + (index + 1) * 2.0F / segments;
            float y0 = t0 * halfHeight;
            float y1 = t1 * halfHeight;
            float envelope0 = Mth.sqrt(Math.max(0.0F, 1.0F - t0 * t0));
            float envelope1 = Mth.sqrt(Math.max(0.0F, 1.0F - t1 * t1));
            float left0 = xOffset - halfWidth * envelope0;
            float right0 = xOffset + halfWidth * envelope0;
            float left1 = xOffset - halfWidth * envelope1;
            float right1 = xOffset + halfWidth * envelope1;
            quadDoubleSided(pose, translucent,
                    left0, y0, zOffset,
                    right0, y0, zOffset,
                    right1, y1, zOffset,
                    left1, y1, zOffset,
                    0.68F * brightness, 0.91F * brightness, 1.0F, interiorAlpha);

            for (int side = -1; side <= 1; side += 2) {
                float outer0 = xOffset + side * halfWidth * envelope0;
                float outer1 = xOffset + side * halfWidth * envelope1;
                float edgeWidth0 = 0.055F + envelope0 * 0.055F;
                float edgeWidth1 = 0.055F + envelope1 * 0.055F;
                float inner0 = outer0 - side * edgeWidth0;
                float inner1 = outer1 - side * edgeWidth1;
                quadDoubleSided(pose, translucent,
                        outer0, y0, zOffset - 0.012F,
                        inner0, y0, zOffset - 0.012F,
                        inner1, y1, zOffset - 0.012F,
                        outer1, y1, zOffset - 0.012F,
                        0.9F * brightness, 0.99F * brightness, 1.0F, 0.72F);
                line(pose, lines,
                        outer0, y0, zOffset - 0.02F,
                        outer1, y1, zOffset - 0.02F,
                        0.95F, 1.0F, 1.0F, 0.92F);
            }
        }
    }

    private static void drawCannon(
            PoseStack.Pose pose,
            VertexConsumer translucent,
            VertexConsumer lines,
            float time,
            float pulse
    ) {
        float radius = 0.78F + pulse * 0.16F;
        drawCorePlanes(pose, translucent, radius * 0.7F,
                0.94F, 1.0F, 1.0F, 0.46F);
        drawCorePlanes(pose, translucent, radius,
                0.48F, 0.9F, 1.0F, 0.16F);

        drawRingXY(pose, lines, radius * 1.2F, time * 5.5F,
                0.78F, 0.97F, 1.0F, 0.9F);
        drawRingXZ(pose, lines, radius * 1.34F, -time * 4.2F,
                0.5F, 0.9F, 1.0F, 0.68F);
        drawRingYZ(pose, lines, radius * 1.48F, time * 3.4F,
                0.88F, 1.0F, 1.0F, 0.5F);
        drawSphericalSpiral(pose, lines, radius * 1.42F, time * 0.14F,
                3.0F, 0.76F, 0.96F, 1.0F, 0.72F);
        drawSphericalSpiral(pose, lines, radius * 1.58F, -time * 0.11F,
                -2.5F, 0.52F, 0.9F, 1.0F, 0.48F);

        int rays = 18;
        for (int index = 0; index < rays; index++) {
            float angle = index * Mth.TWO_PI / rays + time * 0.045F;
            float x = Mth.cos(angle) * radius * 0.82F;
            float y = Mth.sin(angle) * radius * 0.82F;
            float spread = 1.6F + (index % 3) * 0.18F;
            line(pose, lines,
                    x, y, radius * 0.2F,
                    x * spread, y * spread, radius * (1.85F + (index % 2) * 0.4F),
                    0.72F, 0.95F, 1.0F, 0.34F);
        }
    }

    private static void drawCorePlanes(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float radius,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        quadDoubleSided(pose, consumer,
                -radius, -radius, 0.0F,
                radius, -radius, 0.0F,
                radius, radius, 0.0F,
                -radius, radius, 0.0F,
                red, green, blue, alpha);
        quadDoubleSided(pose, consumer,
                -radius, 0.0F, -radius,
                radius, 0.0F, -radius,
                radius, 0.0F, radius,
                -radius, 0.0F, radius,
                red, green, blue, alpha * 0.82F);
        quadDoubleSided(pose, consumer,
                0.0F, -radius, -radius,
                0.0F, radius, -radius,
                0.0F, radius, radius,
                0.0F, -radius, radius,
                red, green, blue, alpha * 0.82F);
    }

    private static void drawRingXY(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float radius,
            float rotation,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        int segments = 32;
        float radians = rotation * Mth.DEG_TO_RAD;
        for (int index = 0; index < segments; index++) {
            float a0 = radians + index * Mth.TWO_PI / segments;
            float a1 = radians + (index + 1) * Mth.TWO_PI / segments;
            line(pose, consumer,
                    Mth.cos(a0) * radius, Mth.sin(a0) * radius, 0.0F,
                    Mth.cos(a1) * radius, Mth.sin(a1) * radius, 0.0F,
                    red, green, blue, alpha);
        }
    }

    private static void drawRingXZ(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float radius,
            float rotation,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        int segments = 32;
        float radians = rotation * Mth.DEG_TO_RAD;
        for (int index = 0; index < segments; index++) {
            float a0 = radians + index * Mth.TWO_PI / segments;
            float a1 = radians + (index + 1) * Mth.TWO_PI / segments;
            line(pose, consumer,
                    Mth.cos(a0) * radius, 0.0F, Mth.sin(a0) * radius,
                    Mth.cos(a1) * radius, 0.0F, Mth.sin(a1) * radius,
                    red, green, blue, alpha);
        }
    }

    private static void drawRingYZ(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float radius,
            float rotation,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        int segments = 32;
        float radians = rotation * Mth.DEG_TO_RAD;
        for (int index = 0; index < segments; index++) {
            float a0 = radians + index * Mth.TWO_PI / segments;
            float a1 = radians + (index + 1) * Mth.TWO_PI / segments;
            line(pose, consumer,
                    0.0F, Mth.cos(a0) * radius, Mth.sin(a0) * radius,
                    0.0F, Mth.cos(a1) * radius, Mth.sin(a1) * radius,
                    red, green, blue, alpha);
        }
    }

    private static void drawSphericalSpiral(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float radius,
            float rotation,
            float turns,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        int segments = 48;
        for (int index = 0; index < segments; index++) {
            float t0 = -1.0F + index * 2.0F / segments;
            float t1 = -1.0F + (index + 1) * 2.0F / segments;
            float ring0 = Mth.sqrt(Math.max(0.0F, 1.0F - t0 * t0));
            float ring1 = Mth.sqrt(Math.max(0.0F, 1.0F - t1 * t1));
            float angle0 = rotation + turns * Mth.PI * t0;
            float angle1 = rotation + turns * Mth.PI * t1;
            line(pose, consumer,
                    Mth.cos(angle0) * ring0 * radius, t0 * radius,
                    Mth.sin(angle0) * ring0 * radius,
                    Mth.cos(angle1) * ring1 * radius, t1 * radius,
                    Mth.sin(angle1) * ring1 * radius,
                    red, green, blue, alpha);
        }
    }

    private static void quadDoubleSided(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float red, float green, float blue, float alpha
    ) {
        quad(pose, consumer,
                x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3,
                red, green, blue, alpha);
        quad(pose, consumer,
                x3, y3, z3, x2, y2, z2, x1, y1, z1, x0, y0, z0,
                red, green, blue, alpha);
    }

    private static void quad(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float red, float green, float blue, float alpha
    ) {
        vertex(pose, consumer, x0, y0, z0, red, green, blue, alpha, 0.0F, 0.0F);
        vertex(pose, consumer, x1, y1, z1, red, green, blue, alpha, 1.0F, 0.0F);
        vertex(pose, consumer, x2, y2, z2, red, green, blue, alpha, 1.0F, 1.0F);
        vertex(pose, consumer, x3, y3, z3, red, green, blue, alpha, 0.0F, 1.0F);
    }

    private static void vertex(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float x, float y, float z,
            float red, float green, float blue, float alpha,
            float u, float v
    ) {
        consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
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
    public ResourceLocation getTextureLocation(AirflowBladeEntity entity) {
        return BEACON_TEXTURE;
    }
}
