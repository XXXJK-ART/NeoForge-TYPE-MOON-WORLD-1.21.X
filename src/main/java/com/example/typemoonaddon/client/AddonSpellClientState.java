package com.example.typemoonaddon.client;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.network.AddonSpellVisualPayload;
import com.example.typemoonaddon.network.AntoresBeamVisualPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.nio.charset.StandardCharsets;
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

/** Shared deterministic geometry for the addon's large spell silhouettes. */
@EventBusSubscriber(modid = TypeMoonAddon.MOD_ID, value = Dist.CLIENT)
public final class AddonSpellClientState {
    private static final int MAX_VISUALS = 512;
    private static final double MAX_RENDER_DISTANCE_SQR = 224.0D * 224.0D;
    private static final double DETAIL_DISTANCE_SQR = 112.0D * 112.0D;
    private static final Vec3 X_AXIS = new Vec3(1.0D, 0.0D, 0.0D);
    private static final Vec3 Y_AXIS = new Vec3(0.0D, 1.0D, 0.0D);
    private static final Vec3 Z_AXIS = new Vec3(0.0D, 0.0D, 1.0D);
    private static final ResourceLocation BEACON_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");
    private static final RenderType GLOW_RENDER_TYPE =
            RenderType.entityTranslucentEmissive(BEACON_TEXTURE);
    private static final Map<VisualKey, SpellVisual> VISUALS = new LinkedHashMap<>();

    private static ClientLevel activeLevel;
    private static long clientTick;
    private static int localSequence;

    private AddonSpellClientState() {
    }

    public static void apply(AddonSpellVisualPayload message) {
        if (message == null || message.ownerId() == null) {
            return;
        }
        if (message.action() == AddonSpellVisualPayload.CLEAR_SPELL) {
            VISUALS.entrySet().removeIf(entry ->
                    entry.getKey().spell == message.spell()
                            && entry.getKey().ownerId.equals(message.ownerId()));
            return;
        }
        if (!finite(message.origin()) || !finite(message.target())) {
            return;
        }
        VisualKey key = new VisualKey(
                message.spell(), message.stage(), message.ownerId(),
                message.entityId(), message.variant());
        SpellVisual previous = VISUALS.get(key);
        long startedAt = previous == null ? clientTick : previous.startedAt;
        put(key, new SpellVisual(
                message, startedAt, clientTick + message.durationTicks()));
    }

    /** Converts Antores' existing authoritative beam packet into persistent beam geometry. */
    public static void applyAntores(List<AntoresBeamVisualPayload.BeamPath> beams) {
        if (beams == null || beams.isEmpty()) {
            return;
        }
        UUID owner = UUID.nameUUIDFromBytes(("antores:" + clientTick + ':' + localSequence++)
                .getBytes(StandardCharsets.UTF_8));
        int variant = 0;
        for (AntoresBeamVisualPayload.BeamPath beam : beams) {
            Vec3 start = beam.start();
            Vec3 end = beam.end();
            if (!finite(start) || !finite(end) || start.distanceToSqr(end) < 1.0E-6D) {
                variant++;
                continue;
            }
            AddonSpellVisualPayload cue = new AddonSpellVisualPayload(
                    AddonSpellVisualPayload.SHOW,
                    AddonSpellVisualPayload.ANTORES,
                    AddonSpellVisualPayload.BEAM,
                    owner,
                    -1,
                    start,
                    end,
                    0.72F,
                    2.25F,
                    14,
                    variant++
            );
            VisualKey key = new VisualKey(
                    cue.spell(), cue.stage(), cue.ownerId(), cue.entityId(), cue.variant());
            put(key, new SpellVisual(cue, clientTick, clientTick + cue.durationTicks()));
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != activeLevel) {
            VISUALS.clear();
            activeLevel = minecraft.level;
            clientTick = 0L;
        }
        if (activeLevel == null) {
            return;
        }
        clientTick++;
        VISUALS.values().removeIf(visual -> clientTick > visual.expiresAt);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || VISUALS.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        float partialTick = event.getPartialTick().getGameTimeDeltaTicks();
        float time = clientTick + partialTick;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        VertexConsumer glow = buffers.getBuffer(GLOW_RENDER_TYPE);

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        for (SpellVisual visual : VISUALS.values()) {
            renderVisual(minecraft.level, visual, poseStack, lines, glow, camera, partialTick, time);
        }
        buffers.endBatch(RenderType.lines());
        buffers.endBatch(GLOW_RENDER_TYPE);
        poseStack.popPose();
    }

