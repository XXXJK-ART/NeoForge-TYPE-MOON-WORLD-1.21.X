package io.github.typemoonaddon.client;

import io.github.typemoonaddon.network.EffectPayload;
import io.github.typemoonaddon.network.DeathFadePayload;
import io.github.typemoonaddon.network.VoidAbsorptionLinkPayload;
import io.github.typemoonaddon.magic.GrailParticleService;
import io.github.typemoonaddon.shadowlogic.client.renderer.ShadowRibbonPlaneGeometry;
import io.github.typemoonaddon.shadowlogic.client.renderer.ShadowRibbonRenderStyle;
import io.github.typemoonaddon.shadowlogic.network.ShadowBindingEffectPayload;
import io.github.typemoonaddon.network.MagicOutputShockwavePayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class ClientEffects {
    private static final Map<Integer, ActiveEffect> ACTIVE = new HashMap<>();
    private static final List<DeathFade> DEATH_FADES = new ArrayList<>();
    private static final List<MagicOutputShockwave> MAGIC_OUTPUT_SHOCKWAVES = new ArrayList<>();
    private static final Map<VoidLinkKey, ActiveVoidLink> VOID_LINKS = new HashMap<>();
    private static final Map<VoidLinkKey, ActiveShadowBinding> SHADOW_BINDINGS = new HashMap<>();
    private static final double VOID_LINK_PARTICLE_SPACING = 0.5D;
    private static final int VOID_LINK_REFRESH_TICKS = 4;
    private static final double RED_LINK_COIL_RADIUS = 0.32D;
    private static final int SHADOW_RING_SUBDIVISIONS_PER_MODEL = 2;
    private static final double GOLDEN_ANGLE = Math.PI * (3.0D - Math.sqrt(5.0D));
    private static final DustParticleOptions LIGHT_BLUE_SHOCKWAVE = new DustParticleOptions(
        new Vector3f(0.58F, 0.9F, 1.0F),
        2.5F
    );

    public static void start(EffectPayload payload) {
        ACTIVE.put(
            payload.entityId(),
            new ActiveEffect(payload.effect(), Math.max(1, payload.durationTicks()), 0, payload.palette())
        );
    }

    public static void startDeathFade(DeathFadePayload payload) {
        DEATH_FADES.removeIf(fade -> fade.entityId() == payload.entityId());
        DEATH_FADES.add(new DeathFade(
            payload.entityId(),
            payload.x(),
            payload.y(),
            payload.z(),
            payload.width(),
            payload.height(),
            0,
            payload.palette()
        ));
    }

    public static void maintainVoidAbsorptionLink(VoidAbsorptionLinkPayload payload) {
        VoidLinkKey key = new VoidLinkKey(payload.sourceEntityId(), payload.targetEntityId());
        ActiveVoidLink link = VOID_LINKS.computeIfAbsent(key, ignored -> new ActiveVoidLink());
        link.remainingTicks = Math.max(1, payload.durationTicks());
        link.palette = payload.palette();
    }

    public static void maintainShadowBinding(ShadowBindingEffectPayload payload) {
        VoidLinkKey key = new VoidLinkKey(payload.sourceEntityId(), payload.targetEntityId());
        ActiveShadowBinding binding = SHADOW_BINDINGS.computeIfAbsent(key, ignored -> new ActiveShadowBinding());
        binding.remainingTicks = Math.max(1, payload.durationTicks());
        binding.palette = payload.palette();
    }

    public static void startMagicOutputShockwave(MagicOutputShockwavePayload payload) {
        MAGIC_OUTPUT_SHOCKWAVES.add(new MagicOutputShockwave(
            new Vec3(payload.x(), payload.y(), payload.z()),
            Math.clamp(payload.maximumRadius(), 1.0F, 256.0F),
            Math.clamp(payload.durationTicks(), 1, 120)
        ));
    }

    public static float deathDissolutionScale(int entityId, float partialTick) {
        for (DeathFade fade : DEATH_FADES) {
            if (fade.entityId() == entityId) {
                return Math.max(0.0F, 1.0F - (fade.elapsed() + partialTick) / 60.0F);
            }
        }
        return 1.0F;
    }

    public static void renderMagicOutputShockwaves(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
            || MAGIC_OUTPUT_SHOCKWAVES.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        PoseStack poseStack = event.getPoseStack();
        var bufferSource = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        for (MagicOutputShockwave shockwave : MAGIC_OUTPUT_SHOCKWAVES) {
            float progress = Math.clamp(
                (shockwave.elapsedTicks + partialTick) / shockwave.durationTicks,
                0.0F,
                1.0F
            );
            float expansion = 1.0F - (1.0F - progress) * (1.0F - progress);
            float radius = Math.max(0.25F, shockwave.maximumRadius * expansion);
            float fade = 1.0F - progress;
            float thickness = 0.28F + radius * 0.012F;

            poseStack.pushPose();
            poseStack.translate(
                shockwave.center.x - camera.x,
                shockwave.center.y - camera.y,
                shockwave.center.z - camera.z
            );
            Matrix4f matrix = poseStack.last().pose();
            renderShockwaveSphere(consumer, matrix, radius, shockwaveColor(220.0F * fade, 0x8DEAFF));
            renderShockwaveSphere(
                consumer,
                matrix,
                radius + thickness,
                shockwaveColor(105.0F * fade, 0xD2F7FF)
            );
            renderShockwaveSphere(
                consumer,
                matrix,
                Math.max(0.1F, radius - thickness),
                shockwaveColor(135.0F * fade, 0x54CFFF)
            );
            poseStack.popPose();
        }
        bufferSource.endBatch(RenderType.lightning());
    }

    private static int shockwaveColor(float alpha, int rgb) {
        return Math.clamp(Math.round(alpha), 0, 255) << 24 | rgb;
    }

    private static void renderShockwaveSphere(
        VertexConsumer consumer,
        Matrix4f matrix,
        float radius,
        int color
    ) {
        int latitudeSegments = 20;
        int longitudeSegments = 40;
        for (int latitude = 0; latitude < latitudeSegments; latitude++) {
            double latitudeA = -Math.PI * 0.5D + Math.PI * latitude / latitudeSegments;
            double latitudeB = -Math.PI * 0.5D + Math.PI * (latitude + 1) / latitudeSegments;
            for (int longitude = 0; longitude < longitudeSegments; longitude++) {
                double longitudeA = Math.PI * 2.0D * longitude / longitudeSegments;
                double longitudeB = Math.PI * 2.0D * (longitude + 1) / longitudeSegments;
                shockwaveVertex(consumer, matrix, radius, latitudeA, longitudeA, color);
                shockwaveVertex(consumer, matrix, radius, latitudeA, longitudeB, color);
                shockwaveVertex(consumer, matrix, radius, latitudeB, longitudeB, color);
                shockwaveVertex(consumer, matrix, radius, latitudeB, longitudeA, color);
            }
        }
    }

    private static void shockwaveVertex(
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

    public static void renderShadowBinding(
        LivingEntity target,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        float partialTick
    ) {
        Byte palette = shadowBindingPalette(target.getId());
        if (palette == null) {
            return;
        }

        float width = Math.max(0.1F, target.getBbWidth());
        float height = Math.max(0.25F, target.getBbHeight());
        float radius = Math.max(0.34F, width * 0.76F + 0.035F);
        float animation = (target.tickCount + partialTick) * 0.025F;
        int surface = ShadowRibbonRenderStyle.surface(palette, animation);

        PoseStack.Pose pose = poseStack.last();
        VertexConsumer consumer = ShadowRibbonPlaneGeometry.consumer(bufferSource);
        renderModelRing(consumer, pose, radius, height, 0.80F, 0.12F, 0.055F, animation, surface);
        renderModelRing(consumer, pose, radius + 0.01F, height, 0.53F, 0.15F, 0.17F, animation + 2.1F, surface);
        renderModelRing(consumer, pose, radius, height, 0.25F, 0.13F, 0.04F, animation + 4.2F, surface);
        renderShadowBindingLinks(target, poseStack, bufferSource, partialTick);
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.level == null) {
            ACTIVE.clear();
            DEATH_FADES.clear();
            VOID_LINKS.clear();
            SHADOW_BINDINGS.clear();
            MAGIC_OUTPUT_SHOCKWAVES.clear();
            return;
        }

        tickVoidAbsorptionLinks(minecraft);
        tickShadowBindings(minecraft);
        tickMagicOutputShockwaves(minecraft);

        Iterator<Map.Entry<Integer, ActiveEffect>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, ActiveEffect> entry = iterator.next();
            Entity entity = minecraft.level.getEntity(entry.getKey());
            ActiveEffect effect = entry.getValue();
            if (entity == null || effect.elapsed() >= effect.duration()) {
                iterator.remove();
                continue;
            }

            AABB box = entity.getBoundingBox();
            double progress = effect.elapsed() / (double) effect.duration();
            double planeY = box.maxY - Math.max(0.1D, box.getYsize()) * progress;
            int particles = 4;
            for (int i = 0; i < particles; i++) {
                double x = box.minX + minecraft.level.random.nextDouble() * Math.max(0.1D, box.getXsize());
                double z = box.minZ + minecraft.level.random.nextDouble() * Math.max(0.1D, box.getZsize());
                double y = planeY + (minecraft.level.random.nextDouble() - 0.5D) * 0.08D;
                minecraft.level.addParticle(
                    GrailParticleService.particle(
                        effect.palette(),
                        (long)effect.elapsed() * particles + i,
                        ParticleTypes.REVERSE_PORTAL
                    ),
                    x,
                    y,
                    z,
                    0.0D,
                    0.015D,
                    0.0D
                );
            }
            entry.setValue(new ActiveEffect(
                effect.kind(), effect.duration(), effect.elapsed() + 1, effect.palette()
            ));
        }

        Iterator<DeathFade> fades = DEATH_FADES.iterator();
        while (fades.hasNext()) {
            DeathFade fade = fades.next();
            if (fade.elapsed() >= 60) {
                fades.remove();
                continue;
            }
            double progress = fade.elapsed() / 60.0D;
            double planeY = fade.y() + fade.height() * (1.0D - progress);
            for (int i = 0; i < 7; i++) {
                double x = fade.x() + (minecraft.level.random.nextDouble() - 0.5D) * Math.max(0.3D, fade.width());
                double z = fade.z() + (minecraft.level.random.nextDouble() - 0.5D) * Math.max(0.3D, fade.width());
                minecraft.level.addParticle(
                    GrailParticleService.particle(
                        fade.palette(), (long)fade.elapsed() * 7L + i, ParticleTypes.SQUID_INK
                    ),
                    x,
                    planeY,
                    z,
                    0.0D,
                    0.012D,
                    0.0D
                );
            }
            fade.advance();
        }
    }

    private static void tickMagicOutputShockwaves(Minecraft minecraft) {
        Iterator<MagicOutputShockwave> shockwaves = MAGIC_OUTPUT_SHOCKWAVES.iterator();
        while (shockwaves.hasNext()) {
            MagicOutputShockwave shockwave = shockwaves.next();
            if (shockwave.elapsedTicks >= shockwave.durationTicks) {
                shockwaves.remove();
                continue;
            }

            float progress = (shockwave.elapsedTicks + 1.0F) / shockwave.durationTicks;
            double radius = shockwave.maximumRadius * progress;
            int points = 160 + (int)Math.ceil(radius * 8.0D);
            double rotation = shockwave.elapsedTicks * 0.09D;
            for (int index = 0; index < points; index++) {
                double vertical = 1.0D - 2.0D * (index + 0.5D) / points;
                double horizontal = Math.sqrt(Math.max(0.0D, 1.0D - vertical * vertical));
                double angle = index * GOLDEN_ANGLE + rotation;
                Vec3 direction = new Vec3(
                    Math.cos(angle) * horizontal,
                    vertical,
                    Math.sin(angle) * horizontal
                );
                Vec3 point = shockwave.center.add(direction.scale(radius));
                minecraft.level.addAlwaysVisibleParticle(
                    LIGHT_BLUE_SHOCKWAVE,
                    point.x,
                    point.y,
                    point.z,
                    direction.x * 0.018D,
                    direction.y * 0.018D,
                    direction.z * 0.018D
                );
                if (index % 6 == 0) {
                    minecraft.level.addAlwaysVisibleParticle(
                        ParticleTypes.END_ROD,
                        point.x,
                        point.y,
                        point.z,
                        direction.x * 0.035D,
                        direction.y * 0.035D,
                        direction.z * 0.035D
                    );
                }
            }
            shockwave.elapsedTicks++;
        }
    }

    private static void tickVoidAbsorptionLinks(Minecraft minecraft) {
        Iterator<Map.Entry<VoidLinkKey, ActiveVoidLink>> links = VOID_LINKS.entrySet().iterator();
        while (links.hasNext()) {
            Map.Entry<VoidLinkKey, ActiveVoidLink> entry = links.next();
            ActiveVoidLink link = entry.getValue();
            if (--link.remainingTicks < 0) {
                links.remove();
                continue;
            }

            Entity source = minecraft.level.getEntity(entry.getKey().sourceEntityId());
            Entity target = minecraft.level.getEntity(entry.getKey().targetEntityId());
            if (source == null || target == null || !source.isAlive() || !target.isAlive()) {
                continue;
            }
            if (link.refreshTicks-- <= 0) {
                spawnContinuousVoidLink(minecraft, source, target, link.palette);
                link.refreshTicks = VOID_LINK_REFRESH_TICKS - 1;
            }
        }
    }

    private static void spawnContinuousVoidLink(
        Minecraft minecraft,
        Entity source,
        Entity target,
        byte palette
    ) {
        Vec3 from = source.position().add(0.0D, 0.08D, 0.0D);
        Vec3 to = target.position().add(0.0D, 0.08D, 0.0D);
        spawnContinuousLink(minecraft, from, to, palette);
    }

    private static void spawnContinuousLink(Minecraft minecraft, Vec3 from, Vec3 to, byte palette) {
        Vec3 delta = to.subtract(from);
        int segments = Math.max(1, (int)Math.ceil(delta.length() / VOID_LINK_PARTICLE_SPACING));
        Vec3 direction = delta.normalize();
        Vec3 reference = Math.abs(direction.y) < 0.9D ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 coilSide = direction.cross(reference).normalize();
        Vec3 coilUp = direction.cross(coilSide).normalize();
        double coilPhase = minecraft.level.getGameTime() * 0.22D;
        for (int index = 0; index <= segments; index++) {
            Vec3 point = from.add(delta.scale(index / (double)segments));
            ParticleOptions bodyParticle = palette == GrailParticleService.SAKURA
                ? GrailParticleService.SAKURA_DUST
                : ParticleTypes.SQUID_INK;
            minecraft.level.addAlwaysVisibleParticle(
                bodyParticle,
                point.x,
                point.y,
                point.z,
                0.0D,
                0.0D,
                0.0D
            );
            if (index % 4 == 0 && palette != GrailParticleService.BLACK) {
                double angle = coilPhase + index * 0.32D;
                Vec3 coilOffset = coilSide.scale(Math.cos(angle) * RED_LINK_COIL_RADIUS)
                    .add(coilUp.scale(Math.sin(angle) * RED_LINK_COIL_RADIUS));
                Vec3 redPoint = point.add(coilOffset);
                minecraft.level.addAlwaysVisibleParticle(
                    palette == GrailParticleService.SAKURA
                        ? GrailParticleService.SAKURA_DUST
                        : DustParticleOptions.REDSTONE,
                    redPoint.x,
                    redPoint.y,
                    redPoint.z,
                    0.0D,
                    0.0D,
                    0.0D
                );
            }
        }
    }

    private static void tickShadowBindings(Minecraft minecraft) {
        Map<Integer, Byte> ringTargets = new HashMap<>();
        boolean spawnMotes = minecraft.level.getGameTime() % 6L == 0L;
        Iterator<Map.Entry<VoidLinkKey, ActiveShadowBinding>> bindings = SHADOW_BINDINGS.entrySet().iterator();
        while (bindings.hasNext()) {
            Map.Entry<VoidLinkKey, ActiveShadowBinding> entry = bindings.next();
            ActiveShadowBinding binding = entry.getValue();
            if (--binding.remainingTicks < 0) {
                bindings.remove();
                continue;
            }

            Entity target = minecraft.level.getEntity(entry.getKey().targetEntityId());
            if (target == null || !target.isAlive()) {
                continue;
            }
            if (spawnMotes) {
                ringTargets.merge(target.getId(), binding.palette, GrailParticleService::highest);
            }

            Entity source = minecraft.level.getEntity(entry.getKey().sourceEntityId());
            if (source == null || !source.isAlive()) {
                continue;
            }
            if (binding.linkRefreshTicks-- <= 0) {
                binding.linkRefreshTicks = VOID_LINK_REFRESH_TICKS - 1;
            }
        }
        for (Map.Entry<Integer, Byte> entry : ringTargets.entrySet()) {
            Entity target = minecraft.level.getEntity(entry.getKey());
            if (target != null && target.isAlive()) {
                spawnShadowBindingMote(minecraft, target, entry.getValue());
            }
        }
    }

    private static void spawnShadowBindingMote(Minecraft minecraft, Entity target, byte palette) {
        AABB box = target.getBoundingBox();
        long firstMoteIndex = (minecraft.level.getGameTime() / 6L + target.getId()) * 2L;
        for (int mote = 0; mote < 2; mote++) {
            minecraft.level.addAlwaysVisibleParticle(
                GrailParticleService.particle(
                    palette, firstMoteIndex + mote, ParticleTypes.SQUID_INK
                ),
                box.minX + minecraft.level.random.nextDouble() * Math.max(0.1D, box.getXsize()),
                box.minY + minecraft.level.random.nextDouble() * Math.max(0.1D, box.getYsize()),
                box.minZ + minecraft.level.random.nextDouble() * Math.max(0.1D, box.getZsize()),
                0.0D,
                palette == GrailParticleService.BLACK ? 0.006D : 0.012D,
                0.0D
            );
        }
    }

    private static void renderShadowBindingLinks(
        LivingEntity target,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        float partialTick
    ) {
        Vec3 targetPosition = target.getPosition(partialTick);
        VertexConsumer consumer = ShadowRibbonPlaneGeometry.consumer(bufferSource);
        float animation = (target.tickCount + partialTick) * 0.025F;
        for (Map.Entry<VoidLinkKey, ActiveShadowBinding> entry : SHADOW_BINDINGS.entrySet()) {
            if (entry.getKey().targetEntityId() != target.getId() || entry.getValue().remainingTicks < 0) {
                continue;
            }
            Entity source = target.level().getEntity(entry.getKey().sourceEntityId());
            if (source == null) {
                continue;
            }
            Vec3 end = source.getPosition(partialTick).subtract(targetPosition)
                .add(0.0D, source.getBbHeight() * 0.62D, 0.0D);
            Vec3 start = new Vec3(0.0D, target.getBbHeight() * 0.52D, 0.0D);
            renderConnectionRibbon(
                consumer,
                poseStack.last(),
                start,
                end,
                ShadowRibbonRenderStyle.surface(entry.getValue().palette, animation),
                entry.getKey().sourceEntityId()
            );
        }
    }

    private static void renderConnectionRibbon(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        Vec3 start,
        Vec3 end,
        int surface,
        int seed
    ) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 0.05D) {
            return;
        }
        Vec3 direction = delta.normalize();
        Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-5D) {
            side = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }
        side = side.normalize();
        Vec3 controlA = start.add(delta.scale(0.28D)).add(side.scale(Math.sin(seed * 0.73D) * Math.min(1.2D, length * 0.12D))).add(0.0D, 0.8D, 0.0D);
        Vec3 controlB = start.add(delta.scale(0.72D)).add(side.scale(Math.cos(seed * 0.51D) * Math.min(0.9D, length * 0.08D))).add(0.0D, 0.35D, 0.0D);
        int segments = Math.clamp(
            (int)Math.ceil(length / ShadowRibbonPlaneGeometry.MODEL_LENGTH),
            8,
            256
        );
        Vec3 previousCenter = start;
        Vec3 previousWidth = side.scale(ShadowRibbonPlaneGeometry.MODEL_WIDTH * 0.5D);
        for (int index = 1; index <= segments; index++) {
            double t = index / (double)segments;
            Vec3 center = cubicBezier(start, controlA, controlB, end, t);
            Vec3 tangent = center.subtract(previousCenter);
            Vec3 widthAxis = tangent.cross(center.subtract(Vec3.ZERO));
            if (widthAxis.lengthSqr() < 1.0E-5D) {
                widthAxis = side;
            } else {
                widthAxis = widthAxis.normalize();
            }
            Vec3 width = widthAxis.scale(ShadowRibbonPlaneGeometry.MODEL_WIDTH * 0.5D);
            ShadowRibbonPlaneGeometry.renderSegment(
                consumer,
                pose,
                previousCenter.add(previousWidth),
                previousCenter.subtract(previousWidth),
                center.subtract(width),
                center.add(width),
                surface
            );
            previousCenter = center;
            previousWidth = width;
        }
    }

    private static Vec3 cubicBezier(Vec3 a, Vec3 b, Vec3 c, Vec3 d, double t) {
        double inverse = 1.0D - t;
        return a.scale(inverse * inverse * inverse)
            .add(b.scale(3.0D * inverse * inverse * t))
            .add(c.scale(3.0D * inverse * t * t))
            .add(d.scale(t * t * t));
    }

    private static Byte shadowBindingPalette(int targetEntityId) {
        Byte palette = null;
        for (Map.Entry<VoidLinkKey, ActiveShadowBinding> entry : SHADOW_BINDINGS.entrySet()) {
            if (entry.getKey().targetEntityId() == targetEntityId && entry.getValue().remainingTicks >= 0) {
                palette = palette == null
                    ? entry.getValue().palette
                    : GrailParticleService.highest(palette, entry.getValue().palette);
            }
        }
        return palette;
    }

    private static void renderModelRing(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        float radius,
        float height,
        float centerRatio,
        float widthRatio,
        float tiltRatio,
        float phase,
        int surface
    ) {
        float halfWidth = height * widthRatio * 0.5F;
        int modelRepeats = Math.clamp(
            (int)Math.ceil(Math.PI * 2.0D * radius / ShadowRibbonPlaneGeometry.MODEL_LENGTH),
            8,
            128
        );
        int segments = modelRepeats * SHADOW_RING_SUBDIVISIONS_PER_MODEL;
        for (int segment = 0; segment < segments; segment++) {
            float angleA = (float)(Math.PI * 2.0D * segment / segments);
            float angleB = (float)(Math.PI * 2.0D * (segment + 1) / segments);
            float centerA = height * (centerRatio + tiltRatio * (float)Math.sin(angleA + phase));
            float centerB = height * (centerRatio + tiltRatio * (float)Math.sin(angleB + phase));
            float xA = (float)Math.cos(angleA) * radius;
            float zA = (float)Math.sin(angleA) * radius;
            float xB = (float)Math.cos(angleB) * radius;
            float zB = (float)Math.sin(angleB) * radius;
            float modelVStart = (segment % SHADOW_RING_SUBDIVISIONS_PER_MODEL)
                / (float)SHADOW_RING_SUBDIVISIONS_PER_MODEL;
            float modelVEnd = (segment % SHADOW_RING_SUBDIVISIONS_PER_MODEL + 1)
                / (float)SHADOW_RING_SUBDIVISIONS_PER_MODEL;
            ShadowRibbonPlaneGeometry.renderSegment(
                consumer,
                pose,
                new Vec3(xA, centerA + halfWidth, zA),
                new Vec3(xA, centerA - halfWidth, zA),
                new Vec3(xB, centerB - halfWidth, zB),
                new Vec3(xB, centerB + halfWidth, zB),
                surface,
                modelVStart,
                modelVEnd
            );
        }
    }

    private record ActiveEffect(byte kind, int duration, int elapsed, byte palette) {
    }

    private record VoidLinkKey(int sourceEntityId, int targetEntityId) {
    }

    private static final class ActiveVoidLink {
        private int remainingTicks;
        private int refreshTicks;
        private byte palette;
    }

    private static final class ActiveShadowBinding {
        private int remainingTicks;
        private int linkRefreshTicks;
        private byte palette;
    }

    private static final class DeathFade {
        private final int entityId;
        private final double x;
        private final double y;
        private final double z;
        private final float width;
        private final float height;
        private final byte palette;
        private int elapsed;

        private DeathFade(
            int entityId,
            double x,
            double y,
            double z,
            float width,
            float height,
            int elapsed,
            byte palette
        ) {
            this.entityId = entityId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.width = width;
            this.height = height;
            this.elapsed = elapsed;
            this.palette = palette;
        }

        private int entityId() { return entityId; }
        private double x() { return x; }
        private double y() { return y; }
        private double z() { return z; }
        private float width() { return width; }
        private float height() { return height; }
        private byte palette() { return palette; }
        private int elapsed() { return elapsed; }
        private void advance() { elapsed++; }
    }

    private static final class MagicOutputShockwave {
        private final Vec3 center;
        private final float maximumRadius;
        private final int durationTicks;
        private int elapsedTicks;

        private MagicOutputShockwave(Vec3 center, float maximumRadius, int durationTicks) {
            this.center = center;
            this.maximumRadius = maximumRadius;
            this.durationTicks = durationTicks;
        }
    }

    private ClientEffects() {
    }
}
