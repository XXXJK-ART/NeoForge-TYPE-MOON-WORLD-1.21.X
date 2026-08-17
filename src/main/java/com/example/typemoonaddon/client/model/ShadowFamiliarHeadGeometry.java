package com.example.typemoonaddon.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** Smooth, single-surface rounded head shared by the familiar and its outline. */
final class ShadowFamiliarHeadGeometry {
    private static final int ARC_SEGMENTS = 24;
    private static final float RADIUS = 5.0F;
    private static final float BOTTOM_Y = -16.0F;
    private static final float ARC_BASE_Y = -21.0F;
    private static final float HALF_DEPTH = 0.125F;
    private static final float OUTLINE_EXPANSION = 0.18F;
    private static final float U_MIN = 36.0F / 64.0F;
    private static final float U_MAX = 60.0F / 64.0F;
    private static final float V_MIN = 0.0F;
    private static final float V_MAX = 20.0F / 64.0F;

    static void render(
        PoseStack poseStack,
        VertexConsumer consumer,
        int packedLight,
        int packedOverlay,
        int packedColor,
        ModelPart root,
        ModelPart familiar,
        boolean outline
    ) {
        float expansion = outline ? OUTLINE_EXPANSION : 0.0F;
        float radius = RADIUS + expansion;
        float bottomY = BOTTOM_Y + expansion;
        float arcBaseY = ARC_BASE_Y;
        float frontZ = -(HALF_DEPTH + expansion);
        float backZ = HALF_DEPTH + expansion;

        poseStack.pushPose();
        root.translateAndRotate(poseStack);
        familiar.translateAndRotate(poseStack);
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        PoseStack.Pose pose = poseStack.last();

        HeadVertex lowerLeftFront = vertex(-radius, bottomY, frontZ);
        HeadVertex lowerRightFront = vertex(radius, bottomY, frontZ);
        HeadVertex arcRightFront = vertex(radius, arcBaseY, frontZ);
        HeadVertex arcLeftFront = vertex(-radius, arcBaseY, frontZ);
        emitQuad(
            pose, consumer, packedLight, packedOverlay, packedColor, outline,
            lowerLeftFront, lowerRightFront, arcRightFront, arcLeftFront,
            0.0F, 0.0F, -1.0F
        );

        HeadVertex lowerLeftBack = vertex(-radius, bottomY, backZ);
        HeadVertex lowerRightBack = vertex(radius, bottomY, backZ);
        HeadVertex arcRightBack = vertex(radius, arcBaseY, backZ);
        HeadVertex arcLeftBack = vertex(-radius, arcBaseY, backZ);
        emitQuad(
            pose, consumer, packedLight, packedOverlay, packedColor, outline,
            lowerRightBack, lowerLeftBack, arcLeftBack, arcRightBack,
            0.0F, 0.0F, 1.0F
        );

        HeadVertex domeCenterFront = vertex(0.0F, arcBaseY, frontZ);
        HeadVertex domeCenterBack = vertex(0.0F, arcBaseY, backZ);
        for (int segment = 0; segment < ARC_SEGMENTS; segment++) {
            float angle0 = Mth.PI - segment * Mth.PI / ARC_SEGMENTS;
            float angle1 = Mth.PI - (segment + 1) * Mth.PI / ARC_SEGMENTS;
            HeadVertex front0 = arcVertex(radius, arcBaseY, frontZ, angle0);
            HeadVertex front1 = arcVertex(radius, arcBaseY, frontZ, angle1);
            HeadVertex back0 = arcVertex(radius, arcBaseY, backZ, angle0);
            HeadVertex back1 = arcVertex(radius, arcBaseY, backZ, angle1);

            emitQuad(
                pose, consumer, packedLight, packedOverlay, packedColor, outline,
                domeCenterFront, front1, front0, domeCenterFront,
                0.0F, 0.0F, -1.0F
            );
            emitQuad(
                pose, consumer, packedLight, packedOverlay, packedColor, outline,
                domeCenterBack, back0, back1, domeCenterBack,
                0.0F, 0.0F, 1.0F
            );

            float middleAngle = (angle0 + angle1) * 0.5F;
            emitQuad(
                pose, consumer, packedLight, packedOverlay, packedColor, outline,
                front0, front1, back1, back0,
                Mth.cos(middleAngle), -Mth.sin(middleAngle), 0.0F
            );
        }

        emitQuad(
            pose, consumer, packedLight, packedOverlay, packedColor, outline,
            lowerLeftBack, lowerLeftFront, arcLeftFront, arcLeftBack,
            -1.0F, 0.0F, 0.0F
        );
        emitQuad(
            pose, consumer, packedLight, packedOverlay, packedColor, outline,
            lowerRightFront, lowerRightBack, arcRightBack, arcRightFront,
            1.0F, 0.0F, 0.0F
        );
        emitQuad(
            pose, consumer, packedLight, packedOverlay, packedColor, outline,
            lowerLeftBack, lowerRightBack, lowerRightFront, lowerLeftFront,
            0.0F, 1.0F, 0.0F
        );
        poseStack.popPose();
    }