    private static void renderVisual(
            ClientLevel level,
            SpellVisual visual,
            PoseStack poseStack,
            VertexConsumer lines,
            VertexConsumer glow,
            Vec3 camera,
            float partialTick,
            float time
    ) {
        AddonSpellVisualPayload cue = visual.cue;
        Vec3 center = visualCenter(level, cue, partialTick);
        double distanceSqr = center.distanceToSqr(camera);
        if (distanceSqr > MAX_RENDER_DISTANCE_SQR) {
            return;
        }
        float age = clientTick - visual.startedAt + partialTick;
        float remaining = visual.expiresAt - clientTick - partialTick;
        float intro = smoothStep(Mth.clamp(age / 5.0F, 0.0F, 1.0F));
        float fade = smoothStep(Mth.clamp(remaining / 7.0F, 0.0F, 1.0F));
        float alpha = intro * fade;
        boolean detailed = distanceSqr <= DETAIL_DISTANCE_SQR;

        switch (cue.stage()) {
            case AddonSpellVisualPayload.SIGIL ->
                    renderSigil(level, cue, poseStack, lines, glow, alpha, time, detailed, partialTick);
            case AddonSpellVisualPayload.TORNADO ->
                    renderTornado(cue, poseStack, lines, glow, center, alpha, time, detailed);
            case AddonSpellVisualPayload.ERUPTION ->
                    renderEruption(cue, poseStack, lines, glow, center, alpha, time, detailed);
            case AddonSpellVisualPayload.ICE_SPHERE ->
                    renderSphere(poseStack, lines, glow, center, cue.scale(), alpha, time,
                            new Color(0.45F, 0.88F, 1.0F), detailed, true);
            case AddonSpellVisualPayload.LIGHTNING_STORM ->
                    renderLightningStorm(cue, poseStack, lines, glow, center, alpha, time, detailed);
            case AddonSpellVisualPayload.LIGHTNING_STRIKE ->
                    renderLightning(cue, poseStack, lines, glow, alpha, time, detailed);
            case AddonSpellVisualPayload.BEAM ->
                    renderBeam(cue, poseStack, lines, glow, alpha, age, detailed);
            case AddonSpellVisualPayload.WATER_FIELD ->
                    renderWaterField(cue, poseStack, lines, glow, center, alpha, time, detailed);
            case AddonSpellVisualPayload.WATER_PRISON ->
                    renderSphere(poseStack, lines, glow, center, cue.scale(), alpha, time,
                            new Color(0.18F, 0.78F, 1.0F), detailed, false);
            case AddonSpellVisualPayload.GROUND_IMPACT ->
                    renderGroundImpact(cue, poseStack, lines, glow, center, alpha, age, detailed);
            case AddonSpellVisualPayload.GROUND_RIFT ->
                    renderGroundRift(cue, poseStack, lines, glow, alpha, age, detailed);
            case AddonSpellVisualPayload.AFTERMATH ->
                    renderAftermath(cue, poseStack, lines, glow, center, alpha, age, detailed);
            case AddonSpellVisualPayload.NEGA_LOCK ->
                    renderNegaLock(cue, poseStack, lines, glow, center, alpha, age, detailed);
            case AddonSpellVisualPayload.NEGA_COLLAPSE ->
                    renderNegaCollapse(cue, poseStack, lines, glow, center, alpha, age, detailed);
            default -> {
            }
        }
    }

    private static void renderSigil(
            ClientLevel level,
            AddonSpellVisualPayload cue,
            PoseStack poseStack,
            VertexConsumer lines,
            VertexConsumer glow,
            float alpha,
            float time,
            boolean detailed,
            float partialTick
    ) {
        Entity entity = level.getEntity(cue.entityId());
        if (entity == null || entity.isRemoved()) {
            return;
        }
        Vec3 forward = entity.getLookAngle();
        forward = new Vec3(forward.x, 0.0D, forward.z);
        if (forward.lengthSqr() < 1.0E-6D) {
            double yaw = entity.getYRot() * Mth.DEG_TO_RAD;
            forward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        } else {
            forward = forward.normalize();
        }
        Vec3 body = entity.getPosition(partialTick).add(0.0D, entity.getBbHeight() * 0.58D, 0.0D);
        double offset = cue.spell() == AddonSpellVisualPayload.ANTORES ? 4.2D : 2.65D;
        Vec3 center = body.subtract(forward.scale(offset));
        Vec3 right = Y_AXIS.cross(forward).normalize();
        Vec3 up = Y_AXIS;
        Color color = spellColor(cue.spell());
        double radius = 2.15D + cue.scale() * 1.15D;
        int segments = detailed ? 64 : 32;
        double rotation = time * 0.012D + cue.spell() * 0.47D;
        float pulse = 0.84F + 0.16F * Mth.sin(time * 0.24F + cue.spell());
        float dimAlpha = alpha * pulse;

        drawGlowPlane(poseStack, glow, center, right, up, radius * 1.06D,
                color.red, color.green, color.blue, dimAlpha * 0.035F);
        drawRing(poseStack, lines, center, right, up, radius, segments,
                color, dimAlpha * 0.68F, rotation);
        drawRing(poseStack, lines, center, right, up, radius * 0.86D, segments,
                color, dimAlpha * 0.52F, -rotation * 1.35D);
        drawRing(poseStack, lines, center, right, up, radius * 0.64D, segments,
                color.brighten(0.22F), dimAlpha * 0.62F, rotation * 1.7D);
        drawRing(poseStack, lines, center, right, up, radius * 0.31D, segments,
                color.brighten(0.4F), dimAlpha * 0.72F, -rotation * 2.1D);
        drawStar(poseStack, lines, center, right, up, radius * 0.74D,
                cue.spell() % 2 == 0 ? 9 : 11, 4, color, dimAlpha * 0.58F, rotation * 0.34D);

        int glyphs = detailed ? 32 : 18;
        for (int index = 0; index < glyphs; index++) {
            double angle = rotation * 0.7D + index * Math.PI * 2.0D / glyphs;
            double innerRadius = radius * (index % 3 == 0 ? 0.69D : 0.76D);
            double outerRadius = radius * (index % 4 == 0 ? 0.97D : 0.91D);
            Vec3 inner = planePoint(center, right, up, innerRadius, angle);
            Vec3 outer = planePoint(center, right, up, outerRadius, angle + (index % 2) * 0.025D);
            addLine(poseStack, lines, inner, outer, color, dimAlpha * 0.48F);
            if (detailed && (index & 1) == 0) {
                double tangent = radius * 0.035D;
                Vec3 tangentVector = right.scale(-Math.sin(angle) * tangent)
                        .add(up.scale(Math.cos(angle) * tangent));
                addLine(poseStack, lines, outer.subtract(tangentVector), outer.add(tangentVector),
                        color.brighten(0.28F), dimAlpha * 0.43F);
            }
        }
    }

