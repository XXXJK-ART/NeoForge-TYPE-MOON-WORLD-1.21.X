package com.example.typemoonaddon.client;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.network.DetectionTargetSyncPayload;
import com.example.typemoonaddon.network.DetectionTargetSyncPayload.TargetMarker;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Client-only model outlines and head-bound ocular ring rendering. */
@EventBusSubscriber(modid = TypeMoonAddon.MOD_ID, value = Dist.CLIENT)
public final class DetectionClientState {
    private static final ResourceLocation BEACON_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");
    private static final RenderType EYE_GLOW_RENDER_TYPE =
            RenderType.entityTranslucentEmissive(BEACON_TEXTURE);
    private static final double MAX_MARKER_DISTANCE_SQR = 112.0D * 112.0D;
    private static final Map<Integer, Byte> TARGETS = new LinkedHashMap<>();
    private static final Map<Integer, EyeVisual> EYE_VISUALS = new HashMap<>();

    private static ClientLevel trackedLevel;
    private static int clientTick;
    private static boolean privateActive;
    private static int localHeartbeatUntil;
    private static int guiPulseStart;
    private static int guiPulseEnd;
    private static boolean guiClosing;

    private DetectionClientState() {
    }

    public static void applyTargets(boolean active, List<TargetMarker> markers) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (!active) {
            boolean wasActive = privateActive;
            privateActive = false;
            clearTargets(level);
            if (wasActive) {
                startGuiPulse(true);
            } else {
                stopGuiPulse();
            }
            return;
        }

        boolean wasActive = privateActive;
        privateActive = true;
        localHeartbeatUntil = clientTick + 60;
        Map<Integer, Byte> replacement = new LinkedHashMap<>();
        if (markers != null) {
            for (TargetMarker marker : markers) {
                if (marker != null
                        && marker.entityId() > 0
                        && replacement.size() < DetectionTargetSyncPayload.MAX_TARGETS) {
                    replacement.put(marker.entityId(), (byte) (marker.category() & 0x07));
                }
            }
        }

