package io.github.typemoonaddon.shadowlogic.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.typemoonaddon.TypeMoonAddon;
import io.github.typemoonaddon.config.GameplayConfig;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Shared runtime tessellation and UV mapping of the plane from shadow_art_ribbon.geo.json. */
public final class ShadowRibbonPlaneGeometry {
    public static final ResourceLocation TEXTURE = TypeMoonAddon.id("textures/entity/shadow_art_ribbon.png");
    public static final double MODEL_WIDTH = GameplayConfig.SHADOW_ART_MODEL_WIDTH;
    public static final double MODEL_LENGTH = GameplayConfig.SHADOW_ART_MODEL_SEGMENT_LENGTH;
    private static final float MODEL_U_MIN = 0.0F / 16.0F;
    private static final float MODEL_U_MAX = 7.0F / 16.0F;
    private static final float MODEL_V_MIN = 6.0F / 16.0F;
    private static final float MODEL_V_MAX = 16.0F / 16.0F;

    public static VertexConsumer consumer(MultiBufferSource buffers) {
        return buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
    }

    public static void renderSegment(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        Vec3 previousLeft,
        Vec3 previousRight,
        Vec3 currentRight,
        Vec3 currentLeft,
        int color
    ) {
        renderSegment(
            consumer,
            pose,
            previousLeft,
            previousRight,
            currentRight,
            currentLeft,
            color,
            0.0F,
            1.0F
        );
    }

    public static void renderSegment(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        Vec3 previousLeft,
        Vec3 previousRight,
        Vec3 currentRight,
        Vec3 currentLeft,
        int color,
        float modelVStart,
        float modelVEnd
    ) {
        Vec3 normal = previousRight.subtract(previousLeft).cross(currentLeft.subtract(previousLeft));
        if (normal.lengthSqr() < 1.0E-8D) {
            normal = new Vec3(0.0D, 1.0D, 0.0D);
        } else {
            normal = normal.normalize();
        }
        float vStart = MODEL_V_MIN + (MODEL_V_MAX - MODEL_V_MIN) * modelVStart;
        float vEnd = MODEL_V_MIN + (MODEL_V_MAX - MODEL_V_MIN) * modelVEnd;
        vertex(consumer, pose, previousLeft, color, MODEL_U_MIN, vStart, normal);
        vertex(consumer, pose, previousRight, color, MODEL_U_MAX, vStart, normal);
        vertex(consumer, pose, currentRight, color, MODEL_U_MAX, vEnd, normal);
        vertex(consumer, pose, currentLeft, color, MODEL_U_MIN, vEnd, normal);
    }

    private static void vertex(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        Vec3 point,
        int color,
        float u,
        float v,
        Vec3 normal
    ) {
        consumer.addVertex(pose, (float)point.x, (float)point.y, (float)point.z)
            .setColor(color)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightTexture.FULL_BRIGHT)
            .setNormal(pose, (float)normal.x, (float)normal.y, (float)normal.z);
    }

    private ShadowRibbonPlaneGeometry() {
    }
}
