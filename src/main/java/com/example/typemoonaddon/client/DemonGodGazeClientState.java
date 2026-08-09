package com.example.typemoonaddon.client;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.network.DemonGodGazeVisualPayload;
import com.example.typemoonaddon.network.DemonGodGazeVisualPayload.TargetLayout;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Stable low-entity-count rendering for the eleven-circle attack sequence. */
@EventBusSubscriber(modid = TypeMoonAddon.MOD_ID, value = Dist.CLIENT)
public final class DemonGodGazeClientState {
    private static final int CIRCLE_COUNT = 11;
    private static final int DEPLOY_TICKS = 12;
    private static final int CUE_TICKS = 54;
    private static final int CAST_LIFETIME_TICKS = 86;
    private static final int FIRED_FADE_TICKS = 7;
    private static final int BEAM_LIFETIME_TICKS = 8;
    private static final int FINAL_BURST_TICKS = 28;
    private static final int MAX_ACTIVE_CASTS = 24;
    private static final double MAX_RENDER_DISTANCE_SQR = 192.0D * 192.0D;
    private static final Vec3 X_AXIS = new Vec3(1.0D, 0.0D, 0.0D);
    private static final Vec3 Y_AXIS = new Vec3(0.0D, 1.0D, 0.0D);
    private static final Vec3 Z_AXIS = new Vec3(0.0D, 0.0D, 1.0D);
    private static final ResourceLocation BEACON_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");
    private static final RenderType GLOW_RENDER_TYPE =
            RenderType.entityTranslucentEmissive(BEACON_TEXTURE);
    private static final Map<UUID, CastVisual> CASTS = new LinkedHashMap<>();

    private static ClientLevel activeLevel;
    private static long clientTick;

    private DemonGodGazeClientState() {
    }

