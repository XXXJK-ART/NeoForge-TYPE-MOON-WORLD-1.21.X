package com.example.typemoonaddon.client;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.client.renderer.WormItemRenderer;
import com.example.typemoonaddon.registry.AddonItems;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Registers the shared three-dimensional renderer for all worm items. */
@EventBusSubscriber(modid = TypeMoonAddon.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WormItemRendererEvents {
    private WormItemRendererEvents() {
    }

    @SubscribeEvent
    public static void registerItemRenderers(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private WormItemRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new WormItemRenderer();
                }
                return renderer;
            }
        }, new Item[]{
                AddonItems.WORM.get(),
                AddonItems.SILVERFISH_WORM.get(),
                AddonItems.ENDERMITE_WORM.get(),
                AddonItems.WINGED_WORM.get(),
                AddonItems.FIREPROOF_WORM.get(),
                AddonItems.DETECTION_WORM.get(),
                AddonItems.ENGRAVED_WORM.get()
        });
    }
}
