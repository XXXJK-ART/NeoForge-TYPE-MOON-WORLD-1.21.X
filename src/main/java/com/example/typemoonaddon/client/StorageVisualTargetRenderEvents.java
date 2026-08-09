package com.example.typemoonaddon.client;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.entity.StorageVisualEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

/** Keeps the locked creature visually inside the shrinking storage prism. */
@EventBusSubscriber(modid = TypeMoonAddon.MOD_ID, value = Dist.CLIENT)
public final class StorageVisualTargetRenderEvents {
    private StorageVisualTargetRenderEvents() {
    }

    @SubscribeEvent
    public static void scaleStoredTarget(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity target = event.getEntity();
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null || target.level() != level || !target.isAlive()) {
            return;
        }

        AABB searchBox = target.getBoundingBox().inflate(4.0D);
        for (StorageVisualEntity visual : level.getEntitiesOfClass(
                StorageVisualEntity.class,
                searchBox,
                candidate -> candidate.isCollapsing()
                        && candidate.targetId().map(target.getUUID()::equals).orElse(false))) {
            float scale = visual.collapseScale(event.getPartialTick());
            if (scale <= 0.02F) {
                // The server transfers the entity on the same tick it removes the
                // visual. Hide the final sub-pixel frame instead of showing a full-size
                // target after the prism has visually collapsed.
                event.setCanceled(true);
            } else {
                // The prism is centered at the target's body center. Translate the
                // model before scaling so the creature collapses around that same point.
                event.getPoseStack().translate(
                        0.0D,
                        target.getBbHeight() * 0.5D * (1.0D - scale),
                        0.0D);
                event.getPoseStack().scale(scale, scale, scale);
            }
            return;
        }
    }
}