        TARGETS.clear();
        TARGETS.putAll(replacement);
        if (!wasActive) {
            startGuiPulse(false);
        }
    }

    public static void applyEyeState(int entityId, boolean active, int ttlTicks) {
        if (entityId <= 0) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(entityId);
        UUID entityUuid = entity == null ? null : entity.getUUID();
        EyeVisual existing = EYE_VISUALS.get(entityId);
        if (active) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive() || entityUuid == null) {
                EYE_VISUALS.remove(entityId);
                return;
            }
            if (existing == null || existing.isFading(clientTick)) {
                EYE_VISUALS.put(entityId, new EyeVisual(entityUuid, clientTick, clientTick + Math.max(1, ttlTicks)));
            } else if (!existing.matches(entityUuid)) {
                EYE_VISUALS.put(entityId, new EyeVisual(entityUuid, clientTick, clientTick + Math.max(1, ttlTicks)));
            } else {
                existing.refresh(clientTick + Math.max(1, ttlTicks));
            }
        } else if (existing != null && (entityUuid == null || existing.matches(entityUuid))) {
            existing.fadeOut(clientTick);
        } else {
            EYE_VISUALS.remove(entityId);
        }

        if (minecraft.player != null && minecraft.player.getId() == entityId) {
            if (active) {
                localHeartbeatUntil = clientTick + Math.max(20, ttlTicks);
            } else {
                boolean wasActive = privateActive;
                privateActive = false;
                clearTargets(minecraft.level);
                if (wasActive) {
                    startGuiPulse(true);
                } else {
                    stopGuiPulse();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        clientTick++;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != trackedLevel) {
            clearTargets(trackedLevel);
            TARGETS.clear();
            EYE_VISUALS.clear();
            privateActive = false;
            trackedLevel = minecraft.level;
        }
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        if (privateActive && clientTick > localHeartbeatUntil) {
            privateActive = false;
            clearTargets(minecraft.level);
        }

        EYE_VISUALS.entrySet().removeIf(entry -> {
            EyeVisual visual = entry.getValue();
            Entity entity = minecraft.level.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity living)
                    || !living.isAlive()
                    || !visual.matches(entity.getUUID())) {
                return true;
            }
            if (visual.expired(clientTick)) {
                visual.fadeOut(clientTick);
            }
            return visual.finished(clientTick);
        });
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            if (privateActive && minecraft.level != null && minecraft.player != null) {
                renderPrivateEntityOutlines(minecraft, event);
            }
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || EYE_VISUALS.isEmpty() && (!privateActive || TARGETS.isEmpty())) {
            return;
        }
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        float partialTick = event.getPartialTick().getGameTimeDeltaTicks();
        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        try (AddonRenderBuffers renderBuffers = AddonRenderBuffers.fixed(RenderType.lines(), EYE_GLOW_RENDER_TYPE)) {
            BufferSource buffers = renderBuffers.source();
            VertexConsumer lines = buffers.getBuffer(RenderType.lines());
            VertexConsumer glow = buffers.getBuffer(EYE_GLOW_RENDER_TYPE);

            poseStack.pushPose();
            try {
                poseStack.translate(-camera.x, -camera.y, -camera.z);
                renderEyeVisuals(minecraft, poseStack, lines, glow, partialTick);
                if (privateActive) {
                    renderPrivateScanMarkers(minecraft, poseStack, lines, camera, partialTick);
                }
                buffers.endBatch(RenderType.lines());
                buffers.endBatch(EYE_GLOW_RENDER_TYPE);
            } finally {
                poseStack.popPose();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (clientTick >= guiPulseEnd) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        float duration = Math.max(1.0F, guiPulseEnd - guiPulseStart);
        float progress = Mth.clamp((clientTick - guiPulseStart) / duration, 0.0F, 1.0F);
        float fade = guiClosing ? 1.0F - progress : (float) Math.sin(progress * Math.PI);
        int alpha = Mth.clamp(Math.round(145.0F * fade), 0, 145);
        if (alpha <= 0) {
            return;
        }

        GuiGraphics gui = event.getGuiGraphics();
        int centerX = gui.guiWidth() / 2;
        int centerY = gui.guiHeight() / 2;
        double scale = guiClosing ? 1.0D - progress * 0.55D : 0.72D + progress * 0.38D;
        int color = (alpha << 24) | 0x00FF334D;
        drawGuiEllipse(gui, centerX, centerY, 38.0D * scale, 23.0D * scale, color);
        drawGuiEllipse(gui, centerX, centerY, 25.0D * scale, 15.0D * scale, color);
    }

    private static void renderEyeVisuals(
            Minecraft minecraft,
            PoseStack poseStack,
            VertexConsumer lines,
            VertexConsumer glow,
            float partialTick
    ) {
        for (Map.Entry<Integer, EyeVisual> entry : EYE_VISUALS.entrySet()) {
            Entity entity = minecraft.level.getEntity(entry.getKey());
            EyeVisual visual = entry.getValue();
            if (!(entity instanceof LivingEntity living)
                    || !living.isAlive()
                    || !visual.matches(entity.getUUID())) {
                continue;
            }
            if (living == minecraft.player && minecraft.options.getCameraType().isFirstPerson()) {
                continue;
            }
            float alpha = visual.alpha(clientTick, partialTick);
            if (alpha <= 0.01F) {
                continue;
            }
            renderEyeRing(living, poseStack, lines, glow, partialTick,
                    visual.scale(clientTick, partialTick), alpha);
        }
    }

    private static void renderEyeRing(
            LivingEntity living,
            PoseStack poseStack,
            VertexConsumer lines,
            VertexConsumer glow,
            float partialTick,
            float scale,
            float alpha
    ) {
        Vec3 eye = living.getPosition(partialTick).add(0.0D, living.getEyeHeight() - 0.08D, 0.0D);
        float yaw = Mth.rotLerp(partialTick, living.yHeadRotO, living.getYHeadRot());
        float pitch = Mth.lerp(partialTick, living.xRotO, living.getXRot());
        Vec3 forward = Vec3.directionFromRotation(pitch, yaw).normalize();
        Vec3 right = new Vec3(0.0D, 1.0D, 0.0D).cross(forward);
        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 up = forward.cross(right).normalize();
        double rotation = Math.sin((clientTick + partialTick) * 0.08D) * 0.16D;
        Vec3 ringRight = right.scale(Math.cos(rotation)).add(up.scale(Math.sin(rotation)));
        Vec3 ringUp = up.scale(Math.cos(rotation)).subtract(right.scale(Math.sin(rotation)));
        Vec3 center = eye.add(forward.scale(0.36D)).add(right.scale(0.11D));
        double pulse = 1.0D + Math.sin((clientTick + partialTick) * 0.32D) * 0.055D;

        drawGlowPlane(
                poseStack, glow, center.subtract(forward.scale(0.006D)), ringRight, ringUp,
                0.235D * scale * pulse, 0.41D * scale * pulse,
                1.0F, 0.02F, 0.09F, alpha * 0.075F);
        for (int thickness = -2; thickness <= 2; thickness++) {
            drawWorldEllipse(
                    poseStack, lines, center, ringRight, ringUp,
                    (0.188D + thickness * 0.004D) * scale * pulse,
                    (0.36D + thickness * 0.006D) * scale * pulse,
                    1.0F, 0.045F, 0.14F, alpha * (0.68F - Math.abs(thickness) * 0.08F));
        }
        drawWorldEllipse(
                poseStack, lines, center.add(forward.scale(0.012D)), ringRight, ringUp,
                0.102D * scale,
                0.235D * scale,
                1.0F, 0.20F, 0.30F, alpha);
        drawWorldEllipse(
                poseStack, lines, center.add(forward.scale(0.018D)), ringRight, ringUp,
                0.046D * scale,
                0.13D * scale,
                1.0F, 0.62F, 0.68F, alpha * 0.88F);

        double runeRotation = (clientTick + partialTick) * 0.035D;
        for (int rune = 0; rune < 16; rune++) {
            double angle = runeRotation + rune * Math.PI * 2.0D / 16.0D;
            Vec3 inner = ellipsePoint(center, ringRight, ringUp,
                    0.126D * scale, 0.27D * scale, angle);
            Vec3 outer = ellipsePoint(center, ringRight, ringUp,
                    (rune % 4 == 0 ? 0.178D : 0.16D) * scale,
                    (rune % 4 == 0 ? 0.342D : 0.318D) * scale,
                    angle + (rune % 2 == 0 ? 0.014D : -0.014D));
            addLine(poseStack, lines, inner, outer,
                    1.0F, rune % 4 == 0 ? 0.5F : 0.12F, 0.22F, alpha * 0.72F);
        }

        double scan = Math.sin((clientTick + partialTick) * 0.22D) * 0.22D * scale;
        Vec3 scanStart = center.add(ringUp.scale(scan)).subtract(ringRight.scale(0.085D * scale));
        Vec3 scanEnd = center.add(ringUp.scale(scan)).add(ringRight.scale(0.085D * scale));
        addLine(poseStack, lines, scanStart, scanEnd, 1.0F, 0.12F, 0.22F, alpha * 0.72F);
    }

    private static void renderPrivateScanMarkers(
            Minecraft minecraft,
            PoseStack poseStack,
            VertexConsumer lines,
            Vec3 camera,
            float partialTick
    ) {
        float time = clientTick + partialTick;
        for (Map.Entry<Integer, Byte> entry : TARGETS.entrySet()) {
            Entity entity = minecraft.level.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity living)
                    || !living.isAlive()
                    || entity == minecraft.player) {
                continue;
            }
            Vec3 center = entity.getPosition(partialTick)
                    .add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
            if (center.distanceToSqr(camera) > MAX_MARKER_DISTANCE_SQR) {
                continue;
            }
            int packed = targetOutlineColor(entry.getValue());
            float red = (packed >> 16 & 0xFF) / 255.0F;
            float green = (packed >> 8 & 0xFF) / 255.0F;
            float blue = (packed & 0xFF) / 255.0F;
            boolean invisible = (entry.getValue() & DetectionTargetSyncPayload.FLAG_INVISIBLE) != 0;
            float alpha = invisible ? 0.28F : 0.52F;
            double radius = Math.max(0.48D,
                    Math.max(entity.getBbWidth() * 0.72D, entity.getBbHeight() * 0.33D));
            double pulse = 1.0D + Math.sin(time * 0.18D + entity.getId() * 0.31D) * 0.08D;
            double rotation = time * 0.022D + entity.getId() * 0.17D;

            drawWorldEllipse(poseStack, lines, center,
                    new Vec3(1.0D, 0.0D, 0.0D), new Vec3(0.0D, 0.0D, 1.0D),
                    radius * pulse, radius * pulse,
                    red, green, blue, alpha);
            drawWorldEllipse(poseStack, lines, center,
                    new Vec3(Math.cos(rotation), 0.0D, Math.sin(rotation)),
                    new Vec3(0.0D, 1.0D, 0.0D),
                    radius * 0.82D, Math.max(radius, entity.getBbHeight() * 0.58D),
                    red, green, blue, alpha * 0.78F);
            if (!invisible) {
                double halfHeight = Math.max(radius, entity.getBbHeight() * 0.55D);
                addLine(poseStack, lines,
                        center.add(0.0D, -halfHeight, 0.0D),
                        center.add(0.0D, halfHeight, 0.0D),
                        red, green, blue, alpha * 0.34F);
            }
        }
    }

    private static void drawWorldEllipse(
            PoseStack poseStack,
            VertexConsumer lines,
            Vec3 center,
            Vec3 right,
            Vec3 up,
            double radiusX,
            double radiusY,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        final int segments = 64;
        Vec3 previous = ellipsePoint(center, right, up, radiusX, radiusY, 0.0D);
        for (int index = 1; index <= segments; index++) {
            double angle = Math.PI * 2.0D * index / segments;
            Vec3 current = ellipsePoint(center, right, up, radiusX, radiusY, angle);
            addLine(poseStack, lines, previous, current, red, green, blue, alpha);
            previous = current;
        }
    }

    private static Vec3 ellipsePoint(
            Vec3 center,
            Vec3 right,
            Vec3 up,
            double radiusX,
            double radiusY,
            double angle
    ) {
        return center.add(right.scale(Math.cos(angle) * radiusX))
                .add(up.scale(Math.sin(angle) * radiusY));
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

    private static void drawGlowPlane(
            PoseStack poseStack,
            VertexConsumer glow,
            Vec3 center,
            Vec3 right,
            Vec3 up,
            double radiusX,
            double radiusY,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        Vec3 rightOffset = right.normalize().scale(radiusX);
        Vec3 upOffset = up.normalize().scale(radiusY);
        Vec3 a = center.subtract(rightOffset).subtract(upOffset);
        Vec3 b = center.add(rightOffset).subtract(upOffset);
        Vec3 c = center.add(rightOffset).add(upOffset);
        Vec3 d = center.subtract(rightOffset).add(upOffset);
        glowQuad(poseStack, glow, a, b, c, d, red, green, blue, alpha);
        glowQuad(poseStack, glow, d, c, b, a, red, green, blue, alpha);
    }

    private static void glowQuad(
            PoseStack poseStack,
            VertexConsumer glow,
            Vec3 a,
            Vec3 b,
            Vec3 c,
            Vec3 d,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        glowVertex(poseStack, glow, a, red, green, blue, alpha, 0.0F, 0.0F);
        glowVertex(poseStack, glow, b, red, green, blue, alpha, 1.0F, 0.0F);
        glowVertex(poseStack, glow, c, red, green, blue, alpha, 1.0F, 1.0F);
        glowVertex(poseStack, glow, d, red, green, blue, alpha, 0.0F, 1.0F);
    }

    private static void glowVertex(
            PoseStack poseStack,
            VertexConsumer glow,
            Vec3 point,
            float red,
            float green,
            float blue,
            float alpha,
            float u,
            float v
    ) {
        glow.addVertex(poseStack.last(), (float) point.x, (float) point.y, (float) point.z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0)
                .setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F);
    }

    private static void renderPrivateEntityOutlines(
            Minecraft minecraft,
            RenderLevelStageEvent event
    ) {
        OutlineBufferSource outlines = minecraft.renderBuffers().outlineBufferSource();
        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        float partialTick = event.getPartialTick().getGameTimeDeltaTicks();
        boolean renderedAny = false;

        for (Map.Entry<Integer, Byte> entry : TARGETS.entrySet()) {
            Entity entity = minecraft.level.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity living)
                    || !living.isAlive()
                    || entity == minecraft.player) {
                continue;
            }

            int color = targetOutlineColor(entry.getValue());
            outlines.setColor(
                    color >> 16 & 0xFF,
                    color >> 8 & 0xFF,
                    color & 0xFF,
                    255
            );
            double x = Mth.lerp((double) partialTick, entity.xOld, entity.getX()) - camera.x;
            double y = Mth.lerp((double) partialTick, entity.yOld, entity.getY()) - camera.y;
            double z = Mth.lerp((double) partialTick, entity.zOld, entity.getZ()) - camera.z;
            float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
            boolean originallyGlowing = entity.hasGlowingTag();
            entity.setGlowingTag(true);
            try {
                minecraft.getEntityRenderDispatcher().render(
                        entity,
                        x,
                        y,
                        z,
                        yaw,
                        partialTick,
                        poseStack,
                        outlines,
                        minecraft.getEntityRenderDispatcher().getPackedLightCoords(entity, partialTick)
                );
                renderedAny = true;
            } finally {
                entity.setGlowingTag(originallyGlowing);
            }
        }
        if (renderedAny) {
            event.getLevelRenderer().requestOutlineEffect();
        }
    }

    private static void clearTargets(ClientLevel level) {
        TARGETS.clear();
    }

    private static int targetOutlineColor(byte category) {
        int base = switch (category & 0x03) {
            case DetectionTargetSyncPayload.CATEGORY_HOSTILE -> 0xFF334D;
            case DetectionTargetSyncPayload.CATEGORY_FRIENDLY -> 0x19FF9E;
            default -> 0xFFCA38;
        };
        if ((category & DetectionTargetSyncPayload.FLAG_INVISIBLE) == 0) {
            return base;
        }
        int red = Math.round((base >> 16 & 0xFF) * 0.58F);
        int green = Math.round((base >> 8 & 0xFF) * 0.58F);
        int blue = Math.round((base & 0xFF) * 0.58F);
        return red << 16 | green << 8 | blue;
    }

    private static void startGuiPulse(boolean closing) {
        guiPulseStart = clientTick;
        guiPulseEnd = clientTick + (closing ? 8 : 14);
        guiClosing = closing;
    }

    private static void stopGuiPulse() {
        guiPulseStart = clientTick;
        guiPulseEnd = clientTick;
        guiClosing = false;
    }

    private static void drawGuiEllipse(
            GuiGraphics gui,
            int centerX,
            int centerY,
            double radiusX,
            double radiusY,
            int color
    ) {
        final int segments = 96;
        for (int index = 0; index < segments; index++) {
            double angle = Math.PI * 2.0D * index / segments;
            int x = centerX + (int) Math.round(Math.cos(angle) * radiusX);
            int y = centerY + (int) Math.round(Math.sin(angle) * radiusY);
            gui.fill(x, y, x + 2, y + 2, color);
        }
    }

    private static final class EyeVisual {
        private final UUID entityUuid;
        private final int startedAt;
        private int expiresAt;
        private int fadeStartedAt = -1;
        private int fadeEndsAt = -1;

        private EyeVisual(UUID entityUuid, int startedAt, int expiresAt) {
            this.entityUuid = entityUuid;
            this.startedAt = startedAt;
            this.expiresAt = expiresAt;
        }

        private boolean matches(UUID uuid) {
            return entityUuid != null && entityUuid.equals(uuid);
        }

        private void refresh(int newExpiresAt) {
            expiresAt = Math.max(expiresAt, newExpiresAt);
            fadeStartedAt = -1;
            fadeEndsAt = -1;
        }

        private boolean expired(int now) {
            return now > expiresAt && fadeStartedAt < 0;
        }

        private boolean isFading(int now) {
            return fadeStartedAt >= 0 && now <= fadeEndsAt;
        }

        private void fadeOut(int now) {
            if (fadeStartedAt < 0) {
                fadeStartedAt = now;
                fadeEndsAt = now + 8;
            }
        }

        private boolean finished(int now) {
            return fadeEndsAt >= 0 && now > fadeEndsAt;
        }

        private float alpha(int now, float partialTick) {
            float time = now + partialTick;
            if (fadeStartedAt >= 0) {
                return 1.0F - Mth.clamp((time - fadeStartedAt) / 8.0F, 0.0F, 1.0F);
            }
            return Mth.clamp((time - startedAt) / 6.0F, 0.0F, 1.0F);
        }

        private float scale(int now, float partialTick) {
            float activation = Mth.clamp((now + partialTick - startedAt) / 8.0F, 0.0F, 1.0F);
            if (fadeStartedAt >= 0) {
                float fade = Mth.clamp((now + partialTick - fadeStartedAt) / 8.0F, 0.0F, 1.0F);
                return Math.max(0.05F, (0.55F + activation * 0.45F) * (1.0F - fade * 0.8F));
            }
            return 0.55F + activation * 0.45F;
        }
    }
}
