package com.example.typemoonaddon.client;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.client.model.WingedWormModel;
import com.example.typemoonaddon.client.renderer.WormRenderer;
import com.example.typemoonaddon.registry.AddonEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client registrations kept outside ClientModEvents so the packaged jar retains worm rendering. */
@EventBusSubscriber(modid = TypeMoonAddon.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WormRendererEvents {
    private WormRendererEvents() {
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WingedWormModel.LAYER_LOCATION, WingedWormModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(AddonEntities.WORM.get(), WormRenderer::new);
    }
}