    private static void renderTornado(
            AddonSpellVisualPayload cue,
            PoseStack poseStack,
            VertexConsumer lines,
            VertexConsumer glow,
            Vec3 center,
            float alpha,
            float time,
            boolean detailed
    ) {
        Color color = new Color(0.54F, 0.9F, 1.0F);
        double radius = Math.max(1.0D, cue.scale());
        double height = Math.max(3.0D, cue.secondaryScale());
        int layers = detailed ? 12 : 7;
        int segments = detailed ? 40 : 22;
        for (int layer = 0; layer < layers; layer++) {
            double progress = layer / (double) (layers - 1);
            double y = progress * height;
            double layerRadius = radius * (0.42D + progress * 0.76D);
            double wobble = Math.sin(time * 0.17D + layer * 1.37D + cue.variant()) * radius * 0.14D;
            Vec3 ringCenter = center.add(
                    Math.cos(time * 0.075D + layer) * wobble,
                    y,
                    Math.sin(time * 0.075D + layer) * wobble);
            drawRing(poseStack, lines, ringCenter, X_AXIS, Z_AXIS, layerRadius, segments,
                    color, alpha * (0.32F + (float) progress * 0.38F),
                    time * 0.055D + layer * 0.48D);
        }
        int helices = detailed ? 5 : 3;
        for (int helix = 0; helix < helices; helix++) {
            Vec3 previous = null;
            int steps = detailed ? 72 : 38;
            for (int step = 0; step <= steps; step++) {
                double progress = step / (double) steps;
                double angle = progress * Math.PI * 7.0D + time * 0.11D
                        + helix * Math.PI * 2.0D / helices;
                double currentRadius = radius * (0.38D + progress * 0.82D);
                Vec3 point = center.add(
                        Math.cos(angle) * currentRadius,
                        progress * height,
                        Math.sin(angle) * currentRadius);
                if (previous != null) {
                    addLine(poseStack, lines, previous, point,
                            color.brighten(helix == 0 ? 0.38F : 0.08F),
                            alpha * (helix == 0 ? 0.78F : 0.42F));
                }
                previous = point;
            }
        }
        drawGlowPlane(poseStack, glow, center.add(0.0D, height * 0.42D, 0.0D),
                X_AXIS, Z_AXIS, radius * 0.82D,
                color.red, color.green, color.blue, alpha * 0.035F);
    }

    private static void renderEruption(
            AddonSpellVisualPayload cue,
            PoseStack poseStack,
            VertexConsumer lines,
            VertexConsumer glow,
            Vec3 center,
            float alpha,
            float time,
            boolean detailed
    ) {
        Color outer = new Color(0.92F, 0.16F, 0.035F);
        Color core = new Color(1.0F, 0.68F, 0.16F);
        double radius = Math.max(2.0D, cue.scale());
        double height = Math.max(5.0D, cue.secondaryScale());
        int segments = detailed ? 64 : 30;
        drawRing(poseStack, lines, center.add(0.0D, 0.08D, 0.0D), X_AXIS, Z_AXIS,
                radius, segments, outer, alpha * 0.82F, time * 0.018D);
        drawRing(poseStack, lines, center.add(0.0D, 0.1D, 0.0D), X_AXIS, Z_AXIS,
                radius * 0.62D, segments, core, alpha * 0.74F, -time * 0.027D);
        int columns = detailed ? 14 : 8;
        for (int index = 0; index < columns; index++) {
            double angle = index * Math.PI * 2.0D / columns + cue.variant() * 0.17D;
            double radial = radius * (0.18D + (index % 4) * 0.11D);
            Vec3 start = center.add(Math.cos(angle) * radial, 0.12D, Math.sin(angle) * radial);
            double lean = radius * (0.08D + (index % 3) * 0.035D);
            Vec3 end = center.add(
                    Math.cos(angle + 0.35D) * lean,
                    height * (0.72D + (index % 5) * 0.055D),
                    Math.sin(angle + 0.35D) * lean);
            addLine(poseStack, lines, start, end,
                    index % 3 == 0 ? core : outer,
                    alpha * (index % 3 == 0 ? 0.9F : 0.55F));
        }
        int rings = detailed ? 8 : 5;
        for (int index = 1; index <= rings; index++) {
            double progress = index / (double) rings;
            drawRing(poseStack, lines, center.add(0.0D, height * progress, 0.0D),
                    X_AXIS, Z_AXIS, radius * (0.5D - progress * 0.32D),
                    detailed ? 36 : 20, core, alpha * (float) (0.42D * (1.0D - progress * 0.55D)),
                    time * 0.04D + index);
        }
        drawGlowPlane(poseStack, glow, center.add(0.0D, height * 0.48D, 0.0D),
                X_AXIS, Z_AXIS, Math.min(radius, 10.0D),
                1.0F, 0.24F, 0.025F, alpha * 0.055F);
    }

