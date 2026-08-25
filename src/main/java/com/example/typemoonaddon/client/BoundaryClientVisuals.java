package com.example.typemoonaddon.client;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.entity.BoundaryMarkEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.xxxjk.TYPE_MOON_WORLD.vfx.client.VFXRenderManager;
import net.xxxjk.TYPE_MOON_WORLD.vfx.client.VFXRingShockwave;
import org.joml.Vector3f;

@EventBusSubscriber(modid = TypeMoonAddon.MOD_ID, value = Dist.CLIENT)
public final class BoundaryClientVisuals {
    private static final int MAX_PREVIEWS = 24;
    private static final int REFRESH_INTERVAL = 10;
    private static final double MAX_RENDER_DISTANCE_SQR = 160.0D * 160.0D;
    private static final Map<UUID, PreviewBoundary> PREVIEWS = new LinkedHashMap<>();

    private static ClientLevel activeLevel;
    private static long clientTick;

    private BoundaryClientVisuals() {
    }

    public static void spawnImpact(Vec3 impact, int argbColor) {
        if (impact == null) {
            return;
        }
        int color = (argbColor & 0x00FFFFFF) | 0x44000000;
        VFXRenderManager.addRingShockwave(new VFXRingShockwave(
                new Vector3f((float) impact.x, (float) impact.y + 0.03F, (float) impact.z),
                color,
                0.14F,
                0.95F,
                0.35F
        ));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != activeLevel) {
            PREVIEWS.clear();
            activeLevel = minecraft.level;
            clientTick = 0L;
        }
        if (minecraft.level == null || minecraft.player == null) {
            PREVIEWS.clear();
            activeLevel = null;
            return;
        }
        clientTick++;
        if ((clientTick % REFRESH_INTERVAL) != 0L) {
            return;
        }
        rebuildPreviews(minecraft);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || PREVIEWS.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        poseStack.pushPose();
        try {
            poseStack.translate(-camera.x, -camera.y, -camera.z);
            BufferSource buffers = minecraft.renderBuffers().bufferSource();
            var lines = buffers.getBuffer(RenderType.lines());
            for (PreviewBoundary preview : PREVIEWS.values()) {
                if (preview.bounds().getCenter().distanceToSqr(camera) > MAX_RENDER_DISTANCE_SQR) {
                    continue;
                }
                float alpha = preview.alpha(camera);
                if (alpha <= 0.02F) {
                    continue;
                }
                LevelRenderer.renderLineBox(poseStack, lines, preview.bounds(), preview.red(), preview.green(), preview.blue(), alpha);
            }
            buffers.endBatch(RenderType.lines());
        } finally {
            poseStack.popPose();
        }
    }

    private static void rebuildPreviews(Minecraft minecraft) {
        PREVIEWS.clear();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        UUID owner = minecraft.player.getUUID();
        Vec3 center = minecraft.player.position();
        AABB searchBox = minecraft.player.getBoundingBox().inflate(160.0D);
        List<BoundaryMarkEntity> marks = minecraft.level.getEntitiesOfClass(BoundaryMarkEntity.class, searchBox,
                mark -> mark.isBound() && owner.equals(mark.getAuthorId()));
        if (marks.isEmpty()) {
            return;
        }

        Map<UUID, PreviewBoundaryBuilder> builders = new LinkedHashMap<>();
        for (BoundaryMarkEntity mark : marks) {
            builders.computeIfAbsent(mark.getBoundaryId(), PreviewBoundaryBuilder::new).include(mark);
        }

        List<PreviewBoundary> previews = new ArrayList<>();
        for (PreviewBoundaryBuilder builder : builders.values()) {
            PreviewBoundary preview = builder.build();
            if (preview != null && preview.bounds().intersects(searchBox)) {
                previews.add(preview);
            }
        }
        previews.sort(Comparator.comparingDouble(preview -> preview.bounds().getCenter().distanceToSqr(center)));
        for (PreviewBoundary preview : previews) {
            if (PREVIEWS.size() >= MAX_PREVIEWS) {
                break;
            }
            PREVIEWS.put(preview.key(), preview);
        }
    }

    private static float[] colorFor(String type) {
        return switch (type) {
            case "warning_boundary" -> new float[]{1.0F, 0.72F, 0.28F};
            case "defense_boundary" -> new float[]{0.45F, 0.75F, 1.0F};
            case "suggestion_boundary" -> new float[]{0.82F, 0.48F, 1.0F};
            case "anti_magic_boundary" -> new float[]{0.38F, 0.55F, 1.0F};
            case "guard_boundary" -> new float[]{1.0F, 0.42F, 0.42F};
            case "interference_boundary" -> new float[]{0.48F, 1.0F, 0.56F};
            default -> new float[]{0.92F, 0.92F, 1.0F};
        };
    }

    private static double scaledSide(BoundaryMarkEntity mark) {
        double remaining = Math.max(0.25D, mark.getRemainingRatio());
        return Math.max(1.0D, mark.getSide() * remaining);
    }

    private record PreviewBoundary(UUID key, AABB bounds, float red, float green, float blue) {
        private float alpha(Vec3 camera) {
            double distance = bounds.getCenter().distanceToSqr(camera);
            double fade = Mth.clamp(1.0D - distance / MAX_RENDER_DISTANCE_SQR, 0.0D, 1.0D);
            return (float) (0.08D + fade * 0.18D);
        }
    }

    private static final class PreviewBoundaryBuilder {
        private final UUID key;
        private AABB bounds;
        private float red = 0.92F;
        private float green = 0.92F;
        private float blue = 1.0F;

        private PreviewBoundaryBuilder(UUID key) {
            this.key = key;
        }

        private void include(BoundaryMarkEntity mark) {
            if (mark == null || mark.getBoundaryId() == null) {
                return;
            }
            double half = scaledSide(mark) / 2.0D;
            AABB markBounds = new AABB(
                    mark.getX() - half,
                    mark.getY() - half,
                    mark.getZ() - half,
                    mark.getX() + half,
                    mark.getY() + half,
                    mark.getZ() + half
            );
            bounds = bounds == null ? markBounds : bounds.minmax(markBounds);
            float[] color = colorFor(mark.getBoundaryType());
            red = color[0];
            green = color[1];
            blue = color[2];
        }

        private PreviewBoundary build() {
            return bounds == null ? null : new PreviewBoundary(key, bounds, red, green, blue);
        }
    }
}