    public static void apply(DemonGodGazeVisualPayload message) {
        if (message == null || message.castId() == null) {
            return;
        }
        if (message.action() == DemonGodGazeVisualPayload.START) {
            if (CASTS.size() >= MAX_ACTIVE_CASTS) {
                UUID oldest = CASTS.keySet().iterator().next();
                CASTS.remove(oldest);
            }
            List<TargetVisual> targets = new ArrayList<>(message.targets().size());
            for (TargetLayout layout : message.targets()) {
                if (layout.offsets().size() == CIRCLE_COUNT
                        && finite(layout.anchor())
                        && layout.offsets().stream().allMatch(DemonGodGazeClientState::finite)) {
                    targets.add(new TargetVisual(layout));
                }
            }
            if (!targets.isEmpty()) {
                CASTS.put(message.castId(), new CastVisual(message.castId(), clientTick, targets));
            }
            return;
        }
        if (message.action() == DemonGodGazeVisualPayload.CLEAR) {
            CASTS.remove(message.castId());
            return;
        }
        CastVisual cast = CASTS.get(message.castId());
        if (cast == null
                || message.action() != DemonGodGazeVisualPayload.FIRE
                || message.circleIndex() < 0
                || message.circleIndex() >= CIRCLE_COUNT
                || !finite(message.shotStart())
                || !finite(message.shotEnd())) {
            return;
        }
        cast.fire(
                message.targetEntityId(),
                message.circleIndex(),
                message.shotStart(),
                message.shotEnd(),
                clientTick
        );
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != activeLevel) {
            CASTS.clear();
            activeLevel = minecraft.level;
            clientTick = 0L;
        }
        if (activeLevel == null) {
            return;
        }
        clientTick++;
        CASTS.values().removeIf(cast -> cast.expired(clientTick));
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || CASTS.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        float partialTick = event.getPartialTick().getGameTimeDeltaTicks();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        try (AddonRenderBuffers renderBuffers = AddonRenderBuffers.fixed(RenderType.lines(), GLOW_RENDER_TYPE)) {
            BufferSource buffers = renderBuffers.source();
            VertexConsumer lines = buffers.getBuffer(RenderType.lines());
            VertexConsumer glow = buffers.getBuffer(GLOW_RENDER_TYPE);

            poseStack.pushPose();
            try {
                poseStack.translate(-camera.x, -camera.y, -camera.z);
                for (CastVisual cast : CASTS.values()) {
                    renderCast(minecraft.level, cast, poseStack, lines, glow, camera, partialTick);
                }
                buffers.endBatch(RenderType.lines());
                buffers.endBatch(GLOW_RENDER_TYPE);
            } finally {
                poseStack.popPose();
            }
        }
    }

    private static void renderCast(
            ClientLevel level,
            CastVisual cast,
            PoseStack poseStack,
            VertexConsumer lines,
            VertexConsumer glow,
            Vec3 camera,
            float partialTick
    ) {
        float castAge = clientTick - cast.startedAt + partialTick;
        float deploy = smoothStep(Mth.clamp(castAge / DEPLOY_TICKS, 0.0F, 1.0F));
        float endFade = castAge <= CUE_TICKS
                ? 1.0F
                : 1.0F - Mth.clamp((castAge - CUE_TICKS) / 12.0F, 0.0F, 1.0F);
        float time = clientTick + partialTick;

        for (int targetIndex = 0; targetIndex < cast.targets.size(); targetIndex++) {
            TargetVisual target = cast.targets.get(targetIndex);
            Vec3 center = target.center(level, partialTick);
            if (center.distanceToSqr(camera) > MAX_RENDER_DISTANCE_SQR) {
                continue;
            }
            renderTargetLock(poseStack, lines, center, deploy, endFade, time, targetIndex);

            for (int circleIndex = 0; circleIndex < CIRCLE_COUNT; circleIndex++) {
                long firedAt = target.firedAt[circleIndex];
                float firedAge = firedAt < 0L ? -1.0F : clientTick - firedAt + partialTick;
                if (firedAge >= FIRED_FADE_TICKS) {
                    continue;
                }
                float fireFade = firedAge < 0.0F
                        ? 1.0F
                        : 1.0F - Mth.clamp(firedAge / FIRED_FADE_TICKS, 0.0F, 1.0F);
                float collapse = firedAge < 0.0F
                        ? 1.0F
                        : 1.0F - 0.45F * smoothStep(Mth.clamp(firedAge / FIRED_FADE_TICKS, 0.0F, 1.0F));
                Vec3 circleCenter = center.add(target.offsets.get(circleIndex));
                double distanceSqr = circleCenter.distanceToSqr(camera);
                if (distanceSqr > MAX_RENDER_DISTANCE_SQR) {
                    continue;
                }
                Vec3 normal = center.subtract(circleCenter);
                if (normal.lengthSqr() < 1.0E-8D) {
                    continue;
                }
                float alpha = deploy * endFade * fireFade;
                float pulse = 0.86F + 0.14F * Mth.sin(time * 0.42F + circleIndex * 0.91F);
                renderSeal(
                        poseStack,
                        lines,
                        glow,
                        circleCenter,
                        normal.normalize(),
                        deploy * collapse,
                        alpha * pulse,
                        time,
                        circleIndex,
                        distanceSqr
                );

                BeamVisual beam = target.beams[circleIndex];
                if (beam != null) {
                    float beamAge = clientTick - beam.startedAt + partialTick;
                    if (beamAge < BEAM_LIFETIME_TICKS) {
                        renderBeam(poseStack, lines, glow, beam, beamAge);
                    }
                }
            }

            if (target.finalStartedAt >= 0L) {
                float finalAge = clientTick - target.finalStartedAt + partialTick;
                if (finalAge < FINAL_BURST_TICKS) {
                    renderFinalBurst(
                            poseStack,
                            lines,
                            glow,
                            target.finalCenter,
                            finalAge,
                            target.entityId
                    );
                }
            }
        }
    }

    private static void renderTargetLock(
            PoseStack poseStack,
            VertexConsumer lines,
            Vec3 center,
            float deploy,
            float fade,
            float time,
            int targetIndex
    ) {
        float alpha = deploy * fade * (0.34F + 0.1F * Mth.sin(time * 0.28F + targetIndex));
        double radius = 2.0D + Mth.sin(time * 0.12F + targetIndex) * 0.12D;
        drawRing(poseStack, lines, center, X_AXIS, Z_AXIS,
                radius, 36, 1.0F, 0.12F, 0.62F, alpha, time * 0.018F);
        drawRing(poseStack, lines, center, X_AXIS, Y_AXIS,
                radius * 0.88D, 36, 0.72F, 0.08F, 1.0F, alpha * 0.72F, -time * 0.014F);
    }

    private static void renderSeal(
            PoseStack poseStack,
            VertexConsumer lines,
            VertexConsumer glow,
            Vec3 center,
            Vec3 normal,
            float scale,
            float alpha,
            float time,
            int circleIndex,
            double distanceSqr
    ) {
        Vec3 reference = Math.abs(normal.y) > 0.92D ? X_AXIS : Y_AXIS;
        Vec3 right = reference.cross(normal).normalize();
        Vec3 up = normal.cross(right).normalize();
        double rotation = time * (0.018D + circleIndex * 0.0007D) + circleIndex * 0.57D;
        Vec3 rotatedRight = right.scale(Math.cos(rotation)).add(up.scale(Math.sin(rotation)));
        Vec3 rotatedUp = up.scale(Math.cos(rotation)).subtract(right.scale(Math.sin(rotation)));
        double radius = 1.36D * scale;
        int segments = distanceSqr < 96.0D * 96.0D ? 48 : 24;

        if (distanceSqr < 128.0D * 128.0D) {
            drawGlowPlane(poseStack, glow, center, rotatedRight, rotatedUp,
                    radius * 1.12D, 1.0F, 0.04F, 0.48F, alpha * 0.075F);
            drawGlowPlane(poseStack, glow, center.add(normal.scale(0.012D)), rotatedRight, rotatedUp,
                    radius * 0.24D, 1.0F, 0.82F, 1.0F, alpha * 0.32F);
        }

        drawThickRing(poseStack, lines, center, rotatedRight, rotatedUp,
                radius, segments, 1.0F, 0.08F, 0.62F, alpha);
        drawRing(poseStack, lines, center, rotatedRight, rotatedUp,
                radius * 0.74D, segments, 1.0F, 0.24F, 0.78F, alpha * 0.92F, -rotation * 1.7D);
        drawRing(poseStack, lines, center, rotatedRight, rotatedUp,
                radius * 0.39D, segments, 0.92F, 0.7F, 1.0F, alpha, rotation * 2.1D);

        if (distanceSqr >= 144.0D * 144.0D) {
            return;
        }
        drawStarPolygon(poseStack, lines, center, rotatedRight, rotatedUp,
                radius * 0.62D, 11, 4, 1.0F, 0.18F, 0.72F, alpha * 0.86F);
        for (int rune = 0; rune < 11; rune++) {
            double angle = rune * Math.PI * 2.0D / 11.0D + rotation * 0.46D;
            Vec3 inner = planePoint(center, rotatedRight, rotatedUp, radius * 0.81D, angle);
            Vec3 outer = planePoint(center, rotatedRight, rotatedUp,
                    radius * (rune % 2 == 0 ? 0.96D : 0.91D), angle);
            addLine(poseStack, lines, inner, outer,
                    1.0F, rune % 2 == 0 ? 0.76F : 0.22F, 1.0F, alpha * 0.84F);
            double tangent = 0.055D * radius;
            Vec3 tangentVector = rotatedRight.scale(-Math.sin(angle) * tangent)
                    .add(rotatedUp.scale(Math.cos(angle) * tangent));
            addLine(poseStack, lines, outer.subtract(tangentVector), outer.add(tangentVector),
                    0.82F, 0.12F, 1.0F, alpha * 0.72F);
        }
    }

    private static void renderBeam(
            PoseStack poseStack,
            VertexConsumer lines,
            VertexConsumer glow,
            BeamVisual beam,
            float age
    ) {
        Vec3 delta = beam.end.subtract(beam.start);
        if (delta.lengthSqr() < 1.0E-8D) {
            return;
        }
        float head = smoothStep(Mth.clamp(age / 2.6F, 0.0F, 1.0F));
        float fade = 1.0F - smoothStep(Mth.clamp((age - 3.0F) / 5.0F, 0.0F, 1.0F));
        Vec3 visibleEnd = beam.start.add(delta.scale(head));
        Vec3 direction = delta.normalize();
        Vec3 reference = Math.abs(direction.y) > 0.9D ? X_AXIS : Y_AXIS;
        Vec3 right = direction.cross(reference).normalize();
        Vec3 up = right.cross(direction).normalize();

        drawBeamRibbon(poseStack, glow, beam.start, visibleEnd, right,
                0.25D * fade, 0.72F, 0.02F, 1.0F, 0.22F * fade);
        drawBeamRibbon(poseStack, glow, beam.start, visibleEnd, up,
                0.25D * fade, 1.0F, 0.04F, 0.62F, 0.22F * fade);
        drawBeamRibbon(poseStack, glow, beam.start, visibleEnd, right,
                0.095D * fade, 1.0F, 0.88F, 1.0F, 0.82F * fade);
        drawBeamRibbon(poseStack, glow, beam.start, visibleEnd, up,
                0.095D * fade, 1.0F, 0.88F, 1.0F, 0.82F * fade);
        addLine(poseStack, lines, beam.start, visibleEnd,
                1.0F, 0.94F, 1.0F, fade);
    }

    private static void renderFinalBurst(
            PoseStack poseStack,
            VertexConsumer lines,
            VertexConsumer glow,
            Vec3 center,
            float age,
            int seed
    ) {
        float expansion = smoothStep(Mth.clamp(age / 12.0F, 0.0F, 1.0F));
        float fade = 1.0F - smoothStep(Mth.clamp((age - 9.0F) / 19.0F, 0.0F, 1.0F));
        double radius = Mth.lerp(expansion, 0.45D, 18.0D);
        float alpha = fade * 0.92F;

        double coreRadius = Mth.lerp(smoothStep(Mth.clamp(age / 5.0F, 0.0F, 1.0F)), 0.35D, 4.6D)
                * Math.max(0.12D, fade);
        drawGlowPlane(poseStack, glow, center, X_AXIS, Y_AXIS,
                coreRadius, 1.0F, 0.72F, 1.0F, 0.44F * fade);
        drawGlowPlane(poseStack, glow, center, X_AXIS, Z_AXIS,
                coreRadius, 1.0F, 0.08F, 0.62F, 0.32F * fade);
        drawGlowPlane(poseStack, glow, center, Z_AXIS, Y_AXIS,
                coreRadius, 0.72F, 0.08F, 1.0F, 0.32F * fade);

        drawThickRing(poseStack, lines, center, X_AXIS, Z_AXIS,
                radius, 72, 1.0F, 0.08F, 0.62F, alpha);
        drawRing(poseStack, lines, center, X_AXIS, Y_AXIS,
                radius * 0.92D, 64, 0.76F, 0.08F, 1.0F, alpha * 0.72F, age * 0.035D);
        drawRing(poseStack, lines, center, Z_AXIS, Y_AXIS,
                radius * 0.84D, 64, 1.0F, 0.58F, 0.94F, alpha * 0.62F, -age * 0.028D);

        int rays = 24;
        double seedAngle = Math.floorMod(seed * 31, 360) * Math.PI / 180.0D;
        for (int index = 0; index < rays; index++) {
            double y = -1.0D + 2.0D * (index + 0.5D) / rays;
            double horizontal = Math.sqrt(Math.max(0.0D, 1.0D - y * y));
            double angle = seedAngle + index * 2.399963229728653D;
            Vec3 direction = new Vec3(Math.cos(angle) * horizontal, y, Math.sin(angle) * horizontal);
            double rayLength = radius * (0.78D + (index % 5) * 0.055D);
            addLine(poseStack, lines,
                    center.add(direction.scale(coreRadius * 0.3D)),
                    center.add(direction.scale(rayLength)),
                    index % 3 == 0 ? 1.0F : 0.82F,
                    index % 3 == 0 ? 0.82F : 0.08F,
                    1.0F,
                    alpha * (0.42F + (index % 4) * 0.09F));
        }
    }

    private static void drawThickRing(
            PoseStack poseStack,
            VertexConsumer lines,
            Vec3 center,
            Vec3 right,
            Vec3 up,
            double radius,
            int segments,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        drawRing(poseStack, lines, center, right, up, radius, segments, red, green, blue, alpha, 0.0D);
        drawRing(poseStack, lines, center, right, up, radius * 0.985D, segments,
                red, green, blue, alpha * 0.62F, 0.0D);
        drawRing(poseStack, lines, center, right, up, radius * 1.015D, segments,
                red, green, blue, alpha * 0.62F, 0.0D);
    }

    private static void drawRing(
            PoseStack poseStack,
            VertexConsumer lines,
            Vec3 center,
            Vec3 right,
            Vec3 up,
            double radius,
            int segments,
            float red,
            float green,
            float blue,
            float alpha,
            double rotation
    ) {
        Vec3 previous = planePoint(center, right, up, radius, rotation);
        for (int index = 1; index <= segments; index++) {
            double angle = rotation + index * Math.PI * 2.0D / segments;
            Vec3 current = planePoint(center, right, up, radius, angle);
            addLine(poseStack, lines, previous, current, red, green, blue, alpha);
            previous = current;
        }
    }

    private static void drawStarPolygon(
            PoseStack poseStack,
            VertexConsumer lines,
            Vec3 center,
            Vec3 right,
            Vec3 up,
            double radius,
            int points,
            int step,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        for (int index = 0; index < points; index++) {
            double startAngle = index * Math.PI * 2.0D / points;
            double endAngle = ((index + step) % points) * Math.PI * 2.0D / points;
            addLine(poseStack, lines,
                    planePoint(center, right, up, radius, startAngle),
                    planePoint(center, right, up, radius, endAngle),
                    red, green, blue, alpha);
        }
    }

    private static Vec3 planePoint(
            Vec3 center,
            Vec3 right,
            Vec3 up,
            double radius,
            double angle
    ) {
        return center.add(right.scale(Math.cos(angle) * radius))
                .add(up.scale(Math.sin(angle) * radius));
    }

    private static void drawGlowPlane(
            PoseStack poseStack,
            VertexConsumer glow,
            Vec3 center,
            Vec3 right,
            Vec3 up,
            double radius,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        Vec3 rightOffset = right.scale(radius);
        Vec3 upOffset = up.scale(radius);
        quadDoubleSided(
                poseStack,
                glow,
                center.subtract(rightOffset).subtract(upOffset),
                center.add(rightOffset).subtract(upOffset),
                center.add(rightOffset).add(upOffset),
                center.subtract(rightOffset).add(upOffset),
                red, green, blue, alpha
        );
    }

    private static void drawBeamRibbon(
            PoseStack poseStack,
            VertexConsumer glow,
            Vec3 start,
            Vec3 end,
            Vec3 side,
            double halfWidth,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        Vec3 offset = side.scale(halfWidth);
        quadDoubleSided(
                poseStack,
                glow,
                start.subtract(offset),
                start.add(offset),
                end.add(offset),
                end.subtract(offset),
                red, green, blue, alpha
        );
    }

    private static void quadDoubleSided(
            PoseStack poseStack,
            VertexConsumer consumer,
            Vec3 a,
            Vec3 b,
            Vec3 c,
            Vec3 d,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        quad(poseStack, consumer, a, b, c, d, red, green, blue, alpha);
        quad(poseStack, consumer, d, c, b, a, red, green, blue, alpha);
    }

    private static void quad(
            PoseStack poseStack,
            VertexConsumer consumer,
            Vec3 a,
            Vec3 b,
            Vec3 c,
            Vec3 d,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        glowVertex(poseStack, consumer, a, red, green, blue, alpha, 0.0F, 0.0F);
        glowVertex(poseStack, consumer, b, red, green, blue, alpha, 1.0F, 0.0F);
        glowVertex(poseStack, consumer, c, red, green, blue, alpha, 1.0F, 1.0F);
        glowVertex(poseStack, consumer, d, red, green, blue, alpha, 0.0F, 1.0F);
    }

    private static void glowVertex(
            PoseStack poseStack,
            VertexConsumer consumer,
            Vec3 point,
            float red,
            float green,
            float blue,
            float alpha,
            float u,
            float v
    ) {
        consumer.addVertex(poseStack.last(), (float) point.x, (float) point.y, (float) point.z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0)
                .setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F);
    }

    private static void addLine(
            PoseStack poseStack,
            VertexConsumer lines,
            Vec3 start,
            Vec3 end,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        lines.addVertex(poseStack.last(), (float) start.x, (float) start.y, (float) start.z)
                .setColor(red, green, blue, alpha)
                .setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F);
        lines.addVertex(poseStack.last(), (float) end.x, (float) end.y, (float) end.z)
                .setColor(red, green, blue, alpha)
                .setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F);
    }

    private static float smoothStep(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private static boolean finite(Vec3 value) {
        return value != null
                && Double.isFinite(value.x)
                && Double.isFinite(value.y)
                && Double.isFinite(value.z);
    }

    private static final class CastVisual {
        private final UUID castId;
        private final long startedAt;
        private final List<TargetVisual> targets;

        private CastVisual(UUID castId, long startedAt, List<TargetVisual> targets) {
            this.castId = castId;
            this.startedAt = startedAt;
            this.targets = List.copyOf(targets);
        }

        private void fire(int targetEntityId, int circleIndex, Vec3 start, Vec3 end, long tick) {
            for (TargetVisual target : targets) {
                if (target.entityId == targetEntityId) {
                    target.fire(circleIndex, start, end, tick);
                    return;
                }
            }
        }

        private boolean expired(long tick) {
            return tick - startedAt > CAST_LIFETIME_TICKS;
        }
    }

    private static final class TargetVisual {
        private final int entityId;
        private final Vec3 anchor;
        private final List<Vec3> offsets;
        private final long[] firedAt = new long[CIRCLE_COUNT];
        private final BeamVisual[] beams = new BeamVisual[CIRCLE_COUNT];
        private long finalStartedAt = -1L;
        private Vec3 finalCenter = Vec3.ZERO;

        private TargetVisual(TargetLayout layout) {
            entityId = layout.entityId();
            anchor = layout.anchor();
            offsets = List.copyOf(layout.offsets());
            Arrays.fill(firedAt, -1L);
        }

        private Vec3 center(ClientLevel level, float partialTick) {
            Entity entity = level.getEntity(entityId);
            if (entity == null || entity.isRemoved()) {
                return anchor;
            }
            return entity.getPosition(partialTick).add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        }

        private void fire(int circleIndex, Vec3 start, Vec3 end, long tick) {
            if (firedAt[circleIndex] >= 0L) {
                return;
            }
            firedAt[circleIndex] = tick;
            beams[circleIndex] = new BeamVisual(start, end, tick);
            if (circleIndex == CIRCLE_COUNT - 1) {
                finalStartedAt = tick;
                finalCenter = end;
            }
        }
    }

    private record BeamVisual(Vec3 start, Vec3 end, long startedAt) {
    }
}