    private static void renderSphere(
            PoseStack poseStack,
            VertexConsumer lines,
            VertexConsumer glow,
            Vec3 center,
            double radius,
            float alpha,
            float time,
            Color color,
            boolean detailed,
            boolean crystalline
    ) {
        int segments = detailed ? 56 : 28;
        drawRing(poseStack, lines, center, X_AXIS, Y_AXIS, radius, segments,
                color, alpha * 0.76F, time * 0.017D);
        drawRing(poseStack, lines, center, Y_AXIS, Z_AXIS, radius, segments,
                color, alpha * 0.68F, -time * 0.021D);
        drawRing(poseStack, lines, center, X_AXIS, Z_AXIS, radius, segments,
                color.brighten(0.28F), alpha * 0.82F, time * 0.013D);
        for (int latitude = -2; latitude <= 2; latitude++) {
            double y = radius * latitude / 3.0D;
            double ringRadius = Math.sqrt(Math.max(0.0D, radius * radius - y * y));
            drawRing(poseStack, lines, center.add(0.0D, y, 0.0D), X_AXIS, Z_AXIS,
                    ringRadius, detailed ? 40 : 20, color, alpha * 0.32F,
                    time * (latitude % 2 == 0 ? 0.012D : -0.012D));
        }
        if (crystalline) {
            int rays = detailed ? 18 : 10;
            for (int index = 0; index < rays; index++) {
                double yaw = index * Math.PI * 2.0D / rays + time * 0.006D;
                double pitch = -0.85D + (index % 6) * 0.34D;
                Vec3 direction = new Vec3(
                        Math.cos(yaw) * Math.cos(pitch), Math.sin(pitch),
                        Math.sin(yaw) * Math.cos(pitch));
                addLine(poseStack, lines,
                        center.add(direction.scale(radius * 0.78D)),
                        center.add(direction.scale(radius * 1.12D)),
                        color.brighten(0.42F), alpha * 0.72F);
            }
        }
        drawGlowPlane(poseStack, glow, center, X_AXIS, Y_AXIS, radius * 0.9D,
                color.red, color.green, color.blue, alpha * 0.045F);
    }

    private static void renderLightningStorm(
            AddonSpellVisualPayload cue,
            PoseStack poseStack,
            VertexConsumer lines,
            VertexConsumer glow,
            Vec3 center,
            float alpha,
            float time,
            boolean detailed
    ) {
        Color gold = new Color(1.0F, 0.72F, 0.12F);
        double radius = Mth.clamp(cue.scale(), 3.0F, 28.0F);
        double cloudHeight = Math.max(10.0D, cue.secondaryScale());
        Vec3 cloud = center.add(0.0D, cloudHeight, 0.0D);
        int rings = detailed ? 7 : 4;
        for (int index = 0; index < rings; index++) {
            double layerRadius = radius * (0.45D + index * 0.09D);
            Vec3 layerCenter = cloud.add(
                    Math.cos(index * 2.13D + time * 0.018D) * radius * 0.13D,
                    (index % 3 - 1) * 0.48D,
                    Math.sin(index * 2.13D + time * 0.018D) * radius * 0.13D);
            drawRing(poseStack, lines, layerCenter, X_AXIS, Z_AXIS, layerRadius,
                    detailed ? 48 : 24, gold, alpha * (0.34F + index * 0.045F),
                    time * 0.021D + index);
        }
        int arcs = detailed ? 14 : 7;
        for (int index = 0; index < arcs; index++) {
            double a = index * Math.PI * 2.0D / arcs + cue.variant() * 0.37D;
            double b = a + 0.65D + (index % 4) * 0.21D;
            Vec3 start = cloud.add(Math.cos(a) * radius * 0.72D,
                    (index % 3 - 1) * 0.55D, Math.sin(a) * radius * 0.72D);
            Vec3 end = cloud.add(Math.cos(b) * radius * 0.72D,
                    ((index + 1) % 3 - 1) * 0.55D, Math.sin(b) * radius * 0.72D);
            drawJaggedLine(poseStack, lines, start, end, 6,
                    radius * 0.018D, cue.variant() * 31 + index,
                    gold.brighten(index % 3 == 0 ? 0.46F : 0.12F),
                    alpha * (index % 3 == 0 ? 0.82F : 0.48F));
        }
        drawGlowPlane(poseStack, glow, cloud, X_AXIS, Z_AXIS, radius * 0.72D,
                1.0F, 0.62F, 0.08F, alpha * 0.04F);
    }

    private static void renderLightning(
            AddonSpellVisualPayload cue,
            PoseStack poseStack,
            VertexConsumer lines,
            VertexConsumer glow,
            float alpha,
            float time,
            boolean detailed
    ) {
        Vec3 bottom = cue.origin();
        Vec3 top = cue.target().distanceToSqr(bottom) > 1.0D
                ? cue.target() : bottom.add(0.0D, Math.max(18.0D, cue.secondaryScale()), 0.0D);
        Color gold = new Color(1.0F, 0.74F, 0.12F);
        Color white = new Color(1.0F, 0.97F, 0.68F);
        int seed = cue.variant() * 97 + cue.entityId() * 13;
        drawJaggedLine(poseStack, lines, top, bottom, detailed ? 18 : 10,
                Math.max(0.35D, cue.scale()), seed, white, alpha);
        Vec3 side = top.subtract(bottom).normalize().cross(X_AXIS);
        if (side.lengthSqr() < 1.0E-6D) {
            side = Z_AXIS;
        }
        side = side.normalize().scale(0.12D);
        drawJaggedLine(poseStack, lines, top.add(side), bottom.add(side), detailed ? 18 : 10,
                Math.max(0.28D, cue.scale() * 0.72D), seed, gold, alpha * 0.84F);
        if (detailed) {
            Vec3 delta = bottom.subtract(top);
            for (int branch = 0; branch < 5; branch++) {
                double progress = 0.2D + branch * 0.14D;
                Vec3 branchStart = top.add(delta.scale(progress));
                double angle = branch * 2.21D + seed * 0.17D;
                Vec3 branchEnd = branchStart.add(
                        Math.cos(angle) * (2.4D + branch * 0.35D),
                        -1.4D - branch * 0.25D,
                        Math.sin(angle) * (2.4D + branch * 0.35D));
                drawJaggedLine(poseStack, lines, branchStart, branchEnd, 6,
                        0.28D, seed + branch * 43, gold, alpha * 0.56F);
            }
        }
        drawGlowPlane(poseStack, glow, bottom.add(0.0D, 0.12D, 0.0D),
                X_AXIS, Z_AXIS, 2.1D + cue.scale(),
                1.0F, 0.72F, 0.12F, alpha * (0.09F + 0.03F * Mth.sin(time * 0.8F)));
    }

