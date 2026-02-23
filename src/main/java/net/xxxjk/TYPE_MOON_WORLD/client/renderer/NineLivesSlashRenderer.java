package net.xxxjk.TYPE_MOON_WORLD.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.entity.RubyProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;

import java.util.ArrayList;
import java.util.List;

public final class NineLivesSlashRenderer {
    private static final ResourceLocation TEX = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");

    public static void renderVisualSlash(ThrowableItemProjectile entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer) {
        if (!(entity instanceof RubyProjectileEntity ruby)) return;
        List<Vec3> trace = ruby.tracePos;
        if (trace.size() < 2) return;

        Level level = entity.level();
        Vec3 currentPos = entity.getPosition(partialTicks);
        Vec3 camPos = net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().camera.getPosition();

        List<Vec3> points = new ArrayList<>(trace);
        if (points.size() < 2) return;
        points.add(currentPos);

        poseStack.pushPose();
        poseStack.translate(-currentPos.x, -currentPos.y, -currentPos.z);
        PoseStack.Pose pose = poseStack.last();

        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEX));
        float baseWidth = 0.1f * ruby.getVisualScale();
        int samples = 5;
        float repeat = Math.max(1.0f, points.size() / 16.0f);

        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 p1 = points.get(i);
            Vec3 p2 = points.get(i + 1);
            Vec3 p0 = i > 0 ? points.get(i - 1) : p1.subtract(p2.subtract(p1));
            Vec3 p3 = i < points.size() - 2 ? points.get(i + 2) : p2.add(p2.subtract(p1));

            for (int j = 0; j < samples; j++) {
                float t1 = (float) j / samples;
                float t2 = (float) (j + 1) / samples;
                Vec3 start = catmullRom(t1, p0, p1, p2, p3);
                Vec3 end = catmullRom(t2, p0, p1, p2, p3);

                float total = (points.size() - 1) * samples;
                float gi1 = i * samples + j;
                float gi2 = gi1 + 1;
                float pgr1 = gi1 / total;
                float pgr2 = gi2 / total;

                float w1 = visualWidth(baseWidth, pgr1);
                float w2 = visualWidth(baseWidth, pgr2);
                float a1 = visualAlpha(pgr1);
                float a2 = visualAlpha(pgr2);
                float u1 = pgr1 * repeat;
                float u2 = pgr2 * repeat;

                draw(level, pose, vc, start, end, camPos, w1, w2, a1, a2, u1, u2, 1f, 1f, 1f, 1f, 1f, 1f);
            }
        }

        poseStack.popPose();
    }

    private static Vec3 catmullRom(float t, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3) {
        float t2 = t * t;
        float t3 = t2 * t;
        double x = 0.5 * ((2 * p1.x) + (-p0.x + p2.x) * t + (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2 + (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t3);
        double y = 0.5 * ((2 * p1.y) + (-p0.y + p2.y) * t + (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2 + (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3);
        double z = 0.5 * ((2 * p1.z) + (-p0.z + p2.z) * t + (2 * p0.z - 5 * p1.z + 4 * p2.z - p3.z) * t2 + (-p0.z + 3 * p1.z - 3 * p2.z + p3.z) * t3);
        return new Vec3(x, y, z);
    }

    private static float visualWidth(float base, float progress) {
        float s = Mth.sin((float) Math.PI * progress);
        return base * (0.4f + 0.6f * s);
    }

    private static float visualAlpha(float progress) {
        float s = Mth.sin((float) Math.PI * progress);
        return 0.6f + 0.4f * s;
    }

    private static void draw(Level level, PoseStack.Pose pose, VertexConsumer consumer, Vec3 start, Vec3 end, Vec3 camPos, float width1, float width2, float alpha1, float alpha2, float u1, float u2, float r1, float g1, float b1, float r2, float g2, float b2) {
        Vec3 dir = end.subtract(start);
        if (dir.lengthSqr() < 0.000001) return;
        Vec3 viewDir = start.subtract(camPos);
        Vec3 right = dir.cross(viewDir).normalize();
        Vec3 offset1 = right.scale(width1 * 0.5);
        Vec3 offset2 = right.scale(width2 * 0.5);
        Vec3 v0 = start.subtract(offset1);
        Vec3 v1 = start.add(offset1);
        Vec3 v2 = end.subtract(offset2);
        Vec3 v3 = end.add(offset2);

        int lightStart = LightTexture.pack(15, 15);
        int lightEnd = LightTexture.pack(15, 15);

        vertex(pose, consumer, v0, r1, g1, b1, alpha1, u1, 0, lightStart);
        vertex(pose, consumer, v2, r2, g2, b2, alpha2, u2, 0, lightEnd);
        vertex(pose, consumer, v3, r2, g2, b2, alpha2, u2, 1, lightEnd);
        vertex(pose, consumer, v1, r1, g1, b1, alpha1, u1, 1, lightStart);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, Vec3 pos, float r, float g, float b, float a, float u, float v, int packedLight) {
        consumer.addVertex(pose, (float) pos.x, (float) pos.y, (float) pos.z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, 0, 1, 0);
    }

    private static int packedLight(Level level, Vec3 pos) {
        BlockPos bp = BlockPos.containing(pos);
        int block = level.getBrightness(LightLayer.BLOCK, bp);
        int sky = level.getBrightness(LightLayer.SKY, bp);
        return LightTexture.pack(block, sky);
    }
}