    private static HeadVertex arcVertex(float radius, float baseY, float z, float angle) {
        return vertex(
            Mth.cos(angle) * radius,
            baseY - Mth.sin(angle) * radius,
            z
        );
    }

    private static HeadVertex vertex(float x, float y, float z) {
        float u = Mth.lerp((x + RADIUS) / (RADIUS * 2.0F), U_MIN, U_MAX);
        float v = Mth.lerp((y - (ARC_BASE_Y - RADIUS)) / (BOTTOM_Y - (ARC_BASE_Y - RADIUS)), V_MIN, V_MAX);
        return new HeadVertex(x, y, z, u, v);
    }

    private static void emitQuad(
        PoseStack.Pose pose,
        VertexConsumer consumer,
        int packedLight,
        int packedOverlay,
        int packedColor,
        boolean reversed,
        HeadVertex first,
        HeadVertex second,
        HeadVertex third,
        HeadVertex fourth,
        float normalX,
        float normalY,
        float normalZ
    ) {
        float direction = reversed ? -1.0F : 1.0F;
        if (reversed) {
            emitVertex(pose, consumer, packedLight, packedOverlay, packedColor, fourth, normalX * direction, normalY * direction, normalZ * direction);
            emitVertex(pose, consumer, packedLight, packedOverlay, packedColor, third, normalX * direction, normalY * direction, normalZ * direction);
            emitVertex(pose, consumer, packedLight, packedOverlay, packedColor, second, normalX * direction, normalY * direction, normalZ * direction);
            emitVertex(pose, consumer, packedLight, packedOverlay, packedColor, first, normalX * direction, normalY * direction, normalZ * direction);
            return;
        }
        emitVertex(pose, consumer, packedLight, packedOverlay, packedColor, first, normalX, normalY, normalZ);
        emitVertex(pose, consumer, packedLight, packedOverlay, packedColor, second, normalX, normalY, normalZ);
        emitVertex(pose, consumer, packedLight, packedOverlay, packedColor, third, normalX, normalY, normalZ);
        emitVertex(pose, consumer, packedLight, packedOverlay, packedColor, fourth, normalX, normalY, normalZ);
    }

    private static void emitVertex(
        PoseStack.Pose pose,
        VertexConsumer consumer,
        int packedLight,
        int packedOverlay,
        int packedColor,
        HeadVertex vertex,
        float normalX,
        float normalY,
        float normalZ
    ) {
        consumer.addVertex(pose, vertex.x(), vertex.y(), vertex.z())
            .setColor(packedColor)
            .setUv(vertex.u(), vertex.v())
            .setOverlay(packedOverlay)
            .setLight(packedLight)
            .setNormal(pose, normalX, normalY, normalZ);
    }

    private record HeadVertex(float x, float y, float z, float u, float v) {
    }

    private ShadowFamiliarHeadGeometry() {
    }
}