    private static void renderBeam(
            AddonSpellVisualPayload cue,
            PoseStack poseStack,
            VertexConsumer lines,
            VertexConsumer glow,
            float alpha,
            float age,
            boolean detailed
    ) {
        Vec3 delta = cue.target().subtract(cue.origin());
        if (delta.lengthSqr() < 1.0E-6D) {
            return;
        }
        float travel = smoothStep(Mth.clamp(age / 3.5F, 0.0F, 1.0F));
        Vec3 end = cue.origin().add(delta.scale(travel));
        Vec3 direction = delta.normalize();
        Vec3 reference = Math.abs(direction.y) > 0.9D ? X_AXIS : Y_AXIS;
        Vec3 right = direction.cross(reference).normalize();
        Vec3 up = right.cross(direction).normalize();
        double gateRadius = Math.max(1.2D, cue.secondaryScale());
        double beamRadius = Math.max(0.22D, cue.scale());
        Color red = new Color(1.0F, 0.035F, 0.09F);
        drawGlowPlane(poseStack, glow, cue.origin(), right, up, gateRadius,
                1.0F, 0.015F, 0.035F, alpha * 0.07F);
        drawRing(poseStack, lines, cue.origin(), right, up, gateRadius,
                detailed ? 64 : 32, red, alpha * 0.9F, age * 0.07D);
        drawRing(poseStack, lines, cue.origin(), right, up, gateRadius * 0.72D,
                detailed ? 48 : 24, red.brighten(0.36F), alpha * 0.75F, -age * 0.1D);
        drawStar(poseStack, lines, cue.origin(), right, up, gateRadius * 0.59D,
                9, 4, red, alpha * 0.68F, age * 0.045D);
        drawBeamRibbon(poseStack, glow, cue.origin(), end, right,
                beamRadius, 1.0F, 0.02F, 0.055F, alpha * 0.72F);
        drawBeamRibbon(poseStack, glow, cue.origin(), end, up,
                beamRadius * 0.74D, 1.0F, 0.48F, 0.52F, alpha * 0.62F);
        addLine(poseStack, lines, cue.origin(), end,
                new Color(1.0F, 0.84F, 0.86F), alpha);
    }

    private static void renderWaterField(
            AddonSpellVisualPayload cue,
            PoseStack poseStack,
            VertexConsumer lines,
            VertexConsumer glow,
            Vec3 center,
            float alpha,
            float time,
            boolean detailed
    ) {
        Color water = new Color(0.12F, 0.68F, 1.0F);
        double radius = Math.max(2.0D, cue.scale());
        int ringCount = detailed ? 9 : 5;
        for (int index = 1; index <= ringCount; index++) {
            double ringRadius = radius * index / ringCount;
            double y = Math.sin(time * 0.16D + index * 1.7D) * 0.22D;
            drawRing(poseStack, lines, center.add(0.0D, y, 0.0D), X_AXIS, Z_AXIS,
                    ringRadius, detailed ? 56 : 28,
                    index % 3 == 0 ? water.brighten(0.3F) : water,
                    alpha * (0.32F + index * 0.045F),
                    time * (index % 2 == 0 ? 0.024D : -0.024D));
        }
        int arms = detailed ? 7 : 4;
        for (int arm = 0; arm < arms; arm++) {
            Vec3 previous = center;
            int steps = detailed ? 30 : 16;
            for (int step = 1; step <= steps; step++) {
                double progress = step / (double) steps;
                double angle = arm * Math.PI * 2.0D / arms + progress * 4.8D + time * 0.055D;
                Vec3 point = center.add(
                        Math.cos(angle) * radius * progress,
                        Math.sin(progress * Math.PI * 4.0D + time * 0.12D) * 0.18D,
                        Math.sin(angle) * radius * progress);
                addLine(poseStack, lines, previous, point, water, alpha * 0.46F);
                previous = point;
            }
        }
        drawGlowPlane(poseStack, glow, center, X_AXIS, Z_AXIS, radius * 0.74D,
                0.06F, 0.52F, 1.0F, alpha * 0.035F);
    }

