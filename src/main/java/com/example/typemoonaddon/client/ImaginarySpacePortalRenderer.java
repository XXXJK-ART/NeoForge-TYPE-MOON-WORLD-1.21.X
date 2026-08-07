package com.example.typemoonaddon.client;

import com.example.typemoonaddon.TypeMoonAddon;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.Registries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Draws the independent starfield skybox without loading client code on a server. */
@EventBusSubscriber(modid = TypeMoonAddon.MOD_ID, value = Dist.CLIENT)
public final class ImaginarySpacePortalRenderer {
    private static final ResourceKey<Level> IMAGINARY_SPACE = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(TypeMoonAddon.MOD_ID, "imaginary_space"));

    private ImaginarySpacePortalRenderer() {
    }

    @SubscribeEvent
    public static void renderPortal(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !IMAGINARY_SPACE.equals(minecraft.level.dimension())) {
            return;
        }

        // Keep the sky geometry in front of the far plane. The imaginary-space client
        // cache is intentionally small, so a fixed 64-block sphere would be clipped
        // completely when the effective render distance is only two chunks.
        float renderDistance = minecraft.gameRenderer.getRenderDistance();
        float halfSize = Math.max(8.0F, renderDistance * 0.78F);
        StarrySkyboxRenderer.render(minecraft, event, halfSize);
    }

}
