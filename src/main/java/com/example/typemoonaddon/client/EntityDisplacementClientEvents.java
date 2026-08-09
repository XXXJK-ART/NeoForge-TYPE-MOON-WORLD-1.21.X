package com.example.typemoonaddon.client;

import com.example.typemoonaddon.TypeMoonAddon;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = TypeMoonAddon.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class EntityDisplacementClientEvents {
    private EntityDisplacementClientEvents() {
    }

    @SubscribeEvent
    public static void renderTarget(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || EntityDisplacementClientState.entityId() < 0) return;
        Entity target = minecraft.level.getEntity(EntityDisplacementClientState.entityId());
        if (target == null || target.isRemoved()) {
            EntityDisplacementClientState.clear();
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        var camera = event.getCamera().getPosition();
        double x = target.getX() - camera.x;
        double y = target.getY() - camera.y;
        double z = target.getZ() - camera.z;
        AABB box = new AABB(x - EntityDisplacementClientState.width() * 0.5D, y,
                z - EntityDisplacementClientState.depth() * 0.5D,
                x + EntityDisplacementClientState.width() * 0.5D, y + EntityDisplacementClientState.height(),
                z + EntityDisplacementClientState.depth() * 0.5D);
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(poseStack, lines, box, 1.0F, 1.0F, 1.0F, 0.95F);
    }
}