    private static void renderGroundImpact(
            AddonSpellVisualPayload cue,
            PoseStack poseStack,
            VertexConsumer lines,
            VertexConsumer glow,
            Vec3 center,
            float alpha,
            float age,
            boolean detailed
    ) {
        Color earth = new Color(1.0F, 0.46F, 0.08F);
        double radius = Math.max(2.0D, cue.scale());
        double expansion = smoothStep(Mth.clamp(age / 9.0F, 0.0F, 1.0F));
        for (int ring = 1; ring <= 3; ring++) {
            drawRing(poseStack, lines, center.add(0.0D, 0.08D + ring * 0.025D, 0.0D),
                    X_AXIS, Z_AXIS, radius * expansion * ring / 3.0D,
                    detailed ? 60 : 30, ring == 3 ? earth : earth.brighten(0.26F),
                    alpha * (0.82F - ring * 0.13F), ring * 0.31D);
        }
        int cracks = detailed ? 18 : 9;
        for (int index = 0; index < cracks; index++) {
            double angle = index * Math.PI * 2.0D / cracks + cue.variant() * 0.23D;
            Vec3 start = center.add(Math.cos(angle) * radius * 0.12D, 0.06D,
                    Math.sin(angle) * radius * 0.12D);
            Vec3 end = center.add(Math.cos(angle) * radius * expansion, 0.06D,
                    Math.sin(angle) * radius * expansion);
            drawJaggedLine(poseStack, lines, start, end, detailed ? 7 : 4,
                    radius * 0.035D, cue.variant() * 37 + index, earth, alpha * 0.68F);
        }
        drawGlowPlane(poseStack, glow, center.add(0.0D, 0.1D, 0.0D), X_AXIS, Z_AXIS,
                radius * expansion * 0.72D, 1.0F, 0.18F, 0.02F, alpha * 0.055F);
    }

    private static void renderGroundRift(
            AddonSpellVisualPayload cue,
            PoseStack poseStack,
            VertexConsumer lines,
            VertexConsumer glow,
            float alpha,
            float age,
            boolean detailed
    ) {
        Vec3 direction = cue.target().subtract(cue.origin());
        direction = new Vec3(direction.x, 0.0D, direction.z);
        if (direction.lengthSqr() < 1.0E-6D) {
            direction = Z_AXIS;
        } else {
            direction = direction.normalize();
        }
        Vec3 right = new Vec3(-direction.z, 0.0D, direction.x);
        Color rift = new Color(1.0F, 0.31F, 0.035F);
        double width = Math.max(1.0D, cue.scale());
        double length = Math.max(2.0D, cue.target().distanceTo(cue.origin()));
        float expansion = smoothStep(Mth.clamp(age / 4.0F, 0.0F, 1.0F));
        Vec3 start = cue.origin().subtract(direction.scale(length * 0.35D));
        Vec3 end = cue.origin().add(direction.scale(length * 0.65D * expansion));
        drawJaggedLine(poseStack, lines, start.add(0.0D, 0.07D, 0.0D),
                end.add(0.0D, 0.07D, 0.0D), detailed ? 12 : 6,
                width * 0.08D, cue.variant() * 71, rift.brighten(0.35F), alpha * 0.94F);
        int branches = detailed ? 8 : 4;
        for (int branch = 0; branch < branches; branch++) {
            double progress = (branch + 1.0D) / (branches + 1.0D);
            Vec3 root = start.lerp(end, progress).add(0.0D, 0.06D, 0.0D);
            double side = branch % 2 == 0 ? 1.0D : -1.0D;
            Vec3 tip = root.add(right.scale(side * width * (0.35D + (branch % 3) * 0.16D)))
                    .add(direction.scale((branch % 2) * width * 0.12D));
            drawJaggedLine(poseStack, lines, root, tip, detailed ? 5 : 3,
                    width * 0.035D, cue.variant() * 71 + branch + 1,
                    rift, alpha * 0.64F);
        }
        double visibleDepth = Math.min(18.0D, Math.max(2.0D, cue.secondaryScale()));
        for (int side = -1; side <= 1; side += 2) {
            Vec3 lip = cue.origin().add(right.scale(side * width * 0.45D));
            addLine(poseStack, lines, lip, lip.add(0.0D, -visibleDepth, 0.0D),
                    rift, alpha * 0.38F);
        }
        drawGlowPlane(poseStack, glow, cue.origin().add(0.0D, 0.08D, 0.0D),
                direction, right, width * 0.72D,
                1.0F, 0.16F, 0.015F, alpha * 0.05F);
    }

    private static void renderAftermath(
            AddonSpellVisualPayload cue,
            PoseStack poseStack,
            VertexConsumer lines,
            VertexConsumer glow,
            Vec3 center,
            float alpha,
            float age,
            boolean detailed
    ) {
        Color color = spellColor(cue.spell()).brighten(0.2F);
        double radius = Math.max(2.0D, cue.scale())
                * smoothStep(Mth.clamp(age / 12.0F, 0.0F, 1.0F));
        drawRing(poseStack, lines, center.add(0.0D, 0.12D, 0.0D), X_AXIS, Z_AXIS,
                radius, detailed ? 64 : 32, color, alpha * 0.8F, age * 0.025D);
        drawRing(poseStack, lines, center.add(0.0D, 0.16D, 0.0D), X_AXIS, Z_AXIS,
                radius * 0.58D, detailed ? 48 : 24, color, alpha * 0.48F, -age * 0.04D);
        drawGlowPlane(poseStack, glow, center.add(0.0D, 0.18D, 0.0D), X_AXIS, Z_AXIS,
                radius * 0.64D, color.red, color.green, color.blue, alpha * 0.045F);
    }

    private static void renderNegaLock(
            AddonSpellVisualPayload cue,
            PoseStack poseStack,
            VertexConsumer lines,
            VertexConsumer glow,
            Vec3 center,
            float alpha,
            float age,
            boolean detailed
    ) {
        Color violet = new Color(0.38F, 0.055F, 0.56F);
        Color gold = new Color(0.92F, 0.68F, 0.18F);
        double radius = Math.max(1.15D, cue.scale());
        double halfHeight = Math.max(radius, cue.secondaryScale() * 0.5D);
        double contraction = 1.48D - smoothStep(Mth.clamp(age / 11.0F, 0.0F, 1.0F)) * 0.52D;
        double rotation = age * 0.07D + cue.variant() * 0.19D;
        int segments = detailed ? 48 : 26;

        drawRing(poseStack, lines, center, X_AXIS, Z_AXIS,
                radius * contraction, segments, gold, alpha * 0.82F, rotation);
        drawRing(poseStack, lines, center, X_AXIS, Y_AXIS,
                radius * contraction, segments, violet.brighten(0.18F), alpha * 0.74F, -rotation);
        drawRing(poseStack, lines, center, Y_AXIS, Z_AXIS,
                radius * contraction, segments, violet, alpha * 0.66F, rotation * 1.3D);
        drawStar(poseStack, lines, center.add(0.0D, -halfHeight, 0.0D),
                X_AXIS, Z_AXIS, radius * 0.82D, 9, 4, gold, alpha * 0.68F, -rotation * 0.5D);

        int bars = detailed ? 16 : 8;
        for (int index = 0; index < bars; index++) {
            double angle = index * Math.PI * 2.0D / bars + rotation * 0.42D;
            Vec3 lower = center.add(
                    Math.cos(angle) * radius * contraction,
                    -halfHeight,
                    Math.sin(angle) * radius * contraction);
            Vec3 upper = center.add(
                    Math.cos(angle + 0.34D) * radius * contraction * 0.72D,
                    halfHeight,
                    Math.sin(angle + 0.34D) * radius * contraction * 0.72D);
            addLine(poseStack, lines, lower, upper,
                    index % 4 == 0 ? gold : violet,
                    alpha * (index % 4 == 0 ? 0.72F : 0.46F));
        }
        drawGlowPlane(poseStack, glow, center, X_AXIS, Y_AXIS, radius * contraction,
                0.31F, 0.015F, 0.42F, alpha * 0.045F);
    }

    private static void renderNegaCollapse(
            AddonSpellVisualPayload cue,
            PoseStack poseStack,
            VertexConsumer lines,
            VertexConsumer glow,
            Vec3 center,
            float alpha,
            float age,
            boolean detailed
    ) {
        Color violet = new Color(0.46F, 0.035F, 0.62F);
        Color gold = new Color(1.0F, 0.72F, 0.16F);
        double baseRadius = Math.max(1.25D, cue.scale());
        float collapse = smoothStep(Mth.clamp(age / 8.0F, 0.0F, 1.0F));
        float burst = smoothStep(Mth.clamp((age - 8.0F) / 13.0F, 0.0F, 1.0F));
        double radius = age < 8.0F
                ? Mth.lerp(collapse, baseRadius * 1.7D, baseRadius * 0.12D)
                : Mth.lerp(burst, baseRadius * 0.12D, baseRadius * 2.9D);
        int segments = detailed ? 52 : 28;
        drawRing(poseStack, lines, center, X_AXIS, Y_AXIS, radius, segments,
                gold, alpha * 0.9F, age * 0.08D);
        drawRing(poseStack, lines, center, Y_AXIS, Z_AXIS, radius * 0.92D, segments,
                violet.brighten(0.24F), alpha * 0.72F, -age * 0.105D);
        drawRing(poseStack, lines, center, X_AXIS, Z_AXIS, radius * 1.08D, segments,
                violet, alpha * 0.62F, age * 0.065D);

        double severHeight = Math.max(2.5D, cue.secondaryScale() * 1.35D);
        addLine(poseStack, lines,
                center.add(0.0D, -severHeight, 0.0D),
                center.add(0.0D, severHeight, 0.0D),
                gold.brighten(0.34F), alpha);
        if (age >= 7.0F) {
            int fragments = detailed ? 20 : 10;
            for (int index = 0; index < fragments; index++) {
                double yaw = index * Math.PI * 2.0D / fragments + cue.variant() * 0.13D;
                double pitch = -0.72D + (index % 6) * 0.29D;
                Vec3 direction = new Vec3(
                        Math.cos(yaw) * Math.cos(pitch),
                        Math.sin(pitch),
                        Math.sin(yaw) * Math.cos(pitch));
                Vec3 start = center.add(direction.scale(baseRadius * 0.18D));
                Vec3 end = center.add(direction.scale(baseRadius * (0.65D + burst * 2.4D)));
                addLine(poseStack, lines, start, end,
                        index % 4 == 0 ? gold : violet,
                        alpha * (index % 4 == 0 ? 0.72F : 0.42F));
            }
        }
        drawGlowPlane(poseStack, glow, center, X_AXIS, Y_AXIS, Math.max(0.35D, radius),
                0.42F, 0.015F, 0.52F, alpha * 0.07F);
    }

    private static Vec3 visualCenter(
            ClientLevel level,
            AddonSpellVisualPayload cue,
            float partialTick
    ) {
        if (cue.entityId() < 0) {
            return cue.origin();
        }
        Entity entity = level.getEntity(cue.entityId());
        if (entity == null || entity.isRemoved()) {
            return cue.origin();
        }
        if (cue.stage() == AddonSpellVisualPayload.TORNADO) {
            return entity.getPosition(partialTick).add(0.0D, 0.12D, 0.0D);
        }
        return entity.getPosition(partialTick).add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
    }

    private static void drawJaggedLine(
            PoseStack poseStack,
            VertexConsumer lines,
            Vec3 start,
            Vec3 end,
            int segments,
            double jitter,
            int seed,
            Color color,
            float alpha
    ) {
        Vec3 delta = end.subtract(start);
        Vec3 direction = delta.lengthSqr() < 1.0E-8D ? Y_AXIS : delta.normalize();
        Vec3 reference = Math.abs(direction.y) > 0.9D ? X_AXIS : Y_AXIS;
        Vec3 right = direction.cross(reference).normalize();
        Vec3 up = right.cross(direction).normalize();
        Vec3 previous = start;
        for (int index = 1; index <= segments; index++) {
            double progress = index / (double) segments;
            Vec3 point = start.add(delta.scale(progress));
            if (index < segments) {
                double envelope = Math.sin(progress * Math.PI);
                double x = signedNoise(seed, index * 2) * jitter * envelope;
                double y = signedNoise(seed, index * 2 + 1) * jitter * envelope;
                point = point.add(right.scale(x)).add(up.scale(y));
            }
            addLine(poseStack, lines, previous, point, color, alpha);
            previous = point;
        }
    }

    private static double signedNoise(int seed, int index) {
        long value = seed * 341873128712L + index * 132897987541L;
        value ^= value >>> 13;
        value *= 1274126177L;
        value ^= value >>> 16;
        return ((value & 0xFFFFL) / 32767.5D) - 1.0D;
    }

    private static void drawRing(
            PoseStack poseStack,
            VertexConsumer lines,
            Vec3 center,
            Vec3 right,
            Vec3 up,
            double radius,
            int segments,
            Color color,
            float alpha,
            double rotation
    ) {
        Vec3 previous = planePoint(center, right, up, radius, rotation);
        for (int index = 1; index <= segments; index++) {
            double angle = rotation + index * Math.PI * 2.0D / segments;
            Vec3 current = planePoint(center, right, up, radius, angle);
            addLine(poseStack, lines, previous, current, color, alpha);
            previous = current;
        }
    }

    private static void drawStar(
            PoseStack poseStack,
            VertexConsumer lines,
            Vec3 center,
            Vec3 right,
            Vec3 up,
            double radius,
            int points,
            int step,
            Color color,
            float alpha,
            double rotation
    ) {
        for (int index = 0; index < points; index++) {
            double startAngle = rotation + index * Math.PI * 2.0D / points;
            double endAngle = rotation + ((index + step) % points) * Math.PI * 2.0D / points;
            addLine(poseStack, lines,
                    planePoint(center, right, up, radius, startAngle),
                    planePoint(center, right, up, radius, endAngle),
                    color, alpha);
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
        Vec3 rightOffset = right.normalize().scale(radius);
        Vec3 upOffset = up.normalize().scale(radius);
        quadDoubleSided(poseStack, glow,
                center.subtract(rightOffset).subtract(upOffset),
                center.add(rightOffset).subtract(upOffset),
                center.add(rightOffset).add(upOffset),
                center.subtract(rightOffset).add(upOffset),
                red, green, blue, alpha);
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
        Vec3 offset = side.normalize().scale(halfWidth);
        quadDoubleSided(poseStack, glow,
                start.subtract(offset), start.add(offset),
                end.add(offset), end.subtract(offset),
                red, green, blue, alpha);
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
            Color color,
            float alpha
    ) {
        lines.addVertex(poseStack.last(), (float) start.x, (float) start.y, (float) start.z)
                .setColor(color.red, color.green, color.blue, alpha)
                .setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F);
        lines.addVertex(poseStack.last(), (float) end.x, (float) end.y, (float) end.z)
                .setColor(color.red, color.green, color.blue, alpha)
                .setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F);
    }

    private static void put(VisualKey key, SpellVisual visual) {
        if (!VISUALS.containsKey(key) && VISUALS.size() >= MAX_VISUALS) {
            VisualKey oldest = VISUALS.keySet().iterator().next();
            VISUALS.remove(oldest);
        }
        VISUALS.put(key, visual);
    }

    private static Color spellColor(byte spell) {
        return switch (spell) {
            case AddonSpellVisualPayload.STORM -> new Color(0.46F, 0.88F, 1.0F);
            case AddonSpellVisualPayload.ORIAS -> new Color(1.0F, 0.2F, 0.045F);
            case AddonSpellVisualPayload.KIMARIS -> new Color(0.42F, 0.86F, 1.0F);
            case AddonSpellVisualPayload.ANDREPHIUS -> new Color(1.0F, 0.7F, 0.08F);
            case AddonSpellVisualPayload.ANTORES -> new Color(1.0F, 0.035F, 0.1F);
            case AddonSpellVisualPayload.ZAGAN -> new Color(0.12F, 0.66F, 1.0F);
            case AddonSpellVisualPayload.ANDRASIAS -> new Color(1.0F, 0.35F, 0.045F);
            case AddonSpellVisualPayload.DETECTION -> new Color(1.0F, 0.08F, 0.17F);
            case AddonSpellVisualPayload.NEGA_SUMMON -> new Color(0.48F, 0.05F, 0.64F);
            default -> new Color(1.0F, 1.0F, 1.0F);
        };
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

    private record VisualKey(byte spell, byte stage, UUID ownerId, int entityId, int variant) {
    }

    private record SpellVisual(AddonSpellVisualPayload cue, long startedAt, long expiresAt) {
    }

    private record Color(float red, float green, float blue) {
        private Color brighten(float amount) {
            return new Color(
                    Mth.clamp(red + (1.0F - red) * amount, 0.0F, 1.0F),
                    Mth.clamp(green + (1.0F - green) * amount, 0.0F, 1.0F),
                    Mth.clamp(blue + (1.0F - blue) * amount, 0.0F, 1.0F));
        }
    }
}
